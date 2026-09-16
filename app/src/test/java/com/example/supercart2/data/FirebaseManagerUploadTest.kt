package com.example.supercart2.data

import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.store
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.data.FirestoreFake.Companion.snapshot
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.SetOptions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseManagerUploadTest : FirebaseManagerTestBase() {

    private val t = LocalDateTime.of(2024, 3, 1, 10, 30, 15, 123_000_000)

    // ---------- checkGroupExists ----------

    @Test
    fun `checkGroupExists is true when the group has a category`() = runTest {
        fake.setDocs("categories", snapshot("c1", mapOf("name" to "x")))
        assertTrue(FirebaseManager.checkGroupExists(GROUP))
        verify { db.collection("groups") }
    }

    @Test
    fun `checkGroupExists is false when the group has no categories`() = runTest {
        fake.setDocs("stores", snapshot("A", mapOf("name" to "x")))
        assertFalse(FirebaseManager.checkGroupExists(GROUP))
    }

    @Test
    fun `checkGroupExists is false when Firestore fails`() = runTest {
        fake.failGet("categories", RuntimeException("offline"))
        assertFalse(FirebaseManager.checkGroupExists(GROUP))
    }

    @Test
    fun `checkGroupExists is false for an unknown group code`() = runTest {
        assertFalse(FirebaseManager.checkGroupExists("other")) // groups.document("other") is not stubbed -> throws
    }

    // ---------- uploadData: preconditions ----------

    @Test
    fun `uploadData throws without a global context`() {
        DataStoreManager.globalContext = null
        val e = assertThrows(IllegalStateException::class.java) { runTest { FirebaseManager.uploadData() } }
        assertEquals("Group code required for upload", e.message)
        verify(exactly = 0) { db.collection(any()) }
    }

    @Test
    fun `uploadData throws without a group code`() {
        runTest { DataStoreManager.clearGroupCode(context) }
        val e = assertThrows(IllegalStateException::class.java) { runTest { FirebaseManager.uploadData() } }
        assertEquals("Group code required for upload", e.message)
        verify(exactly = 0) { db.batch() }
    }

    // ---------- uploadData: maps written ----------

    @Test
    fun `uploadData writes every entity as a map with ISO dates, including the store address`() = runTest {
        val g = grocery("g1", "c1", "s1", storeIds = listOf("A", "B"), inShoppingList = true, isBought = true).copy(
            name = "Milk", expirationDate = LocalDate.of(2024, 2, 29), imageUUID = "img", averageBuyDays = 7,
            buyEvents = listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8)), lastUpdate = t, deleted = true,
        )
        val c = category("c1", viewOrder = 3).copy(name = "Dairy", default = true, protected = true, lastUpdate = t)
        val s = sub("s1", "c1").copy(name = "Milk products", protected = true, lastUpdate = t)
        DataManagerObject.categories.add(node(c, subNode(s, g)))
        DataManagerObject.stores.add(store("A", viewOrder = 2, deleted = true).copy(name = "Shop", address = "Main 1"))

        FirebaseManager.uploadData()

        val byPath = fake.writes.associateBy { it.path }
        assertEquals(
            mapOf(
                "uuid" to "c1", "name" to "Dairy", "default" to true, "viewOrder" to 3, "protected" to true,
                "lastUpdate" to "2024-03-01T10:30:15.123", "deleted" to false,
            ),
            byPath.getValue("categories/c1").data,
        )
        assertEquals(
            mapOf(
                "uuid" to "s1", "categoryId" to "c1", "name" to "Milk products", "protected" to true,
                "lastUpdate" to "2024-03-01T10:30:15.123", "deleted" to false,
            ),
            byPath.getValue("subcategories/s1").data,
        )
        assertEquals(
            mapOf(
                "uuid" to "g1", "name" to "Milk", "categoryId" to "c1", "subCategoryId" to "s1",
                "expirationDate" to "2024-02-29", "inShoppingList" to true, "isBought" to true,
                "buyEvents" to listOf("2024-01-01", "2024-01-08"), "imageUUID" to "img", "averageBuyDays" to 7,
                "storeIds" to listOf("A", "B"), "lastUpdate" to "2024-03-01T10:30:15.123", "deleted" to true,
            ),
            byPath.getValue("groceries/g1").data,
        )
        assertEquals(
            mapOf(
                "uuid" to "A", "name" to "Shop", "address" to "Main 1", "viewOrder" to 2,
                "lastUpdate" to "2020-01-01T12:00:00", "deleted" to true,
            ),
            byPath.getValue("stores/A").data,
        )
        listOf("categories/c1", "subcategories/s1", "groceries/g1", "stores/A").forEach { assertNull(byPath.getValue(it).options) }
        assertEquals(1, fake.commits)
    }

    @Test
    fun `uploadData writes nulls for a grocery without optional values`() = runTest {
        DataManagerObject.categories.add(node(category("c1"), subNode(sub("s1", "c1"), grocery("g1", "c1", "s1"))))

        FirebaseManager.uploadData()

        val data = fake.writes.single { it.path == "groceries/g1" }.data as Map<*, *>
        assertTrue(data.containsKey("expirationDate") && data["expirationDate"] == null)
        assertTrue(data.containsKey("imageUUID") && data["imageUUID"] == null)
        assertTrue(data.containsKey("averageBuyDays") && data["averageBuyDays"] == null)
        assertEquals(emptyList<String>(), data["buyEvents"])
    }

    @Test
    fun `uploadData merges the store category and sub-category orders into a single document, even when empty`() = runTest {
        FirebaseManager.uploadData()
        val empty = fake.writes.single()
        assertEquals("storeCategoryOrders/orders", empty.path)
        assertEquals(mapOf("orders" to emptyMap<String, Any>(), "subOrders" to emptyMap<String, Any>()), empty.data)
        assertEquals(SetOptions.merge(), empty.options)

        fake = FirestoreFake(db, GROUP)
        DataManagerObject.storeCategoryOrders["A"] = listOf("c2", "c1")
        DataManagerObject.storeSubCategoryOrders["A"] = mapOf("c2" to listOf("s2", "s1"))
        FirebaseManager.uploadData()
        assertEquals(
            mapOf("orders" to mapOf("A" to listOf("c2", "c1")), "subOrders" to mapOf("A" to mapOf("c2" to listOf("s2", "s1")))),
            fake.writes.single().data,
        )
        assertEquals(1, fake.commits)
    }

    // ---------- uploadData: conflict resolution ----------

    private fun remote(id: String, lastUpdate: Any?, vararg extra: Pair<String, Any?>) =
        snapshot(id, mapOf("name" to id, "categoryId" to "c", "subCategoryId" to "s", "lastUpdate" to lastUpdate) + extra)

    @Test
    fun `uploadData only writes items that are new or strictly newer than Firebase`() = runTest {
        val older = "2024-03-01T10:30:15.122"
        val same = "2024-03-01T10:30:15.123"
        val newer = "2024-03-01T10:30:16"
        val ids = listOf("new", "older", "same", "newer")
        DataManagerObject.categories.add(
            node(
                category("cnew").copy(lastUpdate = t),
                *ids.map { id ->
                    subNode(sub("s$id", "cnew").copy(lastUpdate = t), grocery("g$id", "cnew", "s$id").copy(lastUpdate = t))
                }.toTypedArray(),
            )
        )
        listOf("colder", "csame", "cnewer").forEach { DataManagerObject.categories.add(node(category(it).copy(lastUpdate = t))) }
        DataManagerObject.stores.addAll(ids.map { store("st$it").copy(lastUpdate = t) })
        for ((collection, prefix) in listOf("categories" to "c", "subcategories" to "s", "groceries" to "g", "stores" to "st")) {
            fake.setDocs(
                collection,
                remote("${prefix}older", older), remote("${prefix}same", same), remote("${prefix}newer", newer),
                remote("unrelated", newer),
            )
        }

        FirebaseManager.uploadData()

        assertEquals(
            setOf(
                "categories/cnew", "categories/colder", "subcategories/snew", "subcategories/solder",
                "groceries/gnew", "groceries/golder", "stores/stnew", "stores/stolder", "storeCategoryOrders/orders",
            ),
            fake.writes.map { it.path }.toSet(),
        )
        assertEquals(9, fake.writes.size)
        assertEquals(1, fake.commits)
    }

    @Test
    fun `uploadData compares against Firestore Timestamp lastUpdate values`() = runTest {
        DataManagerObject.stores.addAll(listOf(store("A").copy(lastUpdate = t), store("B").copy(lastUpdate = t)))
        val zone = java.time.ZoneId.systemDefault()
        fun ts(time: LocalDateTime) = com.google.firebase.Timestamp(java.util.Date.from(time.atZone(zone).toInstant()))
        fake.setDocs("stores", remote("A", ts(t.minusSeconds(1))), remote("B", ts(t.plusSeconds(1))))

        FirebaseManager.uploadData()

        assertEquals(listOf("stores/A", "storeCategoryOrders/orders"), fake.writes.map { it.path })
    }

    @Test
    fun `uploadData throws and commits nothing when an existing Firebase document is invalid`() {
        DataManagerObject.stores.add(store("A"))
        fake.setDocs("groceries", snapshot("g1", mapOf("name" to "x")))
        val e = assertThrows(IllegalStateException::class.java) { runTest { FirebaseManager.uploadData() } }
        assertEquals("Grocery g1 missing categoryId", e.message)
        assertEquals(0, fake.commits)
        assertTrue(fake.writes.isEmpty())
    }

    @Test
    fun `uploadData rethrows fetch failures`() {
        val error = RuntimeException("offline")
        fake.failGet("stores", error)
        val thrown = assertThrows(RuntimeException::class.java) { runTest { FirebaseManager.uploadData() } }
        assertSame(error, thrown)
        assertEquals(0, fake.commits)
    }

    // ---------- uploadData: images ----------

    @Test
    fun `uploadData uploads images after committing`() = runTest {
        FirebaseManager.uploadData()
        coVerify(exactly = 1) { FirebaseStorageManager.batchUploadImages(context) }
    }

    @Test
    fun `uploadData succeeds when the image upload throws`() = runTest {
        coEvery { FirebaseStorageManager.batchUploadImages(any()) } throws RuntimeException("storage down")
        FirebaseManager.uploadData()
        assertEquals(1, fake.commits)
    }

    @Test
    fun `uploadData rethrows a failed commit and skips images`() {
        val error = RuntimeException("commit failed")
        fake.commitResult = Tasks.forException(error)
        val thrown = assertThrows(RuntimeException::class.java) { runTest { FirebaseManager.uploadData() } }
        assertSame(error, thrown)
        coVerify(exactly = 0) { FirebaseStorageManager.batchUploadImages(any()) }
        TestData.reset()
    }
}
