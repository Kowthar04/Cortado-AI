package com.example.cafeshopassignment.data.firestore

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Real-time Firestore query as a cold [Flow].
 *
 * The snapshot listener is registered when collection starts and its
 * `ListenerRegistration` is removed in `awaitClose` as soon as the collector is cancelled
 * (screen stopped, ViewModel cleared), so listeners never outlive the UI that needs them.
 */
fun <T> Query.snapshotFlow(mapper: (QuerySnapshot) -> T): Flow<T> =
    callbackFlow {
        val registration =
            addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) trySend(mapper(snapshot))
            }
        awaitClose { registration.remove() }
    }

/** Real-time single-document listener; same lifecycle guarantees as [Query.snapshotFlow]. */
fun <T> DocumentReference.snapshotFlow(mapper: (DocumentSnapshot) -> T): Flow<T> =
    callbackFlow {
        val registration =
            addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) trySend(mapper(snapshot))
            }
        awaitClose { registration.remove() }
    }

/** Raw field map of a document, or an empty map if it does not exist. */
internal fun DocumentSnapshot.fields(): Map<String, Any?> = data ?: emptyMap()
