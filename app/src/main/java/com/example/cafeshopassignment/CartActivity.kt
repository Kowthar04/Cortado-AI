package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.CartAdapter
import com.example.cafeshopassignment.ui.cart.CartEvent
import com.example.cafeshopassignment.ui.cart.CartViewModel
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.formatPrice
import com.example.cafeshopassignment.ui.common.toast

class CartActivity : AppCompatActivity() {
    private val viewModel: CartViewModel by viewModels { CartViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val totalText = findViewById<TextView>(R.id.cartTotal)
        val emptyText = findViewById<TextView>(R.id.cartEmptyText)
        val placeOrderButton = findViewById<Button>(R.id.placeOrderButton)

        val cartAdapter = CartAdapter(onIncrease = viewModel::increase, onDecrease = viewModel::decrease)
        findViewById<RecyclerView>(R.id.cartRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }

        placeOrderButton.setOnClickListener { viewModel.checkout() }

        collectWhileStarted(viewModel.uiState) { state ->
            cartAdapter.submitList(state.items)
            totalText.text = formatPrice(state.subtotal)
            emptyText.isVisible = state.isEmpty
        }
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                CartEvent.ProceedToPayment -> startActivity(Intent(this, PaymentActivity::class.java))
                CartEvent.CartEmpty -> toast(R.string.cart_empty)
                CartEvent.NotLoggedIn -> toast(R.string.error_not_logged_in)
            }
        }
    }
}
