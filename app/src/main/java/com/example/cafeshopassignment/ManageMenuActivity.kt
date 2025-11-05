package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.AdminMenuAdapter
import com.example.cafeshopassignment.models.MenuItem
import com.google.firebase.firestore.FirebaseFirestore

class ManageMenuActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminMenuAdapter
    private val menuList = mutableListOf<MenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_menu)

        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById<RecyclerView>(R.id.menuRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminMenuAdapter(menuList, ::editItem, ::deleteItem)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.addItemButton).setOnClickListener {
            showAddItemDialog()
        }

        loadMenuItems()
    }
    private fun loadMenuItems() {
        db.collection("menuItems")
            .get()
            .addOnSuccessListener { result ->
                menuList.clear()
                for (doc in result) {
                    val item = doc.toObject(MenuItem::class.java).copy(id = doc.id)
                    menuList.add(item)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load menu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddItemDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.itemNameInput)
        val priceInput = dialogView.findViewById<EditText>(R.id.itemPriceInput)
        val categoryInput = dialogView.findViewById<EditText>(R.id.itemCategoryInput)

        AlertDialog.Builder(this)
            .setTitle("Add Menu Item")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString()
                val price = priceInput.text.toString().toDoubleOrNull() ?: 0.0
                val category = categoryInput.text.toString()

                if (name.isEmpty() || category.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val docRef = db.collection("menuItems").document()
                val newItem = MenuItem(
                    id = docRef.id,
                    name = name,
                    price = price,
                    category = category
                )

                docRef.set(newItem)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Item added!", Toast.LENGTH_SHORT).show()
                        loadMenuItems()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun editItem(item: MenuItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.itemNameInput)
        val priceInput = dialogView.findViewById<EditText>(R.id.itemPriceInput)
        val categoryInput = dialogView.findViewById<EditText>(R.id.itemCategoryInput)

        nameInput.setText(item.name)
        priceInput.setText(item.price.toString())
        categoryInput.setText(item.category)

        AlertDialog.Builder(this)
            .setTitle("Edit Menu Item")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedItem = mapOf(
                    "name" to nameInput.text.toString(),
                    "price" to priceInput.text.toString().toDouble(),
                    "category" to categoryInput.text.toString()
                )

                db.collection("menuItems").document(item.id).update(updatedItem)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Item updated!", Toast.LENGTH_SHORT).show()
                        loadMenuItems()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: MenuItem) {
        db.collection("menuItems").document(item.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Item deleted!", Toast.LENGTH_SHORT).show()
                loadMenuItems()
            }
    }
}
