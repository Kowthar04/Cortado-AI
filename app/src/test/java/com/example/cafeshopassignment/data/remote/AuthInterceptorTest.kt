package com.example.cafeshopassignment.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AuthInterceptorTest {
    private val server = MockWebServer()

    /** Hands out "token-1", "token-2", ... and records whether each call forced a refresh. */
    private class FakeTokenProvider(
        private val signedIn: Boolean = true,
    ) : IdTokenProvider {
        val forceRefreshCalls = mutableListOf<Boolean>()
        private var issued = 0

        override suspend fun getIdToken(forceRefresh: Boolean): String? {
            forceRefreshCalls += forceRefresh
            if (!signedIn) return null
            if (forceRefresh || issued == 0) issued++
            return "token-$issued"
        }
    }

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    private fun execute(tokenProvider: IdTokenProvider): Int {
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenProvider)).build()
        return client
            .newCall(Request.Builder().url(server.url("/api/chat")).build())
            .execute()
            .use { it.code }
    }

    @Test
    fun `attaches the Firebase ID token as a bearer header`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val tokens = FakeTokenProvider()

        assertEquals(200, execute(tokens))

        assertEquals("Bearer token-1", server.takeRequest().getHeader("Authorization"))
        assertEquals(listOf(false), tokens.forceRefreshCalls)
    }

    @Test
    fun `a 401 forces a token refresh and retries exactly once`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))
        val tokens = FakeTokenProvider()

        assertEquals(200, execute(tokens))

        assertEquals("Bearer token-1", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer token-2", server.takeRequest().getHeader("Authorization"))
        assertEquals(listOf(false, true), tokens.forceRefreshCalls)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a second 401 is returned to the caller rather than looping`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        assertEquals(401, execute(FakeTokenProvider()))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `no signed-in user fails with NotSignedInException before any request`() {
        try {
            execute(FakeTokenProvider(signedIn = false))
            fail("expected NotSignedInException")
        } catch (e: NotSignedInException) {
            assertEquals(0, server.requestCount)
        }
    }

    @Test
    fun `token provider failures surface as IOExceptions`() {
        val broken = IdTokenProvider { throw IllegalStateException("Firebase unavailable") }

        try {
            execute(broken)
            fail("expected IOException")
        } catch (e: IOException) {
            assertTrue(e.cause is IllegalStateException)
        }
    }

    @Test
    fun `base url normalisation adds a trailing slash`() {
        assertEquals("http://10.0.2.2:3000/", RetrofitClient.normalizeBaseUrl("http://10.0.2.2:3000"))
        assertEquals("https://api.example.com/cafe/", RetrofitClient.normalizeBaseUrl(" https://api.example.com/cafe/ "))
    }
}
