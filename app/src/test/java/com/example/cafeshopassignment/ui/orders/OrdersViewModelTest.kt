package com.example.cafeshopassignment.ui.orders

import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.OrderRepository
import com.example.cafeshopassignment.data.repository.UserRepository
import com.example.cafeshopassignment.models.Order
import com.example.cafeshopassignment.models.OrderStatus
import com.example.cafeshopassignment.models.User
import com.example.cafeshopassignment.models.UserRole
import com.example.cafeshopassignment.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pending = Order(id = "1", userId = "u1", customerName = "Amina Ali", status = "Pending")
    private val preparing = Order(id = "2", userId = "u2", customerName = "Ben Cole", status = "Preparing")
    private val completed = Order(id = "3", userId = "u1", customerName = "Amina Ali", status = "Completed")

    private val orderRepository = mockk<OrderRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "u1" }

    private fun signedInAs(role: UserRole) {
        coEvery { userRepository.getUser("u1") } returns User(uid = "u1", role = role)
    }

    @Test
    fun `filterOrders matches name or user id case-insensitively and by status`() {
        val all = listOf(pending, preparing, completed)

        assertEquals(listOf(pending, completed), OrdersViewModel.filterOrders(all, "amina", null))
        assertEquals(listOf(preparing), OrdersViewModel.filterOrders(all, "U2", null))
        assertEquals(listOf(completed), OrdersViewModel.filterOrders(all, "amina", OrderStatus.COMPLETED))
        assertEquals(all, OrdersViewModel.filterOrders(all, "  ", null))
    }

    @Test
    fun `customers get a live feed of only their own orders`() =
        runTest {
            signedInAs(UserRole.CUSTOMER)
            val live = MutableStateFlow(listOf(pending))
            every { orderRepository.observeOrdersForUser("u1") } returns live
            val vm = OrdersViewModel(orderRepository, userRepository, authRepository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

            assertFalse(vm.uiState.value.isAdmin)
            assertEquals(listOf(pending), vm.uiState.value.orders)

            // Staff moves the order along: the customer's screen updates without a reload.
            live.value = listOf(pending.copy(status = "Preparing"))
            assertEquals(
                OrderStatus.PREPARING,
                vm.uiState.value.orders[0]
                    .orderStatus,
            )
            verify(exactly = 0) { orderRepository.observeAllOrders() }
        }

    @Test
    fun `admins see every order and filters apply on top of the live list`() =
        runTest {
            signedInAs(UserRole.ADMIN)
            every { orderRepository.observeAllOrders() } returns MutableStateFlow(listOf(pending, preparing, completed))
            val vm = OrdersViewModel(orderRepository, userRepository, authRepository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

            assertTrue(vm.uiState.value.isAdmin)
            vm.setStatusFilter(OrderStatus.PREPARING)
            assertEquals(listOf(preparing), vm.uiState.value.orders)
            assertEquals(3, vm.uiState.value.totalCount)

            vm.setStatusFilter(null)
            vm.setQuery("ben")
            assertEquals(listOf(preparing), vm.uiState.value.orders)
        }

    @Test
    fun `listener errors surface as a load error`() =
        runTest {
            signedInAs(UserRole.CUSTOMER)
            every { orderRepository.observeOrdersForUser("u1") } returns flow { throw IllegalStateException("index missing") }
            val vm = OrdersViewModel(orderRepository, userRepository, authRepository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

            assertEquals(OrdersError.LoadFailed("index missing"), vm.uiState.value.error)
        }

    @Test
    fun `signed-out users get a not-signed-in error`() =
        runTest {
            every { authRepository.currentUserId } returns null
            val vm = OrdersViewModel(orderRepository, userRepository, authRepository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

            assertEquals(OrdersError.NotSignedIn, vm.uiState.value.error)
        }

    @Test
    fun `admins can update status and get confirmation`() =
        runTest {
            signedInAs(UserRole.ADMIN)
            every { orderRepository.observeAllOrders() } returns MutableStateFlow(listOf(pending))
            val vm = OrdersViewModel(orderRepository, userRepository, authRepository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

            vm.updateStatus(pending, OrderStatus.PREPARING)

            coVerify { orderRepository.updateStatus(pending, OrderStatus.PREPARING) }
            assertEquals(OrdersEvent.StatusUpdated(OrderStatus.PREPARING), vm.events.first())
        }

    @Test
    fun `customers cannot update status`() =
        runTest {
            signedInAs(UserRole.CUSTOMER)
            every { orderRepository.observeOrdersForUser("u1") } returns MutableStateFlow(listOf(pending))
            val vm = OrdersViewModel(orderRepository, userRepository, authRepository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

            vm.updateStatus(pending, OrderStatus.COMPLETED)

            coVerify(exactly = 0) { orderRepository.updateStatus(any(), any()) }
        }

    @Test
    fun `status update failures are reported`() =
        runTest {
            signedInAs(UserRole.ADMIN)
            every { orderRepository.observeAllOrders() } returns MutableStateFlow(listOf(pending))
            coEvery { orderRepository.updateStatus(any(), any()) } throws RuntimeException("offline")
            val vm = OrdersViewModel(orderRepository, userRepository, authRepository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

            vm.updateStatus(pending, OrderStatus.PREPARING)

            assertEquals(OrdersEvent.StatusUpdateFailed("offline"), vm.events.first())
        }
}
