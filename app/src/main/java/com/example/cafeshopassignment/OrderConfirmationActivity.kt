package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.cafeshopassignment.models.OrderStatus
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.formatPrice
import com.example.cafeshopassignment.ui.common.showStatusBadge
import com.example.cafeshopassignment.ui.orders.OrderTrackingUiState
import com.example.cafeshopassignment.ui.orders.OrderTrackingViewModel
import com.google.android.material.progressindicator.LinearProgressIndicator

/** Post-checkout screen with a live (Firestore snapshot) view of the new order's status. */
class OrderConfirmationActivity : AppCompatActivity() {
    private val viewModel: OrderTrackingViewModel by viewModels { OrderTrackingViewModel.Factory }

    private lateinit var statusBadge: TextView
    private lateinit var statusMessage: TextView
    private lateinit var statusProgress: LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        val orderTotal = intent.getDoubleExtra(EXTRA_ORDER_TOTAL, 0.0)
        val orderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()

        statusBadge = findViewById(R.id.orderStatusBadge)
        statusMessage = findViewById(R.id.orderStatusMessage)
        statusProgress = findViewById(R.id.orderStatusProgress)
        statusProgress.max = OrderStatus.entries.size

        findViewById<TextView>(R.id.orderIdText).text =
            getString(R.string.confirmation_order_number, orderId.takeLast(8).uppercase())
        findViewById<TextView>(R.id.orderAmountText).text =
            getString(R.string.confirmation_total_paid, formatPrice(orderTotal))

        findViewById<Button>(R.id.leaveReviewButton).setOnClickListener {
            startActivity(Intent(this, ReviewActivity::class.java).putExtra(ReviewActivity.EXTRA_ORDER_ID, orderId))
        }
        findViewById<Button>(R.id.viewOrdersButton).setOnClickListener {
            startActivity(Intent(this, ViewOrdersActivity::class.java))
        }
        findViewById<Button>(R.id.backToMenuButton).setOnClickListener {
            val intent =
                Intent(this, MenuActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            startActivity(intent)
            finish()
        }

        viewModel.track(orderId)
        collectWhileStarted(viewModel.uiState, ::render)
    }

    private fun render(state: OrderTrackingUiState) {
        val status = state.order?.orderStatus
        when {
            state.isLoading -> statusMessage.setText(R.string.tracking_connecting)
            state.error != null -> statusMessage.setText(R.string.tracking_error)
            state.notFound -> statusMessage.setText(R.string.tracking_not_found)
            else -> statusMessage.setText(statusMessageRes(status))
        }
        val label = state.order?.status ?: OrderStatus.PENDING.label
        statusBadge.showStatusBadge(status, label)
        statusProgress.setProgressCompat(status?.step ?: 0, true)
    }

    private fun statusMessageRes(status: OrderStatus?): Int =
        when (status) {
            OrderStatus.PREPARING -> R.string.tracking_preparing
            OrderStatus.READY_FOR_COLLECTION -> R.string.tracking_ready
            OrderStatus.COMPLETED -> R.string.tracking_completed
            OrderStatus.PENDING, null -> R.string.tracking_pending
        }

    companion object {
        const val EXTRA_ORDER_ID = "ORDER_ID"
        const val EXTRA_ORDER_TOTAL = "ORDER_TOTAL"
    }
}
