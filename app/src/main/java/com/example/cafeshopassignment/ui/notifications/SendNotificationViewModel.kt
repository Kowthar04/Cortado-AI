package com.example.cafeshopassignment.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.NotificationRepository
import com.example.cafeshopassignment.data.repository.UserRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.util.runSuspendCatching
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SendNotificationEvent {
    data object MissingTitleOrMessage : SendNotificationEvent

    data object MissingRecipient : SendNotificationEvent

    data object SentToUser : SendNotificationEvent

    data class SentToAll(
        val count: Int,
    ) : SendNotificationEvent

    data class Failed(
        val detail: String?,
    ) : SendNotificationEvent
}

class SendNotificationViewModel(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _events = Channel<SendNotificationEvent>(Channel.BUFFERED)
    val events: Flow<SendNotificationEvent> = _events.receiveAsFlow()

    fun send(
        title: String,
        message: String,
        recipientId: String,
        sendToAll: Boolean,
    ) {
        if (_isSending.value) return
        if (title.isBlank() || message.isBlank()) {
            _events.trySend(SendNotificationEvent.MissingTitleOrMessage)
            return
        }
        if (!sendToAll && recipientId.isBlank()) {
            _events.trySend(SendNotificationEvent.MissingRecipient)
            return
        }
        _isSending.value = true
        viewModelScope.launch {
            runSuspendCatching {
                if (sendToAll) {
                    val ids = userRepository.getAllUserIds()
                    SendNotificationEvent.SentToAll(notificationRepository.sendNotificationToAll(ids, title.trim(), message.trim()))
                } else {
                    notificationRepository.sendNotification(recipientId.trim(), title.trim(), message.trim())
                    SendNotificationEvent.SentToUser
                }
            }.onSuccess { _events.send(it) }
                .onFailure { _events.send(SendNotificationEvent.Failed(it.message)) }
            _isSending.value = false
        }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer { SendNotificationViewModel(ServiceLocator.notificationRepository, ServiceLocator.userRepository) }
            }
    }
}
