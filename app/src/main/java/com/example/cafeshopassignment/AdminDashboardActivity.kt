package com.example.cafeshopassignment

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val welcomeText = findViewById<TextView>(R.id.adminWelcomeText)
        val logoutButton = findViewById<Button>(R.id.AdminLogoutButton)

        // 🧾 Load admin name
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val firstName = document.getString("firstName") ?: "Admin"
                        welcomeText.text = "Welcome, $firstName!"
                    } else {
                        welcomeText.text = "Welcome, Admin!"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load name: ${e.message}", Toast.LENGTH_SHORT).show()
                    welcomeText.text = "Welcome, Admin!"
                }
        } else {
            welcomeText.text = "Welcome!"
        }

        // 🧩 CARD NAVIGATION
        findViewById<androidx.cardview.widget.CardView>(R.id.cardManageMenu)
            .setOnClickListener {
                startActivity(Intent(this, ManageMenuActivity::class.java))
            }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardViewFeedback)
            .setOnClickListener {
                startActivity(Intent(this, ViewReviewsActivity::class.java))
            }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardSendNotifications)
            .setOnClickListener {
                startActivity(Intent(this, SendNotificationActivity::class.java))
            }

        findViewById<Button>(R.id.viewMoreOrdersButton)
            .setOnClickListener {
                startActivity(Intent(this, ViewOrdersActivity::class.java))
            }

        // 🚀 Load recent orders with color-coded status badges
        loadRecentOrders()

        // 🚪 Logout button
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // 🧾 Load latest 3–4 orders dynamically with color-coded badges
    private fun loadRecentOrders() {
        val container = findViewById<LinearLayout>(R.id.ordersPreviewContainer)
        container.removeAllViews()

        db.collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(4)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    val emptyText = TextView(this).apply {
                        text = "No recent orders."
                        setTextColor(Color.parseColor("#7B5A4D"))
                        textSize = 14f
                    }
                    container.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in result) {
                    val customer = doc.getString("customerName") ?: "Unknown"
                    val status = doc.getString("status") ?: "Pending"
                    val total = doc.getDouble("totalPrice") ?: 0.0

                    // Create container for each order row
                    val orderRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(8, 8, 8, 8)
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    // Customer + total text
                    val orderInfo = TextView(this).apply {
                        text = "• $customer  -  £${"%.2f".format(total)}"
                        setTextColor(Color.parseColor("#4A2C2A"))
                        textSize = 14f
                        setTypeface(null, Typeface.BOLD)
                        setPadding(0, 0, 16, 0)
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }

                    // Status badge
                    val badge = TextView(this).apply {
                        text = status
                        textSize = 12f
                        setTextColor(Color.WHITE)
                        setPadding(16, 8, 16, 8)
                        gravity = Gravity.CENTER
                        setTypeface(null, Typeface.BOLD)
                        background = resources.getDrawable(R.drawable.status_badge_background, null)
                        backgroundTintList = when (status.lowercase()) {
                            "completed" -> android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")) // Green
                            "preparing" -> android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")) // Orange
                            "ready for collection" -> android.content.res.ColorStateList.valueOf(Color.parseColor("#8B4513")) // Coffee brown
                            else -> android.content.res.ColorStateList.valueOf(Color.parseColor("#B0BEC5")) // Grey
                        }
                    }

                    orderRow.addView(orderInfo)
                    orderRow.addView(badge)
                    container.addView(orderRow)
                }
            }
            .addOnFailureListener {
                val errorText = TextView(this).apply {
                    text = "Failed to load orders."
                    setTextColor(Color.RED)
                }
                container.addView(errorText)
            }
    }
}