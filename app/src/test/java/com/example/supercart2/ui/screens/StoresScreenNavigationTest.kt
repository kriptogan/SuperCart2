package com.example.supercart2.ui.screens

import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.COSTCO
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.dairy
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.install
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class StoresScreenNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private var consumed = 0
    private val editModeStoreId = mutableStateOf<String?>(null)

    @Before
    fun setUp() {
        install(dairy("Milk", COSTCO))
        compose.setContent {
            StoresScreen(editModeStoreId = editModeStoreId.value, onEditModeStoreConsumed = { consumed++ })
        }
    }

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun openCostco() {
        compose.onNodeWithText("Select a Store").performClick()
        compose.onNodeWithText("Costco").performClick()
    }

    @Test
    fun `landing shows title and both option cards`() {
        compose.onNodeWithText("Stores").assertIsDisplayed()
        compose.onNodeWithText("Select a Store").assertIsDisplayed()
        compose.onNodeWithText("Start shopping at a specific store").assertIsDisplayed()
        compose.onNodeWithText("View Groceries").assertIsDisplayed()
        compose.onNodeWithText("Browse all items by category").assertIsDisplayed()
    }

    @Test
    fun `select a store opens the store list and back returns to landing`() {
        compose.onNodeWithText("Select a Store").performClick()
        compose.onNodeWithText("Costco").assertIsDisplayed()
        compose.onNodeWithText("Browse all items by category").assertDoesNotExist()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Browse all items by category").assertIsDisplayed()
    }

    @Test
    fun `selecting a store opens buying mode and back returns to the store list`() {
        openCostco()
        compose.onNodeWithText(LocalDate.now().toString()).assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit Store").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Select a Store").assertIsDisplayed()
        compose.onNodeWithText("Items: 1").assertIsDisplayed()
    }

    @Test
    fun `edit icon opens edit mode and back returns to buying mode, then to the store list`() {
        openCostco()
        compose.onNodeWithContentDescription("Edit Store").performClick()
        compose.onNodeWithText("Edit Mode").assertIsDisplayed()
        compose.onNodeWithText(LocalDate.now().toString()).assertDoesNotExist()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Edit Mode").assertDoesNotExist()
        compose.onNodeWithText(LocalDate.now().toString()).assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Items: 1").assertIsDisplayed()
    }

    @Test
    fun `search text is kept when switching between buying and edit mode, and reset after leaving the store`() {
        openCostco()
        compose.onNodeWithText("Search groceries...").performTextInput("mil")
        compose.onNodeWithContentDescription("Edit Store").performClick()
        compose.onNodeWithText("mil").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("mil").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Costco").performClick()
        compose.onNodeWithText("mil").assertDoesNotExist()
    }

    @Test
    fun `editModeStoreId opens edit mode, is consumed once, and back goes to the store list`() {
        editModeStoreId.value = COSTCO
        compose.waitForIdle()
        compose.onNodeWithText("Edit Mode").assertIsDisplayed()
        assertEquals(1, consumed)

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Items: 1").assertIsDisplayed()
        assertEquals(1, consumed)
    }

    @Test
    fun `null editModeStoreId keeps the landing and is not consumed`() {
        compose.waitForIdle()
        assertEquals(0, consumed)
        compose.onNodeWithText("View Groceries").assertIsDisplayed()
    }

    @Test
    fun `view groceries opens the shopping list whose back button returns to landing`() {
        compose.onNodeWithText("View Groceries").performClick()
        compose.onNodeWithText("Browse all items by category").assertDoesNotExist()
        compose.onNodeWithText("Things to Buy").assertIsDisplayed()

        compose.onAllNodes(hasClickAction()).onFirst().performClick()
        compose.onNodeWithText("Browse all items by category").assertIsDisplayed()
    }

    @Test
    fun `store chip in the shopping list opens edit mode whose back goes to the store list`() {
        compose.onNodeWithText("View Groceries").performClick()
        compose.onNodeWithText("Dairy").performClick()
        compose.onAllNodesWithText("Cos").onFirst().performClick()
        compose.onNodeWithText("Edit Mode").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Items: 1").assertIsDisplayed()
    }
}
