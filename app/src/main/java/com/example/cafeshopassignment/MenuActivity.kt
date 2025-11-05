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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val categoryTabs = findViewById<TabLayout>(R.id.categoryTabs)
        menuRecyclerView = findViewById(R.id.menuRecyclerView)
        menuRecyclerView.layoutManager = LinearLayoutManager(this)

        // ✅ Create adapter
        menuAdapter = MenuAdapter(emptyList()) { menuItem ->
            Toast.makeText(this, "${menuItem.name} added to cart!", Toast.LENGTH_SHORT).show()
        }
        menuRecyclerView.adapter = menuAdapter

        val categories = listOf("Drinks", "Breakfast", "Lunch", "Pastries & Sweets")
        categories.forEach { categoryTabs.addTab(categoryTabs.newTab().setText(it)) }

        loadMenuItems(categories[0])

        categoryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                loadMenuItems(tab.text.toString())
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    welcomeText.text = "Welcome, ${document.getString("firstName")}!"
                }
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

                val menuList = documents.map { doc ->
                    MenuItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        category = doc.getString("category") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        active = doc.getBoolean("active") ?: true
                    )
                }

                menuAdapter.updateData(menuList)

                if (menuList.isEmpty()) {
                    Toast.makeText(this, "No items in $category", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
