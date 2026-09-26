package com.example.cafeshopassignment

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
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
        val backButton = findViewById<ImageButton>(R.id.backButtonDashboard)

        backButton.setOnClickListener {
            finish()
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            db
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val firstName =
                            document.getString("firstname")
                                ?: document.getString("firstName")
                                ?: "Admin"
                        welcomeText.text = "Welcome, $firstName!"
                    } else {
                        welcomeText.text = "Welcome, Admin!"
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load name: ${e.message}", Toast.LENGTH_SHORT).show()
                    welcomeText.text = "Welcome, Admin!"
                }
        } else {
            welcomeText.text = "Welcome!"
        }

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

        findViewById<androidx.cardview.widget.CardView>(R.id.cardAnalytics)
            .setOnClickListener {
                Toast.makeText(this, "Analytics screen coming soon!", Toast.LENGTH_SHORT).show()
            }

        findViewById<Button>(R.id.viewMoreOrdersButton)
            .setOnClickListener {
                startActivity(Intent(this, ViewOrdersActivity::class.java))
            }

        logoutButton.setOnClickListener {
            auth.signOut()
            CartManager.clear()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardAnalytics()
        loadRecentOrders()
    }

    private fun loadRecentOrders() {
        val container = findViewById<LinearLayout>(R.id.ordersPreviewContainer)

        db
            .collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(4)
            .get()
            .addOnSuccessListener { result ->
                // Clear when results arrive so overlapping loads can't duplicate rows.
                container.removeAllViews()
                if (result.isEmpty) {
                    val emptyText =
                        TextView(this).apply {
                            text = "No recent orders."
                            setTextColor(Color.parseColor("#7B5A4D"))
                            textSize = 14f
                        }
                    container.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in result) {
                    val customer =
                        doc.getString("customerName")
                            ?: doc.getString("customer")
                            ?: "Unknown"
                    val status = doc.getString("status") ?: "Pending"
                    val total = doc.getDouble("totalPrice") ?: 0.0

                    val orderRow =
                        LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(8, 8, 8, 8)
                            gravity = Gravity.CENTER_VERTICAL
                        }

                    val orderInfo =
                        TextView(this).apply {
                            text = "• $customer  -  £${"%.2f".format(total)}"
                            setTextColor(Color.parseColor("#4A2C2A"))
                            textSize = 14f
                            setTypeface(null, Typeface.BOLD)
                            setPadding(0, 0, 16, 0)
                            layoutParams =
                                LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f,
                                )
                        }

                    val badge =
                        TextView(this).apply {
                            text = status
                            textSize = 12f
                            setTextColor(Color.WHITE)
                            setPadding(16, 8, 16, 8)
                            gravity = Gravity.CENTER
                            setTypeface(null, Typeface.BOLD)
                            background = resources.getDrawable(R.drawable.status_badge_background, null)
                            backgroundTintList =
                                when (status.lowercase()) {
                                    "completed" ->
                                        android.content.res.ColorStateList
                                            .valueOf(Color.parseColor("#4CAF50"))
                                    "preparing" ->
                                        android.content.res.ColorStateList
                                            .valueOf(Color.parseColor("#FF9800"))
                                    "ready for collection" ->
                                        android.content.res.ColorStateList
                                            .valueOf(Color.parseColor("#8B4513"))
                                    else ->
                                        android.content.res.ColorStateList
                                            .valueOf(Color.parseColor("#B0BEC5"))
                                }
                        }

                    orderRow.addView(orderInfo)
                    orderRow.addView(badge)
                    container.addView(orderRow)
                }
            }.addOnFailureListener {
                container.removeAllViews()
                val errorText =
                    TextView(this).apply {
                        text = "Failed to load orders."
                        setTextColor(Color.RED)
                    }
                container.addView(errorText)
            }
    }

    private fun loadDashboardAnalytics() {
        loadUsersCount()
        loadTodayOrders()
    }

    private fun loadUsersCount() {
        db
            .collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                findViewById<TextView>(R.id.valueTotalUsers).text = snapshot.size().toString()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load user count: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTodayOrders() {
        val cal = java.util.Calendar.getInstance()

        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val start = cal.time

        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.time

        db
            .collection("orders")
            .whereGreaterThanOrEqualTo("createdAt", start)
            .whereLessThanOrEqualTo("createdAt", end)
            .get()
            .addOnSuccessListener { docs ->
                var revenue = 0.0
                docs.forEach { d ->
                    revenue += d.getDouble("totalPrice") ?: 0.0
                }

                findViewById<TextView>(R.id.valueOrdersToday).text = docs.size().toString()
                findViewById<TextView>(R.id.valueRevenueToday).text = "£%.2f".format(revenue)
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load today's orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
