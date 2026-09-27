package com.example.cafeshopassignment.ui.assistant

import com.example.cafeshopassignment.data.repository.ChatRepository
import com.example.cafeshopassignment.data.repository.ChatResult
import com.example.cafeshopassignment.models.ChatError
import com.example.cafeshopassignment.models.ChatMessage
import com.example.cafeshopassignment.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AiAssistantViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ChatRepository>()
    private val viewModel by lazy { AiAssistantViewModel(repository) }

    @Test
    fun `sending appends the user turn then the assistant reply`() {
        coEvery { repository.send("What's good today?", emptyList()) } returns ChatResult.Success("The mocha!")

        viewModel.send("  What's good today?  ")

        val messages = viewModel.messages.value
        assertEquals(listOf(ChatMessage.Role.USER, ChatMessage.Role.ASSISTANT), messages.map { it.role })
        assertEquals(listOf("What's good today?", "The mocha!"), messages.map { it.content })
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loading is true while waiting and further sends are ignored`() {
        val reply = CompletableDeferred<ChatResult>()
        coEvery { repository.send(any(), any()) } coAnswers { reply.await() }

        viewModel.send("first")
        viewModel.send("second")

        assertTrue(viewModel.isLoading.value)
        assertEquals(1, viewModel.messages.value.size)

        reply.complete(ChatResult.Success("ok"))
        assertFalse(viewModel.isLoading.value)
        coVerify(exactly = 1) { repository.send(any(), any()) }
    }

    @Test
    fun `blank messages are ignored`() {
        viewModel.send("   ")

        assertTrue(viewModel.messages.value.isEmpty())
        coVerify(exactly = 0) { repository.send(any(), any()) }
    }

    @Test
    fun `failures become an inline error bubble instead of crashing`() {
        coEvery { repository.send(any(), any()) } returns ChatResult.Failure(ChatError.Server(502))

        viewModel.send("hello")

        val last = viewModel.messages.value.last()
        assertEquals(ChatMessage.Role.ASSISTANT, last.role)
        assertEquals(ChatError.Server(502), last.error)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `previous turns are passed as history`() {
        coEvery { repository.send("one", emptyList()) } returns ChatResult.Success("reply one")
        coEvery { repository.send("two", any()) } returns ChatResult.Success("reply two")

        viewModel.send("one")
        viewModel.send("two")

        coVerify {
            repository.send(
                "two",
                match { history -> history.map { it.content } == listOf("one", "reply one") },
            )
        }
    }

    @Test
    fun `retry removes the error bubble and resends the same message`() {
        coEvery { repository.send("hello", emptyList()) } returnsMany
            listOf(ChatResult.Failure(ChatError.Network), ChatResult.Success("Hi there!"))

        viewModel.send("hello")
        val error = viewModel.messages.value.last()
        assertTrue(error.isError)

        viewModel.retry(error)

        assertEquals(listOf("hello", "Hi there!"), viewModel.messages.value.map { it.content })
        assertTrue(viewModel.messages.value.none { it.isError })
        coVerify(exactly = 2) { repository.send("hello", emptyList()) }
    }

    @Test
    fun `retry ignores non-error messages`() {
        coEvery { repository.send(any(), any()) } returns ChatResult.Success("ok")
        viewModel.send("hello")

        viewModel.retry(viewModel.messages.value.last())

        coVerify(exactly = 1) { repository.send(any(), any()) }
    }
}
