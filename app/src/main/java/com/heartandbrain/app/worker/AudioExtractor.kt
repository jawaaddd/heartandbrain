package com.heartandbrain.app.worker

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val TAG = "AudioExtractor"
private const val TIMEOUT_US = 10_000L
private const val TARGET_MIME = "audio/mp4a-latm"
private const val TARGET_BITRATE = 32_000
private const val WHISPER_MAX_BYTES = 25L * 1024 * 1024

/**
 * Extracts and transcodes the audio track from a video content URI into a
 * speech-optimised M4A file (AAC-LC, 32kbps, 16kHz, mono).
 *
 * At 32kbps a 90-minute recording produces ~21MB, comfortably under Whisper's 25MB limit.
 * The returned File is in cacheDir and must be deleted by the caller when no longer needed.
 */
suspend fun extractAudio(context: Context, contentUriString: String): File =
    withContext(Dispatchers.IO) {
        val outputFile = File.createTempFile("audio_", ".m4a", context.cacheDir)
        var muxerStarted = false
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var success = false

        try {
            // Open the video URI
            try {
                extractor.setDataSource(context, Uri.parse(contentUriString), null)
            } catch (e: Exception) {
                throw IOException("Cannot open video URI: ${e.javaClass.simpleName} — ${e.message}", e)
            }

            Log.d(TAG, "Opened URI, track count: ${extractor.trackCount}")

            // Find audio track
            val audioTrackIdx = (0 until extractor.trackCount).firstOrNull { i ->
                val mime = try { extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) } catch (_: Exception) { null }
                Log.d(TAG, "Track $i MIME: $mime")
                mime?.startsWith("audio/") == true
            } ?: throw IOException("No audio track found in video (${extractor.trackCount} tracks total)")

            extractor.selectTrack(audioTrackIdx)
            val sourceFormat = extractor.getTrackFormat(audioTrackIdx)
            val sourceMime = sourceFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IOException("Audio track has no MIME type")

            // Match encoder to source sample rate and channels — MediaCodec does not resample.
            // File size is controlled by bitrate alone, so 32kbps keeps output tiny regardless.
            val sampleRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            Log.d(TAG, "Audio track: $sourceMime, ${sampleRate}Hz, ${channels}ch")

            // Create decoder
            decoder = try {
                MediaCodec.createDecoderByType(sourceMime).also {
                    it.configure(sourceFormat, null, null, 0)
                    it.start()
                }
            } catch (e: Exception) {
                throw IOException("Cannot create decoder for $sourceMime: ${e.javaClass.simpleName} — ${e.message}", e)
            }

            // Create encoder — same sample rate / channels as source, just lower bitrate
            val encoderFormat = MediaFormat.createAudioFormat(TARGET_MIME, sampleRate, channels).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }
            encoder = try {
                MediaCodec.createEncoderByType(TARGET_MIME).also {
                    it.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    it.start()
                }
            } catch (e: Exception) {
                throw IOException("Cannot create AAC encoder: ${e.javaClass.simpleName} — ${e.message}", e)
            }

            // Non-null aliases — both are guaranteed assigned at this point
            val dec = decoder!!
            val enc = encoder!!

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrack = -1

            val decoderInfo = MediaCodec.BufferInfo()
            val encoderInfo = MediaCodec.BufferInfo()
            var extractionDone = false
            var decoderDone = false
            var encodingDone = false

            Log.d(TAG, "Starting transcode loop")

            while (!encodingDone) {
                // Feed extractor → decoder
                if (!extractionDone) {
                    val idx = dec.dequeueInputBuffer(TIMEOUT_US)
                    if (idx >= 0) {
                        val buf = dec.getInputBuffer(idx)
                        if (buf == null) {
                            dec.queueInputBuffer(idx, 0, 0, 0, 0)
                        } else {
                            val size = extractor.readSampleData(buf, 0)
                            if (size < 0) {
                                dec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                extractionDone = true
                            } else {
                                dec.queueInputBuffer(idx, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // Drain decoder → feed encoder
                if (!decoderDone) {
                    val idx = dec.dequeueOutputBuffer(decoderInfo, TIMEOUT_US)
                    if (idx >= 0) {
                        val isEos = decoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        val decoded = dec.getOutputBuffer(idx)
                        if (decoded != null) {
                            val encIdx = enc.dequeueInputBuffer(TIMEOUT_US)
                            if (encIdx >= 0) {
                                val encBuf = enc.getInputBuffer(encIdx)
                                if (encBuf != null) {
                                    encBuf.clear()
                                    if (decoderInfo.size > 0) {
                                        decoded.position(decoderInfo.offset)
                                        val copySize = minOf(decoderInfo.size, encBuf.remaining())
                                        decoded.limit(decoderInfo.offset + copySize)
                                        encBuf.put(decoded)
                                    }
                                    enc.queueInputBuffer(
                                        encIdx, 0, decoderInfo.size,
                                        decoderInfo.presentationTimeUs,
                                        if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0,
                                    )
                                } else {
                                    enc.queueInputBuffer(encIdx, 0, 0, 0, 0)
                                }
                            }
                        }
                        dec.releaseOutputBuffer(idx, false)
                        if (isEos) decoderDone = true
                    }
                }

                // Drain encoder → muxer
                val idx = enc.dequeueOutputBuffer(encoderInfo, TIMEOUT_US)
                when {
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        muxerTrack = muxer.addTrack(enc.outputFormat)
                        muxer.start()
                        muxerStarted = true
                        Log.d(TAG, "Muxer started")
                    }
                    idx >= 0 -> {
                        val isConfig = encoderInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                        val isEos = encoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        val encOut = enc.getOutputBuffer(idx)
                        if (!isConfig && encoderInfo.size > 0 && muxerStarted && encOut != null) {
                            muxer.writeSampleData(muxerTrack, encOut, encoderInfo)
                        }
                        enc.releaseOutputBuffer(idx, false)
                        if (isEos) encodingDone = true
                    }
                }
            }

            Log.d(TAG, "Transcode complete, output size: ${outputFile.length()} bytes")

            if (outputFile.length() > WHISPER_MAX_BYTES) {
                throw IOException(
                    "Audio is ${outputFile.length() / 1_000_000}MB after transcoding — " +
                    "exceeds Whisper's 25MB limit. Recording is too long."
                )
            }

            success = true
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
            throw if (e is IOException) e else IOException("Audio extraction failed: ${e.javaClass.simpleName} — ${e.message}", e)
        } finally {
            if (!success) outputFile.delete()
            if (muxerStarted) try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            try { decoder?.stop() } catch (_: Exception) {}
            try { decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            extractor.release()
        }
    }
