package com.example.cafeshopassignment.models

import com.google.firebase.Timestamp
data class Review (
    val reviewId: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
)