package com.example.cafeshopassignment.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.CartRepository
import com.example.cafeshopassignment.data.repository.MenuRepository
import com.example.cafeshopassignment.data.repository.UserRepository
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

data class MenuUiState(
    val firstName: String? = null,
    val categories: List<String> = MenuCategories.all,
    val selectedCategory: String = MenuCategories.all.first(),
    val items: List<MenuItem> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && loadError == null && items.isEmpty()
}

sealed interface MenuEvent {
    data class AddedToCart(
        val itemName: String,
    ) : MenuEvent

    data object LoggedOut : MenuEvent
}

class MenuViewModel(
    private val menuRepository: MenuRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val _events = Channel<MenuEvent>(Channel.BUFFERED)
    val events: Flow<MenuEvent> = _events.receiveAsFlow()

    /** Full menu from a single Firestore query; tab switches filter locally instead of re-querying. */
    private var allItems: List<MenuItem> = emptyList()

    init {
        loadWelcomeName()
        loadMenu()
    }

    fun loadMenu() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            runSuspendCatching { menuRepository.getMenuItems() }
                .onSuccess { items ->
                    allItems = items
                    _uiState.update { it.copy(isLoading = false, items = itemsFor(it.selectedCategory)) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, loadError = e.message ?: "") }
                }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category, items = itemsFor(category)) }
    }

    fun addToCart(item: MenuItem) {
        cartRepository.add(item)
        _events.trySend(MenuEvent.AddedToCart(item.name))
    }

    fun logout() {
        authRepository.signOut()
        _events.trySend(MenuEvent.LoggedOut)
    }

    /** Unavailable items are hidden from customers. */
    private fun itemsFor(category: String): List<MenuItem> = allItems.filter { it.category == category && it.availability }

    private fun loadWelcomeName() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            // A missing name only affects the greeting, so failures fall back silently to "Welcome!".
            val name = runSuspendCatching { userRepository.getUser(uid)?.firstname }.getOrNull()
            _uiState.update { it.copy(firstName = name?.takeIf { n -> n.isNotBlank() }) }
        }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    MenuViewModel(
                        ServiceLocator.menuRepository,
                        ServiceLocator.userRepository,
                        ServiceLocator.authRepository,
                        ServiceLocator.cartRepository,
                    )
                }
            }
    }
}
