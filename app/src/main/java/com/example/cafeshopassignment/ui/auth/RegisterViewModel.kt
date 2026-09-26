package com.example.cafeshopassignment.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.di.ServiceLocator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface RegisterEvent {
    data object Registered : RegisterEvent

    data object MissingFields : RegisterEvent

    data class Failed(
        val detail: String?,
    ) : RegisterEvent
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events: Flow<RegisterEvent> = _events.receiveAsFlow()

    fun register(
        firstname: String,
        surname: String,
        email: String,
        password: String,
    ) {
        if (_isLoading.value) return
        if (listOf(firstname, surname, email, password).any { it.isBlank() }) {
            _events.trySend(RegisterEvent.MissingFields)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authRepository.register(firstname.trim(), surname.trim(), email.trim(), password.trim())
                // Registration signs the user in; send them through the normal login flow instead.
                authRepository.signOut()
                _events.send(RegisterEvent.Registered)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(RegisterEvent.Failed(e.message))
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer { RegisterViewModel(ServiceLocator.authRepository) }
            }
    }
}
