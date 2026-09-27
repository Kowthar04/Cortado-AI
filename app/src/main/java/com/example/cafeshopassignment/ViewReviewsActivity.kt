package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.ReviewAdapter
import com.example.cafeshopassignment.models.Review
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.toast
import com.example.cafeshopassignment.ui.reviews.ViewReviewsEvent
import com.example.cafeshopassignment.ui.reviews.ViewReviewsViewModel

class ViewReviewsActivity : AppCompatActivity() {
    private val viewModel: ViewReviewsViewModel by viewModels { ViewReviewsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_reviews)

        findViewById<ImageButton>(R.id.backButtonFeedback).setOnClickListener { finish() }

        val reviewAdapter = ReviewAdapter(::showReplyDialog)
        findViewById<RecyclerView>(R.id.reviewsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@ViewReviewsActivity)
            adapter = reviewAdapter
        }

        // Registered once; reloading only refreshes the data behind the filter.
        findViewById<EditText>(R.id.reviewSearchInput).doAfterTextChanged {
            viewModel.setQuery(it?.toString().orEmpty())
        }

        collectWhileStarted(viewModel.uiState) { state ->
            reviewAdapter.submitList(state.reviews)
        }
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                ViewReviewsEvent.EmptyReply -> toast(R.string.reviews_reply_empty)
                ViewReviewsEvent.ReplySent -> toast(R.string.reviews_reply_sent)
                is ViewReviewsEvent.ReplyFailed -> toast(R.string.reviews_reply_failed, event.detail.orEmpty())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.load()
    }

    private fun showReplyDialog(review: Review) {
        val input = EditText(this).apply { setHint(R.string.reviews_reply_hint) }
        AlertDialog
            .Builder(this)
            .setTitle(R.string.reviews_reply_title)
            .setView(input)
            .setPositiveButton(R.string.action_send) { _, _ -> viewModel.reply(review, input.text.toString()) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
