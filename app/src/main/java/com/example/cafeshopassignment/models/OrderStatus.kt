package com.example.cafeshopassignment.models

/**
 * Lifecycle of an order. [label] is the exact string stored in Firestore's `orders.status`
 * field, so existing documents keep working.
 */
enum class OrderStatus(
    val label: String,
) {
    PENDING("Pending"),
    PREPARING("Preparing"),
    READY_FOR_COLLECTION("Ready for Collection"),
    COMPLETED("Completed"),
    ;

    /** 1-based position in the order lifecycle, used for the tracking progress bar. */
    val step: Int
        get() = ordinal + 1

    val isFinal: Boolean
        get() = this == COMPLETED

    /** The status an admin would normally move the order to next, or null once completed. */
    fun next(): OrderStatus? = entries.getOrNull(ordinal + 1)

    companion object {
        val labels: List<String> = entries.map { it.label }

        /** Case-insensitive lookup of a stored status label; null for unknown values. */
        fun fromLabel(label: String?): OrderStatus? = entries.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) }
    }
}
