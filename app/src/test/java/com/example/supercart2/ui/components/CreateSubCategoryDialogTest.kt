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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CreateSubCategoryDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        TestData.reset()
        compose.setContent {
            CreateSubCategoryDialog(onDismiss = { events += "dismiss" }, onSubCategoryCreated = { events += "created:$it" })
        }
    }

    @Test
    fun `shows title and name field, confirm disabled while name is blank`() {
        compose.onNodeWithText("Create New Sub-Category").assertIsDisplayed()
        compose.onNodeWithText("Sub-Category Name").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel").assertIsEnabled()
        compose.onNodeWithContentDescription("Create Sub-Category").assertIsNotEnabled()
        compose.onNodeWithText("Sub-Category Name").performTextInput("   ")
        compose.onNodeWithContentDescription("Create Sub-Category").assertIsNotEnabled()
    }

    @Test
    fun `confirm passes the trimmed name and does not dismiss`() {
        compose.onNodeWithText("Sub-Category Name").performTextInput("  Milk ")
        compose.onNodeWithContentDescription("Create Sub-Category").assertIsEnabled().performClick()
        assertEquals(listOf("created:Milk"), events)
    }

    @Test
    fun `cancel dismisses without creating`() {
        compose.onNodeWithText("Sub-Category Name").performTextInput("Milk")
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertEquals(listOf("dismiss"), events)
    }
}
