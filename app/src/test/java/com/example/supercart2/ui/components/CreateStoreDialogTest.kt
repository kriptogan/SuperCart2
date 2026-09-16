package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.store
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CreateStoreDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0
    private val created = mutableListOf<Store>()

    @Before
    fun setUp() {
        TestData.reset()
        compose.setContent {
            CreateStoreDialog(onDismiss = { dismissed++ }, onStoreCreated = { created += it })
        }
    }

    @Test
    fun `shows title and name field, confirm disabled while name is blank`() {
        compose.onNodeWithText("Create Store").assertIsDisplayed()
        compose.onNodeWithText("Store Name").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create Store").assertIsNotEnabled()
        compose.onNodeWithText("Store Name").performTextInput("   ")
        compose.onNodeWithContentDescription("Create Store").assertIsNotEnabled()
    }

    @Test
    fun `confirm adds a trimmed store after the highest viewOrder and dismisses`() {
        DataManagerObject.stores.addAll(listOf(store("A", viewOrder = 4), store("B", viewOrder = 2)))

        compose.onNodeWithText("Store Name").performTextInput("  Costco ")
        compose.onNodeWithContentDescription("Create Store").assertIsEnabled().performClick()

        val added = DataManagerObject.stores.last()
        assertEquals("Costco", added.name)
        assertEquals(5, added.viewOrder)
        assertEquals(listOf(added), created)
        assertEquals(1, dismissed)
    }

    @Test
    fun `first store gets viewOrder 1`() {
        compose.onNodeWithText("Store Name").performTextInput("Aldi")
        compose.onNodeWithContentDescription("Create Store").performClick()
        assertEquals(1, DataManagerObject.stores.single().viewOrder)
    }

    @Test
    fun `cancel dismisses without creating`() {
        compose.onNodeWithText("Store Name").performTextInput("Aldi")
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertEquals(1, dismissed)
        assertTrue(created.isEmpty())
        assertTrue(DataManagerObject.stores.isEmpty())
    }
}
