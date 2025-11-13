package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.Order

class AdminOrderAdapter(
    private val orders: MutableList<Order>,
    private val onUpdateStatus: (Order) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val customerName: TextView = view.findViewById(R.id.customerName)
        val userIdText: TextView = view.findViewById(R.id.userIdText)
        val orderItems: TextView = view.findViewById(R.id.orderItems)
        val orderStatus: TextView = view.findViewById(R.id.orderStatus)
        val orderTotal: TextView = view.findViewById(R.id.orderTotal)
        val updateButton: Button = view.findViewById(R.id.updateStatusButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_admin, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]


        val safeName = if (order.customerName.isNotBlank()) order.customerName else "Unknown"
        holder.customerName.text = "Customer: $safeName"


        holder.userIdText.text = "User ID: ${order.userId}"


        val itemsFormatted = order.items.joinToString(", ") { map ->
            val name = map["name"] as? String ?: "Unknown"
            val qty = (map["quantity"] as? Long)?.toInt() ?: 1
            "$name ×$qty"
        }
        holder.orderItems.text = "Items: $itemsFormatted"


        holder.orderStatus.text = "Status: ${order.status}"


        holder.orderTotal.text = "Total: £${"%.2f".format(order.totalPrice)}"


        holder.updateButton.setOnClickListener { onUpdateStatus(order) }
    }

    override fun getItemCount(): Int = orders.size
}
