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
import com.google.firebase.firestore.Query

class ViewOrdersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AdminOrderAdapter

    private val orderList = mutableListOf<Order>()        // filtered orders
    private val fullOrderList = mutableListOf<Order>()    // all orders

    private val auth = FirebaseAuth.getInstance()

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
            showStatusDialog(order)
        }

        recyclerView.adapter = adapter


        loadUserRoleAndOrders()
    }

    override fun onResume() {
        super.onResume()
        loadUserRoleAndOrders()
    }

    private fun loadUserRoleAndOrders() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "customer"

                if (role == "admin")
                    loadAllOrders()
                else
                    loadUserOrders(uid)
            }
    }

    private fun loadAllOrders() {
        db.collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                fullOrderList.clear()
                orderList.clear()

                for (doc in result) {
                    val order = doc.toObject(Order::class.java).copy(id = doc.id)

                    val safeOrder = order.copy(
                        customerName = doc.getString("customerName") ?: "Unknown"
                    )

                    fullOrderList.add(safeOrder)
                }

                orderList.addAll(fullOrderList)
                adapter.notifyDataSetChanged()

                setupFilters()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserOrders(uid: String) {
        db.collection("orders")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                fullOrderList.clear()
                orderList.clear()

                for (doc in result) {
                    val order = doc.toObject(Order::class.java).copy(id = doc.id)
                    fullOrderList.add(order)
                    orderList.add(order)
                }

                adapter.notifyDataSetChanged()
                setupFilters()
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

                        // Auto-send notification to user
                        val notif = hashMapOf(
                            "recipientId" to order.userId,
                            "title" to "Order Status Update",
                            "message" to "Your order is now $selected ☕",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "isRead" to false
                        )

                        db.collection("notifications").add(notif)

                        Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show()
                        loadAllOrders()
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
            .selectedItem.toString()

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
