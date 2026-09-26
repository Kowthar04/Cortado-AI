package com.example.cafeshopassignment.data.remote

import com.example.cafeshopassignment.data.remote.dto.ChatRequest
import com.example.cafeshopassignment.data.remote.dto.ChatResponse
import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * The CafeShop backend (Node/Express + Claude API, see `backend/`). Every endpoint requires
 * `Authorization: Bearer <Firebase ID token>`, which [AuthInterceptor] attaches.
 *
 * Paths are relative (no leading slash) so the base URL may include a path prefix.
 */
interface CafeApi {
    /** Sends a message to the AI order assistant and returns its reply. */
    @POST("api/chat")
    suspend fun chat(
        @Body request: ChatRequest,
    ): ChatResponse

    /**
     * Current menu as the backend sees it. The app itself still reads the menu from Firestore;
     * this is kept untyped so the client doesn't break if the backend's response shape evolves.
     */
    @GET("api/menu")
    suspend fun getMenu(): JsonElement

    /** Status of a single order (untyped for the same reason as [getMenu]). */
    @GET("api/orders/{id}")
    suspend fun getOrder(
        @Path("id") orderId: String,
    ): JsonElement
}
