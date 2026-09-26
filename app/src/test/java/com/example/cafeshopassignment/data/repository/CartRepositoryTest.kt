package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.models.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartRepositoryTest {
    private val latte = MenuItem(id = "latte", name = "Latte", price = 3.50, category = "Drinks", imageUrl = "latte.png")
    private val croissant = MenuItem(id = "croissant", name = "Croissant", price = 2.25, category = "Pastries & Sweets")

    private val cart = CartRepository()

    @Test
    fun `adding a new item creates a line with quantity 1`() {
        cart.add(latte)

        val line = cart.items.value.single()
        assertEquals("latte", line.id)
        assertEquals("Latte", line.name)
        assertEquals(1, line.quantity)
        assertEquals("latte.png", line.imageUrl)
    }

    @Test
    fun `adding the same item again increments its quantity instead of duplicating it`() {
        cart.add(latte)
        cart.add(latte)
        cart.add(croissant)

        assertEquals(2, cart.items.value.size)
        assertEquals(2, cart.quantityOf("latte"))
        assertEquals(3, cart.itemCount)
    }

    @Test
    fun `increase bumps only the matching line`() {
        cart.add(latte)
        cart.add(croissant)

        cart.increase("croissant")

        assertEquals(1, cart.quantityOf("latte"))
        assertEquals(2, cart.quantityOf("croissant"))
    }

    @Test
    fun `decrease lowers quantity and removes the line at zero`() {
        cart.add(latte)
        cart.add(latte)

        cart.decrease("latte")
        assertEquals(1, cart.items.value[0].quantity)

        cart.decrease("latte")
        assertTrue(cart.items.value.isEmpty())
    }

    @Test
    fun `decrease of an unknown id is a no-op`() {
        cart.add(latte)

        cart.decrease("missing")

        assertEquals(1, cart.items.value[0].quantity)
    }

    @Test
    fun `subtotal sums price times quantity`() {
        cart.add(latte)
        cart.add(latte)
        cart.add(croissant)

        assertEquals(3.50 * 2 + 2.25, cart.subtotal, 0.0001)
    }

    @Test
    fun `clear empties the cart`() {
        cart.add(latte)
        cart.add(croissant)

        cart.clear()

        assertTrue(cart.items.value.isEmpty())
        assertEquals(0.0, cart.subtotal, 0.0)
    }

    @Test
    fun `items without an id are keyed by name`() {
        val noId = MenuItem(id = null, name = "Special", price = 4.0)

        cart.add(noId)
        cart.add(noId)

        assertEquals("Special", cart.items.value[0].id)
        assertEquals(2, cart.items.value[0].quantity)
    }

    @Test
    fun `each update publishes a new immutable list`() {
        cart.add(latte)
        val before = cart.items.value

        cart.increase("latte")

        assertEquals(1, before.single().quantity)
        assertEquals(2, cart.items.value[0].quantity)
    }
}

private fun CartRepository.quantityOf(id: String): Int = items.value.first { it.id == id }.quantity
