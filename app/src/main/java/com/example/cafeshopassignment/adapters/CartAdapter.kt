package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.CartItem
import com.example.cafeshopassignment.ui.common.formatPrice

/** Renders cart lines; quantity changes are delegated to the ViewModel, which re-emits the list. */
class CartAdapter(
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit,
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(DIFF) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CartViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CartViewHolder,
        position: Int,
    ) {
        val item = getItem(position)

        holder.name.text = item.name
        holder.price.text = formatPrice(item.totalPrice)
        holder.quantity.text = item.quantity.toString()

        Glide
            .with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_coffee_cup)
            .error(R.drawable.ic_coffee_cup)
            .centerCrop()
            .into(holder.cartItemImage)

        holder.btnPlus.setOnClickListener { onIncrease(item) }
        holder.btnMinus.setOnClickListener { onDecrease(item) }
    }

    class CartViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.cartItemName)
        val cartItemImage: ImageView = view.findViewById(R.id.cartItemImage)
        val price: TextView = view.findViewById(R.id.cartItemPrice)
        val quantity: TextView = view.findViewById(R.id.cartQty)
        val btnPlus: ImageButton = view.findViewById(R.id.btnPlus)
        val btnMinus: ImageButton = view.findViewById(R.id.btnMinus)
    }

    private companion object {
        val DIFF =
            object : DiffUtil.ItemCallback<CartItem>() {
                override fun areItemsTheSame(
                    oldItem: CartItem,
                    newItem: CartItem,
                ) = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: CartItem,
                    newItem: CartItem,
                ) = oldItem == newItem
            }
    }
}
