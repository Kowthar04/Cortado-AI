package com.example.cafeshopassignment.ui.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.NotificationRepository
import com.example.cafeshopassignment.data.repository.ReviewRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.models.Review
import com.example.cafeshopassignment.util.runSuspendCatching
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViewReviewsUiState(
    val isLoading: Boolean = true,
    val reviews: List<Review> = emptyList(),
    val loadError: String? = null,
)

sealed interface ViewReviewsEvent {
    data object EmptyReply : ViewReviewsEvent

    data object ReplySent : ViewReviewsEvent

    data class ReplyFailed(
        val detail: String?,
    ) : ViewReviewsEvent
}

/** Admin feedback screen: searchable review list with "reply" (sent as an in-app notification). */
class ViewReviewsViewModel(
    private val reviewRepository: ReviewRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private val loaded = MutableStateFlow(ViewReviewsUiState())
    private val query = MutableStateFlow("")

    val uiState: StateFlow<ViewReviewsUiState> =
        combine(loaded, query) { state, q -> state.copy(reviews = filterReviews(state.reviews, q)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ViewReviewsUiState())

    private val _events = Channel<ViewReviewsEvent>(Channel.BUFFERED)
    val events: Flow<ViewReviewsEvent> = _events.receiveAsFlow()

    fun load() {
        loaded.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            runSuspendCatching { reviewRepository.getReviews() }
                .onSuccess { reviews -> loaded.value = ViewReviewsUiState(isLoading = false, reviews = reviews) }
                .onFailure { e -> loaded.update { it.copy(isLoading = false, loadError = e.message ?: "") } }
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun reply(
        review: Review,
        message: String,
    ) {
        if (message.isBlank()) {
            _events.trySend(ViewReviewsEvent.EmptyReply)
            return
        }
        viewModelScope.launch {
            runSuspendCatching {
                notificationRepository.sendNotification(review.customerId, "Response to your review", message.trim())
            }.onSuccess { _events.send(ViewReviewsEvent.ReplySent) }
                .onFailure { _events.send(ViewReviewsEvent.ReplyFailed(it.message)) }
        }
    }

    companion object {
        fun filterReviews(
            reviews: List<Review>,
            query: String,
        ): List<Review> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return reviews
            return reviews.filter {
                it.customerName.lowercase().contains(q) ||
                    it.comment.lowercase().contains(q) ||
                    it.customerId.lowercase().contains(q)
            }
        }

        val Factory =
            viewModelFactory {
                initializer { ViewReviewsViewModel(ServiceLocator.reviewRepository, ServiceLocator.notificationRepository) }
            }
    }
}
