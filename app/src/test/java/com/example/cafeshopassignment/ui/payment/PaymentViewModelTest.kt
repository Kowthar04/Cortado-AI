package com.example.cafeshopassignment.ui.payment

import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.CartRepository
import com.example.cafeshopassignment.data.repository.NewOrder
import com.example.cafeshopassignment.data.repository.OrderRepository
import com.example.cafeshopassignment.data.repository.UserRepository
import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.models.User
import com.example.cafeshopassignment.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PaymentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cart = CartRepository()
    private val orderRepository = mockk<OrderRepository>()
    private val userRepository =
        mockk<UserRepository> {
            coEvery { getUser("u1") } returns
                User(uid = "u1", firstname = "Sam", surname = "Lee")
        }
    private val authRepository =
        mockk<AuthRepository> {
            every { currentUserId } returns "u1"
            every { currentUserDisplayName } returns null
            every { currentUserEmail } returns "sam@example.com"
        }
    private val card = CardDetails("4242424242424242", "Sam Lee", "1229", "123")

    @Before
    fun fillCart() {
        cart.add(MenuItem(id = "latte", name = "Latte", price = 4.0))
        cart.add(MenuItem(id = "latte", name = "Latte", price = 4.0))
    }

    private fun viewModel() = PaymentViewModel(cart, orderRepository, userRepository, authRepository)

    @Test
    fun `summary is computed from the cart`() {
        val summary = viewModel().uiState.value.summary

        assertEquals(8.0, summary.subtotal, 0.0)
        assertEquals(8.5, summary.total, 0.0001)
    }

    @Test
    fun `valid promo lowers the total, invalid promo restores it`() =
        runTest {
            val vm = viewModel()

            vm.applyPromo("thirsty")
            assertEquals(PaymentEvent.PromoApplied, vm.events.first())
            assertEquals(6.8, vm.uiState.value.summary.total, 0.0001)

            vm.applyPromo("nope")
            assertEquals(PaymentEvent.PromoInvalid, vm.events.first())
            assertEquals(8.5, vm.uiState.value.summary.total, 0.0001)
        }

    @Test
    fun `invalid card details are rejected before any write`() =
        runTest {
            val vm = viewModel()

            vm.pay(card.copy(cvv = "1"))

            assertEquals(PaymentEvent.InvalidCard(CardError.INVALID_CVV), vm.events.first())
            coVerify(exactly = 0) { orderRepository.placeOrder(any()) }
        }

    @Test
    fun `card details are not required for Google Pay`() =
        runTest {
            coEvery { orderRepository.placeOrder(any()) } returns "order-1"
            val vm = viewModel()
            vm.selectMethod(PaymentMethod.GOOGLE_PAY)

            vm.pay(CardDetails("", "", "", ""))

            assertTrue(vm.events.first() is PaymentEvent.OrderPlaced)
        }

    @Test
    fun `successful payment places the order, clears the cart and navigates`() =
        runTest {
            val placed = slot<NewOrder>()
            coEvery { orderRepository.placeOrder(capture(placed)) } returns "order-1"
            val vm = viewModel()

            vm.pay(card)

            assertEquals(PaymentEvent.OrderPlaced("order-1", 8.5), vm.events.first())
            assertTrue(cart.items.value.isEmpty())
            assertEquals("Sam Lee", placed.captured.customerName)
            assertEquals(PaymentMethod.CARD.label, placed.captured.paymentMethod)
            assertEquals(2, placed.captured.items[0].quantity)
            assertFalse(vm.uiState.value.isProcessing)
        }

    @Test
    fun `failed payment keeps the cart and reports the error`() =
        runTest {
            coEvery { orderRepository.placeOrder(any()) } throws RuntimeException("offline")
            val vm = viewModel()

            vm.pay(card)

            assertEquals(PaymentEvent.Failed("offline"), vm.events.first())
            assertEquals(2, cart.items.value[0].quantity)
            assertFalse(vm.uiState.value.isProcessing)
        }

    @Test
    fun `double tapping pay only places one order`() =
        runTest {
            val gate = CompletableDeferred<String>()
            coEvery { orderRepository.placeOrder(any()) } coAnswers { gate.await() }
            val vm = viewModel()

            vm.pay(card)
            vm.pay(card)
            assertTrue(vm.uiState.value.isProcessing)
            gate.complete("order-1")

            coVerify(exactly = 1) { orderRepository.placeOrder(any()) }
        }

    @Test
    fun `empty cart cannot be paid for`() =
        runTest {
            cart.clear()
            val vm = viewModel()

            vm.pay(card)

            assertEquals(PaymentEvent.CartEmpty, vm.events.first())
        }
}
