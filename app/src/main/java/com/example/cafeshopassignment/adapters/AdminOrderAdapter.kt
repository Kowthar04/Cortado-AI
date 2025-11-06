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
        holder.customerName.text = "Customer: ${order.customerName}"
        holder.orderStatus.text = "Status: ${order.status ?: "Pending"}"
        holder.orderTotal.text = "Total: £${order.totalPrice ?: 0.0}"
        holder.updateButton.setOnClickListener { onUpdateStatus(order) }
    }

    override fun getItemCount(): Int = orders.size
}


