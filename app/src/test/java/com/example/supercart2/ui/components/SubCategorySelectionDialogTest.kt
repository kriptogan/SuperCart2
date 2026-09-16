package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
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
class SubCategorySelectionDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val created = mutableListOf<SubCategory>()
    private val cat = category("c1", name = "Food")

    @Before
    fun setUp() {
        TestData.reset()
        DataManagerObject.categories.add(
            node(
                cat,
                subNode(sub("s-z", "c1", name = "Zeta")),
                subNode(sub("s-g", "c1", name = "General")),
                subNode(sub("s-d", "c1", name = "Dropped", deleted = true)),
            )
        )
    }

    private fun show(selectedCategory: Category = cat) {
        compose.setContent {
            SubCategorySelectionDialog(
                onDismiss = { events += "dismiss" },
                onSubCategorySelected = { events += "selected:${it.uuid}" },
                onNewSubCategoryCreated = { s ->
                    created += s
                    val inData = DataManagerObject.categories[0].subCategories.any { it.subCategory.uuid == s.uuid }
                    events += "created:${s.name}/inData=$inData"
                },
                selectedCategory = selectedCategory,
                selectedSubCategoryId = "s-g",
            )
        }
    }

    @Test
    fun `lists non-deleted sub-categories in data order`() {
        show()
        compose.onNodeWithText("Select Sub-Category").assertIsDisplayed()
        val zeta = compose.onNodeWithText("Zeta").fetchSemanticsNode().boundsInRoot.top
        val general = compose.onNodeWithText("General").fetchSemanticsNode().boundsInRoot.top
        assertTrue(zeta < general)
        compose.onNodeWithText("Dropped").assertDoesNotExist()
        compose.onNodeWithContentDescription("Close").assertIsDisplayed()
        compose.onNodeWithContentDescription("New Sub-Category").assertIsDisplayed()
    }

    @Test
    fun `unknown category shows an empty list`() {
        show(category("nope"))
        compose.onNodeWithText("Select Sub-Category").assertIsDisplayed()
        compose.onNodeWithText("Zeta").assertDoesNotExist()
    }

    @Test
    fun `clicking a sub-category reports it without dismissing`() {
        show()
        compose.onNodeWithText("Zeta").performClick()
        assertEquals(listOf("selected:s-z"), events)
    }

    @Test
    fun `close dismisses`() {
        show()
        compose.onNodeWithContentDescription("Close").performClick()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun `new sub-category opens create dialog, cancel closes it`() {
        show()
        compose.onNodeWithContentDescription("New Sub-Category").performClick()
        compose.onNodeWithText("Sub-Category Name").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Sub-Category Name").assertDoesNotExist()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `created sub-category (trimmed, unprotected) is added before the callback and shown`() {
        show()
        val versionBefore = DataManagerObject.version
        compose.onNodeWithContentDescription("New Sub-Category").performClick()
        compose.onNodeWithText("Sub-Category Name").performTextInput(" Milk ")
        compose.onNodeWithContentDescription("Create Sub-Category").performClick()

        assertEquals(listOf("created:Milk/inData=true"), events)
        val s = created.single()
        assertEquals("c1", s.categoryId)
        assertFalse(s.protected)
        assertEquals(s.uuid, DataManagerObject.categories[0].subCategories.last().subCategory.uuid)
        assertEquals(versionBefore + 1, DataManagerObject.version)
        compose.onNodeWithText("Sub-Category Name").assertDoesNotExist()
        assertEquals("Milk", s.name)
        compose.onNodeWithText("Milk").assertIsDisplayed()
    }
}
