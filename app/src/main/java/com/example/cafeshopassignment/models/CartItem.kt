package com.example.cafeshopassignment.models

data class CartItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    var quantity: Int = 1,
    val imageUrl: String = "",
) {
    val totalPrice: Double
        get() = price * quantity
}
