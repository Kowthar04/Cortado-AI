package com.example.cafeshopassignment.ui.cart

import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.CartRepository
import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CartViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cart = CartRepository()
    private val auth = mockk<AuthRepository> { every { currentUserId } returns "u1" }
    private val latte = MenuItem(id = "latte", name = "Latte", price = 3.0)

    @Test
    fun `ui state mirrors the cart and its subtotal`() {
        cart.add(latte)
        val viewModel = CartViewModel(cart, auth)

        cart.add(latte)

        val state = viewModel.uiState.value
        assertEquals(2, state.items.single().quantity)
        assertEquals(6.0, state.subtotal, 0.0)
    }

    @Test
    fun `increase and decrease update the shared cart`() {
        cart.add(latte)
        val viewModel = CartViewModel(cart, auth)

        viewModel.increase(
            viewModel.uiState.value.items[0],
        )
        assertEquals(2, cart.items.value[0].quantity)

        viewModel.decrease(
            viewModel.uiState.value.items[0],
        )
        viewModel.decrease(
            viewModel.uiState.value.items[0],
        )
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `checkout with an empty cart reports it`() =
        runTest {
            val viewModel = CartViewModel(cart, auth)

            viewModel.checkout()

            assertEquals(CartEvent.CartEmpty, viewModel.events.first())
        }

    @Test
    fun `checkout requires a signed-in user`() =
        runTest {
            cart.add(latte)
            every { auth.currentUserId } returns null
            val viewModel = CartViewModel(cart, auth)

            viewModel.checkout()

            assertEquals(CartEvent.NotLoggedIn, viewModel.events.first())
        }

    @Test
    fun `checkout proceeds to payment when ready`() =
        runTest {
            cart.add(latte)
            val viewModel = CartViewModel(cart, auth)

            viewModel.checkout()

            assertEquals(CartEvent.ProceedToPayment, viewModel.events.first())
        }
}
