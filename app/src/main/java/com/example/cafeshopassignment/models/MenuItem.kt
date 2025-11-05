package com.example.cafeshopassignment.models

data class MenuItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val available: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)