package com.example.cafeshopassignment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.ReviewAdapter
import com.example.cafeshopassignment.models.Review
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ViewReviewsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ReviewAdapter

    private val fullReviewList = mutableListOf<Review>()
    private val reviewList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_reviews)

        findViewById<ImageButton>(R.id.backButtonFeedback).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        db = FirebaseFirestore.getInstance()

        val recycler = findViewById<RecyclerView>(R.id.reviewsRecyclerView)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter =
            ReviewAdapter(reviewList) { review ->
                showReplyDialog(review)
            }

        recycler.adapter = adapter

        // Register the search listener once; reloads only refresh the data.
        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        loadReviews()
    }

    private fun loadReviews() {
        db
            .collection("reviews")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                fullReviewList.clear()
                reviewList.clear()

                for (doc in result) {
                    val review =
                        doc
                            .toObject(Review::class.java)
                            .copy(reviewId = doc.id)

                    fullReviewList.add(review)
                }

                reviewList.addAll(fullReviewList)
                adapter.notifyDataSetChanged()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReplyDialog(review: Review) {
        val input = EditText(this)
        input.hint = "Write your reply…"

        AlertDialog
            .Builder(this)
            .setTitle("Reply to Customer")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val reply = input.text.toString().trim()

                if (reply.isNotEmpty()) {
                    sendReply(review.customerId, reply)
                } else {
                    Toast.makeText(this, "Reply cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendReply(
        userId: String,
        message: String,
    ) {
        val notif =
            hashMapOf(
                "recipientId" to userId,
                "title" to "Response to your review",
                "message" to message,
                "createdAt" to FieldValue.serverTimestamp(),
                "isRead" to false,
            )

        db
            .collection("notifications")
            .add(notif)
            .addOnSuccessListener {
                Toast.makeText(this, "Reply sent!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to send reply", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearch() {
        val search = findViewById<EditText>(R.id.reviewSearchInput)

        search.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    val query = s.toString().lowercase()

                    reviewList.clear()
                    reviewList.addAll(
                        fullReviewList.filter { review ->
                            review.customerName.lowercase().contains(query) ||
                                review.comment.lowercase().contains(query)
                        },
                    )

                    adapter.notifyDataSetChanged()
                }
            },
        )
    }
}
