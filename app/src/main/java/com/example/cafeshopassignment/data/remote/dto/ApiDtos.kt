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

/** `GET /api/menu` → `{ "menuItems": [...] }`. Fields are nullable because Gson ignores Kotlin nullability. */
data class MenuResponse(
    val menuItems: List<MenuItemDto>?,
)

data class MenuItemDto(
    val id: String?,
    val name: String?,
    val category: String?,
    val price: Double?,
    val imageUrl: String?,
    val availability: Boolean?,
)

/** `GET /api/orders/{id}` → `{ "order": { ... } }`. */
data class OrderResponse(
    val order: OrderDto?,
)

data class OrderDto(
    val id: String?,
    val userId: String?,
    val customerName: String?,
    val totalPrice: Double?,
    val status: String?,
    val paymentMethod: String?,
)
