package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MenuActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        // Get current user UID
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            // Fetch user's first name from Firestore
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val firstname = document.getString("firstName")
                        welcomeText.text = "Welcome, $firstname!"
                    } else {
                        welcomeText.text = "Welcome!"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load user info: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No user logged in (should not happen if flow is correct)
            welcomeText.text = "Welcome!"
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
