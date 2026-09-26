package com.example.cafeshopassignment.ui.admin

import com.example.cafeshopassignment.data.repository.MenuRepository
import com.example.cafeshopassignment.models.MenuItem
import com.example.cafeshopassignment.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ManageMenuViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val latte = MenuItem(id = "1", name = "Latte", category = "Drinks", price = 3.5)
    private val menuRepository =
        mockk<MenuRepository>(relaxed = true) {
            coEvery { getMenuItems() } returns listOf(latte, MenuItem(id = "2", name = "Soup", category = "Lunch", price = 4.0))
        }

    @Test
    fun `parseInput normalises valid input`() {
        val result = ManageMenuViewModel.parseInput(" Flat White ", "3.20", "drinks")

        assertEquals(ManageMenuViewModel.InputResult.Valid(MenuItemInput("Flat White", 3.2, "Drinks")), result)
    }

    @Test
    fun `parseInput rejects missing fields, bad prices and unknown categories`() {
        assertEquals(invalid(MenuInputError.MISSING_FIELDS), ManageMenuViewModel.parseInput("", "3", "Drinks"))
        assertEquals(invalid(MenuInputError.INVALID_PRICE), ManageMenuViewModel.parseInput("Tea", "abc", "Drinks"))
        assertEquals(invalid(MenuInputError.INVALID_PRICE), ManageMenuViewModel.parseInput("Tea", "0", "Drinks"))
        assertEquals(invalid(MenuInputError.UNKNOWN_CATEGORY), ManageMenuViewModel.parseInput("Tea", "2", "Drnks"))
    }

    @Test
    fun `items are grouped into every known category`() {
        val state = ManageMenuViewModel(menuRepository).uiState.value

        assertEquals(listOf("Drinks", "Breakfast", "Lunch", "Pastries & Sweets"), state.itemsByCategory.keys.toList())
        assertEquals(listOf(latte), state.itemsByCategory["Drinks"])
        assertEquals(emptyList<MenuItem>(), state.itemsByCategory["Breakfast"])
    }

    @Test
    fun `delete failure is reported instead of failing silently`() =
        runTest {
            coEvery { menuRepository.deleteMenuItem("1") } throws RuntimeException("permission denied")
            val vm = ManageMenuViewModel(menuRepository)

            vm.deleteItem(latte)

            assertEquals(ManageMenuEvent.OperationFailed("permission denied"), vm.events.first())
        }

    @Test
    fun `items without an id are never sent to Firestore`() =
        runTest {
            val vm = ManageMenuViewModel(menuRepository)

            vm.deleteItem(latte.copy(id = null))

            assertEquals(ManageMenuEvent.OperationFailed(null), vm.events.first())
            coVerify(exactly = 0) { menuRepository.deleteMenuItem(any()) }
        }

    @Test
    fun `successful add reloads the menu`() =
        runTest {
            val vm = ManageMenuViewModel(menuRepository)

            vm.addItem("Scone", "2.5", "pastries & sweets")

            coVerify { menuRepository.addMenuItem("Scone", 2.5, "Pastries & Sweets") }
            assertEquals(ManageMenuEvent.ItemAdded, vm.events.first())
            coVerify(exactly = 2) { menuRepository.getMenuItems() }
        }

    private fun invalid(error: MenuInputError) = ManageMenuViewModel.InputResult.Invalid(error)
}
