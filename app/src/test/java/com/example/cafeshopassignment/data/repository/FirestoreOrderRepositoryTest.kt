package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.models.CartItem
import com.example.cafeshopassignment.models.Order
import com.example.cafeshopassignment.models.OrderStatus
import com.example.cafeshopassignment.testutil.successfulTask
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreOrderRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val orders = mockk<CollectionReference>()
    private val query = mockk<Query>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val listener = slot<EventListener<QuerySnapshot>>()

    private val repository = FirestoreOrderRepository(firestore)

    @Before
    fun setUp() {
        every { firestore.collection("orders") } returns orders
        every { orders.whereEqualTo("userId", "u1") } returns query
        every { query.orderBy("createdAt", Query.Direction.DESCENDING) } returns query
        every { query.addSnapshotListener(capture(listener)) } returns registration
    }

    private fun snapshotOf(vararg docs: Pair<String, Map<String, Any>>): QuerySnapshot {
        val documents =
            docs.map { (id, data) ->
                mockk<DocumentSnapshot> {
                    every { this@mockk.id } returns id
                    every { this@mockk.data } returns data
                }
            }
        return mockk { every { this@mockk.documents } returns documents }
    }

    @Test
    fun `observeOrdersForUser emits every snapshot as mapped orders`() =
        runTest(UnconfinedTestDispatcher()) {
            val emissions = mutableListOf<List<Order>>()
            val job = launch { repository.observeOrdersForUser("u1").toList(emissions) }

            listener.captured.onEvent(snapshotOf("o1" to mapOf("status" to "Pending", "userId" to "u1")), null)
            listener.captured.onEvent(snapshotOf("o1" to mapOf("status" to "Preparing", "userId" to "u1")), null)

            assertEquals(listOf(OrderStatus.PENDING, OrderStatus.PREPARING), emissions.map { it.single().orderStatus })
            job.cancel()
        }

    @Test
    fun `cancelling the collector removes the snapshot listener`() =
        runTest(UnconfinedTestDispatcher()) {
            val job = launch { repository.observeOrdersForUser("u1").collect { } }
            verify(exactly = 0) { registration.remove() }

            job.cancel()

            verify(exactly = 1) { registration.remove() }
        }

    @Test
    fun `a listener error terminates the flow with that error and removes the listener`() =
        runTest(UnconfinedTestDispatcher()) {
            val error = FirebaseFirestoreException("PERMISSION_DENIED", FirebaseFirestoreException.Code.PERMISSION_DENIED)
            every { query.addSnapshotListener(capture(listener)) } answers {
                listener.captured.onEvent(null, error)
                registration
            }

            try {
                repository.observeOrdersForUser("u1").first()
                fail("expected the flow to fail")
            } catch (e: FirebaseFirestoreException) {
                assertEquals("PERMISSION_DENIED", e.message)
            }
            verify { registration.remove() }
        }

    @Test
    fun `placeOrder writes the order and payment in one batch and returns the order id`() =
        runTest {
            val orderRef = mockk<DocumentReference> { every { id } returns "order-123" }
            val paymentRef = mockk<DocumentReference>()
            val payments = mockk<CollectionReference> { every { document() } returns paymentRef }
            val batch = mockk<WriteBatch>()
            val orderData = slot<Any>()
            val paymentData = slot<Any>()
            every { orders.document() } returns orderRef
            every { firestore.collection("payments") } returns payments
            every { firestore.batch() } returns batch
            every { batch.set(orderRef, capture(orderData)) } returns batch
            every { batch.set(paymentRef, capture(paymentData)) } returns batch
            every { batch.commit() } returns successfulTask<Void?>(null)

            val id =
                repository.placeOrder(
                    NewOrder(
                        userId = "u1",
                        customerName = "Sam",
                        items = listOf(CartItem(id = "latte", name = "Latte", price = 3.5, quantity = 2)),
                        subtotal = 7.0,
                        serviceFee = 0.5,
                        discount = 0.0,
                        total = 7.5,
                        paymentMethod = "Google Pay",
                        promoApplied = false,
                    ),
                )

            assertEquals("order-123", id)
            val order = orderData.captured as Map<*, *>
            assertEquals("Pending", order["status"])
            assertEquals(7.5, order["totalPrice"])
            assertEquals(listOf(mapOf("menuItemId" to "latte", "name" to "Latte", "quantity" to 2, "price" to 3.5)), order["items"])
            assertEquals("order-123", (paymentData.captured as Map<*, *>)["orderId"])
            verify(exactly = 1) { batch.commit() }
        }

    @Test
    fun `updateStatus updates the order and notifies the customer atomically`() =
        runTest {
            val orderRef = mockk<DocumentReference>()
            val notificationRef = mockk<DocumentReference>()
            val notifications = mockk<CollectionReference> { every { document() } returns notificationRef }
            val batch = mockk<WriteBatch>()
            val notification = slot<Any>()
            every { orders.document("o1") } returns orderRef
            every { firestore.collection("notifications") } returns notifications
            every { firestore.batch() } returns batch
            every { batch.update(orderRef, "status", "Ready for Collection") } returns batch
            every { batch.set(notificationRef, capture(notification)) } returns batch
            every { batch.commit() } returns successfulTask<Void?>(null)

            repository.updateStatus(Order(id = "o1", userId = "u1"), OrderStatus.READY_FOR_COLLECTION)

            val data = notification.captured as Map<*, *>
            assertEquals("u1", data["recipientId"])
            assertTrue((data["message"] as String).contains("Ready for Collection"))
            verify(exactly = 1) { batch.commit() }
        }
}
