package com.example.cafeshopassignment.models


data class MenuItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val active: Boolean = true

)