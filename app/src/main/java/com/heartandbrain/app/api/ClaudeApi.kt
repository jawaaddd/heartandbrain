package com.heartandbrain.app.api

import retrofit2.http.Body
import retrofit2.http.POST

data class ClaudeRequest(
    val model: String,
    val max_tokens: Int,
    val system: String,
    val messages: List<ClaudeMessage>,
)

data class ClaudeMessage(
    val role: String,
    val content: String,
)

data class ClaudeResponse(
    val id: String,
    val content: List<ClaudeContentBlock>,
    val stop_reason: String,
)

data class ClaudeContentBlock(
    val type: String,
    val text: String,
)

data class ClaudeSegment(
    val start_time: Float,
    val end_time: Float,
    val title: String,
    val type: String,
    val quote_text: String?,
    val category: String,
)

interface ClaudeApi {
    @POST("v1/messages")
    suspend fun segment(@Body request: ClaudeRequest): ClaudeResponse
}
