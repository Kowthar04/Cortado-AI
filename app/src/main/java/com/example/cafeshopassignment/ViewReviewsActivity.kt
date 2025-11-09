package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.ReviewAdapter
import com.example.cafeshopassignment.models.Review
import com.google.firebase.firestore.FirebaseFirestore

class ViewReviewsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ReviewAdapter
    private val reviewList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_reviews)

        db = FirebaseFirestore.getInstance()

        val recyclerView = findViewById<RecyclerView>(R.id.reviewsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ReviewAdapter(reviewList)
        recyclerView.adapter = adapter

        loadReviews()
    }

    private fun loadReviews() {
        db.collection("reviews")
            .get()
            .addOnSuccessListener { result ->
                reviewList.clear()
                for (doc in result) {
                    val review = doc.toObject(Review::class.java).copy(reviewId = doc.id)
                    reviewList.add(review)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()

            }
                }

            }


