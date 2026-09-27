package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.MenuAdapter
import com.example.cafeshopassignment.ui.assistant.AiAssistantBottomSheet
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.toast
import com.example.cafeshopassignment.ui.menu.MenuEvent
import com.example.cafeshopassignment.ui.menu.MenuUiState
import com.example.cafeshopassignment.ui.menu.MenuViewModel
import com.google.android.material.tabs.TabLayout

class MenuActivity : AppCompatActivity() {
    private val viewModel: MenuViewModel by viewModels { MenuViewModel.Factory }

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var welcomeText: TextView
    private lateinit var categoryTabs: TabLayout
    private lateinit var progress: ProgressBar
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)

        welcomeText = findViewById(R.id.welcomeText)
        categoryTabs = findViewById(R.id.categoryTabs)
        progress = findViewById(R.id.menuProgress)
        emptyText = findViewById(R.id.menuEmptyText)

        findViewById<ImageButton>(R.id.mailButton).setOnClickListener {
            startActivity(Intent(this, NotificationInboxActivity::class.java))
        }
        findViewById<ImageButton>(R.id.ordersButton).setOnClickListener {
            startActivity(Intent(this, ViewOrdersActivity::class.java))
        }
        findViewById<ImageButton>(R.id.cartButton).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
        findViewById<Button>(R.id.logoutButton).setOnClickListener { viewModel.logout() }
        findViewById<View>(R.id.askAiFab).setOnClickListener { AiAssistantBottomSheet.show(supportFragmentManager) }
        emptyText.setOnClickListener { if (viewModel.uiState.value.loadError != null) viewModel.loadMenu() }

        menuAdapter = MenuAdapter { menuItem -> viewModel.addToCart(menuItem) }
        findViewById<RecyclerView>(R.id.menuRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@MenuActivity)
            adapter = menuAdapter
        }

        val state = viewModel.uiState.value
        state.categories.forEach { category ->
            val tab = categoryTabs.newTab().setText(category).setTag(category)
            categoryTabs.addTab(tab, category == state.selectedCategory)
        }
        categoryTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    (tab.tag as? String)?.let(viewModel::selectCategory)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            },
        )

        collectWhileStarted(viewModel.uiState, ::render)
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                is MenuEvent.AddedToCart -> toast(R.string.menu_added_to_cart, event.itemName)
                MenuEvent.LoggedOut -> {
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

    private fun render(state: MenuUiState) {
        welcomeText.text =
            state.firstName?.let { getString(R.string.menu_welcome_name, it) } ?: getString(R.string.menu_welcome)
        progress.isVisible = state.isLoading
        menuAdapter.submitList(state.items)
        emptyText.isVisible = state.loadError != null || state.isEmpty
        emptyText.text =
            when {
                state.loadError != null -> getString(R.string.menu_load_failed)
                else -> getString(R.string.menu_empty_category, state.selectedCategory)
            }
    }
}
