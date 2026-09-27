package com.example.cafeshopassignment.models

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val subtotal: Double = 0.0,
    val serviceFee: Double = 0.0,
    val discount: Double = 0.0,
    val totalPrice: Double = 0.0,
    val status: String = OrderStatus.PENDING.label,
    val createdAt: Timestamp? = null,
    val paymentMethod: String = "",
) {
    val orderStatus: OrderStatus?
        get() = OrderStatus.fromLabel(status)

    /** e.g. "Latte ×1, Cappuccino ×2" */
    val itemsSummary: String
        get() =
            items.joinToString(", ") { map ->
                val name = map["name"] as? String ?: "Unknown"
                val qty = (map["quantity"] as? Number)?.toInt() ?: 1
                "$name ×$qty"
            }
}
