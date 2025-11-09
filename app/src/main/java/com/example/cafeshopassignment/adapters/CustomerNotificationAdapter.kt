package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.Notification
import java.text.SimpleDateFormat
import java.util.*


class CustomerNotificationAdapter (
    private val notifications: List<Notification>
) : RecyclerView.Adapter<CustomerNotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val message: TextView = view.findViewById(R.id.notificationMessage)
        val date: TextView = view.findViewById(R.id.notificationDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_customer, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.title.text = notification.title
        holder.message.text = notification.message

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.date.text = notification.createdAt?.toDate()?.let { sdf.format(it) } ?: ""
    }
    override fun getItemCount() = notifications.size

}