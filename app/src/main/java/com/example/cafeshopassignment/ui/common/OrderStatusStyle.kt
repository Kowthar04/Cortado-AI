package com.example.cafeshopassignment.ui.common

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.OrderStatus

@ColorRes
fun OrderStatus?.badgeColorRes(): Int =
    when (this) {
        OrderStatus.PREPARING -> R.color.status_preparing
        OrderStatus.READY_FOR_COLLECTION -> R.color.status_ready
        OrderStatus.COMPLETED -> R.color.status_completed
        OrderStatus.PENDING, null -> R.color.status_pending
    }

/** Styles a TextView (with `status_badge_background`) as a coloured status pill. */
fun TextView.showStatusBadge(
    status: OrderStatus?,
    label: String,
) {
    text = label
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, status.badgeColorRes()))
}
