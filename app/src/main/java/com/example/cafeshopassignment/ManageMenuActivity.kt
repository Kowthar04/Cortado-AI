package com.example.cafeshopassignment

import android.os.Bundle
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

    private lateinit var drinksRecycler: RecyclerView
    private lateinit var breakfastRecycler: RecyclerView
    private lateinit var lunchRecycler: RecyclerView
    private lateinit var pastriesRecycler: RecyclerView

    private lateinit var drinksAdapter: AdminMenuAdapter
    private lateinit var breakfastAdapter: AdminMenuAdapter
    private lateinit var lunchAdapter: AdminMenuAdapter
    private lateinit var pastriesAdapter: AdminMenuAdapter

    private val drinksList = mutableListOf<MenuItem>()
    private val breakfastList = mutableListOf<MenuItem>()
    private val lunchList = mutableListOf<MenuItem>()
    private val pastriesList = mutableListOf<MenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_menu)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        db = FirebaseFirestore.getInstance()

        drinksRecycler = findViewById(R.id.recyclerDrinks)
        breakfastRecycler = findViewById(R.id.recyclerBreakfast)
        lunchRecycler = findViewById(R.id.recyclerLunch)
        pastriesRecycler = findViewById(R.id.recyclerPastries)

        drinksAdapter = AdminMenuAdapter(drinksList, ::editItem, ::deleteItem)
        breakfastAdapter = AdminMenuAdapter(breakfastList, ::editItem, ::deleteItem)
        lunchAdapter = AdminMenuAdapter(lunchList, ::editItem, ::deleteItem)
        pastriesAdapter = AdminMenuAdapter(pastriesList, ::editItem, ::deleteItem)

        drinksRecycler.layoutManager = LinearLayoutManager(this)
        breakfastRecycler.layoutManager = LinearLayoutManager(this)
        lunchRecycler.layoutManager = LinearLayoutManager(this)
        pastriesRecycler.layoutManager = LinearLayoutManager(this)

        drinksRecycler.adapter = drinksAdapter
        breakfastRecycler.adapter = breakfastAdapter
        lunchRecycler.adapter = lunchAdapter
        pastriesRecycler.adapter = pastriesAdapter

        loadAllCategories()

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.addItemFAB)
            .setOnClickListener { showAddItemDialog() }
    }

    private fun loadAllCategories() {
        loadCategory("Drinks", drinksList, drinksAdapter)
        loadCategory("Breakfast", breakfastList, breakfastAdapter)
        loadCategory("Lunch", lunchList, lunchAdapter)
        loadCategory("Pastries & Sweets", pastriesList, pastriesAdapter)
    }

    private fun loadCategory(
        category: String,
        list: MutableList<MenuItem>,
        adapter: AdminMenuAdapter,
    ) {
        db
            .collection("menuItems")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { result ->
                list.clear()
                for (doc in result) {
                    val priceAny = doc.get("price")
                    val price =
                        when (priceAny) {
                            is Number -> priceAny.toDouble()
                            is String -> priceAny.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                    val item =
                        MenuItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            category = category,
                            price = price,
                            availability = doc.getBoolean("availability") ?: true,
                        )
                    list.add(item)
                }
                adapter.notifyDataSetChanged()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load $category", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddItemDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_item, null)

        val nameInput = view.findViewById<EditText>(R.id.itemNameInput)
        val priceInput = view.findViewById<EditText>(R.id.itemPriceInput)
        val categoryInput = view.findViewById<EditText>(R.id.itemCategoryInput)

        AlertDialog
            .Builder(this)
            .setTitle("Add Menu Item")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->

                val name = nameInput.text.toString().trim()
                val price = priceInput.text.toString().toDoubleOrNull()
                val category = categoryInput.text.toString().trim()

                if (name.isEmpty() || price == null || category.isEmpty()) {
                    Toast.makeText(this, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newItem =
                    MenuItem(
                        name = name,
                        price = price,
                        category = category,
                        availability = true,
                    )

                db
                    .collection("menuItems")
                    .add(newItem)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Item added!", Toast.LENGTH_SHORT).show()
                        loadAllCategories()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun editItem(item: MenuItem) {
        val view = layoutInflater.inflate(R.layout.dialog_add_item, null)

        val nameInput = view.findViewById<EditText>(R.id.itemNameInput)
        val priceInput = view.findViewById<EditText>(R.id.itemPriceInput)
        val categoryInput = view.findViewById<EditText>(R.id.itemCategoryInput)

        nameInput.setText(item.name)
        priceInput.setText(item.price.toString())
        categoryInput.setText(item.category)

        AlertDialog
            .Builder(this)
            .setTitle("Edit Item")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val itemId = item.id
                val name = nameInput.text.toString().trim()
                val price = priceInput.text.toString().toDoubleOrNull()
                val category = categoryInput.text.toString().trim()
                if (itemId.isNullOrBlank() || name.isEmpty() || price == null || category.isEmpty()) {
                    Toast.makeText(this, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedData =
                    mapOf(
                        "name" to name,
                        "price" to price,
                        "category" to category,
                    )

                db
                    .collection("menuItems")
                    .document(itemId)
                    .update(updatedData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show()
                        loadAllCategories()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: MenuItem) {
        AlertDialog
            .Builder(this)
            .setTitle("Delete Item")
            .setMessage("Remove ${item.name}?")
            .setPositiveButton("Delete") { _, _ ->
                val itemId = item.id
                if (itemId.isNullOrBlank()) {
                    Toast.makeText(this, "Cannot delete: item has no ID", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db
                    .collection("menuItems")
                    .document(itemId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show()
                        loadAllCategories()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }.setNegativeButton("Cancel", null)
            .show()
    }
}
