package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class ReviewActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var ratingBar: RatingBar
    private lateinit var commentInput: EditText
    private lateinit var submitReviewButton: Button
    private var orderId: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        orderId = intent.getStringExtra("ORDER_ID") ?: ""

        ratingBar = findViewById(R.id.ratingBar)
        commentInput = findViewById(R.id.reviewInput)
        submitReviewButton = findViewById(R.id.submitReviewBtn)

        submitReviewButton.setOnClickListener {
            submitReview()
        }
    }

    private fun submitReview() {
        val ratingValue = ratingBar.rating.toInt()
        val commentText = commentInput.text.toString().trim()

        if (ratingValue == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        if (commentText.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to review", Toast.LENGTH_SHORT).show()
            return
        }

        val reviewData = hashMapOf(
            "orderId" to orderId,
            "customerId" to currentUser.uid,
            "rating" to ratingValue,
            "comment" to commentText,
            "createdAt" to Timestamp.now()
        )

        db.collection("reviews")
            .add(reviewData)
            .addOnSuccessListener {
                Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to submit review: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}