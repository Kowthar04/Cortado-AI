package com.example.cafeshopassignment.ui.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.ReviewRepository
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

sealed interface ReviewEvent {
    data object MissingRating : ReviewEvent

    data object MissingComment : ReviewEvent

    data object NotLoggedIn : ReviewEvent

    data object Submitted : ReviewEvent

    data class Failed(
        val detail: String?,
    ) : ReviewEvent
}

/** Customer-side "leave a review" form. */
class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _events = Channel<ReviewEvent>(Channel.BUFFERED)
    val events: Flow<ReviewEvent> = _events.receiveAsFlow()

    fun submit(
        orderId: String,
        rating: Int,
        comment: String,
    ) {
        if (_isSubmitting.value) return
        val event =
            when {
                rating <= 0 -> ReviewEvent.MissingRating
                comment.isBlank() -> ReviewEvent.MissingComment
                authRepository.currentUserId == null -> ReviewEvent.NotLoggedIn
                else -> null
            }
        if (event != null) {
            _events.trySend(event)
            return
        }
        val uid = authRepository.currentUserId ?: return

        _isSubmitting.value = true
        viewModelScope.launch {
            runSuspendCatching {
                val name =
                    runSuspendCatching { userRepository.getUser(uid)?.fullName }.getOrNull()?.takeIf { it.isNotBlank() }
                        ?: authRepository.currentUserEmail?.substringBefore("@")
                        ?: "Customer"
                reviewRepository.submitReview(orderId, uid, name, rating, comment.trim())
            }.onSuccess { _events.send(ReviewEvent.Submitted) }
                .onFailure { _events.send(ReviewEvent.Failed(it.message)) }
            _isSubmitting.value = false
        }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    ReviewViewModel(ServiceLocator.reviewRepository, ServiceLocator.userRepository, ServiceLocator.authRepository)
                }
            }
    }
}
