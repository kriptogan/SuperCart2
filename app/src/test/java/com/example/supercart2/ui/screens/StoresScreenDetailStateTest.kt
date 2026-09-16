package com.example.supercart2.ui.screens

import com.example.supercart2.TestData
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.COSTCO
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.costco
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.dairy
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.install
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoresScreenDetailStateTest {

    private val state = StoresScreenDetailState(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `opening the create dialog clears any grocery being edited`() {
        state.openGroceryEdit(dairy("Milk", COSTCO))
        state.openGroceryCreation()
        assertTrue(state.showGroceryCreation)
        assertFalse(state.isGroceryEditMode)
        assertNull(state.groceryToEdit)
    }

    @Test
    fun `opening and closing the edit dialog`() {
        val milk = dairy("Milk", COSTCO)
        state.openGroceryEdit(milk)
        assertTrue(state.showGroceryCreation)
        assertTrue(state.isGroceryEditMode)
        assertEquals(milk, state.groceryToEdit)

        state.closeGroceryDialog()
        assertFalse(state.showGroceryCreation)
        assertFalse(state.isGroceryEditMode)
        assertNull(state.groceryToEdit)
    }

    @Test
    fun `loading a store fills the edit fields, a missing store empties them`() {
        state.loadStore(costco)
        assertEquals("Costco", state.editStoreName)
        assertEquals("Main St", state.editStoreAddress)
        state.loadStore(null)
        assertEquals("", state.editStoreName)
        assertEquals("", state.editStoreAddress)
    }

    @Test
    fun `confirming store info trims and saves, blank names are ignored`() {
        install()
        state.editStoreName = "   "
        state.confirmStoreInfo(COSTCO, "Done")
        assertEquals(TestData.OLD, DataManagerObject.stores.first().lastUpdate)

        state.editStoreName = " Big Costco "
        state.editStoreAddress = " Elm "
        state.confirmStoreInfo(COSTCO, "Done")
        assertEquals("Big Costco", DataManagerObject.stores.first().name)
        assertEquals("Elm", DataManagerObject.stores.first().address)
    }

    @Test
    fun `confirming store info for an unknown or deleted store saves nothing and shows no message`() {
        install(stores = listOf(costco.copy(deleted = true)))
        state.editStoreName = "Ghost"
        state.confirmStoreInfo("missing", "Done")
        state.confirmStoreInfo(COSTCO, "Done")
        assertNull(state.snackbarHostState.currentSnackbarData)
        assertEquals("Costco", DataManagerObject.stores.first().name)
        assertFalse(state.storeExists)
    }

    @Test
    fun `loading an existing store marks it as existing`() {
        state.loadStore(costco)
        assertTrue(state.storeExists)
        state.loadStore(null)
        assertFalse(state.storeExists)
    }

    @Test
    fun `unmarking a bought grocery clears isBought`() {
        install(dairy("Milk", COSTCO, bought = true))
        state.unmarkBought(findGrocery("Milk")!!)
        assertFalse(findGrocery("Milk")!!.isBought)
        assertTrue(findGrocery("Milk")!!.inShoppingList)
    }

    @Test
    fun `finishing shopping confirms the store's bought items and closes the confirmation`() {
        install(dairy("Milk", COSTCO, bought = true))
        state.showFinishConfirmation = true
        state.finishShopping(COSTCO)
        assertFalse(findGrocery("Milk")!!.inShoppingList)
        assertEquals(1, findGrocery("Milk")!!.buyEvents.size)
        assertFalse(state.showFinishConfirmation)
    }
}
