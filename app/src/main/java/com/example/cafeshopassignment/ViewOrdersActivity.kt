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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "customer"
                if (role == "admin") loadAllOrders() else loadUserOrders(uid)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user role: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAllOrders() {
        db.collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                orderList.clear()
                for (doc in result) {
                    println("🔥 ORDER FOUND: ${doc.data}") // Optional debug
                    val order = doc.toObject(Order::class.java).copy(id = doc.id)
                    val safeOrder = order.copy(
                        customerName = doc.getString("customerName") ?: "Unknown"
                    )
                    orderList.add(safeOrder)
                }
                adapter.notifyDataSetChanged()

                if (orderList.isEmpty()) {
                    Toast.makeText(this, "No orders found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load orders: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserOrders(uid: String) {
        db.collection("orders")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                orderList.clear()
                for (doc in result) {
                    val order = doc.toObject(Order::class.java).copy(id = doc.id)
                    orderList.add(order.copy(customerName = doc.getString("customerName") ?: "You"))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load your orders: ${it.message}", Toast.LENGTH_SHORT).show()
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
                        val notificationData = hashMapOf(
                            "recipientId" to order.userId,
                            "title" to "Order Status Update",
                            "message" to "Your order is now $selected ☕",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "isRead" to false
                        )

                        db.collection("notifications").add(notificationData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Notification sent!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        Toast.makeText(this, "Status updated to $selected", Toast.LENGTH_SHORT).show()
                        loadAllOrders()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update order status", Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
    }
}
