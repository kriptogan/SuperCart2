package com.example.supercart2.data

import android.os.Looper
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.example.supercart2.TestData.OLD
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.store
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var context: Context
    private val dm = DataManagerObject

    // Run Compose snapshot notifications still queued on the main looper before Robolectric discards them;
    // a lost one leaves later Compose tests unable to go idle (AppNotIdleException).
    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        TestData.reset()
        BackupManager.listBackupFiles().forEach { it.delete() }
    }

    private fun installData() {
        dm.categories.add(
            node(
                category("c1"),
                subNode(
                    sub("s1", "c1"),
                    grocery("g1", "c1", "s1", storeIds = listOf("A")).copy(
                        imageUUID = "img1",
                        buyEvents = listOf(LocalDate.of(2024, 5, 1)),
                        expirationDate = LocalDate.of(2024, 6, 1),
                    ),
                    grocery("g2", "c1", "s1", deleted = true).copy(imageUUID = "img2"),
                ),
            )
        )
        dm.stores.addAll(listOf(store("A"), store("B", deleted = true)))
        dm.hiddenStoreIds.add("A")
        dm.storeCategoryOrders["A"] = listOf("c1")
        dm.storeSubCategoryOrders["A"] = mapOf("c1" to listOf("s1"))
    }

    @Test
    fun `createBackup writes a zip with data json and images of non-deleted groceries`() = runTest {
        installData()
        ImageManager.getImageFile("img1", context).writeBytes(byteArrayOf(1, 2, 3))
        ImageManager.getImageFile("img2", context).writeBytes(byteArrayOf(9))

        val file = BackupManager.createBackup(context)

        assertNotNull(file)
        assertTrue(file!!.name.matches(Regex("supercart_backup_\\d+\\.zip")))
        ZipFile(file).use { zip ->
            assertEquals(setOf("data.json", "images/img1.jpg"), zip.entries().toList().map { it.name }.toSet())
            val json = zip.getInputStream(zip.getEntry("data.json")).readBytes().toString(Charsets.UTF_8)
            assertTrue(json.contains("\"g2\"")) // deleted groceries are kept in data
            assertTrue(json.contains("\"expirationDate\": \"2024-06-01\""))
        }
        assertEquals(listOf(file), BackupManager.listBackupFiles())
    }

    @Test
    fun `backup then restore round-trips data and images, stamping lastUpdate`() = runTest {
        installData()
        ImageManager.getImageFile("img1", context).writeBytes(byteArrayOf(1, 2, 3))
        val file = BackupManager.createBackup(context)!!
        val before = dm.categories.single().subCategories.single().groceries.first()

        TestData.reset()
        ImageManager.deleteLocalImage("img1", context)
        val v = dm.version

        assertTrue(BackupManager.restoreFromBackup(context, file))

        val restored = dm.categories.single().subCategories.single().groceries.single() // deleted g2 hidden
        assertEquals(before.copy(lastUpdate = restored.lastUpdate), restored)
        assertTrue(restored.lastUpdate.isAfter(OLD))
        assertTrue(dm.categories.single().category.lastUpdate.isAfter(OLD))
        assertEquals(listOf("A"), dm.stores.map { it.uuid })
        assertTrue(dm.stores.single().lastUpdate.isAfter(OLD))
        assertEquals(listOf("A"), dm.hiddenStoreIds.toList())
        assertEquals(mapOf("A" to listOf("c1")), dm.storeCategoryOrders)
        // Pinned: sub-category orders are not part of backups
        assertTrue(dm.storeSubCategoryOrders.isEmpty())
        assertTrue(dm.version > v)
        assertEquals(listOf<Byte>(1, 2, 3), ImageManager.getLocalImageFile("img1", context)!!.readBytes().toList())
    }

    @Test
    fun `restore accepts legacy json files`() = runTest {
        val json = File(tmp.root, "supercart_backup_1.json")
        json.writeText(
            """
            {
              "categories": [{"uuid":"c1","name":"Cat","default":false,"viewOrder":0,"protected":false,"lastUpdate":"2020-01-01T00:00:00","deleted":false}],
              "subCategories": [{"uuid":"s1","categoryId":"c1","name":"Sub","protected":false,"lastUpdate":"2020-01-01T00:00:00","deleted":false}],
              "groceries": [{"uuid":"g1","name":"Milk","categoryId":"c1","subCategoryId":"s1","storeIds":[],"buyEvents":["2024-01-02"],"lastUpdate":"not-a-date"}],
              "stores": [],
              "hiddenStoreIds": [],
              "storeCategoryOrders": {}
            }
            """.trimIndent()
        )

        assertTrue(BackupManager.restoreFromBackup(context, json))

        val g = dm.categories.single().subCategories.single().groceries.single()
        assertEquals("Milk", g.name)
        assertEquals(listOf(LocalDate.of(2024, 1, 2)), g.buyEvents)
        assertNull(g.expirationDate)
        assertFalse(g.inShoppingList)
        assertTrue(g.lastUpdate.isAfter(OLD)) // unparseable date tolerated, then re-stamped
    }

    @Test
    fun `restore fails when a grocery lacks non-null list fields`() = runTest {
        // Pinned: Gson bypasses Kotlin defaults, leaving storeIds/buyEvents null; the re-stamping
        // copy() then throws on the non-null parameter and the whole restore is rejected.
        val json = File(tmp.root, "old.json")
        json.writeText(
            """{"categories":[{"uuid":"c1","name":"Cat","lastUpdate":"2020-01-01T00:00:00"}],
               "subCategories":[{"uuid":"s1","categoryId":"c1","name":"Sub","lastUpdate":"2020-01-01T00:00:00"}],
               "groceries":[{"uuid":"g1","name":"Milk","categoryId":"c1","subCategoryId":"s1","lastUpdate":"2020-01-01T00:00:00"}],
               "stores":[],"hiddenStoreIds":[],"storeCategoryOrders":{}}"""
        )
        assertFalse(BackupManager.restoreFromBackup(context, json))
        assertTrue(dm.categories.isEmpty())
    }

    @Test
    fun `restore rejects invalid relationships without touching current data`() = runTest {
        installData()
        val json = File(tmp.root, "bad.json")
        json.writeText(
            """{"categories":[],"subCategories":[{"uuid":"s1","categoryId":"missing","name":"x","lastUpdate":"2020-01-01T00:00:00"}],
               "groceries":[],"stores":[],"hiddenStoreIds":[],"storeCategoryOrders":{}}"""
        )
        assertFalse(BackupManager.restoreFromBackup(context, json))
        assertEquals(listOf("c1"), dm.categories.map { it.category.uuid })
    }

    @Test
    fun `restore fails for missing file, garbage, or zip without data json`() = runTest {
        assertFalse(BackupManager.restoreFromBackup(context, File(tmp.root, "nope.zip")))

        val garbage = File(tmp.root, "garbage.json").apply { writeText("{not json") }
        assertFalse(BackupManager.restoreFromBackup(context, garbage))

        val zip = File(tmp.root, "empty.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("images/x.jpg"))
            out.write(byteArrayOf(1))
            out.closeEntry()
        }
        assertFalse(BackupManager.restoreFromBackup(context, zip))
        assertNull(ImageManager.getLocalImageFile("x", context)) // images not written when data is missing
    }

    @Test
    fun `listBackupFiles only lists backup-named zip and json files, newest first`() {
        val dir = BackupManager.getBackupDirectory()!!
        val older = File(dir, "supercart_backup_1.json").apply { writeText("{}"); setLastModified(1_000_000) }
        val newer = File(dir, "supercart_backup_2.zip").apply { writeText(""); setLastModified(2_000_000) }
        File(dir, "supercart_backup_3.txt").writeText("")
        File(dir, "other_4.zip").writeText("")

        assertEquals(listOf(newer, older), BackupManager.listBackupFiles())
    }

    // ---------- extra characterization pins ----------

    @Test
    fun `restore keeps in-memory sub-category orders, saves globally and skips image directory entries`() = runTest {
        installData()
        val file = BackupManager.createBackup(context)!!
        val withDir = File(tmp.root, "withdir.zip")
        ZipOutputStream(withDir.outputStream()).use { out ->
            ZipFile(file).use { zip ->
                out.putNextEntry(ZipEntry("data.json"))
                zip.getInputStream(zip.getEntry("data.json")).copyTo(out)
                out.closeEntry()
            }
            out.putNextEntry(ZipEntry("images/"))
            out.closeEntry()
            out.putNextEntry(ZipEntry("other/y.jpg"))
            out.write(byteArrayOf(5))
            out.closeEntry()
        }
        TestData.reset()
        dm.storeSubCategoryOrders["Z"] = mapOf("c" to listOf("s"))
        DataStoreManager.setGlobalContext(context)

        assertTrue(BackupManager.restoreFromBackup(context, withDir))

        assertEquals(mapOf("Z" to mapOf("c" to listOf("s"))), dm.storeSubCategoryOrders)
        assertNull(ImageManager.getLocalImageFile("y", context))
        TestData.reset()
        DataStoreManager.loadData(context) // restore was persisted through saveDataGlobally
        assertEquals(listOf("c1"), dm.categories.map { it.category.uuid })
    }

    @Test
    fun `createBackup skips images whose local file is missing`() = runTest {
        installData() // no image files written
        val file = BackupManager.createBackup(context)!!
        ZipFile(file).use { zip -> assertEquals(listOf("data.json"), zip.entries().toList().map { it.name }) }
    }

    @Test
    fun `non-zip extension is restored as legacy json`() = runTest {
        installData()
        val zip = BackupManager.createBackup(context)!!
        val renamed = File(tmp.root, "backup.bak").apply { writeBytes(zip.readBytes()) }
        assertFalse(BackupManager.restoreFromBackup(context, renamed))
    }

    @Test
    fun `restore of json without hiddenStoreIds fails half-way after replacing categories (pinned)`() = runTest {
        installData()
        val json = File(tmp.root, "partial.json")
        json.writeText(
            """{"categories":[{"uuid":"n1","name":"New","lastUpdate":"2020-01-01T00:00:00"}],
               "subCategories":[],"groceries":[],"stores":[],"storeCategoryOrders":{}}"""
        )
        assertFalse(BackupManager.restoreFromBackup(context, json))
        assertEquals(listOf("n1"), dm.categories.map { it.category.uuid })
        assertTrue(dm.stores.isEmpty())
        assertTrue(dm.hiddenStoreIds.isEmpty()) // cleared before the null list failed
        assertEquals(mapOf("A" to listOf("c1")), dm.storeCategoryOrders) // never reached
    }
}
