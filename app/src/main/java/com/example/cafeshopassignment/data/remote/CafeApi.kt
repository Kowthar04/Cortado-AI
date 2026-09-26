package com.example.cafeshopassignment.data.remote

import com.example.cafeshopassignment.data.remote.dto.ChatRequest
import com.example.cafeshopassignment.data.remote.dto.ChatResponse
import com.example.cafeshopassignment.data.remote.dto.MenuResponse
import com.example.cafeshopassignment.data.remote.dto.OrderResponse
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

    /** Current menu as the backend sees it (the app's own screens read Firestore directly). */
    @GET("api/menu")
    suspend fun getMenu(): MenuResponse

    /** A single order, including its status. */
    @GET("api/orders/{id}")
    suspend fun getOrder(
        @Path("id") orderId: String,
    ): OrderResponse
}
