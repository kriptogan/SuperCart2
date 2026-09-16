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
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CreateCategoryDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val created = mutableListOf<Pair<Category, SubCategory>>()

    @Before
    fun setUp() {
        TestData.reset()
    }

    private fun show() {
        compose.setContent {
            CreateCategoryDialog(
                onDismiss = { events += "dismiss" },
                onCategoryCreated = { c, s -> created += c to s; events += "created" },
            )
        }
    }

    @Test
    fun `shows title and name field, confirm disabled while name is blank`() {
        show()
        compose.onNodeWithText("Create Category").assertIsDisplayed()
        compose.onNodeWithText("Category Name").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel").assertIsEnabled()
        compose.onNodeWithContentDescription("Create Category").assertIsNotEnabled()
        compose.onNodeWithText("Category Name").performTextInput("  ")
        compose.onNodeWithContentDescription("Create Category").assertIsNotEnabled()
    }

    @Test
    fun `confirm builds trimmed category after highest viewOrder with protected General sub, then dismisses`() {
        DataManagerObject.categories.addAll(listOf(node(category("a", viewOrder = 4)), node(category("b", viewOrder = 2))))
        show()

        compose.onNodeWithText("Category Name").performTextInput("  Dairy ")
        compose.onNodeWithContentDescription("Create Category").assertIsEnabled().performClick()

        val (cat, sub) = created.single()
        assertEquals("Dairy", cat.name)
        assertEquals(5, cat.viewOrder)
        assertFalse(cat.default)
        assertFalse(cat.protected)
        assertEquals("General", sub.name)
        assertEquals(cat.uuid, sub.categoryId)
        assertTrue(sub.protected)
        assertEquals(listOf("created", "dismiss"), events)
        assertEquals("dialog itself does not add to DataManagerObject", 2, DataManagerObject.categories.size)
    }

    @Test
    fun `first category gets viewOrder 1`() {
        show()
        compose.onNodeWithText("Category Name").performTextInput("Dairy")
        compose.onNodeWithContentDescription("Create Category").performClick()
        assertEquals(1, created.single().first.viewOrder)
    }

    @Test
    fun `pinned - deleted categories still count for the highest viewOrder`() {
        DataManagerObject.categories.add(node(category("gone", viewOrder = 9, deleted = true)))
        show()
        compose.onNodeWithText("Category Name").performTextInput("Dairy")
        compose.onNodeWithContentDescription("Create Category").performClick()
        assertEquals(10, created.single().first.viewOrder)
    }

    @Test
    fun `cancel dismisses without creating`() {
        show()
        compose.onNodeWithText("Category Name").performTextInput("Dairy")
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertEquals(listOf("dismiss"), events)
        assertTrue(created.isEmpty())
    }
}
