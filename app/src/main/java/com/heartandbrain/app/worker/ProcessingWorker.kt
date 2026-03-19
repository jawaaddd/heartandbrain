package com.heartandbrain.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.heartandbrain.app.App.Companion.NOTIFICATION_CHANNEL_ID
import com.heartandbrain.app.MainActivity
import com.heartandbrain.app.api.ApiClient
import com.heartandbrain.app.api.ClaudeMessage
import com.heartandbrain.app.api.ClaudeRequest
import com.heartandbrain.app.api.ClaudeSegment
import com.heartandbrain.app.data.AppDatabase
import com.heartandbrain.app.data.Category
import com.heartandbrain.app.data.Clip
import com.heartandbrain.app.data.ClipType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

private const val CLAUDE_MODEL = "claude-sonnet-4-20250514"
private const val CLAUDE_MAX_TOKENS = 2048
private const val PROGRESS_NOTIFICATION_ID = 1
private const val DONE_NOTIFICATION_ID = 2

private val SYSTEM_PROMPT = """
You are an AI that analyzes personal vlog transcripts and identifies meaningful moments.
You will receive a timestamped transcript and must return a JSON array of segments.
Each segment represents a distinct key idea worth revisiting.

Rules:
- Identify 3 to 10 segments. Never return fewer than 1.
- Prefer quality over quantity. Only include genuinely meaningful moments.
- Each segment must map cleanly onto a contiguous span of the transcript.
- Do not fabricate timestamps. Use only values present in the transcript.
- Return ONLY the JSON array. No preamble. No explanation. No markdown code fences.

Each object in the array must have exactly these fields:
{
  "start_time": <float, seconds>,
  "end_time": <float, seconds>,
  "title": <string, 3-7 words, the key idea>,
  "type": <"clip" | "quote">,
  "quote_text": <string if type is "quote", otherwise null>,
  "category": <"GOAL" | "COMMITMENT" | "EMOTIONAL" | "REMINDER" | "REFLECTION">
}

Use "quote" type only when the person says a single, punchy, self-contained sentence worth surfacing verbatim.
The quote_text must be taken word-for-word from the transcript. Maximum 15 words. No run-on sentences.
Use "clip" for everything else.
""".trimIndent()

class ProcessingWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val vlogId = inputData.getLong(KEY_VLOG_ID, -1L)
        if (vlogId == -1L) return fail("Invalid vlog ID")

        val db = AppDatabase.getInstance(applicationContext)
        val vlog = db.vlogDao().getById(vlogId) ?: return fail("Vlog not found")

        // Run as a foreground service so Samsung's battery manager can't freeze us mid-extraction
        setForeground(buildForegroundInfo("Starting…"))

        var audioFile: File? = null
        return try {
            step(STEP_EXTRACTING)
            audioFile = extractAudio(applicationContext, vlog.filePath)

            step(STEP_TRANSCRIBING)
            val audioPart = MultipartBody.Part.createFormData(
                "file", audioFile.name,
                audioFile.asRequestBody("audio/m4a".toMediaType()),
            )
            val whisperResponse = try {
                ApiClient.whisper.transcribe(
                    audio = audioPart,
                    model = "whisper-1".toRequestBody("text/plain".toMediaType()),
                    responseFormat = "verbose_json".toRequestBody("text/plain".toMediaType()),
                )
            } catch (e: HttpException) {
                return if (e.code() == 429 || e.code() in 500..599)
                    retryOrFail("Whisper API error ${e.code()} — rate limited, will retry")
                else
                    fail("Whisper API error ${e.code()}: ${e.message()}")
            }
            if (whisperResponse.segments.isEmpty()) return fail("Transcript was empty")

            val transcript = whisperResponse.segments.joinToString("\n") { seg ->
                "[%.2f - %.2f] %s".format(seg.start, seg.end, seg.text.trim())
            }

            step(STEP_ANALYZING)
            val claudeResponse = try {
                ApiClient.claude.segment(
                    ClaudeRequest(
                        model = CLAUDE_MODEL,
                        max_tokens = CLAUDE_MAX_TOKENS,
                        system = SYSTEM_PROMPT,
                        messages = listOf(ClaudeMessage(role = "user", content = transcript)),
                    )
                )
            } catch (e: HttpException) {
                return if (e.code() == 429 || e.code() in 500..599)
                    retryOrFail("Claude API error ${e.code()} — rate limited, will retry")
                else
                    fail("Claude API error ${e.code()}: ${e.message()}")
            }
            if (claudeResponse.content.isEmpty() || claudeResponse.stop_reason != "end_turn") {
                return retryOrFail("Claude returned an unexpected response")
            }

            val segmentType = object : TypeToken<List<ClaudeSegment>>() {}.type
            val claudeSegments: List<ClaudeSegment> = try {
                Gson().fromJson(claudeResponse.content[0].text, segmentType)
            } catch (e: JsonSyntaxException) {
                return retryOrFail("Claude returned malformed JSON")
            }

            val clips = claudeSegments.map { seg ->
                Clip(
                    vlogId = vlogId,
                    filePath = vlog.filePath,
                    startTime = seg.start_time,
                    endTime = seg.end_time,
                    title = seg.title,
                    type = if (seg.type == "quote") ClipType.QUOTE else ClipType.CLIP,
                    quoteText = seg.quote_text,
                    category = Category.valueOf(seg.category.uppercase().trim()),
                )
            }
            db.clipDao().insertAll(clips)

            notify("Your moments are ready", "${clips.size} clips added to your board")
            Result.success()
        } catch (e: IOException) {
            retryOrFail(e.message ?: "Network or audio error")
        } catch (e: Exception) {
            fail(e.message ?: "Unexpected error")
        } finally {
            audioFile?.delete()
        }
    }

    private suspend fun step(label: String) {
        setProgress(workDataOf(KEY_STEP to label))
        setForeground(buildForegroundInfo(label))
    }

    private fun buildForegroundInfo(label: String): ForegroundInfo {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Processing vlog")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setContentIntent(pendingIntent)
            .build()
        return ForegroundInfo(
            PROGRESS_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun fail(reason: String): Result {
        notify("Processing failed", reason)
        return Result.failure(workDataOf(KEY_ERROR to reason))
    }

    private fun retryOrFail(reason: String): Result {
        return if (runAttemptCount < 4) {
            Result.retry()
        } else {
            fail(reason)
        }
    }

    private fun notify(title: String, text: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(DONE_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }

    companion object {
        const val TAG = "processing"
        const val KEY_VLOG_ID = "vlog_id"
        const val KEY_STEP = "step"
        const val KEY_ERROR = "error"

        const val STEP_EXTRACTING = "Extracting audio…"
        const val STEP_TRANSCRIBING = "Transcribing…"
        const val STEP_ANALYZING = "Analyzing moments…"

        fun enqueue(context: Context, vlogId: Long) {
            val request = OneTimeWorkRequestBuilder<ProcessingWorker>()
                .setInputData(workDataOf(KEY_VLOG_ID to vlogId))
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
