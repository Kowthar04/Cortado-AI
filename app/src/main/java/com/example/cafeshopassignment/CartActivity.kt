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
        val cartContentLayout = findViewById<View>(R.id.cartContentLayout)
        val orderConfirmedLayout = findViewById<View>(R.id.orderConfirmedLayout)
        val backToMenuButton = findViewById<Button>(R.id.backToMenuButton)

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

            val orderData = hashMapOf(
                "userId" to currentUser.uid,
                "items" to cartItems.map {
                    mapOf(
                        "name" to it.name,
                        "quantity" to it.quantity,
                        "price" to it.price
                    )
                },
                "total" to CartManager.getTotal(),
                "status" to "Preparing",
                "timestamp" to Date()
            )

            db.collection("orders").add(orderData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                    CartManager.clear()

                    // Hide cart layout and show confirmation
                    cartContentLayout.visibility = View.GONE
                    orderConfirmedLayout.visibility = View.VISIBLE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to place order: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        backToMenuButton.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
