package com.example.cafeshopassignment.models

class Order {
    val orderId: String = ""
    val customerId: String = ""
    val items: List<CartItem> = emptyList()
    val total: Double = 0.0
    val status: String = "pending"
    val createdAt: Long = System.currentTimeMillis()
    val updatedAt: Long = System.currentTimeMillis()

}