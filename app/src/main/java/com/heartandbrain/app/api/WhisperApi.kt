package com.heartandbrain.app.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class WhisperResponse(
    val task: String,
    val language: String,
    val duration: Float,
    val text: String,
    val segments: List<WhisperSegment>,
)

data class WhisperSegment(
    val id: Int,
    val start: Float,
    val end: Float,
    val text: String,
)

interface WhisperApi {
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribe(
        @Part audio: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("response_format") responseFormat: RequestBody,
    ): WhisperResponse
}
