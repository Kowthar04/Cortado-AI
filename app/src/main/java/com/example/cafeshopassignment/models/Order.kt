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
    val status: String = "Pending",
    val createdAt: Timestamp? = null
)
