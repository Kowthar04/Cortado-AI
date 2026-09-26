package com.example.cafeshopassignment.data.repository

import com.example.cafeshopassignment.data.firestore.FirestoreCollections
import com.example.cafeshopassignment.data.firestore.FirestoreMappers
import com.example.cafeshopassignment.data.firestore.fields
import com.example.cafeshopassignment.models.MenuItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface MenuRepository {
    /** Every menu item in every category (one query; screens filter locally). */
    suspend fun getMenuItems(): List<MenuItem>

    suspend fun addMenuItem(
        name: String,
        price: Double,
        category: String,
    ): String

    suspend fun updateMenuItem(
        id: String,
        name: String,
        price: Double,
        category: String,
    )

    suspend fun deleteMenuItem(id: String)
}

class FirestoreMenuRepository(
    private val firestore: FirebaseFirestore,
) : MenuRepository {
    private val menuItems get() = firestore.collection(FirestoreCollections.MENU_ITEMS)

    override suspend fun getMenuItems(): List<MenuItem> =
        menuItems
            .get()
            .await()
            .documents
            .map { FirestoreMappers.menuItem(it.id, it.fields()) }

    override suspend fun addMenuItem(
        name: String,
        price: Double,
        category: String,
    ): String {
        val data =
            mapOf(
                "name" to name,
                "price" to price,
                "category" to category,
                "imageUrl" to "",
                "availability" to true,
            )
        return menuItems.add(data).await().id
    }

    override suspend fun updateMenuItem(
        id: String,
        name: String,
        price: Double,
        category: String,
    ) {
        menuItems
            .document(id)
            .update(mapOf("name" to name, "price" to price, "category" to category))
            .await()
    }

    override suspend fun deleteMenuItem(id: String) {
        menuItems.document(id).delete().await()
    }
}
