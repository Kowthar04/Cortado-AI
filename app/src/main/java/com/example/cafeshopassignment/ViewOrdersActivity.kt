package com.example.cafeshopassignment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.AdminOrderAdapter
import com.example.cafeshopassignment.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ViewOrdersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AdminOrderAdapter

    private val orderList = mutableListOf<Order>()        // filtered orders
    private val fullOrderList = mutableListOf<Order>()    // all orders

    private val auth = FirebaseAuth.getInstance()

    // Real-time listener; attached in onStart, removed in onStop so it never leaks.
    private var ordersRegistration: ListenerRegistration? = null
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_orders)

        findViewById<ImageButton>(R.id.backButtonOrders).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        db = FirebaseFirestore.getInstance()

        val recyclerView = findViewById<RecyclerView>(R.id.ordersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminOrderAdapter(orderList) { order ->
            if (isAdmin) showStatusDialog(order)
        }

        recyclerView.adapter = adapter

        // Register filter listeners exactly once.
        setupFilters()
    }

    override fun onStart() {
        super.onStart()
        loadUserRoleAndOrders()
    }

    override fun onStop() {
        ordersRegistration?.remove()
        ordersRegistration = null
        super.onStop()
    }

    private fun loadUserRoleAndOrders() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "customer"
                isAdmin = role == "admin"

                val query = if (isAdmin) {
                    db.collection("orders")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                } else {
                    db.collection("orders")
                        .whereEqualTo("userId", uid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                }
                listenToOrders(query)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load your profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenToOrders(query: Query) {
        ordersRegistration?.remove()
        ordersRegistration = query.addSnapshotListener { result, error ->
            if (error != null) {
                Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (result == null) return@addSnapshotListener

            fullOrderList.clear()
            for (doc in result) {
                val order = doc.toObject(Order::class.java).copy(
                    id = doc.id,
                    customerName = doc.getString("customerName") ?: "Unknown"
                )
                fullOrderList.add(order)
            }
            filterOrders()
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
                        val notif = hashMapOf(
                            "recipientId" to order.userId,
                            "title" to "Order Status Update",
                            "message" to "Your order is now $selected ☕",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "isRead" to false
                        )

                        db.collection("notifications").add(notif)
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Customer notification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Status update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
    }

    private fun setupFilters() {
        val searchInput = findViewById<EditText>(R.id.orderSearchInput)
        val statusFilter = findViewById<Spinner>(R.id.orderStatusFilter)

        val statuses = listOf("All", "Pending", "Preparing", "Ready for Collection", "Completed")
        statusFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                filterOrders()
            }
        })

        statusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, pos: Int, id: Long
            ) {
                filterOrders()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun filterOrders() {
        val query = findViewById<EditText>(R.id.orderSearchInput)
            .text.toString().lowercase()

        val selectedStatus = findViewById<Spinner>(R.id.orderStatusFilter)
            .selectedItem?.toString() ?: "All"

        orderList.clear()

        orderList.addAll(
            fullOrderList.filter { order ->
                val matchesSearch =
                    order.customerName.lowercase().contains(query) ||
                            order.userId.lowercase().contains(query)

                val matchesStatus =
                    selectedStatus == "All" || order.status == selectedStatus

                matchesSearch && matchesStatus
            }
        )

        adapter.notifyDataSetChanged()
    }
}
