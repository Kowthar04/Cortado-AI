package com.example.cafeshopassignment.models

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0,
    val status: String = "Pending",
    val timestamp: Timestamp? = null,
    val customerName: String = ""
)
