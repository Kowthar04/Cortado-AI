package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PaymentActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var cardPaymentRadio: RadioButton
    private lateinit var googlePayRadio: RadioButton
    private lateinit var cardDetailsSection: android.view.View

    private lateinit var cardNumberInput: EditText
    private lateinit var cardholderNameInput: EditText
    private lateinit var expiryDateInput: EditText
    private lateinit var cvvInput: EditText

    private lateinit var promoCodeInput: EditText
    private lateinit var applyPromoButton: Button

    private var totalAmount: Double = 0.0
    private val serviceFee = 0.50
    private var discountAmount = 0.0
    private var finalTotal = 0.0
    private var promoApplied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)
        title = "Payment"

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()


        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)
        finalTotal = totalAmount + serviceFee


        promoCodeInput = findViewById(R.id.promoCodeInput)
        applyPromoButton = findViewById(R.id.applyPromoButton)

        val subtotalText = findViewById<TextView>(R.id.subtotalAmount)
        val serviceFeeText = findViewById<TextView>(R.id.serviceFeeAmount)
        val totalText = findViewById<TextView>(R.id.totalAmount)
        val payNowButton = findViewById<Button>(R.id.payNowButton)

        cardPaymentRadio = findViewById(R.id.cardPaymentRadio)
        googlePayRadio = findViewById(R.id.googlePayRadio)
        cardDetailsSection = findViewById(R.id.cardDetailsSection)
        cardNumberInput = findViewById(R.id.cardNumberInput)
        cardholderNameInput = findViewById(R.id.cardholderNameInput)
        expiryDateInput = findViewById(R.id.expiryDateInput)
        cvvInput = findViewById(R.id.cvvInput)

        val cardPaymentOption = findViewById<MaterialCardView>(R.id.cardPaymentOption)
        val googlePayOption = findViewById<MaterialCardView>(R.id.googlePayOption)


        subtotalText.text = "£${"%.2f".format(totalAmount)}"
        serviceFeeText.text = "£${"%.2f".format(serviceFee)}"
        totalText.text = "£${"%.2f".format(finalTotal)}"


        applyPromoButton.setOnClickListener {
            val code = promoCodeInput.text.toString().trim().uppercase()
            applyPromoCode(code, totalText)
        }


        cardPaymentOption.setOnClickListener { selectPaymentMethod("card") }
        googlePayOption.setOnClickListener { selectPaymentMethod("googlepay") }

        cardPaymentRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cardDetailsSection.visibility = android.view.View.VISIBLE
                googlePayRadio.isChecked = false
            }
        }
        googlePayRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cardDetailsSection.visibility = android.view.View.GONE
                cardPaymentRadio.isChecked = false
            }
        }


        payNowButton.setOnClickListener { processPayment(finalTotal) }
    }

    private fun applyPromoCode(code: String, totalText: TextView) {
        if (code.isEmpty()) {
            Toast.makeText(this, "Enter promo code first", Toast.LENGTH_SHORT).show()
            return
        }

        if (code == "THIRSTY") {
            // 20% off subtotal + service fee
            discountAmount = (totalAmount + serviceFee) * 0.20
            finalTotal = (totalAmount + serviceFee) - discountAmount
            if (finalTotal < 0) finalTotal = 0.0
            promoApplied = true

            totalText.text = "£${"%.2f".format(finalTotal)}"
            Toast.makeText(this, "Promo code applied: 20% off!", Toast.LENGTH_SHORT).show()
        } else {
            promoApplied = false
            discountAmount = 0.0
            finalTotal = totalAmount + serviceFee
            totalText.text = "£${"%.2f".format(finalTotal)}"
            Toast.makeText(this, "Invalid promo code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectPaymentMethod(method: String) {
        when (method) {
            "card" -> {
                cardPaymentRadio.isChecked = true
                cardDetailsSection.visibility = android.view.View.VISIBLE
            }
            "googlepay" -> {
                googlePayRadio.isChecked = true
                cardDetailsSection.visibility = android.view.View.GONE
            }
        }
    }

    private fun processPayment(amount: Double) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }


        if (cardPaymentRadio.isChecked && !validateCardDetails()) return
        if (!cardPaymentRadio.isChecked && !googlePayRadio.isChecked) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Processing payment...", Toast.LENGTH_SHORT).show()

        val userId = currentUser.uid
        val cartItems = CartManager.getCart()


        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val firstName = doc.getString("firstname") ?: ""
                val lastName = doc.getString("surname") ?: ""
                val customerName = listOf(firstName, lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank {
                        currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Customer"
                    }

                // Build order
                val orderData = hashMapOf(
                    "userId" to userId,
                    "customerName" to customerName,
                    "items" to cartItems.map {
                        mapOf(
                            "name" to it.name,
                            "quantity" to it.quantity,
                            "price" to it.price
                        )
                    },
                    "subtotal" to totalAmount,
                    "serviceFee" to serviceFee,
                    "discount" to discountAmount,
                    "totalPrice" to amount,
                    "paymentMethod" to getSelectedPaymentMethod(),
                    "status" to "Pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "paymentStatus" to "Completed"
                )

                // 1) Save ORDER
                db.collection("orders").add(orderData)
                    .addOnSuccessListener { orderRef ->
                        // 2) Save PAYMENT record (separate collection)
                        val paymentData = hashMapOf(
                            "orderId" to orderRef.id,
                            "userId" to userId,
                            "customerName" to customerName,
                            "amountPaid" to amount,
                            "paymentMethod" to getSelectedPaymentMethod(),
                            "paymentStatus" to "Completed",
                            "promoApplied" to promoApplied,
                            "discountAmount" to discountAmount,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        db.collection("payments").add(paymentData)
                            .addOnSuccessListener {
                                // Optional: toast for payment log
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to record payment: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        // Clear cart & go to confirmation
                        CartManager.clear()
                        val intent = Intent(this, OrderConfirmationActivity::class.java).apply {
                            putExtra("ORDER_TOTAL", amount)
                            putExtra("ORDER_ID", orderRef.id)
                        }
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Payment failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load user info: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateCardDetails(): Boolean {
        val cardNumber = cardNumberInput.text.toString().trim()
        val cardholderName = cardholderNameInput.text.toString().trim()
        val expiryDate = expiryDateInput.text.toString().trim()
        val cvv = cvvInput.text.toString().trim()

        if (cardNumber.isEmpty() || cardNumber.length < 13) {
            cardNumberInput.error = "Invalid card number"
            return false
        }
        if (cardholderName.isEmpty()) {
            cardholderNameInput.error = "Enter cardholder name"
            return false
        }
        if (expiryDate.length != 4) {
            expiryDateInput.error = "Use MMYY format"
            return false
        }
        val month = expiryDate.substring(0, 2).toIntOrNull()
        if (month == null || month !in 1..12) {
            expiryDateInput.error = "Invalid month"
            return false
        }
        if (cvv.isEmpty() || cvv.length < 3) {
            cvvInput.error = "Invalid CVV"
            return false
        }
        return true
    }

    private fun getSelectedPaymentMethod(): String {
        return when {
            cardPaymentRadio.isChecked -> "Credit/Debit Card"
            googlePayRadio.isChecked -> "Google Pay"
            else -> "Unknown"
        }
    }
}
