package com.example.cafeshopassignment

import com.example.cafeshopassignment.models.CartItem
import com.example.cafeshopassignment.models.MenuItem

object CartManager {

    private val cartList = mutableListOf<CartItem>()

    fun addItem(item: MenuItem) {
        val itemId = item.id ?: ""
        val existing = cartList.find { it.id == itemId }

        if (existing != null) {
            existing.quantity++
        } else {
            cartList.add(
                CartItem(
                    id = itemId,
                    name = item.name,

                    price = item.price,
                    quantity = 1,
                    imageUrl = item.imageUrl
                )
            )
        }
    }

    fun getCart(): MutableList<CartItem> = cartList

    fun increaseQuantity(id: String) {
        cartList.find { it.id == id }?.apply { quantity++ }
    }

    fun decreaseQuantity(id: String) {
        val item = cartList.find { it.id == id }
        item?.let {
            it.quantity--
            if (it.quantity <= 0) cartList.remove(it)
        }
    }

    fun getTotal(): Double = cartList.sumOf { it.totalPrice }

    fun clear() {
        cartList.clear()
    }
}

