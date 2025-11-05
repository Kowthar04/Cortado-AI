package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.MenuAdapter
import com.example.cafeshopassignment.models.MenuItem
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MenuActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var menuRecyclerView: RecyclerView
    private lateinit var menuAdapter: MenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        //firebase database

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val categoryTabs = findViewById<TabLayout>(R.id.categoryTabs)
        menuRecyclerView = findViewById(R.id.menuRecyclerView)

        menuRecyclerView.layoutManager = LinearLayoutManager(this)
        menuAdapter = MenuAdapter(emptyList())
        menuRecyclerView.adapter = menuAdapter

        // Add specific category tabs
        val categories = listOf("Drinks", "Breakfast", "Lunch", "Pastries")
        categories.forEach { categoryTabs.addTab(categoryTabs.newTab().setText(it)) }

        // Load items for the first tab (Drinks)
        loadMenuItems("Drinks")

        categoryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = tab.text.toString()
                loadMenuItems(category)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Get current user UID
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
        // Fetch user's first name from Firestore
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstname =
                        document.getString("firstname")
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



    private fun loadMenuItems(category: String) {
        db.collection("menuItems")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { documents ->
                val menuList = documents.mapNotNull { it.toObject(MenuItem::class.java) }
                menuAdapter.updateData(menuList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load items: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
