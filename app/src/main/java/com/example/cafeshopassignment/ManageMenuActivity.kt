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
                val name = nameInput.text.toString().trim()
                val price = priceInput.text.toString().toDoubleOrNull()
                val category = categoryInput.text.toString().trim()

                if (name.isEmpty() || category.isEmpty() && price == null) {
                    Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT)
                        .show()
                    return@setPositiveButton
                }

                val newItem = MenuItem(
                    name = name,
                    price = price,
                    category = category
                )
                db.collection("menuItems")
                    .add(newItem)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Item added Successfully!", Toast.LENGTH_SHORT).show()
                        loadMenuItems()

                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error adding item : ${e.message}", Toast.LENGTH_SHORT)
                            .show()
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
                    "name" to nameInput.text.toString().trim(),
                    "price" to priceInput.text.toString().toDoubleOrNull(),
                    "category" to categoryInput.text.toString().trim(),
                )
                val itemId = item.id
                if (itemId != null) {
                    db.collection("menuItems").document(itemId).update(updatedItem)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Item updated successfully!", Toast.LENGTH_SHORT)
                                .show()
                            loadMenuItems()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                } else {
                    Toast.makeText(this, "Missing item ID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: MenuItem) {
        val itemId = item.id
        if (itemId == null) {
            Toast.makeText(this, "Error: Missing item ID", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete ${item.name}?")
            .setPositiveButton("Yes") { _, _ ->

            }
        db.collection("menuItems").document(itemId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Item deleted Successfully!", Toast.LENGTH_SHORT).show()
                loadMenuItems()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}


