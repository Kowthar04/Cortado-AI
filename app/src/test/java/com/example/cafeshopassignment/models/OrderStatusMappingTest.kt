package com.example.cafeshopassignment.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStatusMappingTest {
    @Test
    fun `labels round-trip to the stored Firestore strings`() {
        assertEquals(listOf("Pending", "Preparing", "Ready for Collection", "Completed"), OrderStatus.labels)
        OrderStatus.entries.forEach { assertEquals(it, OrderStatus.fromLabel(it.label)) }
    }

    @Test
    fun `fromLabel is case and whitespace insensitive`() {
        assertEquals(OrderStatus.READY_FOR_COLLECTION, OrderStatus.fromLabel("  ready for collection "))
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromLabel("COMPLETED"))
    }

    @Test
    fun `unknown or missing labels map to null`() {
        assertNull(OrderStatus.fromLabel("In Progress"))
        assertNull(OrderStatus.fromLabel(null))
        assertNull(Order(status = "Shipped").orderStatus)
    }

    @Test
    fun `steps and next status follow the lifecycle`() {
        assertEquals(1, OrderStatus.PENDING.step)
        assertEquals(4, OrderStatus.COMPLETED.step)
        assertEquals(OrderStatus.PREPARING, OrderStatus.PENDING.next())
        assertEquals(OrderStatus.READY_FOR_COLLECTION, OrderStatus.PREPARING.next())
        assertNull(OrderStatus.COMPLETED.next())
        assertTrue(OrderStatus.COMPLETED.isFinal)
        assertFalse(OrderStatus.PREPARING.isFinal)
    }

    @Test
    fun `new orders default to pending`() {
        assertEquals(OrderStatus.PENDING, Order().orderStatus)
    }
}
