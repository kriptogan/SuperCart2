package com.example.supercart2.ui.screens

import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store

/** Shared data for the StoresScreen tests: categories Dairy (viewOrder 1) and Bakery (viewOrder 2). */
object StoresScreenTestFixtures {
    const val COSTCO = "costco"
    const val ALDI = "aldi"

    val costco = Store(uuid = COSTCO, name = "Costco", address = "Main St", viewOrder = 2, lastUpdate = TestData.OLD)
    val aldi = Store(uuid = ALDI, name = "Aldi", viewOrder = 1, lastUpdate = TestData.OLD)

    fun dairy(name: String, vararg storeIds: String, inList: Boolean = true, bought: Boolean = false): Grocery =
        TestData.grocery(name, "Dairy", "DairySub", storeIds.toList(), inShoppingList = inList, isBought = bought)

    fun bakery(name: String, vararg storeIds: String, inList: Boolean = true, bought: Boolean = false): Grocery =
        TestData.grocery(name, "Bakery", "BakerySub", storeIds.toList(), inShoppingList = inList, isBought = bought)

    fun install(vararg groceries: Grocery, stores: List<Store> = listOf(costco, aldi)) {
        TestData.reset()
        DataManagerObject.stores.addAll(stores)
        DataManagerObject.categories.addAll(
            listOf(categoryNode("Dairy", 1, groceries), categoryNode("Bakery", 2, groceries))
        )
    }

    private fun categoryNode(name: String, viewOrder: Int, groceries: Array<out Grocery>) = node(
        category(name, viewOrder = viewOrder),
        subNode(sub("${name}Sub", name), *groceries.filter { it.categoryId == name }.toTypedArray()),
    )
}
