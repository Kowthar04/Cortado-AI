package com.example.cafeshopassignment

import com.example.cafeshopassignment.models.Order
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OrderStatusTest {
    private lateinit var baseOrder: Order

    @Before
    fun setup() {
        baseOrder = Order(
            id = "001",
            customerName = "Amina Ali",
            status = "Pending",
            totalPrice = 7.50
        )
    }


    @Test
    fun updateOrderStatus_isSuccessful() {
        val updated = baseOrder.copy(status = "Preparing")
        assertEquals("Preparing", updated.status)
        assertEquals("Pending", baseOrder.status)
    }


    @Test
    fun orderCompletion_setsCorrectStatus() {
        val completed = baseOrder.copy(status = "Completed")
        assertEquals("Completed", completed.status)
        assertEquals("Pending", baseOrder.status)
    }


    @Test
    fun invalidStatus_doesNotMatchExpectedStates() {
        val validStatuses = listOf("Pending", "Preparing", "Ready for Collection", "Completed")
        val invalid = baseOrder.copy(status = "In Progress")
        assertFalse(validStatuses.contains(invalid.status))
        assertEquals("Pending", baseOrder.status)
    }


    @Test
    fun statusTransition_flowIsCorrect() {
        val preparing = baseOrder.copy(status = "Preparing")
        assertEquals("Preparing", preparing.status)
        val ready = preparing.copy(status = "Ready for Collection")
        assertEquals("Ready for Collection", ready.status)
        val completed = ready.copy(status = "Completed")
        assertEquals("Completed", completed.status)
        assertEquals("Pending", baseOrder.status)
    }


    @Test
    fun updatingStatus_doesNotAffectTotalPrice() {
        val originalTotal = baseOrder.totalPrice
        val updated = baseOrder.copy(status = "Preparing")
        assertEquals(originalTotal, updated.totalPrice, 0.0)
        assertEquals(originalTotal, baseOrder.totalPrice, 0.0)
    }
}