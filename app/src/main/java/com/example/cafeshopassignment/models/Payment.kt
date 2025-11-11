package com.example.cafeshopassignment.models

import com.google.firebase.Timestamp

data class Payment (
    val id: String = "",
    val orderId: String = "",
    val userId: String = "",
    val customerName: String = "",
    val amountPaid: Double = 0.0,
    val paymentMethod: String = "",
    val paymentStatus: String = "Completed",
    val promoApplied: Boolean = false,
    val discountAmount: Double = 0.0,
    val createdAt: Timestamp? = null  )
