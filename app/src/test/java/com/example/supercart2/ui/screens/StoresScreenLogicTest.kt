package com.example.supercart2.ui.screens

import androidx.compose.ui.unit.dp
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Store
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.ALDI
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.COSTCO
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.aldi
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.costco
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.bakery
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.dairy
import com.example.supercart2.ui.screens.StoresScreenTestFixtures.install
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoresScreenLogicTest {

    private fun tree(): List<CategoryWithSubCategories> = DataManagerObject.categories.toList()

    private fun names(categories: List<CategoryWithSubCategories>) =
        categories.flatMap { c -> c.subCategories.flatMap { s -> s.groceries.map { it.name } } }

    @Test
    fun `active stores keep store order and count items and single-store items`() {
        install(
            dairy("Milk", COSTCO), dairy("Cheese", COSTCO, ALDI), bakery("Bread", COSTCO, bought = true),
            bakery("Bagel", ALDI, inList = false), dairy("Old", ALDI).copy(deleted = true),
        )
        val stores = listOf(aldi, costco, Store(uuid = "x", name = "X"))

        val result = storesScreenActiveStores(tree(), stores)

        assertEquals(
            listOf(StoreCardData(ALDI, "Aldi", 1, 0), StoreCardData(COSTCO, "Costco", 3, 2)),
            result,
        )
    }

    @Test
    fun `no shopping items means no active stores`() {
        install(dairy("Milk", COSTCO, inList = false))
        assertTrue(storesScreenActiveStores(tree(), listOf(costco)).isEmpty())
    }

    @Test
    fun `buying to-buy keeps unbought shopping items of the store matching the search`() {
        install(
            dairy("Milk", COSTCO), dairy("Buttermilk", COSTCO), dairy("Cheese", COSTCO),
            dairy("Soy milk", ALDI), dairy("Rice milk", COSTCO, inList = false),
            bakery("Milk bread", COSTCO, bought = true),
        )
        val result = storesScreenToBuyCategories(tree(), COSTCO, StoreMode.Buying, "MILK")
        assertEquals(listOf("Milk", "Buttermilk"), names(result))
        assertEquals(listOf("Dairy"), result.map { it.category.name })
    }

    @Test
    fun `edit to-buy keeps every grocery linked to the store`() {
        install(dairy("Milk", COSTCO), dairy("Cheese", COSTCO, inList = false), bakery("Bread", COSTCO, bought = true),
            bakery("Bagel", ALDI))
        val all = storesScreenToBuyCategories(tree(), COSTCO, StoreMode.Edit, "")
        assertEquals(listOf("Milk", "Cheese", "Bread"), names(all))
        assertEquals(listOf("Bread"), names(storesScreenToBuyCategories(tree(), COSTCO, StoreMode.Edit, "rea")))
    }

    @Test
    fun `bought categories keep bought shopping items of the store matching the search`() {
        install(dairy("Milk", COSTCO, bought = true), dairy("Cheese", COSTCO), bakery("Bread", ALDI, bought = true),
            bakery("Bagel", COSTCO, bought = true))
        assertEquals(listOf("Milk", "Bagel"), names(storesScreenBoughtCategories(tree(), COSTCO, "")))
        assertEquals(listOf("Bagel"), names(storesScreenBoughtCategories(tree(), COSTCO, "bag")))
    }

    @Test
    fun `grocery count and bought entries flatten the tree`() {
        install(dairy("Milk", COSTCO), dairy("Cheese", COSTCO), bakery("Bread", COSTCO))
        assertEquals(3, storesScreenGroceryCount(tree()))
        assertEquals(0, storesScreenGroceryCount(emptyList()))
        assertEquals(
            listOf("Dairy" to "Milk", "Dairy" to "Cheese", "Bakery" to "Bread"),
            storesScreenBoughtEntries(tree()).map { (category, grocery) -> category to grocery.name },
        )
    }

    @Test
    fun `header top padding depends on mode and bought items`() {
        assertEquals(100.dp, storesScreenHeaderTopPadding(StoreMode.Edit, 3))
        assertEquals(248.dp, storesScreenHeaderTopPadding(StoreMode.Buying, 1))
        assertEquals(196.dp, storesScreenHeaderTopPadding(StoreMode.Buying, 0))
    }

    @Test
    fun `exit from edit returns to buying only when entered from buying`() {
        val fromBuying = StoresSubScreen.StoreDetail(COSTCO, StoreMode.Edit, returnToBuying = true)
        assertEquals(StoresSubScreen.StoreDetail(COSTCO, StoreMode.Buying), fromBuying.storesScreenExitTarget())
        val fromOutside = StoresSubScreen.StoreDetail(COSTCO, StoreMode.Edit)
        assertEquals(StoresSubScreen.SelectStore, fromOutside.storesScreenExitTarget())
        val buying = StoresSubScreen.StoreDetail(COSTCO, StoreMode.Buying, returnToBuying = true)
        assertEquals(StoresSubScreen.SelectStore, buying.storesScreenExitTarget())
    }

    @Test
    fun `edit target keeps the store and remembers to return to buying`() {
        val expected = StoresSubScreen.StoreDetail(COSTCO, StoreMode.Edit, returnToBuying = true)
        assertEquals(expected, StoresSubScreen.StoreDetail(COSTCO, StoreMode.Buying).storesScreenEditTarget())
    }
}
