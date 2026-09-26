package com.example.cafeshopassignment.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.UserRepository
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.models.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface LoginEvent {
    data object LoggedInAsCustomer : LoginEvent

    data object LoggedInAsAdmin : LoginEvent

    data object MissingFields : LoginEvent

    data object NotAnAdmin : LoginEvent

    data object ProfileMissing : LoginEvent

    data class Failed(
        val detail: String?,
    ) : LoginEvent
}

/** Shared by the customer and admin login screens; the admin path additionally checks the role. */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun login(
        email: String,
        password: String,
        requireAdmin: Boolean,
    ) {
        if (_isLoading.value) return
        if (email.isBlank() || password.isBlank()) {
            _events.trySend(LoginEvent.MissingFields)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = authRepository.signIn(email.trim(), password.trim())
                _events.send(if (requireAdmin) checkAdmin(uid) else LoginEvent.LoggedInAsCustomer)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(LoginEvent.Failed(e.message))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun checkAdmin(uid: String): LoginEvent {
        val profile =
            try {
                userRepository.getUser(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                authRepository.signOut()
                return LoginEvent.Failed(e.message)
            }
        return when {
            profile == null -> {
                authRepository.signOut()
                LoginEvent.ProfileMissing
            }
            profile.role != UserRole.ADMIN -> {
                authRepository.signOut()
                LoginEvent.NotAnAdmin
            }
            else -> LoginEvent.LoggedInAsAdmin
        }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer { LoginViewModel(ServiceLocator.authRepository, ServiceLocator.userRepository) }
            }
    }
}
