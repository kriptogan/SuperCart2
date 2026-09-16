package com.example.supercart2.data

import com.example.supercart2.TestData
import com.example.supercart2.TestData.OLD
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** swapCategoryOrder / swapStoreOrder: moving always changes the order, even with equal viewOrders. */
class DataManagerObjectReorderTest {

    private val dm = DataManagerObject

    @Before
    fun setUp() = TestData.reset()

    private fun sortedCategoryIds() = dm.getSortedCategories().map { it.category.uuid }
    private fun categoryOf(uuid: String) = dm.categories.single { it.category.uuid == uuid }.category
    private fun storeOf(uuid: String) = dm.stores.single { it.uuid == uuid }

    @Test
    fun `swapCategoryOrder moves categories that share the same viewOrder`() {
        dm.categories.addAll(listOf(node(category("a", 1)), node(category("b", 2)), node(category("c", 2))))

        dm.swapCategoryOrder("c", "b")

        assertEquals(listOf("a", "c", "b"), sortedCategoryIds())
        assertEquals(listOf(1, 2, 3), listOf("a", "c", "b").map { categoryOf(it).viewOrder })
        // only items whose viewOrder changed are stamped (a stays 1, c stays 2, b goes 2 → 3)
        assertEquals(OLD, categoryOf("a").lastUpdate)
        assertEquals(OLD, categoryOf("c").lastUpdate)
        assertTrue(categoryOf("b").lastUpdate.isAfter(OLD))
    }

    @Test
    fun `swapCategoryOrder renumbers non-deleted categories and leaves deleted ones alone`() {
        dm.categories.addAll(
            listOf(node(category("a", 0)), node(category("gone", 0, deleted = true)), node(category("b", 0)))
        )

        dm.swapCategoryOrder("a", "b")

        assertEquals(listOf("b", "a"), sortedCategoryIds())
        assertEquals(0, categoryOf("gone").viewOrder)
        assertEquals(OLD, categoryOf("gone").lastUpdate)
    }

    @Test
    fun `swapCategoryOrder with the same category twice changes nothing`() {
        dm.categories.addAll(listOf(node(category("a", 5)), node(category("b", 5))))
        val v = dm.version
        dm.swapCategoryOrder("a", "a")
        assertEquals(5, categoryOf("a").viewOrder)
        assertEquals(v, dm.version)
    }

    @Test
    fun `swapStoreOrder moves stores that share the same viewOrder`() {
        dm.stores.addAll(listOf(store("A", 0), store("B", 0), store("C", 0), store("X", 0, deleted = true)))

        dm.swapStoreOrder("B", "C")

        assertEquals(listOf("A", "C", "B"), dm.getSortedStores().map { it.uuid })
        assertEquals(listOf(1, 2, 3), listOf("A", "C", "B").map { storeOf(it).viewOrder })
        assertTrue(storeOf("B").lastUpdate.isAfter(OLD))
        assertEquals(0, storeOf("X").viewOrder)
        assertEquals(OLD, storeOf("X").lastUpdate)
    }

    @Test
    fun `swapStoreOrder with a deleted store does nothing`() {
        dm.stores.addAll(listOf(store("A", 1), store("X", 2, deleted = true)))
        dm.swapStoreOrder("A", "X")
        assertEquals(1, storeOf("A").viewOrder)
        assertEquals(2, storeOf("X").viewOrder)
    }
}
