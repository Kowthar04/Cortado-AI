package com.example.cafeshopassignment.ui.menu

import com.example.cafeshopassignment.data.repository.AuthRepository
import com.example.cafeshopassignment.data.repository.CartRepository
import com.example.cafeshopassignment.data.repository.MenuRepository
import com.example.cafeshopassignment.data.repository.UserRepository
import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.models.User
import com.example.cafeshopassignment.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MenuViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val latte = MenuItem(id = "1", name = "Latte", category = "Drinks", price = 3.5)
    private val soldOutMocha = MenuItem(id = "2", name = "Mocha", category = "Drinks", price = 3.8, availability = false)
    private val toastie = MenuItem(id = "3", name = "Toastie", category = "Lunch", price = 5.0)

    private val menuRepository = mockk<MenuRepository> { coEvery { getMenuItems() } returns listOf(latte, soldOutMocha, toastie) }
    private val userRepository = mockk<UserRepository> { coEvery { getUser("u1") } returns User(uid = "u1", firstname = "Sam") }
    private val authRepository =
        mockk<AuthRepository>(relaxed = true) {
            every { currentUserId } returns "u1"
        }
    private val cart = CartRepository()

    private fun viewModel() = MenuViewModel(menuRepository, userRepository, authRepository, cart)

    @Test
    fun `loads the menu once and shows available items of the first category`() {
        val vm = viewModel()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Drinks", state.selectedCategory)
        assertEquals(listOf(latte), state.items)
        assertEquals("Sam", state.firstName)
    }

    @Test
    fun `switching category filters locally without re-querying`() {
        val vm = viewModel()

        vm.selectCategory("Lunch")

        assertEquals(listOf(toastie), vm.uiState.value.items)
        coVerify(exactly = 1) { menuRepository.getMenuItems() }
    }

    @Test
    fun `empty category is reported as empty state`() {
        val vm = viewModel()

        vm.selectCategory("Breakfast")

        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `load failure is exposed and can be retried`() {
        coEvery { menuRepository.getMenuItems() } throws RuntimeException("offline")
        val vm = viewModel()
        assertEquals("offline", vm.uiState.value.loadError)

        coEvery { menuRepository.getMenuItems() } returns listOf(latte)
        vm.loadMenu()

        assertNull(vm.uiState.value.loadError)
        assertEquals(listOf(latte), vm.uiState.value.items)
    }

    @Test
    fun `addToCart puts the item in the shared cart and confirms`() =
        runTest {
            val vm = viewModel()

            vm.addToCart(latte)

            assertEquals(1, cart.items.value[0].quantity)
            assertEquals(MenuEvent.AddedToCart("Latte"), vm.events.first())
        }

    @Test
    fun `logout signs out through the auth repository`() =
        runTest {
            val vm = viewModel()

            vm.logout()

            verify { authRepository.signOut() }
            assertEquals(MenuEvent.LoggedOut, vm.events.first())
        }

    @Test
    fun `profile lookup failure falls back to a generic greeting`() {
        coEvery { userRepository.getUser(any()) } throws RuntimeException("offline")

        assertNull(viewModel().uiState.value.firstName)
    }
}
