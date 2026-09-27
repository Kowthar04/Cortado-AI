package com.example.cafeshopassignment.data.firestore

import com.example.cafeshopassignment.models.OrderStatus
import com.example.cafeshopassignment.models.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreMappersTest {
    @Test
    fun `parsePrice accepts numbers and numeric strings, defaults to zero`() {
        assertEquals(3.5, FirestoreMappers.parsePrice(3.5), 0.0)
        assertEquals(4.0, FirestoreMappers.parsePrice(4L), 0.0)
        assertEquals(2.75, FirestoreMappers.parsePrice(" 2.75 "), 0.0)
        assertEquals(0.0, FirestoreMappers.parsePrice("free"), 0.0)
        assertEquals(0.0, FirestoreMappers.parsePrice(null), 0.0)
    }

    @Test
    fun `menu item mapping tolerates missing fields`() {
        val item = FirestoreMappers.menuItem("m1", mapOf("name" to "Latte", "price" to "3.20", "category" to "Drinks"))

        assertEquals("m1", item.id)
        assertEquals("Latte", item.name)
        assertEquals(3.2, item.price, 0.0)
        assertEquals("", item.imageUrl)
        assertTrue(item.availability)
    }

    @Test
    fun `order mapping reads items, totals and status`() {
        val order =
            FirestoreMappers.order(
                "o1",
                mapOf(
                    "userId" to "u1",
                    "customerName" to "Amina Ali",
                    "items" to listOf(mapOf("name" to "Latte", "quantity" to 2L, "price" to 3.5)),
                    "totalPrice" to 7.5,
                    "status" to "Ready for Collection",
                ),
            )

        assertEquals("o1", order.id)
        assertEquals("Amina Ali", order.customerName)
        assertEquals(7.5, order.totalPrice, 0.0)
        assertEquals(OrderStatus.READY_FOR_COLLECTION, order.orderStatus)
        assertEquals("Latte ×2", order.itemsSummary)
    }

    @Test
    fun `order mapping falls back for blank names, missing status and malformed items`() {
        val order =
            FirestoreMappers.order(
                "o2",
                mapOf("customerName" to "  ", "items" to listOf("not a map", mapOf("name" to "Tea", "note" to null))),
            )

        assertEquals("Unknown", order.customerName)
        assertEquals(OrderStatus.PENDING.label, order.status)
        assertEquals(listOf(mapOf("name" to "Tea")), order.items)
        assertNull(order.createdAt)
    }

    @Test
    fun `user mapping treats only explicit admin as admin`() {
        assertEquals(UserRole.ADMIN, FirestoreMappers.user("u", mapOf("role" to "Admin")).role)
        assertEquals(UserRole.CUSTOMER, FirestoreMappers.user("u", mapOf("role" to "customer")).role)
        assertEquals(UserRole.CUSTOMER, FirestoreMappers.user("u", emptyMap()).role)
    }

    @Test
    fun `user mapping supports the legacy firstName key`() {
        val user = FirestoreMappers.user("u", mapOf("firstName" to "Kowthar", "surname" to "A"))

        assertEquals("Kowthar", user.firstname)
        assertEquals("Kowthar A", user.fullName)
    }

    @Test
    fun `notification mapping reads the isRead flag`() {
        val notification = FirestoreMappers.notification("n1", mapOf("title" to "Hi", "isRead" to true, "recipientId" to "u1"))

        assertEquals("Hi", notification.title)
        assertTrue(notification.isRead)
        assertFalse(FirestoreMappers.notification("n2", emptyMap()).isRead)
    }

    @Test
    fun `review mapping converts numeric ratings`() {
        val review = FirestoreMappers.review("r1", mapOf("rating" to 4L, "comment" to "Great", "customerName" to "Sam"))

        assertEquals(4, review.rating)
        assertEquals("r1", review.reviewId)
    }
}
