package com.example.cafeshopassignment

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.cafeshopassignment.models.Order
import com.example.cafeshopassignment.ui.admin.AdminDashboardViewModel
import com.example.cafeshopassignment.ui.admin.DashboardEvent
import com.example.cafeshopassignment.ui.admin.DashboardStats
import com.example.cafeshopassignment.ui.admin.RecentOrdersState
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.formatPrice
import com.example.cafeshopassignment.ui.common.showStatusBadge
import com.example.cafeshopassignment.ui.common.toast

class AdminDashboardActivity : AppCompatActivity() {
    private val viewModel: AdminDashboardViewModel by viewModels { AdminDashboardViewModel.Factory }

    private lateinit var ordersContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        ordersContainer = findViewById(R.id.ordersPreviewContainer)

        findViewById<ImageButton>(R.id.backButtonDashboard).setOnClickListener { finish() }
        findViewById<CardView>(R.id.cardManageMenu).setOnClickListener {
            startActivity(Intent(this, ManageMenuActivity::class.java))
        }
        findViewById<CardView>(R.id.cardViewFeedback).setOnClickListener {
            startActivity(Intent(this, ViewReviewsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardSendNotifications).setOnClickListener {
            startActivity(Intent(this, SendNotificationActivity::class.java))
        }
        findViewById<CardView>(R.id.cardAnalytics).setOnClickListener {
            toast(R.string.dashboard_analytics_coming_soon)
        }
        findViewById<Button>(R.id.viewMoreOrdersButton).setOnClickListener {
            startActivity(Intent(this, ViewOrdersActivity::class.java))
        }
        findViewById<Button>(R.id.AdminLogoutButton).setOnClickListener { viewModel.logout() }

        collectWhileStarted(viewModel.stats, ::renderStats)
        collectWhileStarted(viewModel.recentOrders, ::renderRecentOrders)
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                is DashboardEvent.StatsFailed -> toast(R.string.dashboard_stats_failed, event.detail.orEmpty())
                DashboardEvent.LoggedOut -> {
                    toast(R.string.logged_out)
                    startActivity(
                        Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        },
                    )
                    finish()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshStats()
    }

    private fun renderStats(stats: DashboardStats) {
        findViewById<TextView>(R.id.adminWelcomeText).text =
            stats.adminName?.let { getString(R.string.menu_welcome_name, it) } ?: getString(R.string.dashboard_welcome_admin)
        findViewById<TextView>(R.id.valueTotalUsers).text = stats.totalUsers?.toString() ?: "–"
        findViewById<TextView>(R.id.valueOrdersToday).text = stats.ordersToday?.toString() ?: "–"
        findViewById<TextView>(R.id.valueRevenueToday).text = stats.revenueToday?.let(::formatPrice) ?: "–"
    }

    /** Rebuilds the preview from scratch on every emission, so rows can never be duplicated. */
    private fun renderRecentOrders(state: RecentOrdersState) {
        ordersContainer.removeAllViews()
        when (state) {
            RecentOrdersState.Loading -> Unit
            RecentOrdersState.Failed -> ordersContainer.addView(messageRow(getString(R.string.dashboard_orders_failed), R.color.deep_red))
            is RecentOrdersState.Loaded ->
                if (state.orders.isEmpty()) {
                    ordersContainer.addView(messageRow(getString(R.string.dashboard_no_recent_orders), R.color.medium_gray))
                } else {
                    state.orders.forEach { ordersContainer.addView(orderRow(it)) }
                }
        }
    }

    private fun messageRow(
        text: String,
        colorRes: Int,
    ) = TextView(this).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(context, colorRes))
        textSize = 14f
    }

    private fun orderRow(order: Order): LinearLayout {
        val row =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 8, 8, 8)
                gravity = Gravity.CENTER_VERTICAL
            }
        val info =
            TextView(this).apply {
                text = getString(R.string.dashboard_order_row, order.customerName, formatPrice(order.totalPrice))
                setTextColor(ContextCompat.getColor(context, R.color.dark_coffee))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 16, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        val badge =
            TextView(this).apply {
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setPadding(16, 8, 16, 8)
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                background = ContextCompat.getDrawable(context, R.drawable.status_badge_background)
                showStatusBadge(order.orderStatus, order.status)
            }
        row.addView(info)
        row.addView(badge)
        return row
    }
}
