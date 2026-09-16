package com.example.supercart2.data

import com.example.supercart2.TestData
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.TestData.grocery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** confirmBoughtItems / confirmBoughtItemsForStore: buy events, averageBuyDays and the pinned lost-update bug. */
class DataManagerObjectConfirmBoughtTest {

    private val dm = DataManagerObject

    @Before
    fun setUp() = TestData.installStandardTree()

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

    /**
     * Pinned BUG: both confirm functions rebuild each category from the snapshot taken at the start of
     * the loop, so when several sub-categories of one category have bought items, only the last
     * sub-category's update survives. See CLAUDE.md known issues.
     */
    @Test
    fun `confirmBoughtItems only keeps the last sub-category's update within a category (bug)`() {
        dm.addGrocery(grocery("g4", "c1", "s2"))
        dm.updateGrocery("g1") { it.copy(isBought = true, inShoppingList = true) }
        dm.updateGrocery("g4") { it.copy(isBought = true, inShoppingList = true) }

        dm.confirmBoughtItems()

        assertTrue(findGrocery("g1")!!.isBought) // lost update
        assertTrue(findGrocery("g1")!!.buyEvents.isEmpty())
        assertFalse(findGrocery("g4")!!.isBought)
        assertEquals(listOf(today), findGrocery("g4")!!.buyEvents)
    }

    @Test
    fun `confirmBoughtItemsForStore only keeps the last sub-category's update within a category (bug)`() {
        dm.addGrocery(grocery("g4", "c1", "s2"))
        dm.updateGrocery("g1") { it.copy(isBought = true, storeIds = listOf("A")) }
        dm.updateGrocery("g4") { it.copy(isBought = true, storeIds = listOf("A")) }

        dm.confirmBoughtItemsForStore("A")

        assertTrue(findGrocery("g1")!!.isBought) // lost update
        assertEquals(listOf(today), findGrocery("g4")!!.buyEvents)
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
    fun `confirmBoughtItems keeps an earlier sub-category's update when later ones have nothing bought`() {
        dm.addGrocery(grocery("g4", "c1", "s2"))
        dm.updateGrocery("g1") { it.copy(isBought = true) }

        dm.confirmBoughtItems()

        assertFalse(findGrocery("g1")!!.isBought)
        assertEquals(listOf(today), findGrocery("g1")!!.buyEvents)
    }

    @Test
    fun `confirmBoughtItemsForStore keeps an earlier update when later sub-categories only match other stores`() {
        dm.addGrocery(grocery("g4", "c1", "s2", storeIds = listOf("B"), isBought = true))
        dm.updateGrocery("g1") { it.copy(isBought = true, storeIds = listOf("A")) }

        dm.confirmBoughtItemsForStore("A")

        assertEquals(listOf(today), findGrocery("g1")!!.buyEvents)
        assertTrue(findGrocery("g4")!!.isBought)
    }

    @Test
    fun `confirm functions bump version even when nothing is bought`() {
        val v = dm.version
        dm.confirmBoughtItems()
        val v2 = dm.version
        dm.confirmBoughtItemsForStore("A")
        assertTrue(v2 > v)
        assertTrue(dm.version > v2)
    }

    @Test
    fun `confirmed groceries get lastUpdate stamped, untouched ones do not`() {
        dm.categories[1] = TestData.node(
            TestData.category("c2", viewOrder = 1),
            TestData.subNode(TestData.sub("s3", "c2"), grocery("g3", "c2", "s3", isBought = true)),
        )
        dm.confirmBoughtItems()
        assertTrue(findGrocery("g3")!!.lastUpdate.isAfter(TestData.OLD))
        assertEquals(TestData.OLD, findGrocery("g1")!!.lastUpdate)
    }

    @Test
    fun `confirmBoughtItems tolerates null buyEvents left by Gson`() {
        val json = """{"uuid":"g9","name":"g9","categoryId":"c2","subCategoryId":"s3","storeIds":[],"isBought":true}"""
        val legacy = gsonBuilder().create().fromJson(json, com.example.supercart2.models.Grocery::class.java)
        dm.categories[1].subCategories[0].groceries.add(legacy)

        dm.confirmBoughtItems()

        assertEquals(listOf(today), findGrocery("g9")!!.buyEvents)
        assertNull(findGrocery("g9")!!.averageBuyDays)
    }

    @Test
    fun `averageBuyDays is floored across uneven gaps`() {
        dm.updateGrocery("g1") { it.copy(isBought = true, buyEvents = listOf(today.minusDays(5), today.minusDays(2))) }
        dm.confirmBoughtItems()
        // gaps 2, 3 → 2.5 → 2
        assertEquals(2, findGrocery("g1")!!.averageBuyDays)
    }
}
