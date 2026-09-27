package com.example.cafeshopassignment.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.OrderRepository
import com.example.cafeshopassignment.data.repository.UserRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.models.Order
import com.example.cafeshopassignment.util.runSuspendCatching
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class DashboardStats(
    val adminName: String? = null,
    val totalUsers: Int? = null,
    val ordersToday: Int? = null,
    val revenueToday: Double? = null,
)

sealed interface RecentOrdersState {
    data object Loading : RecentOrdersState

    data class Loaded(
        val orders: List<Order>,
    ) : RecentOrdersState

    data object Failed : RecentOrdersState
}

sealed interface DashboardEvent {
    data class StatsFailed(
        val detail: String?,
    ) : DashboardEvent

    data object LoggedOut : DashboardEvent
}

class AdminDashboardViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    /** Live preview of the latest orders; the listener only runs while the dashboard is visible. */
    val recentOrders: StateFlow<RecentOrdersState> =
        orderRepository
            .observeRecentOrders(RECENT_ORDER_COUNT)
            .map<List<Order>, RecentOrdersState> { RecentOrdersState.Loaded(it) }
            .catch { emit(RecentOrdersState.Failed) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecentOrdersState.Loading)

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events: Flow<DashboardEvent> = _events.receiveAsFlow()

    init {
        loadAdminName()
    }

    /** Refreshes the headline numbers; called each time the dashboard becomes visible. */
    fun refreshStats() {
        viewModelScope.launch {
            runSuspendCatching { userRepository.countUsers() }
                .onSuccess { count -> _stats.update { it.copy(totalUsers = count) } }
                .onFailure { _events.send(DashboardEvent.StatsFailed(it.message)) }

            val (start, end) = todayRange()
            runSuspendCatching { orderRepository.getOrdersBetween(start, end) }
                .onSuccess { orders ->
                    _stats.update { it.copy(ordersToday = orders.size, revenueToday = orders.sumOf { o -> o.totalPrice }) }
                }.onFailure { _events.send(DashboardEvent.StatsFailed(it.message)) }
        }
    }

    fun logout() {
        authRepository.signOut()
        _events.trySend(DashboardEvent.LoggedOut)
    }

    private fun loadAdminName() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val name = runSuspendCatching { userRepository.getUser(uid)?.firstname }.getOrNull()
            _stats.update { it.copy(adminName = name?.takeIf { n -> n.isNotBlank() }) }
        }
    }

    companion object {
        const val RECENT_ORDER_COUNT = 4L

        /** Local-midnight to 23:59:59.999 today. */
        fun todayRange(now: Calendar = Calendar.getInstance()): Pair<Date, Date> {
            val cal = now.clone() as Calendar
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.time
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MILLISECOND, -1)
            return start to cal.time
        }

        val Factory =
            viewModelFactory {
                initializer {
                    AdminDashboardViewModel(
                        ServiceLocator.authRepository,
                        ServiceLocator.userRepository,
                        ServiceLocator.orderRepository,
                    )
                }
            }
    }
}
