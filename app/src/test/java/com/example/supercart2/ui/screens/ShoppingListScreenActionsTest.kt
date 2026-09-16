package com.example.supercart2.ui.screens

import com.example.supercart2.TestData
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ShoppingListScreenActionsTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val state = ShoppingListScreenState()

    @Before
    fun setUp() {
        TestData.installStandardTree()
        mockkObject(DataStoreManager)
        coEvery { DataStoreManager.saveDataGlobally() } returns Unit
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `unmarkBought clears isBought only and saves`() {
        DataManagerObject.updateGrocery("g1") { it.copy(inShoppingList = true, isBought = true) }
        ShoppingListScreenActions.unmarkBought(TestData.findGrocery("g1")!!, scope)
        val g1 = TestData.findGrocery("g1")!!
        assertFalse(g1.isBought)
        assertTrue(g1.inShoppingList)
        coVerify(exactly = 1) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `finishShopping confirms bought items, saves and closes the confirmation`() {
        DataManagerObject.updateGrocery("g3") { it.copy(inShoppingList = true, isBought = true) }
        state.showFinishConfirmation = true
        ShoppingListScreenActions.finishShopping(state, scope)
        val g3 = TestData.findGrocery("g3")!!
        assertFalse(g3.inShoppingList)
        assertEquals(listOf(LocalDate.now()), g3.buyEvents)
        assertFalse(state.showFinishConfirmation)
        coVerify(exactly = 1) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `onGroceryCreated in create mode adds the grocery, saves twice and closes the dialog`() {
        state.createGrocery()
        val eggs = TestData.grocery("eggs", "c2", "s3", inShoppingList = true)
        ShoppingListScreenActions.onGroceryCreated(state, eggs, scope)
        assertEquals("c2" to "s3", TestData.locationOf("eggs"))
        assertFalse(state.showGroceryCreation)
        coVerify(exactly = 2) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `onGroceryCreated in edit mode updates in place and keeps the edit state`() {
        state.editGrocery(TestData.findGrocery("g1")!!)
        ShoppingListScreenActions.onGroceryCreated(state, TestData.findGrocery("g1")!!.copy(name = "new"), scope)
        assertEquals("new", TestData.findGrocery("g1")!!.name)
        assertEquals("c1" to "s1", TestData.locationOf("g1"))
        assertFalse(state.showGroceryCreation)
        assertTrue(state.isEditMode) // not reset, unlike onDismiss
        coVerify(exactly = 2) { DataStoreManager.saveDataGlobally() }
    }

    @Test
    fun `onGroceryCreated in edit mode moves the grocery when its sub-category changed`() {
        val g1 = TestData.findGrocery("g1")!!
        state.editGrocery(g1)
        ShoppingListScreenActions.onGroceryCreated(state, g1.copy(categoryId = "c2", subCategoryId = "s3"), scope)
        assertEquals("c2" to "s3", TestData.locationOf("g1"))
        assertEquals(1, DataManagerObject.categories.flatMap { it.subCategories }.flatMap { it.groceries }
            .count { it.uuid == "g1" })
    }
}
