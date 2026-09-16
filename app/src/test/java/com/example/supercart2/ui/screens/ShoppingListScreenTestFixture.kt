package com.example.supercart2.ui.screens

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.store
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import io.mockk.coEvery
import io.mockk.mockkObject

/** Shared data + mocks for the ShoppingListScreen UI tests. */
class ShoppingListScreenTestFixture(private val compose: ComposeContentTestRule) {
    var backClicks = 0
    val navigatedStores = mutableListOf<String>()

    /** Mocks DataStoreManager persistence; [storeView] is the saved view mode. */
    fun mockPersistence(storeView: Boolean = false) {
        mockkObject(DataStoreManager)
        coEvery { DataStoreManager.saveDataGlobally() } returns Unit
        coEvery { DataStoreManager.loadStoreViewMode(any()) } returns storeView
        coEvery { DataStoreManager.saveStoreViewMode(any(), any()) } returns Unit
        coEvery { DataStoreManager.loadGroupCode(any()) } returns null
        coEvery { DataStoreManager.hasGroupCode(any()) } returns false
    }

    /**
     * Dairy (viewOrder 1): s1 = milk (to buy, store "shop"), cheese (bought), butter (not in list).
     * Bakery (viewOrder 2): s2 = bread (to buy).
     */
    fun installShoppingData() {
        TestData.reset()
        DataManagerObject.stores.add(store("shop"))
        DataManagerObject.categories.addAll(
            listOf(
                node(
                    category("c1", viewOrder = 1, name = "Dairy"),
                    subNode(
                        sub("s1", "c1", name = "Milk products"),
                        grocery("milk", "c1", "s1", storeIds = listOf("shop"), inShoppingList = true),
                        grocery("cheese", "c1", "s1", inShoppingList = true, isBought = true),
                        grocery("butter", "c1", "s1"),
                    ),
                ),
                node(
                    category("c2", viewOrder = 2, name = "Bakery"),
                    subNode(sub("s2", "c2", name = "Breads"), grocery("bread", "c2", "s2", inShoppingList = true)),
                ),
            )
        )
    }

    fun setScreen(withBack: Boolean = false) {
        val onBack: (() -> Unit)? = if (withBack) ({ backClicks++ }) else null
        compose.setContent {
            ShoppingListScreen(onBack = onBack, onNavigateToStoreEdit = { navigatedStores += it })
        }
    }
}
