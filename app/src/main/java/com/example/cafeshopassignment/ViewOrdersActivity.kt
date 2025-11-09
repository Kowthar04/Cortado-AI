package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.AdminOrderAdapter
import com.example.cafeshopassignment.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ViewOrdersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AdminOrderAdapter
    private val orderList = mutableListOf<Order>()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_orders)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)

        }

        db = FirebaseFirestore.getInstance()
        val recyclerView = findViewById<RecyclerView>(R.id.ordersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminOrderAdapter(orderList) { order ->
            showStatusDialog(order)
        }
        recyclerView.adapter = adapter

        loadUserRoleAndOrders()
    }


    private fun loadUserRoleAndOrders() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { doc ->
            val role = doc.getString("role")

            if (role == "admin") {
                loadAllOrders()
            } else {
                loadUserOrders(uid)
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load user role: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun loadAllOrders() {
        db.collection("orders")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                orderList.clear()
                for (doc in result) {
                    val order = doc.toObject(Order::class.java).copy(id = doc.id)
                    orderList.add(order)
                }
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Loaded ${orderList.size} orders", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load orders: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    private fun loadUserOrders(uid: String) {
        db.collection("orders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->
                orderList.clear()
                for (doc in result) {
                    val order = doc.toObject(Order::class.java).copy(id = doc.id)
                    orderList.add(order)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to load your orders: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun showStatusDialog(order: Order) {
        val statuses = arrayOf("Pending", "Preparing", "Ready for Collection", "Completed")
        AlertDialog.Builder(this)
            .setTitle("Update Order Status")
            .setItems(statuses) { _, which ->
                val selected = statuses[which]

                db.collection("orders").document(order.id)
                    .update("status", selected)
                    .addOnSuccessListener {
                        // ✅ Notification creation moved INSIDE this block
                        val notificationData = hashMapOf(
                            "recipientId" to order.userId,  // must exist in Order model
                            "title" to "Order Status Update",
                            "message" to "Your order is now $selected ☕",
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "isRead" to false
                        )

                        db.collection("notifications").add(notificationData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Notification sent to user",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to send notification: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        Toast.makeText(this, "Status updated to $selected", Toast.LENGTH_SHORT)
                            .show()
                        loadAllOrders()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update order status", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
            .show()
    }
}