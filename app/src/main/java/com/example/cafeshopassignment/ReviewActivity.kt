package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.toast
import com.example.cafeshopassignment.ui.reviews.ReviewEvent
import com.example.cafeshopassignment.ui.reviews.ReviewViewModel

class ReviewActivity : AppCompatActivity() {
    private val viewModel: ReviewViewModel by viewModels { ReviewViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val orderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val commentInput = findViewById<EditText>(R.id.reviewInput)
        val submitButton = findViewById<Button>(R.id.submitReviewBtn)

        submitButton.setOnClickListener {
            viewModel.submit(orderId, ratingBar.rating.toInt(), commentInput.text.toString())
        }

        collectWhileStarted(viewModel.isSubmitting) { submitting -> submitButton.isEnabled = !submitting }
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                ReviewEvent.MissingRating -> toast(R.string.review_missing_rating)
                ReviewEvent.MissingComment -> toast(R.string.review_missing_comment)
                ReviewEvent.NotLoggedIn -> toast(R.string.error_not_logged_in)
                is ReviewEvent.Failed -> toast(R.string.review_failed, event.detail.orEmpty())
                ReviewEvent.Submitted -> {
                    toast(R.string.review_submitted)
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_ORDER_ID = "ORDER_ID"
    }
}
