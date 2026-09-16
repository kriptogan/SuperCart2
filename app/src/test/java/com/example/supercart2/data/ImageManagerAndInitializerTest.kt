package com.example.supercart2.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageManagerAndInitializerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        TestData.reset()
        File(context.cacheDir, "images").deleteRecursively()
    }

    // ---------- ImageManager file helpers ----------

    @Test
    fun `image files live in cacheDir images`() {
        val f = ImageManager.getImageFile("abc", context)
        assertEquals(File(File(context.cacheDir, "images"), "abc.jpg"), f)
        assertTrue(f.parentFile!!.isDirectory) // created on demand
    }

    @Test
    fun `getLocalImageFile returns null until the file exists`() {
        assertNull(ImageManager.getLocalImageFile("abc", context))
        ImageManager.getImageFile("abc", context).writeBytes(byteArrayOf(1))
        assertNotNull(ImageManager.getLocalImageFile("abc", context))
    }

    @Test
    fun `deleteLocalImage`() {
        ImageManager.getImageFile("abc", context).writeBytes(byteArrayOf(1))
        assertTrue(ImageManager.deleteLocalImage("abc", context))
        assertNull(ImageManager.getLocalImageFile("abc", context))
        assertFalse(ImageManager.deleteLocalImage("abc", context))
    }

    @Test
    fun `getAllLocalImageUUIDs lists stored images`() {
        ImageManager.getImageFile("a", context).writeBytes(byteArrayOf(1))
        ImageManager.getImageFile("b", context).writeBytes(byteArrayOf(1))
        assertEquals(setOf("a", "b"), ImageManager.getAllLocalImageUUIDs(context).toSet())
    }

    @Test
    fun `deleteGrocery removes the local image when a global context is set`() {
        DataManagerObject.categories.add(
            node(category("c1"), subNode(sub("s1", "c1"), grocery("g1", "c1", "s1").copy(imageUUID = "img")))
        )
        ImageManager.getImageFile("img", context).writeBytes(byteArrayOf(1))
        DataStoreManager.setGlobalContext(context)

        DataManagerObject.deleteGrocery("g1")

        assertNull(ImageManager.getLocalImageFile("img", context))
    }

    // ---------- DataInitializer ----------

    @Test
    fun `initializeDefaultData creates Other, Vegetables, Fruits with protected General`() = runTest {
        DataInitializer.initializeDefaultData(context)

        val cats = DataManagerObject.categories
        assertEquals(listOf("Other", "Vegetables", "Fruits"), cats.map { it.category.name })
        assertEquals(listOf(1, 2, 3), cats.map { it.category.viewOrder })
        assertEquals(listOf(true, false, false), cats.map { it.category.protected })
        assertTrue(cats.all { it.category.default && !it.category.deleted })
        cats.forEach { c ->
            val s = c.subCategories.single().subCategory
            assertEquals("General", s.name)
            assertEquals(c.category.uuid, s.categoryId)
            assertTrue(s.protected)
        }
    }

    @Test
    fun `initializeDefaultData does nothing when categories exist`() = runTest {
        TestData.installStandardTree()
        DataInitializer.initializeDefaultData(context)
        assertEquals(listOf("c1", "c2"), DataManagerObject.categories.map { it.category.uuid })
    }
}
