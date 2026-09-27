package com.example.cafeshopassignment

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.OrderAdapter
import com.example.cafeshopassignment.models.Order
import com.example.cafeshopassignment.models.OrderStatus
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.toast
import com.example.cafeshopassignment.ui.orders.OrdersError
import com.example.cafeshopassignment.ui.orders.OrdersEvent
import com.example.cafeshopassignment.ui.orders.OrdersUiState
import com.example.cafeshopassignment.ui.orders.OrdersViewModel

/**
 * Orders list for both roles, updated in real time from a Firestore snapshot listener.
 * Customers see their own orders (reached from the menu's receipt icon or the confirmation
 * screen); admins see every order and can change its status.
 */
class ViewOrdersActivity : AppCompatActivity() {
    private val viewModel: OrdersViewModel by viewModels { OrdersViewModel.Factory }

    private lateinit var orderAdapter: OrderAdapter
    private lateinit var titleText: TextView
    private lateinit var searchInput: EditText
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_orders)

        findViewById<ImageButton>(R.id.backButtonOrders).setOnClickListener { finish() }

        titleText = findViewById(R.id.ordersTitle)
        searchInput = findViewById(R.id.orderSearchInput)
        emptyText = findViewById(R.id.ordersEmptyText)

        orderAdapter = OrderAdapter(::showStatusDialog)
        findViewById<RecyclerView>(R.id.ordersRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@ViewOrdersActivity)
            adapter = orderAdapter
        }

        // Filter listeners are registered exactly once; they only push values into the ViewModel.
        searchInput.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        setUpStatusFilter(findViewById(R.id.orderStatusFilter))

        collectWhileStarted(viewModel.uiState, ::render)
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                is OrdersEvent.StatusUpdated -> toast(R.string.orders_status_updated, event.status.label)
                is OrdersEvent.StatusUpdateFailed -> toast(R.string.orders_status_update_failed, event.detail.orEmpty())
            }
        }
    }

    private fun setUpStatusFilter(spinner: Spinner) {
        val options = listOf(getString(R.string.orders_filter_all)) + OrderStatus.labels
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    // Position 0 is "All"; the rest map 1:1 onto OrderStatus.entries.
                    viewModel.setStatusFilter(OrderStatus.entries.getOrNull(position - 1))
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
    }

    private fun render(state: OrdersUiState) {
        titleText.setText(if (state.isAdmin) R.string.orders_title_admin else R.string.orders_title_customer)
        searchInput.isVisible = state.isAdmin
        orderAdapter.showAdminActions = state.isAdmin
        orderAdapter.submitList(state.orders)

        emptyText.isVisible = !state.isLoading && state.orders.isEmpty()
        emptyText.text =
            when (val error = state.error) {
                OrdersError.NotSignedIn -> getString(R.string.error_not_logged_in)
                is OrdersError.LoadFailed -> getString(R.string.orders_load_failed, error.detail.orEmpty())
                null -> getString(if (state.totalCount == 0) R.string.orders_empty else R.string.orders_no_matches)
            }
    }

    private fun showStatusDialog(order: Order) {
        val statuses = OrderStatus.entries
        AlertDialog
            .Builder(this)
            .setTitle(R.string.orders_update_status_title)
            .setItems(statuses.map { it.label }.toTypedArray()) { _, which ->
                viewModel.updateStatus(order, statuses[which])
            }.show()
    }
}
