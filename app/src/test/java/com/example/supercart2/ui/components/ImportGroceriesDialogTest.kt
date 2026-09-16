package com.example.supercart2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
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
class ImportGroceriesDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        TestData.installStandardTree()
        DataManagerObject.categories.add(
            node(category("other", viewOrder = 0, name = "Other"), subNode(sub("general", "other", name = "General")))
        )
        compose.setContent {
            ImportGroceriesDialog(onDismiss = { events += "dismiss" }, onImportComplete = { events += "complete" })
        }
    }

    private fun all(): List<Grocery> =
        DataManagerObject.categories.flatMap { it.subCategories }.flatMap { it.groceries }
    private fun named(name: String) = all().filter { it.name == name }

    private fun typeText(text: String) = compose.onNode(hasSetTextAction()).performTextInput(text)

    private fun import(text: String) {
        typeText(text)
        compose.onNodeWithText("Import").performClick()
    }

    @Test
    fun `shows title, prompt and disables import while the text is blank`() {
        compose.onNodeWithText("Import Groceries").assertIsDisplayed()
        compose.onNodeWithText("Enter grocery names (one per line):").assertIsDisplayed()
        compose.onNodeWithText("Import").assertIsNotEnabled()
        typeText(" \n  ")
        compose.onNodeWithText("Import").assertIsNotEnabled()
        typeText("Milk")
        compose.onNodeWithText("Import").assertIsEnabled()
    }

    @Test
    fun `cancel dismisses without importing`() {
        typeText("Milk")
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("dismiss"), events)
        assertTrue(named("Milk").isEmpty())
    }

    @Test
    fun `missing names are created under Other - General, trimmed, blank lines skipped, then asks`() {
        import("  Milk \r\n\n Bread\n")
        assertEquals("other" to "general", TestData.locationOf(named("Milk").single().uuid))
        assertEquals("other" to "general", TestData.locationOf(named("Bread").single().uuid))
        assertFalse(named("Milk").single().inShoppingList)
        compose.onNodeWithText("Add to Shopping List?").assertIsDisplayed()
        compose.onNodeWithText("Should we add the items to the shopping list?").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `answering yes adds new and existing groceries to the shopping list`() {
        DataManagerObject.updateGrocery("g2") { it.copy(inShoppingList = true, isBought = true) }
        import("Milk\ng1\ng2")
        compose.onNodeWithText("Yes").performClick()

        assertTrue(named("Milk").single().inShoppingList)
        assertTrue(TestData.findGrocery("g1")!!.inShoppingList)
        val g2 = TestData.findGrocery("g2")!!
        assertTrue("already listed items are not toggled off", g2.inShoppingList)
        assertTrue("and keep their bought flag", g2.isBought)
        assertEquals(listOf("complete"), events)
        compose.onNodeWithText("Add to Shopping List?").assertDoesNotExist()
    }

    @Test
    fun `answering no leaves the shopping list alone but keeps created groceries`() {
        import("Milk\ng1")
        compose.onNodeWithText("No").performClick()
        assertFalse(named("Milk").single().inShoppingList)
        assertFalse(TestData.findGrocery("g1")!!.inShoppingList)
        assertEquals(listOf("complete"), events)
    }

    @Test
    fun `existing names are not duplicated and a repeated new name is created once`() {
        import("g1\nMilk\nMilk")
        assertEquals(1, named("g1").size)
        assertEquals(1, named("Milk").size)
        compose.onNodeWithText("Yes").performClick()
        assertTrue(named("Milk").single().inShoppingList)
    }

    @Test
    fun `every grocery with an existing name is added, in any category`() {
        DataManagerObject.addGrocery(grocery("dup", "c2", "s3").copy(name = "g1"))
        import("g1")
        compose.onNodeWithText("Yes").performClick()
        assertTrue(named("g1").all { it.inShoppingList })
        assertEquals(2, named("g1").size)
    }

    @Test
    fun `a deleted grocery with the same name counts as existing (pinned)`() {
        DataManagerObject.updateGrocery("g3") { it.copy(deleted = true) }
        import("g3")
        compose.onNodeWithText("Yes").performClick()
        assertEquals(1, named("g3").size)
        assertTrue(TestData.findGrocery("g3")!!.inShoppingList)
    }

    @Test
    fun `missing Other category and General sub-category are created`() {
        DataManagerObject.categories.removeAll { it.category.name == "Other" }
        import("Milk")
        val other = DataManagerObject.categories.single { it.category.name == "Other" }
        assertEquals(listOf("General"), other.subCategories.map { it.subCategory.name })
        assertEquals(other.category.uuid, named("Milk").single().categoryId)
        assertEquals(other.subCategories.single().subCategory.uuid, named("Milk").single().subCategoryId)
    }

    @Test
    fun `missing General sub-category is added to the existing Other category`() {
        DataManagerObject.categories.removeAll { it.category.name == "Other" }
        DataManagerObject.categories.add(node(category("other", name = "Other"), subNode(sub("misc", "other"))))
        import("Milk")
        val other = DataManagerObject.categories.single { it.category.name == "Other" }
        assertEquals(listOf("misc", "General"), other.subCategories.map { it.subCategory.name })
        val milkSub = other.subCategories.first { it.subCategory.uuid == named("Milk").single().subCategoryId }
        assertEquals("General", milkSub.subCategory.name)
    }
}
