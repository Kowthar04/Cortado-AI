package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.data.firestore.FirestoreCollections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val currentUserId: String?
    val currentUserEmail: String?
    val currentUserDisplayName: String?

    /** Signs in and returns the user's uid. Throws on invalid credentials or network failure. */
    suspend fun signIn(
        email: String,
        password: String,
    ): String

    /** Creates the auth account plus its `users/{uid}` customer profile and returns the uid. */
    suspend fun register(
        firstname: String,
        surname: String,
        email: String,
        password: String,
    ): String

    /** Signs out and clears all per-session state (the cart). */
    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val cartRepository: CartRepository,
) : AuthRepository {
    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override val currentUserDisplayName: String?
        get() = auth.currentUser?.displayName

    override suspend fun signIn(
        email: String,
        password: String,
    ): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        // A new session always starts with an empty cart.
        cartRepository.clear()
        return result.user?.uid ?: throw IllegalStateException("Sign-in returned no user")
    }

    override suspend fun register(
        firstname: String,
        surname: String,
        email: String,
        password: String,
    ): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Registration returned no user")
        val profile =
            mapOf(
                "firstname" to firstname,
                "surname" to surname,
                "email" to email,
                "role" to "customer",
            )
        firestore
            .collection(FirestoreCollections.USERS)
            .document(uid)
            .set(profile)
            .await()
        return uid
    }

    override fun signOut() {
        auth.signOut()
        cartRepository.clear()
    }
}
