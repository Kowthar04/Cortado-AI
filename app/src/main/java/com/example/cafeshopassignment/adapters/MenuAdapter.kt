package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.MenuItem

class MenuAdapter(
    private var menuList: List<MenuItem>,
    private val onAddToCartClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuList[position]
        holder.itemName.text = item.name
        holder.itemPrice.text = "£${"%.2f".format(item.price)}"


        holder.addToCartButton.setOnClickListener {
            onAddToCartClick(item)
        }
    }

    override fun getItemCount() = menuList.size

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val addToCartButton: Button = itemView.findViewById(R.id.addToCartButton)
    }

    fun updateData(newList: List<MenuItem>) {
        menuList = newList
        notifyDataSetChanged()
    }
}
