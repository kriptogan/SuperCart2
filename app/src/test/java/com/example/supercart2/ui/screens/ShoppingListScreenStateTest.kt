package com.example.supercart2.ui.screens

import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShoppingListScreenStateTest {

    private val tree = listOf(
        node(
            category("c1", name = "Dairy"),
            subNode(
                sub("s1", "c1"),
                grocery("Milk", "c1", "s1", inShoppingList = true),
                grocery("Cheese", "c1", "s1", inShoppingList = true, isBought = true),
                grocery("Butter", "c1", "s1"),
                grocery("Cream", "c1", "s1", isBought = true), // bought but not in list: ignored
            ),
            subNode(sub("s2", "c1"), grocery("Yogurt", "c1", "s2", inShoppingList = true, isBought = true)),
        ),
        node(category("c2", name = "Bakery"), subNode(sub("s3", "c2"), grocery("Bread", "c2", "s3"))),
    )

    @Before
    fun setUp() = TestData.reset()

    private fun names(lists: List<com.example.supercart2.data.CategoryWithSubCategories>) =
        lists.flatMap { c -> c.subCategories.flatMap { s -> s.groceries.map { it.name } } }

    @Test
    fun `to-buy keeps in-list not-bought groceries and drops emptied branches`() {
        val lists = ShoppingListScreenLists.from(tree, "")
        assertEquals(listOf("Milk"), names(lists.toBuy))
        assertEquals(listOf("c1"), lists.toBuy.map { it.category.uuid })
        assertEquals(listOf("s1"), lists.toBuy.single().subCategories.map { it.subCategory.uuid })
        assertEquals(1, lists.toBuyCount)
    }

    @Test
    fun `bought keeps in-list bought groceries across sub-categories`() {
        val lists = ShoppingListScreenLists.from(tree, "")
        assertEquals(listOf("Cheese", "Yogurt"), names(lists.bought))
        assertEquals(2, lists.boughtCount)
    }

    @Test
    fun `search matches names case-insensitively in both lists`() {
        val lists = ShoppingListScreenLists.from(tree, "yOG")
        assertTrue(lists.toBuy.isEmpty())
        assertEquals(listOf("Yogurt"), names(lists.bought))
        assertEquals(0, lists.toBuyCount)
    }

    @Test
    fun `bought entries pair each bought grocery with its category name in order`() {
        val entries = ShoppingListScreenLists.from(tree, "").boughtEntries
        assertEquals(listOf("Cheese" to "Dairy", "Yogurt" to "Dairy"), entries.map { it.first.name to it.second })
    }

    @Test
    fun `state starts in category mode with nothing open`() {
        val state = ShoppingListScreenState()
        assertEquals(ShoppingListViewMode.CATEGORY, state.viewMode)
        assertFalse(state.isStoreView)
        assertEquals("", state.searchQuery)
        assertFalse(state.isAllExpanded)
        assertFalse(state.showGroceryCreation)
        assertNull(state.groceryToEdit)
    }

    @Test
    fun `toggleViewMode flips between category and store`() {
        val state = ShoppingListScreenState()
        state.toggleViewMode()
        assertEquals(ShoppingListViewMode.STORE, state.viewMode)
        assertTrue(state.isStoreView)
        state.toggleViewMode()
        assertEquals(ShoppingListViewMode.CATEGORY, state.viewMode)
    }

    @Test
    fun `viewModeFor maps the saved flag`() {
        assertEquals(ShoppingListViewMode.STORE, ShoppingListScreenState.viewModeFor(true))
        assertEquals(ShoppingListViewMode.CATEGORY, ShoppingListScreenState.viewModeFor(false))
    }

    @Test
    fun `editGrocery opens the dialog in edit mode, createGrocery resets it, dismiss clears all`() {
        val state = ShoppingListScreenState()
        val milk = grocery("Milk", "c1", "s1")
        state.editGrocery(milk)
        assertSame(milk, state.groceryToEdit)
        assertTrue(state.isEditMode)
        assertTrue(state.showGroceryCreation)

        state.createGrocery()
        assertNull(state.groceryToEdit)
        assertFalse(state.isEditMode)
        assertTrue(state.showGroceryCreation)

        state.editGrocery(milk)
        state.dismissGroceryDialog()
        assertNull(state.groceryToEdit)
        assertFalse(state.isEditMode)
        assertFalse(state.showGroceryCreation)
    }
}
