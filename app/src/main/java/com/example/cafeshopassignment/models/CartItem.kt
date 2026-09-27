package com.example.cafeshopassignment.models

/** A line in the shopping cart. Immutable: quantity changes produce a new instance. */
data class CartItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = "",
) {
    val totalPrice: Double
        get() = price * quantity
}
