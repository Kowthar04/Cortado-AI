package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.data.firestore.FirestoreCollections
import com.example.cafeshopassignment.data.firestore.FirestoreMappers
import com.example.cafeshopassignment.data.firestore.fields
import com.example.cafeshopassignment.models.Review
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

interface ReviewRepository {
    /** All reviews, newest first. */
    suspend fun getReviews(): List<Review>

    suspend fun submitReview(
        orderId: String,
        customerId: String,
        customerName: String,
        rating: Int,
        comment: String,
    )
}

class FirestoreReviewRepository(
    private val firestore: FirebaseFirestore,
) : ReviewRepository {
    private val reviews get() = firestore.collection(FirestoreCollections.REVIEWS)

    override suspend fun getReviews(): List<Review> =
        reviews
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .map { FirestoreMappers.review(it.id, it.fields()) }

    override suspend fun submitReview(
        orderId: String,
        customerId: String,
        customerName: String,
        rating: Int,
        comment: String,
    ) {
        val data =
            hashMapOf(
                "orderId" to orderId,
                "customerId" to customerId,
                "customerName" to customerName,
                "rating" to rating,
                "comment" to comment,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        reviews.add(data).await()
    }
}
