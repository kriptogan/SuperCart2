package com.example.supercart2.data

import com.example.supercart2.TestData
import com.example.supercart2.data.FirestoreFake.Companion.snapshot
import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Malformed documents and orphaned items in Firestore must not break the whole download. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseManagerDownloadSkipTest : FirebaseManagerTestBase() {

    private fun groceryIds() = DataManagerObject.categories.flatMap { it.subCategories }.flatMap { it.groceries }.map { it.uuid }

    private fun subCategoryIds() = DataManagerObject.categories.flatMap { it.subCategories }.map { it.subCategory.uuid }

    private val validDocs: Map<String, List<DocumentSnapshot>>
        get() = mapOf(
            "categories" to listOf(snapshot("c1", mapOf("name" to "c1"))),
            "subcategories" to listOf(snapshot("s1", mapOf("categoryId" to "c1", "name" to "s1"))),
            "groceries" to listOf(snapshot("g1", mapOf("name" to "g1", "categoryId" to "c1", "subCategoryId" to "s1"))),
            "stores" to listOf(snapshot("A", mapOf("name" to "A"))),
        )

    private fun installValidTree(extra: Pair<String, DocumentSnapshot>? = null) {
        validDocs.forEach { (collection, docs) ->
            val all = if (extra?.first == collection) docs + extra.second else docs
            fake.setDocs(collection, *all.toTypedArray())
        }
    }

    private fun assertValidTreeLoaded() {
        assertEquals(listOf("c1"), DataManagerObject.categories.map { it.category.uuid })
        assertEquals(listOf("s1"), subCategoryIds())
        assertEquals(listOf("g1"), groceryIds())
        assertEquals(listOf("A"), DataManagerObject.stores.map { it.uuid })
    }

    @Test
    fun `downloadData skips documents missing required data and keeps the rest`() = runTest {
        val malformed = listOf(
            "categories" to snapshot("c2", mapOf("default" to true)),
            "categories" to snapshot("c2", null),
            "subcategories" to snapshot("s2", mapOf("name" to "s")),
            "subcategories" to snapshot("s2", mapOf("categoryId" to "c1")),
            "groceries" to snapshot("g2", mapOf("categoryId" to "c1", "subCategoryId" to "s1")),
            "groceries" to snapshot("g2", mapOf("name" to "g", "subCategoryId" to "s1")),
            "groceries" to snapshot("g2", mapOf("name" to "g", "categoryId" to "c1")),
            "stores" to snapshot("B", mapOf("viewOrder" to 1L)),
            "stores" to snapshot("B", null),
        )
        for (case in malformed) {
            fake = FirestoreFake(db, GROUP)
            installValidTree(case)

            FirebaseManager.downloadData()

            assertValidTreeLoaded()
        }
    }

    @Test
    fun `downloadData drops sub-categories and groceries whose parents are missing, keeps and saves the rest`() = runTest {
        installValidTree()
        fake.setDocs(
            "subcategories",
            snapshot("s1", mapOf("categoryId" to "c1", "name" to "s1")),
            snapshot("sOrphan", mapOf("categoryId" to "missing", "name" to "s")),
            snapshot("sBroken", mapOf("categoryId" to "c1")), // malformed -> skipped, so gInBroken is an orphan
        )
        fake.setDocs(
            "groceries",
            snapshot("g1", mapOf("name" to "g1", "categoryId" to "c1", "subCategoryId" to "s1")),
            snapshot("gNoCategory", mapOf("name" to "g", "categoryId" to "missing", "subCategoryId" to "s1")),
            snapshot("gNoSub", mapOf("name" to "g", "categoryId" to "c1", "subCategoryId" to "missing")),
            snapshot("gInOrphan", mapOf("name" to "g", "categoryId" to "missing", "subCategoryId" to "sOrphan")),
            snapshot("gInBroken", mapOf("name" to "g", "categoryId" to "c1", "subCategoryId" to "sBroken")),
        )

        FirebaseManager.downloadData()

        assertValidTreeLoaded()
        coVerify(exactly = 1) { FirebaseStorageManager.batchDownloadImages(context) }
        TestData.reset()
        DataStoreManager.loadData(context)
        assertValidTreeLoaded()
    }
}
