package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.models.CartItem
import com.example.cafeshopassignment.models.MenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory shopping cart shared by the menu, cart and payment screens.
 *
 * Exposed as an immutable [StateFlow] so every screen renders the same list and updates are
 * atomic. [clear] is called by [AuthRepository] on every sign-in/sign-out so a cart never
 * survives into another user's session.
 */
class CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    val subtotal: Double
        get() = _items.value.sumOf { it.totalPrice }

    val itemCount: Int
        get() = _items.value.sumOf { it.quantity }

    fun add(item: MenuItem) {
        val key = cartKey(item)
        _items.update { current ->
            if (current.any { it.id == key }) {
                current.map { if (it.id == key) it.copy(quantity = it.quantity + 1) else it }
            } else {
                current +
                    CartItem(
                        id = key,
                        name = item.name,
                        price = item.price,
                        quantity = 1,
                        imageUrl = item.imageUrl,
                    )
            }
        }
    }

    fun increase(id: String) {
        _items.update { current ->
            current.map { if (it.id == id) it.copy(quantity = it.quantity + 1) else it }
        }
    }

    /** Decrements the quantity, removing the line when it reaches zero. */
    fun decrease(id: String) {
        _items.update { current ->
            current.mapNotNull { line ->
                when {
                    line.id != id -> line
                    line.quantity > 1 -> line.copy(quantity = line.quantity - 1)
                    else -> null
                }
            }
        }
    }

    fun clear() {
        _items.value = emptyList()
    }

    private fun cartKey(item: MenuItem): String = item.id?.takeIf { it.isNotBlank() } ?: item.name
}
