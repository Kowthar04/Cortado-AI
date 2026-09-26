package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.Review
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewAdapter(
    private val onReplyClick: (Review) -> Unit,
) : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(DIFF) {
    class ReviewViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.reviewCustomerName)
        val ratingBar: RatingBar = view.findViewById(R.id.reviewRating)
        val comment: TextView = view.findViewById(R.id.reviewComment)
        val date: TextView = view.findViewById(R.id.reviewDate)
        val replyButton: Button = view.findViewById(R.id.replyButton)
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ReviewViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_review_admin, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ReviewViewHolder,
        position: Int,
    ) {
        val review = getItem(position)
        val context = holder.itemView.context

        holder.name.text = context.getString(R.string.review_from, review.customerName)
        holder.ratingBar.rating = review.rating.toFloat()
        holder.comment.text = review.comment
        holder.date.text = review.createdAt?.let { dateFormat.format(it.toDate()) } ?: ""
        holder.replyButton.setOnClickListener { onReplyClick(review) }
    }

    private companion object {
        val DIFF =
            object : DiffUtil.ItemCallback<Review>() {
                override fun areItemsTheSame(
                    oldItem: Review,
                    newItem: Review,
                ) = oldItem.reviewId == newItem.reviewId

                override fun areContentsTheSame(
                    oldItem: Review,
                    newItem: Review,
                ) = oldItem == newItem
            }
    }
}
