package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val welcomeText = findViewById<TextView>(R.id.adminWelcomeText)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            // Fetch admin name from Firestore
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
    }
}
