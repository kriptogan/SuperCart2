package com.example.supercart2.ui.components

import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import io.mockk.coEvery
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

/** The callbacks CategoriesManagementDialog passes to EditCategoryDialog (which does not call most of them). */
class CategoriesManagementDialogEditHandlersTest {

    private val events = mutableListOf<String>()
    private val handlers = CategoriesManagementDialogEditHandlers(
        "c1", CoroutineScope(Dispatchers.Unconfined)
    ) { events += "close" }

    @Before
    fun setUp() {
        TestData.reset()
        mockkObject(DataStoreManager)
        coEvery { DataStoreManager.saveDataGlobally() } answers { events += "save" }
        DataManagerObject.categories.add(
            node(category("c1"), subNode(sub("s1", "c1"), grocery("g1", "c1", "s1")), subNode(sub("s2", "c1")))
        )
    }

    @After
    fun tearDown() = unmockkAll()

    private fun node() = DataManagerObject.categories.single()

    private fun subIds() = node().subCategories.map { it.subCategory.uuid }

    @Test
    fun `categoryUpdated replaces the category, saves and closes`() {
        handlers.categoryUpdated(node().category.copy(name = "New"))
        assertEquals("New", node().category.name)
        assertEquals(listOf("save", "close"), events)
    }

    @Test
    fun `categoryDeleted soft-deletes, saves and closes`() {
        handlers.categoryDeleted(node().category)
        assertTrue(node().category.deleted)
        assertEquals(listOf("save", "close"), events)
    }

    @Test
    fun `categoryDeleted only closes for a protected category`() {
        handlers.categoryDeleted(node().category.copy(protected = true))
        assertFalse(node().category.deleted)
        assertEquals(listOf("close"), events)
    }

    @Test
    fun `subCategoryCreated appends it and saves`() {
        handlers.subCategoryCreated(sub("s3", "c1"))
        assertEquals(listOf("s1", "s2", "s3"), subIds())
        assertEquals(listOf("save"), events)
    }

    @Test
    fun `subCategoryUpdated replaces it keeping groceries and saves`() {
        handlers.subCategoryUpdated(sub("s1", "c1", name = "Renamed"))
        assertEquals("Renamed", node().subCategories[0].subCategory.name)
        assertEquals(listOf("g1"), node().subCategories[0].groceries.map { it.uuid })
        assertEquals(listOf("save"), events)
    }

    @Test
    fun `subCategoryUpdated ignores an unknown sub-category`() {
        handlers.subCategoryUpdated(sub("nope", "c1"))
        assertEquals(listOf("s1", "s2"), subIds())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `subCategoryDeleted soft-deletes it, keeping it in the list, and saves`() {
        val versionBefore = DataManagerObject.version
        handlers.subCategoryDeleted(sub("s1", "c1"))
        assertEquals(listOf("s1", "s2"), subIds())
        assertTrue(node().subCategories[0].subCategory.deleted)
        assertTrue(node().subCategories[0].subCategory.lastUpdate.isAfter(TestData.OLD))
        assertEquals(listOf("g1"), node().subCategories[0].groceries.map { it.uuid })
        assertFalse(node().subCategories[1].subCategory.deleted)
        assertEquals(versionBefore + 1, DataManagerObject.version)
        assertEquals(listOf("save"), events)
    }

    @Test
    fun `subCategoryDeleted ignores protected and unknown sub-categories`() {
        handlers.subCategoryDeleted(sub("s1", "c1").copy(protected = true))
        handlers.subCategoryDeleted(sub("nope", "c1"))
        assertEquals(listOf("s1", "s2"), subIds())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `sub-category handlers do nothing when the category is missing`() {
        val orphan = CategoriesManagementDialogEditHandlers("missing", CoroutineScope(Dispatchers.Unconfined)) {}
        orphan.subCategoryCreated(sub("s3", "missing"))
        orphan.subCategoryUpdated(sub("s1", "c1", name = "X"))
        orphan.subCategoryDeleted(sub("s1", "c1"))
        assertEquals(listOf("s1", "s2"), subIds())
        assertTrue(events.isEmpty())
    }
}
