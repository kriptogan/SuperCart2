package com.example.supercart2.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.store
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreManagerTest {

    private lateinit var context: Context
    private val dm = DataManagerObject

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        // DataStore instances are process-wide singletons: overwrite whatever a previous test left behind.
        TestData.reset()
        DataStoreManager.saveData(context)
        DataStoreManager.clearGroupCode(context)
        DataStoreManager.saveStoreViewMode(context, false)
    }

    private fun installRichData() {
        val grocery = grocery("g1", "c1", "s1", storeIds = listOf("A"), inShoppingList = true).copy(
            expirationDate = LocalDate.of(2024, 2, 29),
            buyEvents = listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8)),
            averageBuyDays = 7,
            imageUUID = "img",
            lastUpdate = LocalDateTime.of(2024, 3, 1, 10, 30, 15, 123_000_000),
        )
        dm.categories.addAll(
            listOf(
                node(category("c1", viewOrder = 3), subNode(sub("s1", "c1"), grocery), subNode(sub("s2", "c1", deleted = true))),
                node(category("c2", deleted = true), subNode(sub("s3", "c2"), grocery("g2", "c2", "s3"))),
            )
        )
        dm.stores.addAll(listOf(store("A").copy(address = "Main St 1"), store("B", deleted = true)))
        dm.hiddenStoreIds.add("A")
        dm.storeCategoryOrders["A"] = listOf("c1")
        dm.storeSubCategoryOrders["A"] = mapOf("c1" to listOf("s1"))
    }

    @Test
    fun `save then load round-trips all data`() = runTest {
        installRichData()
        val expectedCategories = dm.categories.toList()
        val activeStore = dm.stores.first()
        DataStoreManager.saveData(context)

        TestData.reset()
        DataStoreManager.loadData(context)

        // Deleted items are persisted, but load hides them (buildNestedStructure default)
        assertEquals(DataConverter.flattenCategories(expectedCategories, includeDeleted = false).first, dm.categories.map { it.category })
        val loadedGrocery = dm.categories.single().subCategories.single().groceries.single()
        assertEquals(expectedCategories[0].subCategories[0].groceries[0], loadedGrocery)
        assertEquals(listOf(activeStore), dm.stores.toList()) // deleted store dropped
        assertEquals(listOf("A"), dm.hiddenStoreIds.toList())
        assertEquals(mapOf("A" to listOf("c1")), dm.storeCategoryOrders)
        assertEquals(mapOf("A" to mapOf("c1" to listOf("s1"))), dm.storeSubCategoryOrders)
    }

    @Test
    fun `load with empty storage creates default categories and persists them`() = runTest {
        DataStoreManager.loadData(context)
        assertEquals(listOf("Other", "Vegetables", "Fruits"), dm.categories.map { it.category.name })

        val firstUuids = dm.categories.map { it.category.uuid }
        TestData.reset()
        DataStoreManager.loadData(context)
        assertEquals(firstUuids, dm.categories.map { it.category.uuid }) // loaded, not re-created
    }

    @Test
    fun `load with broken relationships falls back to defaults`() = runTest {
        dm.categories.add(node(category("c1"), subNode(sub("s1", "c1"), grocery("g1", "c1", "s1"))))
        // Orphan sub-category: its category is not in the flattened list
        dm.categories.add(node(category("c2"), subNode(sub("s2", "not-c2"))))
        DataStoreManager.saveData(context)

        TestData.reset()
        DataStoreManager.loadData(context)

        assertEquals(listOf("Other", "Vegetables", "Fruits"), dm.categories.map { it.category.name })
    }

    @Test
    fun `load does not replace in-memory data when relationships are broken but memory is non-empty`() = runTest {
        dm.categories.add(node(category("c2"), subNode(sub("s2", "not-c2"))))
        DataStoreManager.saveData(context)
        // memory still holds the (invalid) tree, so defaults are not created
        DataStoreManager.loadData(context)
        assertEquals(listOf("c2"), dm.categories.map { it.category.uuid })
    }

    @Test
    fun `saveDataGlobally without context is a no-op, with context persists`() = runTest {
        dm.categories.add(node(category("c1"), subNode(sub("s1", "c1"))))
        DataStoreManager.saveDataGlobally() // context null → nothing written

        TestData.reset()
        DataStoreManager.loadData(context)
        assertEquals("Other", dm.categories.first().category.name)

        TestData.reset()
        dm.categories.add(node(category("c1"), subNode(sub("s1", "c1"))))
        DataStoreManager.setGlobalContext(context)
        DataStoreManager.saveDataGlobally()
        TestData.reset()
        DataStoreManager.loadData(context)
        assertEquals(listOf("c1"), dm.categories.map { it.category.uuid })
    }

    @Test
    fun `store view mode`() = runTest {
        assertFalse(DataStoreManager.loadStoreViewMode(context))
        DataStoreManager.saveStoreViewMode(context, true)
        assertTrue(DataStoreManager.loadStoreViewMode(context))
    }

    @Test
    fun `group code save, load, clear`() = runTest {
        assertNull(DataStoreManager.loadGroupCode(context))
        assertFalse(DataStoreManager.hasGroupCode(context))

        DataStoreManager.saveGroupCode(context, "abc")
        assertEquals("abc", DataStoreManager.loadGroupCode(context))
        assertTrue(DataStoreManager.hasGroupCode(context))

        DataStoreManager.clearGroupCode(context)
        assertNull(DataStoreManager.loadGroupCode(context))
    }

    @Test
    fun `saving data does not touch group code or view mode`() = runTest {
        DataStoreManager.saveGroupCode(context, "abc")
        DataStoreManager.saveStoreViewMode(context, true)
        DataStoreManager.saveData(context)
        assertEquals("abc", DataStoreManager.loadGroupCode(context))
        assertTrue(DataStoreManager.loadStoreViewMode(context))
    }
}
