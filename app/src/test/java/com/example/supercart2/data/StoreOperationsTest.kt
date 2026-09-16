package com.example.supercart2.data

import com.example.supercart2.TestData
import com.example.supercart2.TestData.OLD
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.store
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Stores, hiding, per-store ordering and store grouping. */
class StoreOperationsTest {

    private val dm = DataManagerObject

    @Before
    fun setUp() {
        TestData.reset()
        dm.stores.addAll(listOf(store("A", viewOrder = 2), store("B", viewOrder = 1), store("X", deleted = true)))
        // c1(viewOrder 1): s1 [g1 → A, g2 → A+B], s2 [g3 → no store]
        // c2(viewOrder 2): s3 [g4 → B], s4 [g5 → A]
        // c3(viewOrder 3): s5 [g6 → X (deleted store)]
        dm.categories.addAll(
            listOf(
                node(
                    category("c1", viewOrder = 1),
                    subNode(sub("s1", "c1"), grocery("g1", "c1", "s1", listOf("A")), grocery("g2", "c1", "s1", listOf("A", "B"))),
                    subNode(sub("s2", "c1"), grocery("g3", "c1", "s2")),
                ),
                node(
                    category("c2", viewOrder = 2),
                    subNode(sub("s3", "c2"), grocery("g4", "c2", "s3", listOf("B"))),
                    subNode(sub("s4", "c2"), grocery("g5", "c2", "s4", listOf("A"))),
                ),
                node(category("c3", viewOrder = 3), subNode(sub("s5", "c3"), grocery("g6", "c3", "s5", listOf("X")))),
            )
        )
    }

    // ---------- store CRUD ----------

    @Test
    fun `getSortedStores filters deleted and sorts by viewOrder`() {
        assertEquals(listOf("B", "A"), dm.getSortedStores().map { it.uuid })
    }

    @Test
    fun `addStore and updateStore`() {
        dm.addStore(store("C", viewOrder = 0))
        dm.updateStore("C") { it.copy(name = "Costco") }
        val c = dm.stores.single { it.uuid == "C" }
        assertEquals("Costco", c.name)
        assertTrue(c.lastUpdate.isAfter(OLD))
        assertEquals("C", dm.getSortedStores().first().uuid)
    }

    @Test
    fun `swapStoreOrder swaps viewOrder`() {
        dm.swapStoreOrder("A", "B")
        assertEquals(listOf("A", "B"), dm.getSortedStores().map { it.uuid })
    }

    @Test
    fun `unlinkStoreFromAllGroceries removes the id without stamping lastUpdate`() {
        dm.unlinkStoreFromAllGroceries("A")
        assertEquals(emptyList<String>(), findGrocery("g1")!!.storeIds)
        assertEquals(listOf("B"), findGrocery("g2")!!.storeIds)
        assertEquals(OLD, findGrocery("g2")!!.lastUpdate) // pinned: see CLAUDE.md known issues
    }

    @Test
    fun `deleteStore unlinks, soft-deletes and clears hidden flag and orders`() {
        dm.toggleStoreHidden("A")
        dm.setStoreCategoryOrder("A", listOf("c2"))
        dm.setStoreSubCategoryOrders("A", mapOf("c1" to listOf("s2")))

        dm.deleteStore("A")

        assertTrue(dm.stores.single { it.uuid == "A" }.deleted)
        assertFalse(dm.isStoreHidden("A"))
        assertNull(dm.getStoreCategoryOrder("A"))
        assertNull(dm.getStoreSubCategoryOrder("A", "c1"))
        assertEquals(listOf("B"), findGrocery("g2")!!.storeIds)
        assertEquals(listOf("B"), dm.getSortedStores().map { it.uuid })
    }

    @Test
    fun `toggleStoreHidden toggles`() {
        dm.toggleStoreHidden("A")
        assertTrue(dm.isStoreHidden("A"))
        dm.toggleStoreHidden("A")
        assertFalse(dm.isStoreHidden("A"))
    }

    // ---------- per-store order storage ----------

    @Test
    fun `setStoreCategoryOrder stores a defensive copy`() {
        val order = mutableListOf("c2", "c1")
        dm.setStoreCategoryOrder("A", order)
        order.clear()
        assertEquals(listOf("c2", "c1"), dm.getStoreCategoryOrder("A"))
    }

    @Test
    fun `sub-category order lookup`() {
        dm.setStoreSubCategoryOrders("A", mapOf("c1" to listOf("s2", "s1")))
        assertEquals(listOf("s2", "s1"), dm.getStoreSubCategoryOrder("A", "c1"))
        assertNull(dm.getStoreSubCategoryOrder("A", "c2"))
        assertNull(dm.getStoreSubCategoryOrder("B", "c1"))
    }

    // ---------- getSortedCategoriesForStore ----------

    @Test
    fun `getSortedCategoriesForStore without custom order equals global order`() {
        assertEquals(listOf("c1", "c2", "c3"), dm.getSortedCategoriesForStore("A").map { it.category.uuid })
    }

    @Test
    fun `getSortedCategoriesForStore applies partial custom orders, rest by viewOrder`() {
        dm.setStoreCategoryOrder("A", listOf("c3", "unknown"))
        dm.setStoreSubCategoryOrders("A", mapOf("c1" to listOf("s2"), "c2" to listOf("s4", "s3")))

        val result = dm.getSortedCategoriesForStore("A")

        assertEquals(listOf("c3", "c1", "c2"), result.map { it.category.uuid })
        assertEquals(listOf("s2", "s1"), result[1].subCategories.map { it.subCategory.uuid })
        assertEquals(listOf("s4", "s3"), result[2].subCategories.map { it.subCategory.uuid })
        // other stores are unaffected
        assertEquals(listOf("c1", "c2", "c3"), dm.getSortedCategoriesForStore("B").map { it.category.uuid })
    }

    @Test
    fun `getSortedCategoriesForStore excludes deleted items`() {
        dm.deleteCategory("c3")
        dm.deleteSubCategory("c1", "s2")
        val result = dm.getSortedCategoriesForStore("A")
        assertEquals(listOf("c1", "c2"), result.map { it.category.uuid })
        assertEquals(listOf("s1"), result[0].subCategories.map { it.subCategory.uuid })
    }

    // ---------- groupGroceriesByStore ----------

    private fun grouped() = dm.groupGroceriesByStore(dm.getSortedCategories())
        .map { group ->
            (group.store?.uuid ?: "none") to group.categories.flatMap { c ->
                c.subCategories.flatMap { s -> s.groceries.map { "${c.category.uuid}/${s.subCategory.uuid}/${it.uuid}" } }
            }
        }

    @Test
    fun `groupGroceriesByStore groups by visible store in store order, unlinked last`() {
        assertEquals(
            listOf(
                "B" to listOf("c1/s1/g2", "c2/s3/g4"),
                "A" to listOf("c1/s1/g1", "c1/s1/g2", "c2/s4/g5"),
                "none" to listOf("c1/s2/g3"),
            ),
            grouped()
        )
        // g6 is linked only to a deleted store, so it appears in no group
    }

    @Test
    fun `groupGroceriesByStore skips hidden stores`() {
        dm.toggleStoreHidden("B")
        assertEquals(listOf("A", "none"), grouped().map { it.first })
    }

    @Test
    fun `groupGroceriesByStore applies custom category order`() {
        dm.setStoreCategoryOrder("A", listOf("c2"))
        val a = dm.groupGroceriesByStore(dm.getSortedCategories()).single { it.store?.uuid == "A" }
        assertEquals(listOf("c2", "c1"), a.categories.map { it.category.uuid })
    }

    @Test
    fun `groupGroceriesByStore with nothing linked returns only the unlinked group or nothing`() {
        TestData.reset()
        assertEquals(emptyList<StoreWithCategoriesAlias>(), dm.groupGroceriesByStore(emptyList()))
    }
    // ---------- extra characterization pins ----------

    @Test
    fun `store helpers with unknown ids change nothing`() {
        val v = dm.version
        dm.updateStore("nope") { it.copy(name = "x") }
        dm.swapStoreOrder("A", "nope")
        assertEquals(v, dm.version)
        assertEquals(listOf("B", "A"), dm.getSortedStores().map { it.uuid })
    }

    @Test
    fun `unlinkStoreFromAllGroceries edits grocery lists in place and bumps version`() {
        val node = dm.categories[0]
        val list = node.subCategories[0].groceries
        val v = dm.version
        dm.unlinkStoreFromAllGroceries("A")
        assertSame(node, dm.categories[0])
        assertSame(list, dm.categories[0].subCategories[0].groceries)
        assertTrue(dm.version > v)
    }

    @Test
    fun `groupGroceriesByStore ignores per-store sub-category orders`() {
        dm.setStoreSubCategoryOrders("A", mapOf("c2" to listOf("s4", "s3")))
        dm.addGrocery(grocery("g7", "c2", "s3", listOf("A")))
        val a = dm.groupGroceriesByStore(dm.getSortedCategories()).single { it.store?.uuid == "A" }
        assertEquals(listOf("s3", "s4"), a.categories[1].subCategories.map { it.subCategory.uuid })
    }

    @Test
    fun `unlinked group is sorted by viewOrder even for unsorted input, custom order ids are used once`() {
        dm.updateGrocery("g5") { it.copy(storeIds = emptyList()) }
        val input = dm.getSortedCategories().reversed()
        assertEquals(listOf("c1", "c2"), dm.groupGroceriesByStore(input).last().categories.map { it.category.uuid })

        dm.setStoreCategoryOrder("B", listOf("c2", "c2", "c1"))
        assertEquals(listOf("c2", "c1", "c3"), dm.getSortedCategoriesForStore("B").map { it.category.uuid })
    }
}

private typealias StoreWithCategoriesAlias = com.example.supercart2.ui.components.StoreWithCategories
