package com.example.cafeshopassignment.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.CartRepository
import com.example.cafeshopassignment.data.repository.NewOrder
import com.example.cafeshopassignment.data.repository.OrderRepository
import com.example.cafeshopassignment.data.repository.UserRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.util.runSuspendCatching
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentUiState(
    val summary: PaymentSummary,
    /** Card is pre-selected, matching the layout's default radio button. */
    val method: PaymentMethod? = PaymentMethod.CARD,
    val isProcessing: Boolean = false,
)

sealed interface PaymentEvent {
    data object PromoCodeEmpty : PaymentEvent

    data object PromoApplied : PaymentEvent

    data object PromoInvalid : PaymentEvent

    data object SelectPaymentMethod : PaymentEvent

    data class InvalidCard(
        val error: CardError,
    ) : PaymentEvent

    data object CartEmpty : PaymentEvent

    data object NotLoggedIn : PaymentEvent

    data class OrderPlaced(
        val orderId: String,
        val total: Double,
    ) : PaymentEvent

    data class Failed(
        val detail: String?,
    ) : PaymentEvent
}

class PaymentViewModel(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(PaymentUiState(PaymentCalculator.summarize(cartRepository.subtotal, promoApplied = false)))
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _events = Channel<PaymentEvent>(Channel.BUFFERED)
    val events: Flow<PaymentEvent> = _events.receiveAsFlow()

    fun applyPromo(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) {
            _events.trySend(PaymentEvent.PromoCodeEmpty)
            return
        }
        val valid = PaymentCalculator.isValidPromo(trimmed)
        _uiState.update { it.copy(summary = PaymentCalculator.summarize(cartRepository.subtotal, valid)) }
        _events.trySend(if (valid) PaymentEvent.PromoApplied else PaymentEvent.PromoInvalid)
    }

    fun selectMethod(method: PaymentMethod) {
        _uiState.update { it.copy(method = method) }
    }

    /** [card] is only validated when paying by card. Ignored while a payment is already in flight. */
    fun pay(card: CardDetails) {
        val state = _uiState.value
        if (state.isProcessing) return

        val method = state.method
        if (method == null) {
            _events.trySend(PaymentEvent.SelectPaymentMethod)
            return
        }
        if (method == PaymentMethod.CARD) {
            CardValidator.validate(card)?.let {
                _events.trySend(PaymentEvent.InvalidCard(it))
                return
            }
        }
        val items = cartRepository.items.value
        if (items.isEmpty()) {
            _events.trySend(PaymentEvent.CartEmpty)
            return
        }
        val uid = authRepository.currentUserId
        if (uid == null) {
            _events.trySend(PaymentEvent.NotLoggedIn)
            return
        }

        // Recompute from the cart at the moment of payment so the charged total can't be stale.
        val summary = PaymentCalculator.summarize(cartRepository.subtotal, state.summary.promoApplied)
        _uiState.update { it.copy(summary = summary, isProcessing = true) }

        viewModelScope.launch {
            runSuspendCatching {
                val order =
                    NewOrder(
                        userId = uid,
                        customerName = resolveCustomerName(uid),
                        items = items,
                        subtotal = summary.subtotal,
                        serviceFee = summary.serviceFee,
                        discount = summary.discount,
                        total = summary.total,
                        paymentMethod = method.label,
                        promoApplied = summary.promoApplied,
                    )
                orderRepository.placeOrder(order)
            }.onSuccess { orderId ->
                cartRepository.clear()
                _events.send(PaymentEvent.OrderPlaced(orderId, summary.total))
            }.onFailure { e ->
                _events.send(PaymentEvent.Failed(e.message))
            }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    private suspend fun resolveCustomerName(uid: String): String {
        val profileName = runSuspendCatching { userRepository.getUser(uid)?.fullName }.getOrNull()
        return profileName?.takeIf { it.isNotBlank() }
            ?: authRepository.currentUserDisplayName?.takeIf { it.isNotBlank() }
            ?: authRepository.currentUserEmail?.substringBefore("@")
            ?: "Customer"
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    PaymentViewModel(
                        ServiceLocator.cartRepository,
                        ServiceLocator.orderRepository,
                        ServiceLocator.userRepository,
                        ServiceLocator.authRepository,
                    )
                }
            }
    }
}
