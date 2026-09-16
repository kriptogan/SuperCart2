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
}
