package com.example.cafeshopassignment.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/** Supplies the signed-in user's Firebase ID token, or null when nobody is signed in. */
fun interface IdTokenProvider {
    suspend fun getIdToken(forceRefresh: Boolean): String?
}

class FirebaseIdTokenProvider(
    private val auth: FirebaseAuth,
) : IdTokenProvider {
    // getIdToken(false) returns the cached token while it is valid and transparently
    // refreshes it when it is close to expiry, so calling it per request is cheap.
    override suspend fun getIdToken(forceRefresh: Boolean): String? =
        auth.currentUser
            ?.getIdToken(forceRefresh)
            ?.await()
            ?.token
}

/** Thrown (as an IOException, so OkHttp reports it through the normal failure path) when no user is signed in. */
class NotSignedInException : IOException("No signed-in user to authenticate the request")

/**
 * Attaches `Authorization: Bearer <Firebase ID token>` to every backend request.
 *
 * The token is fetched fresh for each request (never cached here). If the backend still answers
 * 401, the token is force-refreshed and the request retried exactly once, which covers tokens
 * revoked or expired between the client-side check and the server-side verification.
 */
class AuthInterceptor(
    private val tokenProvider: IdTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = fetchToken(forceRefresh = false) ?: throw NotSignedInException()
        val response = chain.proceed(chain.request().withBearer(token))
        if (response.code != HTTP_UNAUTHORIZED) return response

        val refreshed = fetchToken(forceRefresh = true)
        if (refreshed == null || refreshed == token) return response
        response.close()
        return chain.proceed(chain.request().withBearer(refreshed))
    }

    // OkHttp runs interceptors on its own worker threads, so blocking here is safe. Any
    // failure is rethrown as an IOException: OkHttp only reports IOExceptions to callers.
    private fun fetchToken(forceRefresh: Boolean): String? =
        try {
            runBlocking { tokenProvider.getIdToken(forceRefresh) }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Could not obtain a Firebase ID token", e)
        }

    private fun Request.withBearer(token: String): Request = newBuilder().header("Authorization", "Bearer $token").build()

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
