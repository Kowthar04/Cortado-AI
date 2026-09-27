package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.data.remote.CafeApi
import com.example.cafeshopassignment.data.remote.NotSignedInException
import com.example.cafeshopassignment.data.remote.dto.ChatRequest
import com.example.cafeshopassignment.data.remote.dto.ChatTurnDto
import com.example.cafeshopassignment.models.ChatError
import com.example.cafeshopassignment.models.ChatMessage
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

sealed interface ChatResult {
    data class Success(
        val reply: String,
    ) : ChatResult

    data class Failure(
        val error: ChatError,
    ) : ChatResult
}

interface ChatRepository {
    /**
     * Sends [message] to the AI assistant with the prior conversation as context.
     * Never throws (except for coroutine cancellation): failures come back as [ChatResult.Failure].
     */
    suspend fun send(
        message: String,
        history: List<ChatMessage>,
    ): ChatResult
}

class RemoteChatRepository(
    private val api: CafeApi,
    private val currentUserId: () -> String?,
) : ChatRepository {
    override suspend fun send(
        message: String,
        history: List<ChatMessage>,
    ): ChatResult =
        try {
            val request =
                ChatRequest(
                    message = message,
                    conversationHistory = toHistory(history),
                    userId = currentUserId(),
                )
            val reply = api.chat(request).reply?.trim()
            if (reply.isNullOrEmpty()) ChatResult.Failure(ChatError.EmptyReply) else ChatResult.Success(reply)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ChatResult.Failure(toChatError(e))
        }

    companion object {
        /** Cap on prior turns sent as context, to bound request size and token usage. */
        const val MAX_HISTORY_TURNS = 20

        /** Prior user/assistant turns, oldest first, excluding local error bubbles. */
        fun toHistory(history: List<ChatMessage>): List<ChatTurnDto> =
            history
                .filterNot { it.isError }
                .takeLast(MAX_HISTORY_TURNS)
                .map { ChatTurnDto(role = it.role.apiName, content = it.content) }

        fun toChatError(e: Throwable): ChatError =
            when (e) {
                is NotSignedInException -> ChatError.Unauthorized
                is HttpException ->
                    when (val code = e.code()) {
                        401, 403 -> ChatError.Unauthorized
                        429 -> ChatError.RateLimited
                        in 500..599 -> ChatError.Server(code)
                        else -> ChatError.Unknown("HTTP $code")
                    }
                // Gson reports unparseable bodies as MalformedJsonException, an IOException subclass:
                // that's a backend contract problem, not a connectivity one.
                is MalformedJsonException, is JsonParseException -> ChatError.Unknown("Unexpected response from the assistant")
                is IOException -> ChatError.Network
                else -> ChatError.Unknown(e.message)
            }
    }
}
