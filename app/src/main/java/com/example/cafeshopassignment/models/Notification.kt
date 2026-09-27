package com.example.cafeshopassignment.models

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val recipientId: String = "",
    val createdAt: Timestamp? = null,
    val isRead: Boolean = false,
)
