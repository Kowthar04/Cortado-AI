package com.example.cafeshopassignment

import org.junit.Assert.assertEquals
import org.junit.Test

class CartTotalTest {
    data class CartItem(
        val name: String,
        val quantity: Int,
        val price: Double,
    )

    private fun calculateTotal(cart: List<CartItem>): Double = cart.sumOf { it.quantity * it.price }

    @Test
    fun `calculates the total of cart items`() {
        val cart =
            listOf(
                CartItem("Coffee", 2, 3.0),
                CartItem("Donut", 1, 2.50),
            )

        val result = calculateTotal(cart)

        assertEquals(8.50, result, 0.001)
    }

    @Test
    fun `returns 0 when the cart is empty`() {
        val cart = emptyList<CartItem>()
        val result = calculateTotal(cart)
        assertEquals(0.0, result, 0.001)
    }
}
