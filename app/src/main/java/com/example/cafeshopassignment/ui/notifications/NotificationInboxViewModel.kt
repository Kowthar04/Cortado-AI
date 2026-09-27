package com.example.cafeshopassignment.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.NotificationRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.models.Notification
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class InboxUiState(
    val isLoading: Boolean = true,
    val notifications: List<Notification> = emptyList(),
    val error: String? = null,
    val notSignedIn: Boolean = false,
)

/** Customer inbox; a live listener so staff replies and status updates arrive instantly. */
class NotificationInboxViewModel(
    notificationRepository: NotificationRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    val uiState: StateFlow<InboxUiState> =
        (
            authRepository.currentUserId?.let { uid ->
                notificationRepository
                    .observeNotifications(uid)
                    .map { InboxUiState(isLoading = false, notifications = it) }
                    .catch { e -> emit(InboxUiState(isLoading = false, error = e.message ?: "")) }
            } ?: flowOf(InboxUiState(isLoading = false, notSignedIn = true))
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    companion object {
        val Factory =
            viewModelFactory {
                initializer { NotificationInboxViewModel(ServiceLocator.notificationRepository, ServiceLocator.authRepository) }
            }
    }
}
