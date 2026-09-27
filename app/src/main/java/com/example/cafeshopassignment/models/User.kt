package com.example.cafeshopassignment.models

enum class UserRole {
    CUSTOMER,
    ADMIN,
    ;

    companion object {
        /** Anything other than an explicit "admin" is treated as a customer (least privilege). */
        fun fromString(value: String?): UserRole = if (value.equals("admin", ignoreCase = true)) ADMIN else CUSTOMER
    }
}

/** Profile document stored at `users/{uid}`. */
data class User(
    val uid: String = "",
    val firstname: String = "",
    val surname: String = "",
    val email: String = "",
    val role: UserRole = UserRole.CUSTOMER,
) {
    val fullName: String
        get() = listOf(firstname, surname).filter { it.isNotBlank() }.joinToString(" ")
}
