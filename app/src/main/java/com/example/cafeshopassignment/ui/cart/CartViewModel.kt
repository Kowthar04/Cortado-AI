package com.example.cafeshopassignment.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.CartRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.models.CartItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

data class CartUiState(
    val items: List<CartItem> = emptyList(),
) {
    val subtotal: Double
        get() = items.sumOf { it.totalPrice }

    val isEmpty: Boolean
        get() = items.isEmpty()
}

sealed interface CartEvent {
    data object ProceedToPayment : CartEvent

    data object CartEmpty : CartEvent

    data object NotLoggedIn : CartEvent
}

class CartViewModel(
    private val cartRepository: CartRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val uiState: StateFlow<CartUiState> =
        cartRepository.items
            .map { CartUiState(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CartUiState(cartRepository.items.value))

    private val _events = Channel<CartEvent>(Channel.BUFFERED)
    val events: Flow<CartEvent> = _events.receiveAsFlow()

    fun increase(item: CartItem) = cartRepository.increase(item.id)

    fun decrease(item: CartItem) = cartRepository.decrease(item.id)

    fun checkout() {
        val event =
            when {
                cartRepository.items.value.isEmpty() -> CartEvent.CartEmpty
                authRepository.currentUserId == null -> CartEvent.NotLoggedIn
                else -> CartEvent.ProceedToPayment
            }
        _events.trySend(event)
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer { CartViewModel(ServiceLocator.cartRepository, ServiceLocator.authRepository) }
            }
    }
}
