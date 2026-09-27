package com.example.cafeshopassignment

import com.example.cafeshopassignment.models.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManageMenuTest {
    private val menuList = mutableListOf<MenuItem>()

    @Before
    fun setup() {
        menuList.add(MenuItem(id = "1", name = "Latte", price = 3.50, category = "Drinks"))
    }

    @Test
    fun addMenuItem_isSuccessful() {
        val newItem = MenuItem(id = "2", name = "Mocha", price = 3.75, category = "Drinks")
        menuList.add(newItem)
        assertTrue(menuList.any { it.name == "Mocha" })
    }

    @Test
    fun editMenuItem_updatesCorrectly() {
        val index = menuList.indexOfFirst { it.id == "1" }
        if (index != -1) {
            val updatedItem = menuList[index].copy(price = 4.00)
            menuList[index] = updatedItem
        }

        assertEquals(4.00, menuList.find { it.id == "1" }?.price ?: 0.0, 0.0)
    }

    @Test
    fun deleteMenuItem_removesCorrectly() {
        val initialSize = menuList.size
        menuList.removeIf { it.id == "1" }
        assertEquals(initialSize - 1, menuList.size)
    }
}
