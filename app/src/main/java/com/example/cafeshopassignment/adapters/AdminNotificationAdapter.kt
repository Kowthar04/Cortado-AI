package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.Notification
import java.text.SimpleDateFormat
import java.util.Locale

class AdminNotificationAdapter(
    private val notifications: List<Notification>,
) : RecyclerView.Adapter<AdminNotificationAdapter.NotifViewHolder>() {
    inner class NotifViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val message: TextView = view.findViewById(R.id.notificationMessage)
        val date: TextView = view.findViewById(R.id.notificationDate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): NotifViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_notification_admin, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NotifViewHolder,
        position: Int,
    ) {
        val notif = notifications[position]
        holder.title.text = notif.title
        holder.message.text = notif.message
        notif.createdAt?.let {
            val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it.toDate())
            holder.date.text = formatted
        }
    }

    override fun getItemCount() = notifications.size
}
