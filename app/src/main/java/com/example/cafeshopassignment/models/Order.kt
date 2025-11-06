package com.example.cafeshopassignment.models

data class Order(
    val id: String = "",
    val customerName: String = "",
    val items: List<CartItem> = emptyList(),
    val totalPrice: Double? = 0.0,
    val status: String? = "pending",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)