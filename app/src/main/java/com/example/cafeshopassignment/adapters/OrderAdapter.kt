package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.Order
import com.example.cafeshopassignment.ui.common.formatPrice
import com.example.cafeshopassignment.ui.common.showStatusBadge

/**
 * Order cards for both roles. Admin mode shows the customer's user id and an
 * "Update Status" button; customer mode hides both.
 */
class OrderAdapter(
    private val onUpdateStatus: (Order) -> Unit,
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(DIFF) {
    var showAdminActions: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        }

    class OrderViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val customerName: TextView = view.findViewById(R.id.customerName)
        val userIdText: TextView = view.findViewById(R.id.userIdText)
        val orderItems: TextView = view.findViewById(R.id.orderItems)
        val orderStatus: TextView = view.findViewById(R.id.orderStatus)
        val orderTotal: TextView = view.findViewById(R.id.orderTotal)
        val updateButton: Button = view.findViewById(R.id.updateStatusButton)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): OrderViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_order_admin, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: OrderViewHolder,
        position: Int,
    ) {
        val order = getItem(position)
        val context = holder.itemView.context

        holder.customerName.text = context.getString(R.string.order_customer, order.customerName)
        holder.orderItems.text = context.getString(R.string.order_items, order.itemsSummary)
        holder.orderTotal.text = context.getString(R.string.order_total, formatPrice(order.totalPrice))
        holder.orderStatus.showStatusBadge(order.orderStatus, order.status)

        holder.userIdText.isVisible = showAdminActions
        holder.userIdText.text = context.getString(R.string.order_user_id, order.userId)
        holder.updateButton.isVisible = showAdminActions
        holder.updateButton.setOnClickListener { onUpdateStatus(order) }
    }

    private companion object {
        val DIFF =
            object : DiffUtil.ItemCallback<Order>() {
                override fun areItemsTheSame(
                    oldItem: Order,
                    newItem: Order,
                ) = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: Order,
                    newItem: Order,
                ) = oldItem == newItem
            }
    }
}
