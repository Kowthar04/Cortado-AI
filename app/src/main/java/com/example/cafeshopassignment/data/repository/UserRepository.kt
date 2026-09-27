package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.data.firestore.FirestoreCollections
import com.example.cafeshopassignment.data.firestore.FirestoreMappers
import com.example.cafeshopassignment.data.firestore.fields
import com.example.cafeshopassignment.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface UserRepository {
    /** The `users/{uid}` profile, or null if no profile document exists. */
    suspend fun getUser(uid: String): User?

    suspend fun countUsers(): Int

    suspend fun getAllUserIds(): List<String>
}

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore,
) : UserRepository {
    private val users get() = firestore.collection(FirestoreCollections.USERS)

    override suspend fun getUser(uid: String): User? {
        val doc = users.document(uid).get().await()
        return if (doc.exists()) FirestoreMappers.user(doc.id, doc.fields()) else null
    }

    override suspend fun countUsers(): Int = users.get().await().size()

    override suspend fun getAllUserIds(): List<String> =
        users
            .get()
            .await()
            .documents
            .map { it.id }
}
