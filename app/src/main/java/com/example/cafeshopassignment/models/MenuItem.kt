package com.example.cafeshopassignment.models

data class MenuItem(
    val id: String? = null,
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val availability: Boolean = true,
)

/** The fixed set of menu categories shown as tabs to customers and as sections to admins. */
object MenuCategories {
    const val DRINKS = "Drinks"
    const val BREAKFAST = "Breakfast"
    const val LUNCH = "Lunch"
    const val PASTRIES = "Pastries & Sweets"

    val all: List<String> = listOf(DRINKS, BREAKFAST, LUNCH, PASTRIES)

    /** Returns the canonical category name for [input] (case/whitespace-insensitive), or null if unknown. */
    fun normalize(input: String): String? = all.firstOrNull { it.equals(input.trim(), ignoreCase = true) }
}
