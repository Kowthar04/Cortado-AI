package com.example.cafeshopassignment

import org.junit.Assert.*
import org.junit.Test

class LoginValidationTest {

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && email.contains("@") && email.contains(".")
    }

    private fun isValidPassword(password: String): Boolean {
        return password.isNotEmpty() && password.length >= 6
    }

    @Test
    fun `returns true when the email is valid`() {
        assertTrue(isValidEmail("test@example.com"))
    }

    @Test
    fun `returns false when the email is invalid`() {
        assertFalse(isValidEmail("testexample.com"))
    }

    @Test
    fun `returns true when the password is valid`() {
        assertTrue(isValidPassword("123456"))
    }

    @Test
    fun `returns false when the password is too short`() {
        assertFalse(isValidPassword("123"))
    }
}
