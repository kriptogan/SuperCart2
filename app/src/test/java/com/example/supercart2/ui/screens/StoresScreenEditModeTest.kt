package com.example.supercart2.ui.screens

import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.ALDI
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.COSTCO
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.bakery
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.dairy
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.install
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "h1200dp")
class StoresScreenEditModeTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun openEditMode(storeId: String = COSTCO) {
        compose.setContent { StoresScreen(editModeStoreId = storeId) }
        compose.waitForIdle()
    }

    private fun costco() = DataManagerObject.stores.first { it.uuid == COSTCO }

    @Test
    fun `header and store info card replace the buying header`() {
        install(dairy("Milk", COSTCO))
        openEditMode()
        compose.onNodeWithText("Edit Mode").assertIsDisplayed()
        compose.onNodeWithText("Store Info").assertIsDisplayed()
        compose.onNodeWithText("Store Name").assertTextContains("Costco")
        compose.onNodeWithText("Store Address").assertTextContains("Main St")
        compose.onNodeWithText("Reorder Categories").assertIsDisplayed()
        compose.onNodeWithText("Confirm").assertIsEnabled()
        compose.onNodeWithText("Search groceries...").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add New Grocery").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit Store").assertDoesNotExist()
        compose.onNodeWithText(LocalDate.now().toString()).assertDoesNotExist()
    }

    @Test
    fun `lists every grocery linked to the store without section labels`() {
        install(
            dairy("Milk", COSTCO), dairy("Cheese", COSTCO, inList = false), dairy("Cream", COSTCO, bought = true),
            dairy("Butter", ALDI), bakery("Bread", ALDI),
        )
        openEditMode()
        compose.onNodeWithText("Things to Buy").assertDoesNotExist()
        compose.onNodeWithText("Already Bought").assertDoesNotExist()
        compose.onNodeWithText("Bakery").assertDoesNotExist()
        compose.onNodeWithText("3 items").assertIsDisplayed()

        compose.onNodeWithText("Dairy").performClick()
        compose.onNodeWithText("Milk").assertIsDisplayed()
        compose.onNodeWithText("Cheese").assertIsDisplayed()
        compose.onNodeWithText("Cream").assertIsDisplayed()
        compose.onNodeWithText("Butter").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mark as bought").assertDoesNotExist()
    }

    @Test
    fun `store without linked groceries shows the empty card`() {
        install(dairy("Milk", ALDI))
        openEditMode()
        compose.onNodeWithText("No items to buy").assertIsDisplayed()
    }

    @Test
    fun `search filters the linked groceries`() {
        install(dairy("Milk", COSTCO), bakery("Bread", COSTCO))
        openEditMode()
        compose.onNodeWithText("Search groceries...").performTextInput("bre")
        compose.onNodeWithText("Dairy").assertDoesNotExist()
        compose.onNodeWithText("Bakery").assertIsDisplayed()
    }

    @Test
    fun `blank store name disables confirm`() {
        install(dairy("Milk", COSTCO))
        openEditMode()
        compose.onNodeWithText("Store Name").performTextClearance()
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
        compose.onNodeWithText("Store Name").performTextInput("   ")
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
    }

    @Test
    fun `confirm saves trimmed name and address and shows the done snackbar`() {
        install(dairy("Milk", COSTCO))
        openEditMode()
        compose.onNodeWithText("Store Name").performTextClearance()
        compose.onNodeWithText("Store Name").performTextInput("  Costco Wholesale ")
        compose.onNodeWithText("Store Address").performTextClearance()
        compose.onNodeWithText("Store Address").performTextInput(" Elm St  ")
        compose.onNodeWithText("Confirm").performClick()

        assertEquals("Costco Wholesale", costco().name)
        assertEquals("Elm St", costco().address)
        assertTrue(costco().lastUpdate.isAfter(TestData.OLD))
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Done").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Edit Mode").assertIsDisplayed()
        compose.onNodeWithText("Store Name").assertTextContains("Costco Wholesale")
    }

    @Test
    fun `unconfirmed edits are not saved`() {
        install(dairy("Milk", COSTCO))
        openEditMode()
        compose.onNodeWithText("Store Name").performTextInput("X")
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals("Costco", costco().name)
        assertEquals(TestData.OLD, costco().lastUpdate)
    }

    @Test
    fun `unknown store id shows empty fields and confirm stays disabled, nothing saved and no done`() {
        install(dairy("Milk", COSTCO))
        openEditMode("missing")
        compose.onNodeWithText("Edit Mode").assertIsDisplayed()
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
        compose.onNodeWithText("Store Name").performTextInput("Ghost")
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
        compose.onNodeWithText("Confirm").performClick()
        compose.waitForIdle()
        assertEquals(2, DataManagerObject.stores.size)
        compose.onAllNodesWithText("Done").assertCountEquals(0)
        compose.onNodeWithText("Reorder Categories").performClick()
        compose.onNodeWithText("Reorder Categories for", substring = true).assertDoesNotExist()
    }

    @Test
    fun `reorder categories opens the store category order dialog and cancel closes it`() {
        install(dairy("Milk", COSTCO))
        openEditMode()
        compose.onNodeWithText("Reorder Categories").performClick()
        compose.onNodeWithText("Reorder Categories for Costco").assertIsDisplayed()

        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Reorder Categories for Costco").assertDoesNotExist()
    }

    @Test
    fun `saving the reorder dialog closes it`() {
        install(dairy("Milk", COSTCO))
        openEditMode()
        compose.onNodeWithText("Reorder Categories").performClick()
        compose.onNodeWithContentDescription("Save Order").performClick()
        compose.onNodeWithText("Reorder Categories for Costco").assertDoesNotExist()
    }

    @Test
    fun `add grocery links the store but does not add it to the shopping list`() {
        install(dairy("Milk", COSTCO))
        openEditMode()
        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithText("Grocery Name").performTextInput("Kefir")
        compose.onNodeWithContentDescription("Create Grocery").performClick()

        val kefir = DataManagerObject.categories[0].subCategories[0].groceries.last()
        assertEquals("Kefir", kefir.name)
        assertEquals(listOf(COSTCO), kefir.storeIds)
        assertFalse(kefir.inShoppingList)
        compose.onNodeWithText("2 items").assertIsDisplayed()
    }

    @Test
    fun `edit item opens the edit dialog and saving updates the grocery`() {
        install(dairy("Milk", COSTCO))
        openEditMode()
        compose.onNodeWithText("Dairy").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Edit item").performClick()
        compose.onNodeWithText("Edit Grocery").assertIsDisplayed()
        compose.onNodeWithText("Grocery Name").performTextClearance()
        compose.onNodeWithText("Grocery Name").performTextInput("Milk 3%")
        compose.onNodeWithContentDescription("Save Changes").performClick()

        compose.onNodeWithText("Edit Grocery").assertDoesNotExist()
        assertEquals("Milk 3%", findGrocery("Milk")!!.name)
    }

    @Test
    fun `after editing a grocery the add button opens a create dialog again`() {
        install(dairy("Milk", COSTCO))
        openEditMode()
        compose.onNodeWithText("Dairy").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Edit item").performClick()
        compose.onNodeWithContentDescription("Cancel").performClick()

        compose.onNodeWithContentDescription("Add New Grocery").performClick()
        compose.onNodeWithText("Create New Grocery").assertIsDisplayed()
    }
}
