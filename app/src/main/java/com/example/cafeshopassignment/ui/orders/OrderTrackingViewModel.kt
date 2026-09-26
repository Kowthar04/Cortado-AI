package com.example.cafeshopassignment.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.OrderRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.models.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class OrderTrackingUiState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val notFound: Boolean = false,
    val error: String? = null,
)

/** Live status of a single order, shown on the confirmation screen right after checkout. */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderTrackingViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val orderId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OrderTrackingUiState> =
        orderId
            .filterNotNull()
            .flatMapLatest { id ->
                orderRepository
                    .observeOrder(id)
                    .map { order -> OrderTrackingUiState(isLoading = false, order = order, notFound = order == null) }
                    .catch { e -> emit(OrderTrackingUiState(isLoading = false, error = e.message ?: "")) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderTrackingUiState())

    fun track(id: String) {
        if (id.isNotBlank()) orderId.value = id
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer { OrderTrackingViewModel(ServiceLocator.orderRepository) }
            }
    }
}
