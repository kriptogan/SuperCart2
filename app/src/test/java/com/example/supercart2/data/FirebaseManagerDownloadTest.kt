package com.example.supercart2.data

import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.store
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.FirestoreFake.Companion.snapshot
import com.example.supercart2.models.Category
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import com.example.supercart2.models.SubCategory
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseManagerDownloadTest : FirebaseManagerTestBase() {

    private val zone = ZoneId.systemDefault()
    private val t = LocalDateTime.of(2024, 3, 1, 10, 30, 15, 123_000_000)
    private fun ts(time: LocalDateTime) = Timestamp(Date.from(time.atZone(zone).toInstant()))

    private fun groceries() = DataManagerObject.categories.flatMap { it.subCategories }.flatMap { it.groceries }

    private fun installLocal() {
        DataManagerObject.categories.add(node(category("local"), subNode(sub("localSub", "local"))))
        DataManagerObject.stores.add(store("localStore"))
        DataManagerObject.storeCategoryOrders["localStore"] = listOf("local")
    }

    private fun assertLocalUnchanged() {
        assertEquals(listOf("local"), DataManagerObject.categories.map { it.category.uuid })
        assertEquals(listOf("localStore"), DataManagerObject.stores.map { it.uuid })
        assertEquals(mapOf("localStore" to listOf("local")), DataManagerObject.storeCategoryOrders)
    }

    private fun downloadFails(): Exception =
        assertThrows(Exception::class.java) { runTest { FirebaseManager.downloadData() } }

    // ---------- preconditions ----------

    @Test
    fun `downloadData throws without a group code and keeps local data`() {
        installLocal()
        runTest { DataStoreManager.clearGroupCode(context) }
        assertEquals("Group code required for download", downloadFails().message)
        DataStoreManager.globalContext = null
        assertEquals("Group code required for download", downloadFails().message)
        assertLocalUnchanged()
    }

    // ---------- document -> entity mapping ----------

    @Test
    fun `downloadData maps every field, accepting String and Timestamp dates`() = runTest {
        fake.setDocs(
            "categories",
            snapshot(
                "c1",
                mapOf(
                    "uuid" to "ignored", "name" to "Dairy", "default" to true, "viewOrder" to 3L, "protected" to true,
                    "lastUpdate" to "2024-03-01T10:30:15.123", "deleted" to false,
                ),
            ),
        )
        fake.setDocs(
            "subcategories",
            snapshot("s1", mapOf("categoryId" to "c1", "name" to "Milk", "protected" to true, "lastUpdate" to ts(t))),
        )
        val protoTime = com.google.protobuf.Timestamp.newBuilder().setSeconds(1_700_000_000).setNanos(987_654_321).build()
        fake.setDocs(
            "groceries",
            snapshot(
                "g1",
                mapOf(
                    "name" to "Milk", "categoryId" to "c1", "subCategoryId" to "s1", "expirationDate" to "2024-02-29",
                    "inShoppingList" to true, "isBought" to true, "imageUUID" to "img", "averageBuyDays" to 7L,
                    "buyEvents" to listOf("2024-01-01", ts(LocalDateTime.of(2024, 1, 8, 23, 59)), 42, null),
                    "storeIds" to listOf("A", 5, "B"), "lastUpdate" to "2024-03-01T10:30:15.123", "deleted" to true,
                ),
            ),
            snapshot(
                "g2",
                mapOf(
                    "name" to "Eggs", "categoryId" to "c1", "subCategoryId" to "s1",
                    "expirationDate" to ts(LocalDateTime.of(2024, 5, 6, 7, 8)), "lastUpdate" to protoTime,
                ),
            ),
        )
        fake.setDocs(
            "stores",
            snapshot("A", mapOf("name" to "Shop", "address" to "Main 1", "viewOrder" to 2L, "lastUpdate" to "2024-03-01T10:30:15.123")),
        )

        FirebaseManager.downloadData()

        val node = DataManagerObject.categories.single()
        assertEquals(Category("c1", "Dairy", default = true, viewOrder = 3, protected = true, lastUpdate = t), node.category)
        assertEquals(SubCategory("s1", "c1", "Milk", protected = true, lastUpdate = t), node.subCategories.single().subCategory)
        // Pinned: downloaded deleted groceries are dropped from memory (buildNestedStructure excludes deleted).
        assertEquals(listOf("g2"), groceries().map { it.uuid })
        val protoExpected = java.time.Instant.ofEpochMilli(1_700_000_000_987).atZone(zone).toLocalDateTime()
        assertEquals(
            Grocery(
                uuid = "g2", name = "Eggs", categoryId = "c1", subCategoryId = "s1",
                expirationDate = LocalDate.of(2024, 5, 6), lastUpdate = protoExpected,
            ),
            groceries().single(),
        )
        assertEquals(listOf(Store("A", "Shop", address = "Main 1", viewOrder = 2, lastUpdate = t)), DataManagerObject.stores)
    }

    @Test
    fun `downloadData maps grocery lists, numbers and String dates`() = runTest {
        // Non-deleted variant of g1 above: mixed buyEvents are parsed or skipped, non-String storeIds dropped.
        fake.setDocs("categories", snapshot("c1", mapOf("name" to "c")))
        fake.setDocs("subcategories", snapshot("s1", mapOf("categoryId" to "c1", "name" to "s")))
        fake.setDocs(
            "groceries",
            snapshot(
                "g1",
                mapOf(
                    "name" to "Milk", "categoryId" to "c1", "subCategoryId" to "s1", "expirationDate" to "2024-02-29",
                    "inShoppingList" to true, "isBought" to true, "imageUUID" to "img", "averageBuyDays" to 7L,
                    "buyEvents" to listOf("2024-01-01", ts(LocalDateTime.of(2024, 1, 8, 23, 59)), 42, null),
                    "storeIds" to listOf("A", 5, "B"), "lastUpdate" to "2024-03-01T10:30:15.123",
                ),
            ),
        )

        FirebaseManager.downloadData()

        assertEquals(
            Grocery(
                uuid = "g1", name = "Milk", categoryId = "c1", subCategoryId = "s1",
                expirationDate = LocalDate.of(2024, 2, 29), inShoppingList = true, isBought = true,
                buyEvents = listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8)), imageUUID = "img",
                averageBuyDays = 7, storeIds = listOf("A", "B"), lastUpdate = t,
            ),
            groceries().single(),
        )
    }

    @Test
    fun `downloadData uses defaults for missing or mistyped optional fields`() = runTest {
        val before = LocalDateTime.now()
        fake.setDocs("categories", snapshot("c1", mapOf("name" to "c", "viewOrder" to "3", "default" to "yes")))
        fake.setDocs("subcategories", snapshot("s1", mapOf("categoryId" to "c1", "name" to "s", "lastUpdate" to 5L)))
        fake.setDocs(
            "groceries",
            snapshot(
                "g1",
                mapOf(
                    "name" to "g", "categoryId" to "c1", "subCategoryId" to "s1", "expirationDate" to 20240229L,
                    "buyEvents" to "2024-01-01", "storeIds" to "A", "averageBuyDays" to "7", "imageUUID" to 1,
                ),
            ),
        )
        fake.setDocs("stores", snapshot("A", mapOf("name" to "Shop", "viewOrder" to true, "address" to 5)))

        FirebaseManager.downloadData()
        val after = LocalDateTime.now()

        val node = DataManagerObject.categories.single()
        val c = node.category
        assertEquals(Category("c1", "c", default = false, viewOrder = 0, protected = false, lastUpdate = c.lastUpdate), c)
        assertTrue(!c.lastUpdate.isBefore(before) && !c.lastUpdate.isAfter(after))
        val s = node.subCategories.single().subCategory
        assertTrue(!s.lastUpdate.isBefore(before) && !s.lastUpdate.isAfter(after))
        val g = groceries().single()
        assertEquals(Grocery("g1", "g", "c1", "s1", lastUpdate = g.lastUpdate), g)
        val store = DataManagerObject.stores.single()
        assertEquals(Store("A", "Shop", address = "", viewOrder = 0, lastUpdate = store.lastUpdate), store)
    }

    @Test
    fun `downloadData reads Int and Double numbers like Long`() = runTest {
        fake.setDocs("categories", snapshot("c1", mapOf("name" to "c", "viewOrder" to 3)))
        fake.setDocs("subcategories", snapshot("s1", mapOf("categoryId" to "c1", "name" to "s")))
        fake.setDocs(
            "groceries",
            snapshot("g1", mapOf("name" to "g", "categoryId" to "c1", "subCategoryId" to "s1", "averageBuyDays" to 7.0)),
        )
        fake.setDocs("stores", snapshot("A", mapOf("name" to "A", "viewOrder" to 2.0)), snapshot("B", mapOf("name" to "B", "viewOrder" to 4)))

        FirebaseManager.downloadData()

        assertEquals(3, DataManagerObject.categories.single().category.viewOrder)
        assertEquals(7, groceries().single().averageBuyDays)
        assertEquals(listOf(2, 4), DataManagerObject.stores.map { it.viewOrder })
    }

    @Test
    fun `downloadData throws on a missing required field and keeps local data`() {
        val cases = listOf(
            Triple("categories", snapshot("c1", mapOf("default" to true)), "Category c1 missing name"),
            Triple("categories", snapshot("c1", null), "Document c1 has no data"),
            Triple("subcategories", snapshot("s1", mapOf("name" to "s")), "SubCategory s1 missing categoryId"),
            Triple("subcategories", snapshot("s1", mapOf("categoryId" to "c1")), "SubCategory s1 missing name"),
            Triple("groceries", snapshot("g1", mapOf("categoryId" to "c", "subCategoryId" to "s")), "Grocery g1 missing name"),
            Triple("groceries", snapshot("g1", mapOf("name" to "g", "subCategoryId" to "s")), "Grocery g1 missing categoryId"),
            Triple("groceries", snapshot("g1", mapOf("name" to "g", "categoryId" to "c")), "Grocery g1 missing subCategoryId"),
            Triple("stores", snapshot("A", mapOf("viewOrder" to 1L)), "Store A missing name"),
            Triple("stores", snapshot("A", null), "Document A has no data"),
        )
        installLocal()
        for ((collection, doc, message) in cases) {
            fake = FirestoreFake(db, GROUP)
            fake.setDocs(collection, doc)
            val e = downloadFails()
            assertTrue(e is IllegalStateException)
            assertEquals(message, e.message)
        }
        assertLocalUnchanged()
    }

    // ---------- replacing local data ----------

    private fun installRemoteTree() {
        fake.setDocs(
            "categories",
            snapshot("c1", mapOf("name" to "c1")), snapshot("cDel", mapOf("name" to "cDel", "deleted" to true)),
        )
        fake.setDocs("subcategories", snapshot("s1", mapOf("categoryId" to "c1", "name" to "s1")))
        fake.setDocs("groceries", snapshot("g1", mapOf("name" to "g1", "categoryId" to "c1", "subCategoryId" to "s1")))
        fake.setDocs("stores", snapshot("A", mapOf("name" to "A")), snapshot("B", mapOf("name" to "B", "deleted" to true)))
    }

    @Test
    fun `downloadData replaces local data, drops deleted stores, saves and downloads images`() = runTest {
        installLocal()
        DataManagerObject.hiddenStoreIds.add("localStore")
        DataManagerObject.storeSubCategoryOrders["localStore"] = mapOf("local" to listOf("localSub"))
        installRemoteTree()
        fake.setOrders(
            mapOf(
                "orders" to mapOf("A" to listOf("c1"), "B" to "notAList", "C" to listOf("x", 1)),
                "subOrders" to mapOf("A" to mapOf("c1" to listOf("s1", 2), "c2" to "bad"), "B" to listOf("x"), "C" to emptyMap<String, Any>()),
            ),
        )
        val version = DataManagerObject.version

        FirebaseManager.downloadData()

        // Deleted categories and deleted stores are dropped, like DataStoreManager.loadData.
        assertEquals(listOf("c1"), DataManagerObject.categories.map { it.category.uuid })
        assertEquals(listOf("g1"), groceries().map { it.uuid })
        assertEquals(listOf("A"), DataManagerObject.stores.map { it.uuid })
        assertEquals(mapOf("A" to listOf("c1"), "C" to listOf("x")), DataManagerObject.storeCategoryOrders)
        assertEquals(listOf("localStore"), DataManagerObject.hiddenStoreIds.toList())
        assertEquals(mapOf("A" to mapOf("c1" to listOf("s1")), "C" to emptyMap()), DataManagerObject.storeSubCategoryOrders)
        assertEquals(version + 1, DataManagerObject.version)
        coVerify(exactly = 1) { FirebaseStorageManager.batchDownloadImages(context) }

        TestData.reset()
        DataStoreManager.loadData(context)
        assertEquals(listOf("c1"), DataManagerObject.categories.map { it.category.uuid })
    }

    @Test
    fun `downloadData treats a missing, failing or malformed orders document as no orders`() = runTest {
        val setups = listOf<() -> Unit>(
            { fake.setOrders(null) },
            { fake.ordersTask = Tasks.forException(RuntimeException("no doc")) },
            { fake.setOrders(mapOf("orders" to listOf("A"))) },
            { fake.setOrders(mapOf("other" to 1)) },
            { fake.setOrders(mapOf("orders" to listOf("A"), "subOrders" to listOf("A"))) },
        )
        for (setup in setups) {
            DataManagerObject.storeCategoryOrders["old"] = listOf("x")
            DataManagerObject.storeSubCategoryOrders["old"] = mapOf("c" to listOf("s"))
            fake = FirestoreFake(db, GROUP)
            installRemoteTree()
            setup()
            FirebaseManager.downloadData()
            assertEquals(emptyMap<String, List<String>>(), DataManagerObject.storeCategoryOrders)
            assertEquals(emptyMap<String, Map<String, List<String>>>(), DataManagerObject.storeSubCategoryOrders)
        }
    }

    @Test
    fun `downloadData throws on invalid relationships and keeps local data`() {
        installLocal()
        fake.setDocs("subcategories", snapshot("s1", mapOf("categoryId" to "missing", "name" to "s")))
        fake.setDocs("stores", snapshot("A", mapOf("name" to "A")))

        val e = downloadFails()

        assertTrue(e is IllegalStateException)
        assertEquals("Invalid data relationships in downloaded data", e.message)
        assertLocalUnchanged()
        coVerify(exactly = 0) { FirebaseStorageManager.batchDownloadImages(any()) }
    }

    @Test
    fun `downloadData rethrows fetch failures`() {
        installLocal()
        val error = RuntimeException("offline")
        fake.failGet("groceries", error)
        assertSame(error, downloadFails())
        assertLocalUnchanged()
    }

    @Test
    fun `downloadData succeeds when the image download throws`() = runTest {
        installRemoteTree()
        coEvery { FirebaseStorageManager.batchDownloadImages(any()) } throws RuntimeException("storage down")
        FirebaseManager.downloadData()
        assertEquals(listOf("c1"), DataManagerObject.categories.map { it.category.uuid })
    }
}
