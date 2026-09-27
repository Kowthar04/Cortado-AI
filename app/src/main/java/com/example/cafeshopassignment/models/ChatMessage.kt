package com.example.cafeshopassignment.models

/** One bubble in the AI assistant conversation. */
data class ChatMessage(
    val id: Long,
    val role: Role,
    val content: String,
    /** Set for inline error bubbles; these are never sent back to the backend as history. */
    val error: ChatError? = null,
) {
    val isError: Boolean
        get() = error != null

    enum class Role(
        val apiName: String,
    ) {
        USER("user"),
        ASSISTANT("assistant"),
    }
}

/** Why a chat request failed, mapped from transport/HTTP errors so the UI can explain it. */
sealed interface ChatError {
    /** No connectivity, DNS failure, timeout, or the backend is not running. */
    data object Network : ChatError

    /** Not signed in, or the backend rejected the Firebase ID token (401/403). */
    data object Unauthorized : ChatError

    /** The backend is throttling requests (429). */
    data object RateLimited : ChatError

    /** The backend or the upstream Claude API failed (5xx). */
    data class Server(
        val code: Int,
    ) : ChatError

    /** The backend answered 2xx but without a usable reply. */
    data object EmptyReply : ChatError

    data class Unknown(
        val message: String?,
    ) : ChatError
}
