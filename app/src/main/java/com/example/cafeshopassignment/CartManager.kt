package com.example.cafeshopassignment

import com.example.cafeshopassignment.models.CartItem
import com.example.cafeshopassignment.models.MenuItem

object CartManager {

    private val cartList = mutableListOf<CartItem>()

    fun addItem(item: MenuItem) {
        val exists = cartList.find { it.id == item.id }

        if (exists != null) {
            exists.quantity++
        } else {
            cartList.add(
                CartItem(
                    id = item.id,
                    name = item.name,
                    price = item.price,
                    quantity = 1
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
