package com.example.cafeshopassignment.models


data class MenuItem(
    val id: String? = null,
    val name: String = "",
    val category: String = "",
    val price: Double? = 0.00,
    val availability: Boolean = true

)