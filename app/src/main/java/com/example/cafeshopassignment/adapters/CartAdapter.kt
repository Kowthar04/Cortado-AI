package com.example.cafeshopassignment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.CartManager
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.CartItem

class CartAdapter(private var cartItems: MutableList<CartItem>, private val updateTotal: () -> Unit) :
    RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]

        holder.name.text = item.name
        holder.price.text = "£${"%.2f".format(item.totalPrice)}"
        holder.quantity.text = item.quantity.toString()

        holder.plus.setOnClickListener {
            CartManager.increaseQuantity(item.id)
            refresh()
        }

        holder.minus.setOnClickListener {
            CartManager.decreaseQuantity(item.id)
            refresh()
        }
    }

    override fun getItemCount() = cartItems.size

    private fun refresh() {
        cartItems = CartManager.getCart()
        notifyDataSetChanged()
        updateTotal()
    }

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.cartItemName)
        val price: TextView = view.findViewById(R.id.cartItemPrice)
        val quantity: TextView = view.findViewById(R.id.cartQty)
        val plus: Button = view.findViewById(R.id.btnPlus)
        val minus: Button = view.findViewById(R.id.btnMinus)
    }
}
