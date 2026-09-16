package com.example.supercart2.data

import com.example.supercart2.TestData
import com.example.supercart2.TestData.OLD
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.locationOf
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Grocery / category / sub-category mutations on the in-memory tree. */
class DataManagerObjectTest {

    private val dm = DataManagerObject

    @Before
    fun setUp() = TestData.installStandardTree()

    // ---------- getSortedCategories ----------

    @Test
    fun `getSortedCategories sorts by viewOrder`() {
        assertEquals(listOf("c2", "c1"), dm.getSortedCategories().map { it.category.uuid })
    }

    @Test
    fun `getSortedCategories hides deleted categories, sub-categories and groceries`() {
        dm.categories.add(node(category("gone", deleted = true), subNode(sub("sx", "gone"))))
        dm.updateSubCategory("c1", "s2") { it.copy(deleted = true) }
        dm.updateGrocery("g2") { it.copy(deleted = true) }

        val sorted = dm.getSortedCategories()
        assertEquals(listOf("c2", "c1"), sorted.map { it.category.uuid })
        val c1 = sorted.single { it.category.uuid == "c1" }
        assertEquals(listOf("s1"), c1.subCategories.map { it.subCategory.uuid })
        assertEquals(listOf("g1"), c1.subCategories.single().groceries.map { it.uuid })
    }

    @Test
    fun `getSortedCategories returns copies that do not alias the source lists`() {
        dm.getSortedCategories().single { it.category.uuid == "c1" }.subCategories.clear()
        assertEquals(2, dm.categories.single { it.category.uuid == "c1" }.subCategories.size)
    }

    // ---------- groceries ----------

    @Test
    fun `updateGrocery applies the change, stamps lastUpdate and bumps version`() {
        val v = dm.version
        dm.updateGrocery("g3") { it.copy(name = "milk") }

        val g = findGrocery("g3")!!
        assertEquals("milk", g.name)
        assertTrue(g.lastUpdate.isAfter(OLD))
        assertTrue(dm.version > v)
    }

    @Test
    fun `updateGrocery with unknown uuid is a no-op`() {
        val v = dm.version
        dm.updateGrocery("nope") { it.copy(name = "x") }
        assertEquals(v, dm.version)
    }

    @Test
    fun `toggleShoppingListStatus flips inShoppingList and always resets isBought`() {
        dm.updateGrocery("g1") { it.copy(inShoppingList = true, isBought = true) }
        dm.toggleShoppingListStatus("g1")
        assertFalse(findGrocery("g1")!!.inShoppingList)
        assertFalse(findGrocery("g1")!!.isBought)

        dm.updateGrocery("g1") { it.copy(isBought = true) }
        dm.toggleShoppingListStatus("g1")
        assertTrue(findGrocery("g1")!!.inShoppingList)
        assertFalse(findGrocery("g1")!!.isBought)
    }

    @Test
    fun `toggleBoughtStatus only flips isBought`() {
        dm.toggleBoughtStatus("g1")
        val g = findGrocery("g1")!!
        assertTrue(g.isBought)
        assertFalse(g.inShoppingList)
        assertTrue(g.buyEvents.isEmpty())
        dm.toggleBoughtStatus("g1")
        assertFalse(findGrocery("g1")!!.isBought)
    }

    @Test
    fun `addGrocery appends to the matching sub-category`() {
        dm.addGrocery(grocery("g4", "c1", "s2"))
        assertEquals("c1" to "s2", locationOf("g4"))
    }

    @Test
    fun `addGrocery is ignored when category and sub-category do not match`() {
        val v = dm.version
        dm.addGrocery(grocery("g4", "c2", "s2")) // s2 belongs to c1
        dm.addGrocery(grocery("g5", "missing", "s1"))
        assertNull(findGrocery("g4"))
        assertNull(findGrocery("g5"))
        assertEquals(v, dm.version)
    }

    @Test
    fun `updateGroceryLocation moves grocery and rewrites its ids`() {
        dm.updateGroceryLocation("g1", "c2", "s3")
        assertEquals("c2" to "s3", locationOf("g1"))
        val g = findGrocery("g1")!!
        assertEquals("c2", g.categoryId)
        assertEquals("s3", g.subCategoryId)
        // Pinned current behaviour: the move does NOT stamp lastUpdate (see CLAUDE.md known issues)
        assertEquals(OLD, g.lastUpdate)
    }

    @Test
    fun `updateGroceryLocation within the same sub-category moves it to the end`() {
        dm.updateGroceryLocation("g1", "c1", "s1")
        val groceries = dm.categories.single { it.category.uuid == "c1" }
            .subCategories.single { it.subCategory.uuid == "s1" }.groceries.map { it.uuid }
        assertEquals(listOf("g2", "g1"), groceries)
    }

    @Test
    fun `updateGroceryLocation with invalid target does nothing`() {
        val v = dm.version
        dm.updateGroceryLocation("g1", "c2", "s1") // s1 not under c2
        assertEquals("c1" to "s1", locationOf("g1"))
        assertEquals(v, dm.version)
    }

    @Test
    fun `deleteGrocery soft-deletes`() {
        dm.deleteGrocery("g1")
        val g = findGrocery("g1")!!
        assertTrue(g.deleted)
        assertTrue(g.lastUpdate.isAfter(OLD))
    }

    // ---------- categories ----------

    @Test
    fun `updateCategory stamps lastUpdate`() {
        dm.updateCategory("c1") { it.copy(name = "Dairy") }
        val c = dm.categories.single { it.category.uuid == "c1" }.category
        assertEquals("Dairy", c.name)
        assertTrue(c.lastUpdate.isAfter(OLD))
    }

    @Test
    fun `swapCategoryOrder swaps viewOrder values`() {
        dm.swapCategoryOrder("c1", "c2")
        assertEquals(listOf("c1", "c2"), dm.getSortedCategories().map { it.category.uuid })
        assertEquals(1, dm.categories.single { it.category.uuid == "c1" }.category.viewOrder)
        assertEquals(2, dm.categories.single { it.category.uuid == "c2" }.category.viewOrder)
    }

    @Test
    fun `swapCategoryOrder with unknown uuid does nothing`() {
        dm.swapCategoryOrder("c1", "nope")
        assertEquals(2, dm.categories.single { it.category.uuid == "c1" }.category.viewOrder)
    }

    @Test
    fun `addCategory appends with given sub-categories`() {
        dm.addCategory(category("c3"), listOf(subNode(sub("s9", "c3"))))
        val added = dm.categories.last()
        assertEquals("c3", added.category.uuid)
        assertEquals(listOf("s9"), added.subCategories.map { it.subCategory.uuid })
    }

    @Test
    fun `deleteCategory soft-deletes and strips it from every store category order`() {
        dm.setStoreCategoryOrder("storeA", listOf("c1", "c2"))
        dm.setStoreCategoryOrder("storeB", listOf("c1"))

        dm.deleteCategory("c1")

        assertTrue(dm.categories.single { it.category.uuid == "c1" }.category.deleted)
        assertEquals(listOf("c2"), dm.getStoreCategoryOrder("storeA"))
        assertEquals(emptyList<String>(), dm.getStoreCategoryOrder("storeB"))
    }

    // ---------- sub-categories ----------

    @Test
    fun `updateSubCategory stamps lastUpdate and keeps groceries`() {
        dm.updateSubCategory("c1", "s1") { it.copy(name = "Cheese") }
        val s = dm.categories.single { it.category.uuid == "c1" }.subCategories.single { it.subCategory.uuid == "s1" }
        assertEquals("Cheese", s.subCategory.name)
        assertTrue(s.subCategory.lastUpdate.isAfter(OLD))
        assertEquals(listOf("g1", "g2"), s.groceries.map { it.uuid })
    }

    @Test
    fun `updateSubCategory requires the right parent category`() {
        dm.updateSubCategory("c2", "s1") { it.copy(name = "x") }
        assertEquals("s1", dm.categories.single { it.category.uuid == "c1" }.subCategories[0].subCategory.name)
    }

    @Test
    fun `addSubCategory appends empty sub-category, ignores unknown category`() {
        dm.addSubCategory("c2", sub("s4", "c2"))
        dm.addSubCategory("missing", sub("s5", "missing"))
        val c2 = dm.categories.single { it.category.uuid == "c2" }
        assertEquals(listOf("s3", "s4"), c2.subCategories.map { it.subCategory.uuid })
        assertTrue(c2.subCategories.last().groceries.isEmpty())
        assertTrue(dm.categories.none { c -> c.subCategories.any { it.subCategory.uuid == "s5" } })
    }

    @Test
    fun `deleteSubCategory soft-deletes`() {
        dm.deleteSubCategory("c1", "s2")
        assertTrue(dm.categories.single { it.category.uuid == "c1" }.subCategories[1].subCategory.deleted)
    }

    @Test
    fun `getGroceriesCount counts every grocery including deleted`() {
        dm.deleteGrocery("g1")
        assertEquals(2, dm.categories.single { it.category.uuid == "c1" }.getGroceriesCount())
    }

    // ---------- confirm bought ----------

    private val today: LocalDate = LocalDate.now()

    @Test
    fun `confirmBoughtItems records today's buy event and clears list flags`() {
        dm.updateGrocery("g1") { it.copy(inShoppingList = true, isBought = true) }
        dm.updateGrocery("g2") { it.copy(inShoppingList = true, isBought = false) }
        val g2Before = findGrocery("g2")!!

        dm.confirmBoughtItems()

        val g1 = findGrocery("g1")!!
        assertEquals(listOf(today), g1.buyEvents)
        assertFalse(g1.isBought)
        assertFalse(g1.inShoppingList)
        assertNull(g1.averageBuyDays) // fewer than 2 events
        assertEquals(g2Before, findGrocery("g2")) // untouched
    }

    @Test
    fun `confirmBoughtItems does not duplicate today's event`() {
        dm.updateGrocery("g1") { it.copy(isBought = true, buyEvents = listOf(today.minusDays(4), today)) }
        dm.confirmBoughtItems()
        val g1 = findGrocery("g1")!!
        assertEquals(listOf(today.minusDays(4), today), g1.buyEvents)
        assertEquals(4, g1.averageBuyDays)
    }

    @Test
    fun `averageBuyDays is the floored mean gap over the newest four events`() {
        val events = listOf(
            today.minusDays(100), // ignored: only the newest 4 (including today) count
            today.minusDays(10),
            today.minusDays(7),
            today.minusDays(3),
        )
        dm.updateGrocery("g1") { it.copy(isBought = true, buyEvents = events) }
        dm.confirmBoughtItems()
        // newest 4: today, -3, -7, -10 → gaps 3, 4, 3 → mean 3.33 → 3
        assertEquals(3, findGrocery("g1")!!.averageBuyDays)
        assertEquals(events + today, findGrocery("g1")!!.buyEvents) // all events kept
    }

    @Test
    fun `confirmBoughtItemsForStore only confirms groceries linked to that store`() {
        dm.updateGrocery("g1") { it.copy(isBought = true, inShoppingList = true, storeIds = listOf("A")) }
        dm.updateGrocery("g2") { it.copy(isBought = true, inShoppingList = true, storeIds = listOf("B")) }
        dm.updateGrocery("g3") { it.copy(isBought = true, inShoppingList = true) }

        dm.confirmBoughtItemsForStore("A")

        assertEquals(listOf(today), findGrocery("g1")!!.buyEvents)
        assertFalse(findGrocery("g1")!!.inShoppingList)
        assertTrue(findGrocery("g2")!!.isBought)
        assertTrue(findGrocery("g3")!!.isBought)
    }

    @Test
    fun `confirmBoughtItemsForStore computes averageBuyDays`() {
        dm.updateGrocery("g1") { it.copy(isBought = true, storeIds = listOf("A"), buyEvents = listOf(today.minusDays(6))) }
        dm.confirmBoughtItemsForStore("A")
        assertEquals(6, findGrocery("g1")!!.averageBuyDays)
    }

    @Test
    fun `every mutation bumps version`() {
        val versions = mutableListOf(dm.version)
        dm.addCategory(category("c9"))
        versions += dm.version
        dm.addSubCategory("c9", sub("s9", "c9"))
        versions += dm.version
        dm.addGrocery(grocery("g9", "c9", "s9"))
        versions += dm.version
        dm.confirmBoughtItems()
        versions += dm.version
        assertEquals(versions.distinct(), versions)
        assertNotEquals(versions.first(), versions.last())
    }
}
