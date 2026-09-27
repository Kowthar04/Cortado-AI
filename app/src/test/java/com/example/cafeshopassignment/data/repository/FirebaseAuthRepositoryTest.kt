package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.testutil.failedTask
import com.example.cafeshopassignment.testutil.successfulTask
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirebaseAuthRepositoryTest {
    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>()
    private val cart = CartRepository()
    private val repository = FirebaseAuthRepository(auth, firestore, cart)

    private fun userWithUid(uid: String): AuthResult {
        val user = mockk<FirebaseUser> { every { this@mockk.uid } returns uid }
        return mockk { every { this@mockk.user } returns user }
    }

    @Test
    fun `signOut signs out of Firebase and clears the previous user's cart`() {
        cart.add(MenuItem(id = "latte", name = "Latte", price = 3.5))

        repository.signOut()

        verify { auth.signOut() }
        assertTrue(cart.items.value.isEmpty())
    }

    @Test
    fun `signIn returns the uid and starts the session with an empty cart`() =
        runTest {
            cart.add(MenuItem(id = "latte", name = "Latte", price = 3.5))
            every { auth.signInWithEmailAndPassword("a@b.com", "secret") } returns successfulTask(userWithUid("uid-1"))

            val uid = repository.signIn("a@b.com", "secret")

            assertEquals("uid-1", uid)
            assertTrue(cart.items.value.isEmpty())
        }

    @Test
    fun `signIn propagates auth failures`() =
        runTest {
            every { auth.signInWithEmailAndPassword(any(), any()) } returns failedTask(IllegalArgumentException("bad password"))

            try {
                repository.signIn("a@b.com", "wrong")
                fail("expected an exception")
            } catch (e: IllegalArgumentException) {
                assertEquals("bad password", e.message)
            }
        }

    @Test
    fun `register creates a customer profile document for the new uid`() =
        runTest {
            every { auth.createUserWithEmailAndPassword("a@b.com", "secret") } returns successfulTask(userWithUid("uid-2"))
            val users = mockk<CollectionReference>()
            val userDoc = mockk<DocumentReference>()
            val profile = slot<Any>()
            every { firestore.collection("users") } returns users
            every { users.document("uid-2") } returns userDoc
            every { userDoc.set(capture(profile)) } returns successfulTask<Void?>(null)

            val uid = repository.register("Ada", "Lovelace", "a@b.com", "secret")

            assertEquals("uid-2", uid)
            assertEquals(
                mapOf("firstname" to "Ada", "surname" to "Lovelace", "email" to "a@b.com", "role" to "customer"),
                profile.captured,
            )
        }
}
