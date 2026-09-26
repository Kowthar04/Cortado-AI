package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.data.firestore.FirestoreCollections
import com.example.cafeshopassignment.data.firestore.FirestoreMappers
import com.example.cafeshopassignment.data.firestore.fields
import com.example.cafeshopassignment.data.firestore.snapshotFlow
import com.example.cafeshopassignment.models.Notification
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

interface NotificationRepository {
    /** Live inbox for [recipientId], newest first. */
    fun observeNotifications(recipientId: String): Flow<List<Notification>>

    suspend fun sendNotification(
        recipientId: String,
        title: String,
        message: String,
    )

    /** Sends the same notification to every recipient using batched writes; returns the count sent. */
    suspend fun sendNotificationToAll(
        recipientIds: List<String>,
        title: String,
        message: String,
    ): Int
}

class FirestoreNotificationRepository(
    private val firestore: FirebaseFirestore,
) : NotificationRepository {
    private val notifications get() = firestore.collection(FirestoreCollections.NOTIFICATIONS)

    override fun observeNotifications(recipientId: String): Flow<List<Notification>> =
        notifications
            .whereEqualTo("recipientId", recipientId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshotFlow { snapshot ->
                snapshot.documents.map { FirestoreMappers.notification(it.id, it.fields()) }
            }

    override suspend fun sendNotification(
        recipientId: String,
        title: String,
        message: String,
    ) {
        notifications.add(notificationData(recipientId, title, message)).await()
    }

    override suspend fun sendNotificationToAll(
        recipientIds: List<String>,
        title: String,
        message: String,
    ): Int {
        // Firestore batches are capped at 500 writes.
        recipientIds.chunked(MAX_BATCH_WRITES).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { uid -> batch.set(notifications.document(), notificationData(uid, title, message)) }
            batch.commit().await()
        }
        return recipientIds.size
    }

    private fun notificationData(
        recipientId: String,
        title: String,
        message: String,
    ) = hashMapOf(
        "recipientId" to recipientId,
        "title" to title,
        "message" to message,
        "isRead" to false,
        "createdAt" to FieldValue.serverTimestamp(),
    )

    private companion object {
        const val MAX_BATCH_WRITES = 450
    }
}
