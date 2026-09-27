package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.data.remote.IdTokenProvider
import com.example.cafeshopassignment.data.remote.RetrofitClient
import com.example.cafeshopassignment.models.ChatError
import com.example.cafeshopassignment.models.ChatMessage
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises the real Retrofit + OkHttp stack (including the auth interceptor) against a local
 * MockWebServer, so JSON shapes and HTTP error mapping are tested without any real network.
 */
class ChatRepositoryTest {
    private val server = MockWebServer()
    private var signedIn = true
    private val tokens = IdTokenProvider { if (signedIn) "id-token" else null }

    private lateinit var repository: RemoteChatRepository

    @Before
    fun setUp() {
        server.start()
        val api = RetrofitClient.create(server.url("/").toString(), tokens)
        repository = RemoteChatRepository(api) { "user-42" }
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(
        body: String,
        code: Int = 200,
    ) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun `successful reply is returned and the request matches the backend contract`() =
        runTest {
            server.enqueue(json("""{"reply":"  Try our hazelnut latte!  "}"""))
            val history =
                listOf(
                    ChatMessage(1, ChatMessage.Role.USER, "Hi"),
                    ChatMessage(2, ChatMessage.Role.ASSISTANT, "Hello! How can I help?"),
                    ChatMessage(3, ChatMessage.Role.ASSISTANT, "", error = ChatError.Network),
                )

            val result = repository.send("Recommend a drink", history)

            assertEquals(ChatResult.Success("Try our hazelnut latte!"), result)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/chat", request.path)
            assertEquals("Bearer id-token", request.getHeader("Authorization"))
            val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("Recommend a drink", body["message"].asString)
            assertEquals("user-42", body["userId"].asString)
            val turns = body["conversationHistory"].asJsonArray
            // Error bubbles are local-only and never sent as context.
            assertEquals(2, turns.size())
            assertEquals("user", turns[0].asJsonObject["role"].asString)
            assertEquals("assistant", turns[1].asJsonObject["role"].asString)
            assertEquals("Hello! How can I help?", turns[1].asJsonObject["content"].asString)
        }

    @Test
    fun `history is capped to the most recent turns`() {
        val long = (1..30L).map { ChatMessage(it, ChatMessage.Role.USER, "m$it") }

        val turns = RemoteChatRepository.toHistory(long)

        assertEquals(RemoteChatRepository.MAX_HISTORY_TURNS, turns.size)
        assertEquals("m30", turns.last().content)
    }

    @Test
    fun `401 maps to Unauthorized`() =
        runTest {
            // The interceptor retries once after a forced refresh; both attempts are rejected.
            server.enqueue(json("""{"error":"invalid token"}""", 401))
            server.enqueue(json("""{"error":"invalid token"}""", 401))

            assertEquals(ChatResult.Failure(ChatError.Unauthorized), repository.send("hi", emptyList()))
        }

    @Test
    fun `not being signed in maps to Unauthorized without hitting the network`() =
        runTest {
            signedIn = false

            assertEquals(ChatResult.Failure(ChatError.Unauthorized), repository.send("hi", emptyList()))
            assertEquals(0, server.requestCount)
        }

    @Test
    fun `5xx maps to a server error with the status code`() =
        runTest {
            server.enqueue(json("""{"error":"upstream timeout"}""", 503))

            assertEquals(ChatResult.Failure(ChatError.Server(503)), repository.send("hi", emptyList()))
        }

    @Test
    fun `429 maps to RateLimited`() =
        runTest {
            server.enqueue(json("""{"error":"slow down"}""", 429))

            assertEquals(ChatResult.Failure(ChatError.RateLimited), repository.send("hi", emptyList()))
        }

    @Test
    fun `dropped connection maps to a network error`() =
        runTest {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

            assertEquals(ChatResult.Failure(ChatError.Network), repository.send("hi", emptyList()))
        }

    @Test
    fun `missing or blank reply is reported instead of showing an empty bubble`() =
        runTest {
            server.enqueue(json("""{"reply":"   "}"""))
            server.enqueue(json("""{}"""))

            assertEquals(ChatResult.Failure(ChatError.EmptyReply), repository.send("hi", emptyList()))
            assertEquals(ChatResult.Failure(ChatError.EmptyReply), repository.send("hi", emptyList()))
        }

    @Test
    fun `malformed JSON maps to an unknown error rather than crashing`() =
        runTest {
            server.enqueue(json("not json"))

            assertTrue((repository.send("hi", emptyList()) as ChatResult.Failure).error is ChatError.Unknown)
        }
}
