package com.example.supercart2.ui.screens

import com.example.supercart2.TestData
import com.example.supercart2.TestData.OLD
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.TestData.grocery
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** HomeScreen's onGroceryCreated handling (standard tree: c1{s1[g1,g2], s2[]}, c2{s3[g3]}). */
class HomeScreenGroceryResultTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        TestData.installStandardTree()
        mockkObject(DataStoreManager)
        coEvery { DataStoreManager.saveDataGlobally() } returns Unit
    }

    @After
    fun tearDown() = unmockkAll()

    private fun names(subCategoryId: String) = DataManagerObject.categories.flatMap { it.subCategories }
        .single { it.subCategory.uuid == subCategoryId }.groceries.map { it.name }

    private fun apply(original: Grocery?, result: Grocery) = homeScreenApplyGroceryResult(original, result, scope)

    private fun saves(count: Int) = coVerify(exactly = count) { DataStoreManager.saveDataGlobally() }

    private fun g(uuid: String) = findGrocery(uuid)!!

    @Test
    fun `create appends to the target sub-category, bumps version and saves once`() {
        val version = DataManagerObject.version
        apply(null, grocery("new", "c2", "s3"))
        assertEquals(listOf("g3", "new"), names("s3"))
        assertEquals(version + 1, DataManagerObject.version)
        saves(1)
    }

    @Test
    fun `create into an unknown category or sub-category does nothing and does not save`() {
        apply(null, grocery("new", "cx", "s3"))
        apply(null, grocery("new", "c2", "sx"))
        assertEquals(null, findGrocery("new"))
        saves(0)
    }

    @Test
    fun `edit in the same sub-category replaces in place without stamping lastUpdate`() {
        val version = DataManagerObject.version
        apply(g("g1"), g("g1").copy(name = "g1x"))
        assertEquals(listOf("g1x", "g2"), names("s1"))
        assertEquals(OLD, g("g1").lastUpdate)
        assertEquals(version + 1, DataManagerObject.version)
        saves(1)
    }

    @Test
    fun `edit of a grocery missing from its sub-category changes nothing but still saves`() {
        val version = DataManagerObject.version
        apply(grocery("ghost", "c1", "s1"), grocery("ghost", "c1", "s1"))
        assertEquals(listOf("g1", "g2"), names("s1"))
        assertEquals(version, DataManagerObject.version)
        saves(1)
    }

    @Test
    fun `pinned - same sub-category id is looked up under the new category id`() {
        apply(g("g3"), g("g3").copy(name = "g3x", categoryId = "c1"))
        assertEquals(listOf("g3"), names("s3"))
        saves(1)
    }

    @Test
    fun `moving within a category removes from the old sub-category and appends to the new one`() {
        val version = DataManagerObject.version
        apply(g("g1"), g("g1").copy(subCategoryId = "s2", name = "moved"))
        assertEquals(listOf("g2"), names("s1"))
        assertEquals(listOf("moved"), names("s2"))
        assertEquals(OLD, g("g1").lastUpdate)
        // Pinned: the move path does not call updateData()
        assertEquals(version, DataManagerObject.version)
        saves(1)
    }

    @Test
    fun `moving across categories appends at the end`() {
        apply(g("g3"), g("g3").copy(categoryId = "c1", subCategoryId = "s1"))
        assertEquals(listOf("g1", "g2", "g3"), names("s1"))
        assertEquals(emptyList<String>(), names("s3"))
        saves(1)
    }

    @Test
    fun `pinned - moving to an unknown sub-category loses the grocery`() {
        apply(g("g3"), g("g3").copy(subCategoryId = "sx"))
        assertEquals(null, findGrocery("g3"))
        saves(1)
    }

    @Test
    fun `pinned - moving from an unknown original sub-category duplicates the grocery`() {
        apply(g("g1").copy(subCategoryId = "sx"), g("g1").copy(subCategoryId = "s2"))
        assertEquals(listOf("g1", "g2"), names("s1"))
        assertEquals(listOf("g1"), names("s2"))
        saves(1)
    }
}
