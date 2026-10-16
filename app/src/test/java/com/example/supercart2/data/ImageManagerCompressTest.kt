package com.example.supercart2.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Random

/** ImageManager.compressAndSaveImage: decode from a Uri, EXIF rotation, iterative JPEG compression. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageManagerCompressTest {

    private lateinit var context: Context
    private lateinit var workDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.cacheDir, "images").deleteRecursively()
        workDir = File(context.filesDir, "compress-test").apply { deleteRecursively(); mkdirs() }
    }

    private fun bitmap(width: Int, height: Int, noise: Boolean): Bitmap {
        val random = Random(42)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) for (y in 0 until height) {
            bitmap.setPixel(x, y, if (noise) Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)) else Color.RED)
        }
        return bitmap
    }

    private fun writeJpeg(bitmap: Bitmap, name: String, quality: Int = 100): File =
        File(workDir, name).apply { outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) } }

    private fun decode(file: File): Bitmap = BitmapFactory.decodeFile(file.absolutePath)

    @Test
    fun `small image is saved as jpeg in cacheDir images under the uuid`() = runTest {
        val source = writeJpeg(bitmap(40, 30, noise = false), "small.jpg")

        val saved = ImageManager.compressAndSaveImage(Uri.fromFile(source), "g1", context)

        assertNotNull(saved)
        assertEquals(ImageManager.getImageFile("g1", context), saved)
        assertTrue(saved!!.exists())
        assertTrue(saved.length() in 1..200_000)
        val decoded = decode(saved)
        assertEquals(40, decoded.width)
        assertEquals(30, decoded.height)
    }

    @Test
    fun `large noisy image is re-compressed down to at most 200KB`() = runTest {
        val source = writeJpeg(bitmap(700, 700, noise = true), "noisy.jpg")
        assertTrue("fixture must start above the limit", source.length() > 200_000)

        val saved = ImageManager.compressAndSaveImage(Uri.fromFile(source), "big", context)!!

        assertTrue(saved.exists())
        assertTrue("was ${saved.length()}", saved.length() <= 200_000)
        assertEquals(700, decode(saved).width) // no down-scaling, only quality reduction
    }

    @Test
    fun `EXIF rotate 90 swaps width and height`() = runTest {
        val source = writeJpeg(bitmap(40, 20, noise = false), "rotated.jpg")
        ExifInterface(source.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val saved = ImageManager.compressAndSaveImage(Uri.fromFile(source), "rot", context)!!

        val decoded = decode(saved)
        assertEquals(20, decoded.width)
        assertEquals(40, decoded.height)
    }

    @Test
    fun `EXIF rotate 270 swaps width and height`() = runTest {
        val source = writeJpeg(bitmap(40, 20, noise = false), "rotated270.jpg")
        ExifInterface(source.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_270.toString())
            saveAttributes()
        }

        val decoded = decode(ImageManager.compressAndSaveImage(Uri.fromFile(source), "rot270", context)!!)

        assertEquals(20, decoded.width)
        assertEquals(40, decoded.height)
    }

    @Test
    fun `EXIF rotate 180 keeps dimensions`() = runTest {
        val source = writeJpeg(bitmap(40, 20, noise = false), "rotated180.jpg")
        ExifInterface(source.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_180.toString())
            saveAttributes()
        }

        val decoded = decode(ImageManager.compressAndSaveImage(Uri.fromFile(source), "rot180", context)!!)

        assertEquals(40, decoded.width)
        assertEquals(20, decoded.height)
    }

    @Test
    fun `missing source returns null and writes nothing`() = runTest {
        val saved = ImageManager.compressAndSaveImage(Uri.fromFile(File(workDir, "nope.jpg")), "none", context)
        assertNull(saved)
        assertNull(ImageManager.getLocalImageFile("none", context))
    }

    private fun imagesDirNames(): List<String> =
        File(context.cacheDir, "images").list()?.sorted() ?: emptyList()

    @Test
    fun `failed write keeps the existing image of the uuid and leaves no partial file`() = runTest {
        val existing = ImageManager.getImageFile("keep", context).apply { writeText("old photo") }
        val source = writeJpeg(bitmap(40, 30, noise = false), "keep-src.jpg")
        mockkConstructor(FileOutputStream::class)
        try {
            every { anyConstructed<FileOutputStream>().write(any<ByteArray>()) } throws IOException("disk full")
            assertNull(ImageManager.compressAndSaveImage(Uri.fromFile(source), "keep", context))
        } finally {
            unmockkConstructor(FileOutputStream::class)
        }
        assertEquals("old photo", existing.readText())
        assertEquals(listOf("keep.jpg"), imagesDirNames())
    }

    @Test
    fun `undecodable source keeps the existing image of the uuid`() = runTest {
        val existing = ImageManager.getImageFile("keep2", context).apply { writeText("old photo") }
        val text = File(workDir, "bad.jpg").apply { writeText("not an image") }
        assertNull(ImageManager.compressAndSaveImage(Uri.fromFile(text), "keep2", context))
        assertEquals("old photo", existing.readText())
    }

    @Test
    fun `saving again under the same uuid replaces the existing image`() = runTest {
        val existing = ImageManager.getImageFile("same", context).apply { writeText("old photo") }
        val source = writeJpeg(bitmap(40, 30, noise = false), "same-src.jpg")

        val saved = ImageManager.compressAndSaveImage(Uri.fromFile(source), "same", context)!!

        assertEquals(existing, saved)
        assertEquals(40, decode(saved).width)
        assertEquals(listOf("same.jpg"), imagesDirNames())
    }

    @Test
    fun `non-image source returns null`() = runTest {
        val text = File(workDir, "text.jpg").apply { writeText("not an image") }
        assertNull(ImageManager.compressAndSaveImage(Uri.fromFile(text), "txt", context))
        assertNull(ImageManager.getLocalImageFile("txt", context))
    }
}
