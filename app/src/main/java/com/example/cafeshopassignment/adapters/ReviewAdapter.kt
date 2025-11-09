package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.Review
import java.util.Locale
import java.text.SimpleDateFormat


class ReviewAdapter(private val reviewList: MutableList<Review>) :
        RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ratingBar: RatingBar = view.findViewById(R.id.reviewRating)
        val comment: TextView = view.findViewById(R.id.reviewComment)
        val date: TextView = view.findViewById(R.id.reviewDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviewList[position]
        holder.ratingBar.rating = review.rating.toFloat()
        holder.comment.text = review.comment

        review.createdAt?.let {
            val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(it.toDate())
            holder.date.text = formatted
        }

    }

    override fun getItemCount() = reviewList.size
    }

