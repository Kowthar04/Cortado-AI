package com.example.cafeshopassignment.models

import com.google.firebase.Timestamp

data class Review(
    val reviewId: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Timestamp? = null,
)
