package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.CartAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CartActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        title = "Your Cart"

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val recycler = findViewById<RecyclerView>(R.id.cartRecyclerView)
        val totalText = findViewById<TextView>(R.id.cartTotal)
        val placeOrderButton = findViewById<Button>(R.id.placeOrderButton)

        fun updateTotal() {
            totalText.text = "Total: £${"%.2f".format(CartManager.getTotal())}"
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = CartAdapter(CartManager.getCart(), ::updateTotal)
        updateTotal()

        placeOrderButton.setOnClickListener {
            val cartItems = CartManager.getCart()
            val currentUser = auth.currentUser

            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentUser == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra("TOTAL_AMOUNT", CartManager.getTotal())
            startActivity(intent)
        }
    }
}



