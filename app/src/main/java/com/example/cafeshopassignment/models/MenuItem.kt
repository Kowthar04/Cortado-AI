package com.example.cafeshopassignment.models

<<<<<<< HEAD
class MenuItem(
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val description: String = ""
=======
data class MenuItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val available: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
>>>>>>> refs/remotes/origin/main
)