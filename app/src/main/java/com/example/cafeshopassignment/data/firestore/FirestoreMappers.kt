package com.example.cafeshopassignment.data.firestore

import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.models.Notification
import com.example.cafeshopassignment.models.Order
import com.example.cafeshopassignment.models.OrderStatus
import com.example.cafeshopassignment.models.Review
import com.example.cafeshopassignment.models.User
import com.example.cafeshopassignment.models.UserRole
import com.google.firebase.Timestamp

/** Firestore collection names, in one place. */
object FirestoreCollections {
    const val USERS = "users"
    const val MENU_ITEMS = "menuItems"
    const val ORDERS = "orders"
    const val PAYMENTS = "payments"
    const val REVIEWS = "reviews"
    const val NOTIFICATIONS = "notifications"
}

/**
 * Pure mapping from raw Firestore document data (`DocumentSnapshot.getData()`) to models.
 *
 * Mapping by hand instead of `toObject()` keeps parsing tolerant of the loosely-typed data
 * already in the database (e.g. prices stored as strings, missing fields) and makes the
 * rules unit-testable without Firebase.
 */
object FirestoreMappers {
    fun parsePrice(raw: Any?): Double =
        when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.trim().toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    fun menuItem(
        id: String,
        data: Map<String, Any?>,
    ): MenuItem =
        MenuItem(
            id = id,
            name = data["name"] as? String ?: "",
            category = data["category"] as? String ?: "",
            price = parsePrice(data["price"]),
            imageUrl = data["imageUrl"] as? String ?: "",
            availability = data["availability"] as? Boolean ?: true,
        )

    fun order(
        id: String,
        data: Map<String, Any?>,
    ): Order =
        Order(
            id = id,
            userId = data["userId"] as? String ?: "",
            customerName = (data["customerName"] as? String)?.takeIf { it.isNotBlank() } ?: "Unknown",
            items = orderItems(data["items"]),
            subtotal = parsePrice(data["subtotal"]),
            serviceFee = parsePrice(data["serviceFee"]),
            discount = parsePrice(data["discount"]),
            totalPrice = parsePrice(data["totalPrice"]),
            status = data["status"] as? String ?: OrderStatus.PENDING.label,
            createdAt = data["createdAt"] as? Timestamp,
            paymentMethod = data["paymentMethod"] as? String ?: "",
        )

    fun review(
        id: String,
        data: Map<String, Any?>,
    ): Review =
        Review(
            reviewId = id,
            orderId = data["orderId"] as? String ?: "",
            customerId = data["customerId"] as? String ?: "",
            customerName = data["customerName"] as? String ?: "",
            rating = (data["rating"] as? Number)?.toInt() ?: 0,
            comment = data["comment"] as? String ?: "",
            createdAt = data["createdAt"] as? Timestamp,
        )

    fun notification(
        id: String,
        data: Map<String, Any?>,
    ): Notification =
        Notification(
            id = id,
            title = data["title"] as? String ?: "",
            message = data["message"] as? String ?: "",
            recipientId = data["recipientId"] as? String ?: "",
            createdAt = data["createdAt"] as? Timestamp,
            isRead = data["isRead"] as? Boolean ?: false,
        )

    fun user(
        uid: String,
        data: Map<String, Any?>,
    ): User =
        User(
            uid = uid,
            firstname = data["firstname"] as? String ?: data["firstName"] as? String ?: "",
            surname = data["surname"] as? String ?: "",
            email = data["email"] as? String ?: "",
            role = UserRole.fromString(data["role"] as? String),
        )

    private fun orderItems(raw: Any?): List<Map<String, Any>> =
        (raw as? List<*>).orEmpty().mapNotNull { entry ->
            (entry as? Map<*, *>)
                ?.entries
                ?.mapNotNull { (key, value) -> if (key is String && value != null) key to value else null }
                ?.toMap()
        }
}
