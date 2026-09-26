package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.Notification
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(DIFF) {
    class NotificationViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val message: TextView = view.findViewById(R.id.notificationMessage)
        val date: TextView = view.findViewById(R.id.notificationDate)
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): NotificationViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_notification_admin, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int,
    ) {
        val notification = getItem(position)
        holder.title.text = notification.title
        holder.message.text = notification.message
        // Recycled views must be reset: a pending server timestamp is null until the write lands.
        holder.date.text = notification.createdAt?.let { dateFormat.format(it.toDate()) } ?: ""
    }

    private companion object {
        val DIFF =
            object : DiffUtil.ItemCallback<Notification>() {
                override fun areItemsTheSame(
                    oldItem: Notification,
                    newItem: Notification,
                ) = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: Notification,
                    newItem: Notification,
                ) = oldItem == newItem
            }
    }
}
