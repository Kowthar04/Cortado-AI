package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.data.firestore.FirestoreCollections
import com.example.cafeshopassignment.data.firestore.FirestoreMappers
import com.example.cafeshopassignment.data.firestore.fields
import com.example.cafeshopassignment.data.firestore.snapshotFlow
import com.example.cafeshopassignment.models.CartItem
import com.example.cafeshopassignment.models.Order
import com.example.cafeshopassignment.models.OrderStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Everything needed to persist a paid order. */
data class NewOrder(
    val userId: String,
    val customerName: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val serviceFee: Double,
    val discount: Double,
    val total: Double,
    val paymentMethod: String,
    val promoApplied: Boolean,
)

interface OrderRepository {
    /** Writes the order and its payment record atomically and returns the new order id. */
    suspend fun placeOrder(order: NewOrder): String

    /** Live list of a customer's orders, newest first. */
    fun observeOrdersForUser(userId: String): Flow<List<Order>>

    /** Live list of every order, newest first (admin). */
    fun observeAllOrders(): Flow<List<Order>>

    fun observeRecentOrders(limit: Long): Flow<List<Order>>

    /** Live updates for a single order; emits null if it does not exist. */
    fun observeOrder(orderId: String): Flow<Order?>

    suspend fun getOrdersBetween(
        start: Date,
        end: Date,
    ): List<Order>

    /** Updates the status and notifies the customer in the same atomic batch. */
    suspend fun updateStatus(
        order: Order,
        status: OrderStatus,
    )
}

class FirestoreOrderRepository(
    private val firestore: FirebaseFirestore,
) : OrderRepository {
    private val orders get() = firestore.collection(FirestoreCollections.ORDERS)

    override suspend fun placeOrder(order: NewOrder): String {
        val orderRef = orders.document()
        val paymentRef = firestore.collection(FirestoreCollections.PAYMENTS).document()

        val orderData =
            hashMapOf(
                "userId" to order.userId,
                "customerName" to order.customerName,
                "items" to
                    order.items.map {
                        mapOf(
                            "menuItemId" to it.id,
                            "name" to it.name,
                            "quantity" to it.quantity,
                            "price" to it.price,
                        )
                    },
                "subtotal" to order.subtotal,
                "serviceFee" to order.serviceFee,
                "discount" to order.discount,
                "totalPrice" to order.total,
                "paymentMethod" to order.paymentMethod,
                "status" to OrderStatus.PENDING.label,
                "createdAt" to FieldValue.serverTimestamp(),
                "paymentStatus" to "Completed",
            )

        val paymentData =
            hashMapOf(
                "orderId" to orderRef.id,
                "userId" to order.userId,
                "customerName" to order.customerName,
                "amountPaid" to order.total,
                "paymentMethod" to order.paymentMethod,
                "paymentStatus" to "Completed",
                "promoApplied" to order.promoApplied,
                "discountAmount" to order.discount,
                "createdAt" to FieldValue.serverTimestamp(),
            )

        firestore
            .batch()
            .set(orderRef, orderData)
            .set(paymentRef, paymentData)
            .commit()
            .await()
        return orderRef.id
    }

    override fun observeOrdersForUser(userId: String): Flow<List<Order>> =
        orders
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshotFlow(::toOrders)

    override fun observeAllOrders(): Flow<List<Order>> =
        orders
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshotFlow(::toOrders)

    override fun observeRecentOrders(limit: Long): Flow<List<Order>> =
        orders
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshotFlow(::toOrders)

    override fun observeOrder(orderId: String): Flow<Order?> =
        orders.document(orderId).snapshotFlow { doc ->
            if (doc.exists()) FirestoreMappers.order(doc.id, doc.fields()) else null
        }

    override suspend fun getOrdersBetween(
        start: Date,
        end: Date,
    ): List<Order> =
        toOrders(
            orders
                .whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThanOrEqualTo("createdAt", end)
                .get()
                .await(),
        )

    override suspend fun updateStatus(
        order: Order,
        status: OrderStatus,
    ) {
        val batch = firestore.batch()
        batch.update(orders.document(order.id), "status", status.label)
        if (order.userId.isNotBlank()) {
            val notification =
                hashMapOf(
                    "recipientId" to order.userId,
                    "title" to "Order Status Update",
                    "message" to "Your order is now ${status.label} ☕",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "isRead" to false,
                )
            batch.set(firestore.collection(FirestoreCollections.NOTIFICATIONS).document(), notification)
        }
        batch.commit().await()
    }

    private fun toOrders(snapshot: QuerySnapshot): List<Order> = snapshot.documents.map { FirestoreMappers.order(it.id, it.fields()) }
}
