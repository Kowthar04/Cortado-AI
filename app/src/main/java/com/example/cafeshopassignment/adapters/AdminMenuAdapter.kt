package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.ui.common.formatPrice

class AdminMenuAdapter(
    private val onEdit: (MenuItem) -> Unit,
    private val onDelete: (MenuItem) -> Unit,
) : ListAdapter<MenuItem, AdminMenuAdapter.MenuViewHolder>(DIFF) {
    class MenuViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.itemName)
        val price: TextView = itemView.findViewById(R.id.itemPrice)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MenuViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_menu_admin, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MenuViewHolder,
        position: Int,
    ) {
        val item = getItem(position)
        holder.name.text = item.name
        holder.price.text = formatPrice(item.price)

        holder.editButton.setOnClickListener { onEdit(item) }
        holder.deleteButton.setOnClickListener { onDelete(item) }
    }

    private companion object {
        val DIFF =
            object : DiffUtil.ItemCallback<MenuItem>() {
                override fun areItemsTheSame(
                    oldItem: MenuItem,
                    newItem: MenuItem,
                ) = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: MenuItem,
                    newItem: MenuItem,
                ) = oldItem == newItem
            }
    }
}
