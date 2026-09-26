package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.ui.common.formatPrice

class MenuAdapter(
    private val onAddToCartClick: (MenuItem) -> Unit,
) : ListAdapter<MenuItem, MenuAdapter.MenuViewHolder>(DIFF) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MenuViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MenuViewHolder,
        position: Int,
    ) {
        val item = getItem(position)
        holder.itemName.text = item.name
        holder.itemPrice.text = formatPrice(item.price)

        Glide
            .with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_coffee_cup)
            .error(R.drawable.ic_coffee_cup)
            .centerCrop()
            .into(holder.itemImage)

        holder.addToCartButton.setOnClickListener { onAddToCartClick(item) }
    }

    class MenuViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val addToCartButton: Button = itemView.findViewById(R.id.addToCartButton)
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
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
