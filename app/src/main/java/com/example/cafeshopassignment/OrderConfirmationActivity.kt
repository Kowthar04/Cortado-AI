package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OrderConfirmationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)


        supportActionBar?.hide()

        val orderTotal = intent.getDoubleExtra("ORDER_TOTAL", 0.0)
        val orderId = intent.getStringExtra("ORDER_ID") ?: "Unknown"

        val orderIdText = findViewById<TextView>(R.id.orderIdText)
        val orderAmountText = findViewById<TextView>(R.id.orderAmountText)
        val backToMenuButton = findViewById<Button>(R.id.backToMenuButton)

        orderIdText.text = "Order #${orderId.takeLast(8).uppercase()}"
        orderAmountText.text = "Total Paid: £${"%.2f".format(orderTotal)}"

        backToMenuButton.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}