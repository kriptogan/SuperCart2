package com.example.supercart2.data

import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.models.hasAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroceryFiltersTest {

    private val today = LocalDate.of(2024, 6, 10)
    private val base = grocery("g", "c", "s")

    @Test
    fun `no expiration and no buy pattern means no alert`() {
        assertFalse(base.hasAlert(today))
    }

    @Test
    fun `expiration alert triggers from tomorrow backwards`() {
        assertTrue(base.copy(expirationDate = today.minusDays(3)).hasAlert(today))
        assertTrue(base.copy(expirationDate = today).hasAlert(today))
        assertTrue(base.copy(expirationDate = today.plusDays(1)).hasAlert(today))
        assertFalse(base.copy(expirationDate = today.plusDays(2)).hasAlert(today))
    }

    @Test
    fun `buy pattern alert triggers once averageBuyDays have passed since the latest buy`() {
        val events = listOf(today.minusDays(20), today.minusDays(7), today.minusDays(14))
        assertTrue(base.copy(averageBuyDays = 7, buyEvents = events).hasAlert(today))
        assertFalse(base.copy(averageBuyDays = 8, buyEvents = events).hasAlert(today))
        assertTrue(base.copy(averageBuyDays = 0, buyEvents = listOf(today)).hasAlert(today))
    }

    @Test
    fun `buy pattern needs both an average and at least one event`() {
        assertFalse(base.copy(averageBuyDays = 1).hasAlert(today))
        assertFalse(base.copy(averageBuyDays = null, buyEvents = listOf(today.minusDays(100))).hasAlert(today))
    }

    @Test
    fun `filterGroceries keeps structure of matches and drops empty branches`() {
        val tree = listOf(
            node(
                category("c1"),
                subNode(sub("s1", "c1"), grocery("keep1", "c1", "s1"), grocery("drop1", "c1", "s1")),
                subNode(sub("s2", "c1"), grocery("drop2", "c1", "s2")),
            ),
            node(category("c2"), subNode(sub("s3", "c2"), grocery("drop3", "c2", "s3"))),
            node(category("c3"), subNode(sub("s4", "c3"), grocery("keep2", "c3", "s4"))),
        )

        val result = tree.filterGroceries { it.uuid.startsWith("keep") }

        assertEquals(listOf("c1", "c3"), result.map { it.category.uuid })
        assertEquals(listOf("s1"), result[0].subCategories.map { it.subCategory.uuid })
        assertEquals(listOf("keep1"), result[0].subCategories[0].groceries.map { it.uuid })
        assertEquals(5, tree.sumOf { it.getGroceriesCount() }) // source untouched
        assertTrue(tree.filterGroceries { false }.isEmpty())
    }

    @Test
    fun `filterGroceries returns independent mutable lists`() {
        TestData.installStandardTree()
        val result = DataManagerObject.categories.filterGroceries { true }
        result[0].subCategories.clear()
        assertEquals(2, DataManagerObject.categories[0].subCategories.size)
    }
}
