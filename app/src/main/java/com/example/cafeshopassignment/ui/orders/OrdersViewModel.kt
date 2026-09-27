package com.example.cafeshopassignment.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.OrderRepository
import com.example.cafeshopassignment.data.repository.UserRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.models.Order
import com.example.cafeshopassignment.models.OrderStatus
import com.example.cafeshopassignment.models.UserRole
import com.example.cafeshopassignment.util.runSuspendCatching
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersUiState(
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val orders: List<Order> = emptyList(),
    val totalCount: Int = 0,
    val error: OrdersError? = null,
)

sealed interface OrdersError {
    data object NotSignedIn : OrdersError

    data class LoadFailed(
        val detail: String?,
    ) : OrdersError
}

sealed interface OrdersEvent {
    data class StatusUpdated(
        val status: OrderStatus,
    ) : OrdersEvent

    data class StatusUpdateFailed(
        val detail: String?,
    ) : OrdersEvent
}

/**
 * Backs the orders screen for both roles: admins see every order (and can change status),
 * customers see their own orders. Either way the list is a live Firestore snapshot, so a status
 * change made by staff shows up on the customer's screen immediately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModel(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<OrderStatus?>(null)

    private val _events = Channel<OrdersEvent>(Channel.BUFFERED)
    val events: Flow<OrdersEvent> = _events.receiveAsFlow()

    private var cachedIsAdmin: Boolean? = null

    private val source: Flow<OrdersSource> =
        flow { emit(resolveRole()) }
            .flatMapLatest { role ->
                when (role) {
                    null -> flowOf(OrdersSource(isAdmin = false, error = OrdersError.NotSignedIn))
                    else -> {
                        val (uid, isAdmin) = role
                        val orders = if (isAdmin) orderRepository.observeAllOrders() else orderRepository.observeOrdersForUser(uid)
                        orders.map { OrdersSource(isAdmin, orders = it) }
                    }
                }
            }.catch { e -> emit(OrdersSource(isAdmin = cachedIsAdmin ?: false, error = OrdersError.LoadFailed(e.message))) }

    /** Collected while the screen is started; the upstream snapshot listener stops 5s after the UI does. */
    val uiState: StateFlow<OrdersUiState> =
        combine(source, query, statusFilter) { src, q, status ->
            OrdersUiState(
                isLoading = false,
                isAdmin = src.isAdmin,
                orders = filterOrders(src.orders, q, status),
                totalCount = src.orders.size,
                error = src.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OrdersUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setStatusFilter(status: OrderStatus?) {
        statusFilter.value = status
    }

    fun updateStatus(
        order: Order,
        status: OrderStatus,
    ) {
        if (cachedIsAdmin != true) return
        viewModelScope.launch {
            runSuspendCatching { orderRepository.updateStatus(order, status) }
                .onSuccess { _events.send(OrdersEvent.StatusUpdated(status)) }
                .onFailure { _events.send(OrdersEvent.StatusUpdateFailed(it.message)) }
        }
    }

    /** Returns (uid, isAdmin), or null when nobody is signed in. Throws if the profile can't be read. */
    private suspend fun resolveRole(): Pair<String, Boolean>? {
        val uid = authRepository.currentUserId ?: return null
        val isAdmin = cachedIsAdmin ?: (userRepository.getUser(uid)?.role == UserRole.ADMIN)
        cachedIsAdmin = isAdmin
        return uid to isAdmin
    }

    private data class OrdersSource(
        val isAdmin: Boolean,
        val orders: List<Order> = emptyList(),
        val error: OrdersError? = null,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Case-insensitive match on customer name or user id, plus an optional exact status. */
        fun filterOrders(
            orders: List<Order>,
            query: String,
            status: OrderStatus?,
        ): List<Order> {
            val q = query.trim().lowercase()
            return orders.filter { order ->
                val matchesQuery =
                    q.isEmpty() ||
                        order.customerName.lowercase().contains(q) ||
                        order.userId.lowercase().contains(q)
                val matchesStatus = status == null || order.orderStatus == status
                matchesQuery && matchesStatus
            }
        }

        val Factory =
            viewModelFactory {
                initializer {
                    OrdersViewModel(
                        ServiceLocator.orderRepository,
                        ServiceLocator.userRepository,
                        ServiceLocator.authRepository,
                    )
                }
            }
    }
}
