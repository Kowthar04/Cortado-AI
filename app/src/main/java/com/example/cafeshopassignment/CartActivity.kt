package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.CartAdapter

class CartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        title = "Your Cart"

        val recycler = findViewById<RecyclerView>(R.id.cartRecyclerView)
        val totalText = findViewById<TextView>(R.id.cartTotal)

        fun updateTotal() {
            totalText.text = "Total: £${"%.2f".format(CartManager.getTotal())}"
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = CartAdapter(CartManager.getCart(), ::updateTotal)

        updateTotal()
    }
}
