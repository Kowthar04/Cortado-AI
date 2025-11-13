package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.Review
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewAdapter(
    private val reviewList: MutableList<Review>,
    private val onReplyClick: (Review) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.reviewCustomerName)
        val ratingBar: RatingBar = view.findViewById(R.id.reviewRating)
        val comment: TextView = view.findViewById(R.id.reviewComment)
        val date: TextView = view.findViewById(R.id.reviewDate)
        val replyButton: Button = view.findViewById(R.id.replyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review_admin, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviewList[position]

        holder.name.text = "From: ${review.customerName}"
        holder.ratingBar.rating = review.rating.toFloat()
        holder.comment.text = review.comment

        review.createdAt.let {
            val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(it.toDate())
            holder.date.text = formatted
        }

        holder.replyButton.setOnClickListener {
            onReplyClick(review)
        }
    }

    override fun getItemCount(): Int = reviewList.size
}
