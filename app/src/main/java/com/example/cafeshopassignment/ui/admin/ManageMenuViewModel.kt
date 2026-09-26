package com.example.cafeshopassignment.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.MenuRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.models.MenuCategories
import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.util.runSuspendCatching
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManageMenuUiState(
    val isLoading: Boolean = true,
    /** Items keyed by category, in [MenuCategories.all] order. */
    val itemsByCategory: Map<String, List<MenuItem>> = emptyMap(),
    val loadError: String? = null,
)

/** A validated add/edit form. */
data class MenuItemInput(
    val name: String,
    val price: Double,
    val category: String,
)

enum class MenuInputError { MISSING_FIELDS, INVALID_PRICE, UNKNOWN_CATEGORY }

sealed interface ManageMenuEvent {
    data class InvalidInput(
        val error: MenuInputError,
    ) : ManageMenuEvent

    data class LoadFailed(
        val detail: String?,
    ) : ManageMenuEvent

    data object ItemAdded : ManageMenuEvent

    data object ItemUpdated : ManageMenuEvent

    data object ItemDeleted : ManageMenuEvent

    data class OperationFailed(
        val detail: String?,
    ) : ManageMenuEvent
}

class ManageMenuViewModel(
    private val menuRepository: MenuRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageMenuUiState())
    val uiState: StateFlow<ManageMenuUiState> = _uiState.asStateFlow()

    private val _events = Channel<ManageMenuEvent>(Channel.BUFFERED)
    val events: Flow<ManageMenuEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            runSuspendCatching { menuRepository.getMenuItems() }
                .onSuccess { items ->
                    val grouped = MenuCategories.all.associateWith { category -> items.filter { it.category == category } }
                    _uiState.value = ManageMenuUiState(isLoading = false, itemsByCategory = grouped)
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, loadError = e.message ?: "") }
                    _events.send(ManageMenuEvent.LoadFailed(e.message))
                }
        }
    }

    fun addItem(
        name: String,
        price: String,
        category: String,
    ) {
        val input = validate(name, price, category) ?: return
        mutate(ManageMenuEvent.ItemAdded) { menuRepository.addMenuItem(input.name, input.price, input.category) }
    }

    fun updateItem(
        item: MenuItem,
        name: String,
        price: String,
        category: String,
    ) {
        val id = item.id
        if (id.isNullOrBlank()) {
            _events.trySend(ManageMenuEvent.OperationFailed(null))
            return
        }
        val input = validate(name, price, category) ?: return
        mutate(ManageMenuEvent.ItemUpdated) { menuRepository.updateMenuItem(id, input.name, input.price, input.category) }
    }

    fun deleteItem(item: MenuItem) {
        val id = item.id
        if (id.isNullOrBlank()) {
            _events.trySend(ManageMenuEvent.OperationFailed(null))
            return
        }
        mutate(ManageMenuEvent.ItemDeleted) { menuRepository.deleteMenuItem(id) }
    }

    private fun validate(
        name: String,
        price: String,
        category: String,
    ): MenuItemInput? =
        when (val result = parseInput(name, price, category)) {
            is InputResult.Valid -> result.input
            is InputResult.Invalid -> {
                _events.trySend(ManageMenuEvent.InvalidInput(result.error))
                null
            }
        }

    private fun mutate(
        successEvent: ManageMenuEvent,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runSuspendCatching { block() }
                .onSuccess {
                    _events.send(successEvent)
                    load()
                }.onFailure { _events.send(ManageMenuEvent.OperationFailed(it.message)) }
        }
    }

    sealed interface InputResult {
        data class Valid(
            val input: MenuItemInput,
        ) : InputResult

        data class Invalid(
            val error: MenuInputError,
        ) : InputResult
    }

    companion object {
        /**
         * Validates the add/edit dialog. The category must be one of [MenuCategories.all]
         * (case-insensitive) because items in any other category would never appear in a tab.
         */
        fun parseInput(
            name: String,
            price: String,
            category: String,
        ): InputResult {
            if (name.isBlank() || price.isBlank() || category.isBlank()) return InputResult.Invalid(MenuInputError.MISSING_FIELDS)
            val parsedPrice = price.trim().toDoubleOrNull()
            if (parsedPrice == null || parsedPrice <= 0.0) return InputResult.Invalid(MenuInputError.INVALID_PRICE)
            val canonicalCategory = MenuCategories.normalize(category) ?: return InputResult.Invalid(MenuInputError.UNKNOWN_CATEGORY)
            return InputResult.Valid(MenuItemInput(name.trim(), parsedPrice, canonicalCategory))
        }

        val Factory =
            viewModelFactory {
                initializer { ManageMenuViewModel(ServiceLocator.menuRepository) }
            }
    }
}
