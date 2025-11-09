package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

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

    private var totalAmount: Double = 0.0

    private lateinit var promoCodeInput: EditText
    private lateinit var applyPromoButton: Button

    private var discountAmount = 0.0
    private var finalTotal = 0.0
    private val serviceFee = 0.50


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        title = "Payment"

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        promoCodeInput = findViewById(R.id.promoCodeInput)
        applyPromoButton = findViewById(R.id.applyPromoButton)

        finalTotal = totalAmount + serviceFee

        applyPromoButton.setOnClickListener {
            val code = promoCodeInput.text.toString().trim().uppercase()
            applyPromoCode(code)
        }


        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)
        val serviceFee = 0.50
        val finalTotal = totalAmount + serviceFee


        val subtotalText = findViewById<TextView>(R.id.subtotalAmount)
        val serviceFeeText = findViewById<TextView>(R.id.serviceFeeAmount)
        val totalText = findViewById<TextView>(R.id.totalAmount)

        subtotalText.text = "£${"%.2f".format(totalAmount)}"
        serviceFeeText.text = "£${"%.2f".format(serviceFee)}"
        totalText.text = "£${"%.2f".format(finalTotal)}"


        cardPaymentRadio = findViewById(R.id.cardPaymentRadio)
        googlePayRadio = findViewById(R.id.googlePayRadio)
        cardDetailsSection = findViewById(R.id.cardDetailsSection)


        cardNumberInput = findViewById(R.id.cardNumberInput)
        cardholderNameInput = findViewById(R.id.cardholderNameInput)
        expiryDateInput = findViewById(R.id.expiryDateInput)
        cvvInput = findViewById(R.id.cvvInput)

        val payNowButton = findViewById<Button>(R.id.payNowButton)

        val cardPaymentOption = findViewById<MaterialCardView>(R.id.cardPaymentOption)
        val googlePayOption = findViewById<MaterialCardView>(R.id.googlePayOption)


        cardPaymentOption.setOnClickListener {
            selectPaymentMethod("card")
        }

        googlePayOption.setOnClickListener {
            selectPaymentMethod("googlepay")
        }

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


        payNowButton.setOnClickListener {
            processPayment(finalTotal)
        }



    }

    private fun applyPromoCode(code: String) {
        if (code.isEmpty()) {
            Toast.makeText(this, "Enter promo code first", Toast.LENGTH_SHORT).show()
            return
        }

        if (code == "THIRSTY") {
            discountAmount = (totalAmount + serviceFee) * 0.20   // 20% off
            finalTotal = (totalAmount + serviceFee) - discountAmount

            // Prevent negative totals
            if (finalTotal < 0) finalTotal = 0.0

            val totalText = findViewById<TextView>(R.id.totalAmount)
            totalText.text = "£${"%.2f".format(finalTotal)}"

            Toast.makeText(this, "Promo code applied: 20% off!", Toast.LENGTH_SHORT).show()
        } else {
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
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }


        if (cardPaymentRadio.isChecked) {

            if (!validateCardDetails()) {
                return
            }
        } else if (!googlePayRadio.isChecked) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            return
        }


        Toast.makeText(this, "Processing payment...", Toast.LENGTH_SHORT).show()


        val cartItems = CartManager.getCart()
        val orderData = hashMapOf(
            "userId" to currentUser.uid,
            "items" to cartItems.map {
                mapOf(
                    "name" to it.name,
                    "quantity" to it.quantity,
                    "price" to it.price
                )
            },
            "subtotal" to totalAmount,
            "serviceFee" to 0.50,
            "total" to amount,
            "paymentMethod" to getSelectedPaymentMethod(),
            "status" to "Preparing",
            "timestamp" to Date(),
            "paymentStatus" to "Completed"
        )

        db.collection("orders").add(orderData)
            .addOnSuccessListener {

                CartManager.clear()


                val intent = Intent(this, OrderConfirmationActivity::class.java)
                intent.putExtra("ORDER_TOTAL", amount)
                intent.putExtra("ORDER_ID", it.id)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Payment failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateCardDetails(): Boolean {
        val cardNumber = cardNumberInput.text.toString().trim()
        val cardholderName = cardholderNameInput.text.toString().trim()
        val expiryDate = expiryDateInput.text.toString().trim()
        val cvv = cvvInput.text.toString().trim()

        if (cardNumber.isEmpty() || cardNumber.length < 13) {
            Toast.makeText(this, "Please enter a valid card number", Toast.LENGTH_SHORT).show()
            cardNumberInput.requestFocus()
            return false
        }

        if (cardholderName.isEmpty()) {
            Toast.makeText(this, "Please enter cardholder name", Toast.LENGTH_SHORT).show()
            cardholderNameInput.requestFocus()
            return false
        }

        if (expiryDate.length != 4) {
            Toast.makeText(this, "Please enter expiry date as MMYY", Toast.LENGTH_SHORT).show()
            expiryDateInput.requestFocus()
            return false
        }


        val month = expiryDate.substring(0, 2).toIntOrNull()
        val year = expiryDate.substring(2, 4).toIntOrNull()


        if (month == null || month < 1 || month > 12) {
            Toast.makeText(this, "Please enter a valid month", Toast.LENGTH_SHORT).show()
            expiryDateInput.requestFocus()
            return false
        }

        if (cvv.isEmpty() || cvv.length < 3) {
            Toast.makeText(this, "Please enter valid CVV", Toast.LENGTH_SHORT).show()
            cvvInput.requestFocus()
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