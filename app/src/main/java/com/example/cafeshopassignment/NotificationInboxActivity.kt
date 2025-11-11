package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.AdminNotificationAdapter
import com.example.cafeshopassignment.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationInboxActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AdminNotificationAdapter
    private val notificationList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_inbox)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)

        }

        db = FirebaseFirestore.getInstance()

        val recyclerView = findViewById<RecyclerView>(R.id.inboxRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminNotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        loadNotifications()
    }

    private fun loadNotifications() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        db.collection("notifications")
            .whereEqualTo("recipientId", currentUser.uid)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                notificationList.clear()
                for (doc in result) {
                    val note = doc.toObject(Notification::class.java)
                    notificationList.add(note)
                }
                adapter.notifyDataSetChanged()

            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load notifications: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}