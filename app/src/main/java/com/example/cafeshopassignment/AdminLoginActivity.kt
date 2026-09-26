package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminLoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.adminEmailInput)
        val passwordInput = findViewById<EditText>(R.id.adminPasswordInput)
        val loginButton = findViewById<Button>(R.id.adminLoginButton)
        val backButton = findViewById<Button>(R.id.backToUserLoginButton)

        backButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter Email and Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        db
                            .collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener { doc ->
                                if (doc != null && doc.exists()) {
                                    val role = doc.getString("role")?.lowercase() ?: "customer"

                                    if (role == "admin") {
                                        Toast.makeText(this, "Admin Login Successful", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, AdminDashboardActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(this, "Access denied: Not an admin", Toast.LENGTH_SHORT).show()
                                        auth.signOut()
                                        CartManager.clear()
                                    }
                                } else {
                                    Toast.makeText(this, "User record not found in Firestore", Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                    CartManager.clear()
                                }
                            }.addOnFailureListener { e ->
                                Toast.makeText(this, "Error getting role: ${e.message}", Toast.LENGTH_SHORT).show()
                                auth.signOut()
                                CartManager.clear()
                            }
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
