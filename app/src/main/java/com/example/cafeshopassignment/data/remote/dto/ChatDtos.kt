package com.example.cafeshopassignment.data.remote.dto

/** One prior turn sent as context: role is "user" or "assistant". */
data class ChatTurnDto(
    val role: String,
    val content: String,
)

/** Body of `POST /api/chat`. */
data class ChatRequest(
    val message: String,
    val conversationHistory: List<ChatTurnDto>? = null,
    val userId: String? = null,
)

/** Response of `POST /api/chat`. Nullable because Gson ignores Kotlin nullability. */
data class ChatResponse(
    val reply: String?,
)
