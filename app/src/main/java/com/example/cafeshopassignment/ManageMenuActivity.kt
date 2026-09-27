package com.example.cafeshopassignment

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.AdminMenuAdapter
import com.example.cafeshopassignment.models.MenuCategories
import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.ui.admin.ManageMenuEvent
import com.example.cafeshopassignment.ui.admin.ManageMenuUiState
import com.example.cafeshopassignment.ui.admin.ManageMenuViewModel
import com.example.cafeshopassignment.ui.admin.MenuInputError
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.toast
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ManageMenuActivity : AppCompatActivity() {
    private val viewModel: ManageMenuViewModel by viewModels { ManageMenuViewModel.Factory }

    /** One adapter per category section in the layout. */
    private lateinit var adapters: Map<String, AdminMenuAdapter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_menu)

        val toolbar = findViewById<Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerIds =
            mapOf(
                MenuCategories.DRINKS to R.id.recyclerDrinks,
                MenuCategories.BREAKFAST to R.id.recyclerBreakfast,
                MenuCategories.LUNCH to R.id.recyclerLunch,
                MenuCategories.PASTRIES to R.id.recyclerPastries,
            )
        adapters =
            recyclerIds.mapValues { (_, recyclerId) ->
                AdminMenuAdapter(onEdit = ::showEditDialog, onDelete = ::confirmDelete).also { adapter ->
                    findViewById<RecyclerView>(recyclerId).apply {
                        layoutManager = LinearLayoutManager(this@ManageMenuActivity)
                        this.adapter = adapter
                    }
                }
            }

        findViewById<FloatingActionButton>(R.id.addItemFAB).setOnClickListener { showAddDialog() }

        collectWhileStarted(viewModel.uiState, ::render)
        collectWhileStarted(viewModel.events, ::handleEvent)
    }

    private fun render(state: ManageMenuUiState) {
        adapters.forEach { (category, adapter) -> adapter.submitList(state.itemsByCategory[category].orEmpty()) }
    }

    private fun handleEvent(event: ManageMenuEvent) {
        when (event) {
            is ManageMenuEvent.LoadFailed -> toast(R.string.manage_menu_load_failed, event.detail.orEmpty())
            ManageMenuEvent.ItemAdded -> toast(R.string.manage_menu_item_added)
            ManageMenuEvent.ItemUpdated -> toast(R.string.manage_menu_item_updated)
            ManageMenuEvent.ItemDeleted -> toast(R.string.manage_menu_item_deleted)
            is ManageMenuEvent.OperationFailed -> toast(R.string.manage_menu_operation_failed, event.detail.orEmpty())
            is ManageMenuEvent.InvalidInput ->
                toast(
                    when (event.error) {
                        MenuInputError.MISSING_FIELDS -> R.string.manage_menu_missing_fields
                        MenuInputError.INVALID_PRICE -> R.string.manage_menu_invalid_price
                        MenuInputError.UNKNOWN_CATEGORY -> R.string.manage_menu_unknown_category
                    },
                )
        }
    }

    private fun showAddDialog() {
        val form = MenuItemForm(layoutInflater.inflate(R.layout.dialog_add_item, null))
        AlertDialog
            .Builder(this)
            .setTitle(R.string.manage_menu_add_title)
            .setView(form.root)
            .setPositiveButton(R.string.action_add) { _, _ ->
                viewModel.addItem(form.name, form.price, form.category)
            }.setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showEditDialog(item: MenuItem) {
        val form = MenuItemForm(layoutInflater.inflate(R.layout.dialog_add_item, null))
        form.fill(item)
        AlertDialog
            .Builder(this)
            .setTitle(R.string.manage_menu_edit_title)
            .setView(form.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.updateItem(item, form.name, form.price, form.category)
            }.setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmDelete(item: MenuItem) {
        AlertDialog
            .Builder(this)
            .setTitle(R.string.manage_menu_delete_title)
            .setMessage(getString(R.string.manage_menu_delete_message, item.name))
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteItem(item) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /** Thin wrapper over dialog_add_item.xml. */
    private class MenuItemForm(
        val root: View,
    ) {
        private val nameInput: EditText = root.findViewById(R.id.itemNameInput)
        private val priceInput: EditText = root.findViewById(R.id.itemPriceInput)
        private val categoryInput: EditText = root.findViewById(R.id.itemCategoryInput)

        init {
            // The AlertDialog supplies the title.
            root.findViewById<View>(R.id.dialogTitle).visibility = View.GONE
        }

        val name: String get() = nameInput.text.toString()
        val price: String get() = priceInput.text.toString()
        val category: String get() = categoryInput.text.toString()

        fun fill(item: MenuItem) {
            nameInput.setText(item.name)
            priceInput.setText(item.price.toString())
            categoryInput.setText(item.category)
        }
    }
}
