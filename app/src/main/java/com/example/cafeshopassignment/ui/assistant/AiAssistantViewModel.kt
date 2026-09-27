package com.example.cafeshopassignment.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cafeshopassignment.data.repository.ChatRepository
import com.example.cafeshopassignment.data.repository.ChatResult
import com.example.cafeshopassignment.di.ServiceLocator
import com.example.cafeshopassignment.models.ChatError
import com.example.cafeshopassignment.models.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Conversation state for the "Ask AI" bottom sheet. Scoped to the host activity, so closing
 * and reopening the sheet keeps the conversation.
 */
class AiAssistantViewModel(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var nextId = 0L

    /** Sends [text] as a new user turn. Ignored while blank or while a reply is pending. */
    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _isLoading.value) return
        val history = _messages.value
        append(ChatMessage.Role.USER, message)
        request(message, history)
    }

    /**
     * Retries the user turn that produced [errorMessage]: the error bubble is removed and the
     * same text is sent again with the same history.
     */
    fun retry(errorMessage: ChatMessage) {
        if (!errorMessage.isError || _isLoading.value) return
        val current = _messages.value
        val errorIndex = current.indexOfFirst { it.id == errorMessage.id }
        if (errorIndex < 0) return
        val userIndex = current.subList(0, errorIndex).indexOfLast { it.role == ChatMessage.Role.USER }
        if (userIndex < 0) return

        _messages.value = current.filterNot { it.id == errorMessage.id }
        request(current[userIndex].content, current.subList(0, userIndex))
    }

    private fun request(
        message: String,
        history: List<ChatMessage>,
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = chatRepository.send(message, history)) {
                is ChatResult.Success -> append(ChatMessage.Role.ASSISTANT, result.reply)
                is ChatResult.Failure -> append(ChatMessage.Role.ASSISTANT, "", result.error)
            }
            _isLoading.value = false
        }
    }

    private fun append(
        role: ChatMessage.Role,
        content: String,
        error: ChatError? = null,
    ) {
        val message = ChatMessage(id = nextId++, role = role, content = content, error = error)
        _messages.update { it + message }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer { AiAssistantViewModel(ServiceLocator.chatRepository) }
            }
    }
}
