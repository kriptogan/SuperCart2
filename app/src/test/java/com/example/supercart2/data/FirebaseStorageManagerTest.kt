package com.example.supercart2.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
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
import java.io.File

/** FirebaseStorageManager's lazy `storage` is created once per class loader, so all tests share this mock. */
object FirebaseStorageManagerSharedStorage {
    const val BUCKET = "gs://supercart2-58caf.firebasestorage.app"
    val storage: FirebaseStorage = mockk()
    var bucketInstanceCalls = 0
    var defaultInstanceCalls = 0

    fun install() {
        mockkStatic(FirebaseStorage::class)
        every { FirebaseStorage.getInstance(BUCKET) } answers { bucketInstanceCalls++; storage }
        every { FirebaseStorage.getInstance() } answers { defaultInstanceCalls++; storage }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseStorageManagerTest {

    private lateinit var context: Context
    private val storage = FirebaseStorageManagerSharedStorage.storage
    private val root = mockk<StorageReference>()
    private val refs = mutableMapOf<String, StorageReference>()
    private var listAllResult: Task<ListResult> = completed(mockk<ListResult>().also { every { it.items } returns emptyList() })

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        TestData.reset()
        File(context.cacheDir, "images").deleteRecursively()
        FirebaseStorageManagerSharedStorage.install()
        clearMocks(storage)
        every { storage.reference } returns root
        every { root.bucket } returns "bucket"
        every { root.path } returns "/"
        every { root.listAll() } answers { listAllResult }
        every { root.child(any()) } answers { ref(firstArg()) }
    }

    /** A mocked, already-completed Task (Tasks.forResult can't produce UploadTask/FileDownloadTask subclasses). */
    private inline fun <reified T : Task<R>, R> completedTask(result: R?, error: Exception?): T {
        val task = mockk<T>()
        every { task.isComplete } returns true
        every { task.isCanceled } returns false
        every { task.exception } returns error
        every { task.result } returns result
        return task
    }

    private fun completed(listResult: ListResult): Task<ListResult> = completedTask<Task<ListResult>, ListResult>(listResult, null)

    private fun ref(path: String): StorageReference = refs.getOrPut(path) {
        val ref = mockk<StorageReference>()
        every { ref.path } returns "/$path"
        every { ref.bucket } returns "bucket"
        every { ref.putFile(any()) } returns completedTask<UploadTask, UploadTask.TaskSnapshot>(mockk(), null)
        every { ref.getFile(any<File>()) } returns completedTask<FileDownloadTask, FileDownloadTask.TaskSnapshot>(mockk(), null)
        ref
    }

    private fun failUpload(uuid: String, error: Exception = RuntimeException("upload failed")) {
        val imageRef = ref("grocery_images/$uuid.jpg")
        every { imageRef.putFile(any()) } returns completedTask<UploadTask, UploadTask.TaskSnapshot>(null, error)
    }

    private fun failDownload(uuid: String) {
        val imageRef = ref("grocery_images/$uuid.jpg")
        every { imageRef.getFile(any<File>()) } returns
            completedTask<FileDownloadTask, FileDownloadTask.TaskSnapshot>(null, RuntimeException("download failed"))
    }

    private fun storageException(code: Int) = mockk<StorageException>(relaxed = true).also { every { it.errorCode } returns code }

    private fun failConnection(error: Exception) {
        listAllResult = completedTask<Task<ListResult>, ListResult>(null, error)
    }

    private fun localImage(uuid: String) = ImageManager.getImageFile(uuid, context).apply { writeBytes(byteArrayOf(1)) }

    private fun installGroceries() {
        val live = subNode(
            sub("s1", "c1"),
            grocery("gLocal", "c1", "s1").copy(imageUUID = "local"),
            grocery("gMissing", "c1", "s1").copy(imageUUID = "missing"),
            grocery("gFailing", "c1", "s1").copy(imageUUID = "failing"),
            grocery("gDeleted", "c1", "s1", deleted = true).copy(imageUUID = "deleted"),
            grocery("gNoImage", "c1", "s1"),
        )
        val inDeletedSub = subNode(sub("sDel", "c1", deleted = true), grocery("gInDeletedSub", "c1", "sDel").copy(imageUUID = "deletedSub"))
        val orphan = subNode(sub("s2", "cDel"), grocery("gInDeletedCat", "cDel", "s2").copy(imageUUID = "orphan"))
        DataManagerObject.categories.add(node(category("c1"), live, inDeletedSub))
        DataManagerObject.categories.add(node(category("cDel", deleted = true), orphan))
    }

    // ---------- testStorageConnection ----------

    @Test
    fun `testStorageConnection lists the root of the bucket-specific instance`() = runTest {
        assertTrue(FirebaseStorageManager.testStorageConnection())
        verify { root.listAll() }
        assertEquals(1, FirebaseStorageManagerSharedStorage.bucketInstanceCalls)
        assertEquals(0, FirebaseStorageManagerSharedStorage.defaultInstanceCalls)
    }

    @Test
    fun `testStorageConnection is false on storage or other errors`() = runTest {
        failConnection(storageException(StorageException.ERROR_BUCKET_NOT_FOUND))
        assertFalse(FirebaseStorageManager.testStorageConnection())
        failConnection(storageException(StorageException.ERROR_UNKNOWN))
        assertFalse(FirebaseStorageManager.testStorageConnection())
        failConnection(RuntimeException("offline"))
        assertFalse(FirebaseStorageManager.testStorageConnection())
    }

    // ---------- single image ----------

    @Test
    fun `uploadImage puts the file under grocery_images`() = runTest {
        val file = localImage("abc")
        assertTrue(FirebaseStorageManager.uploadImage(file, "abc"))
        verify { ref("grocery_images/abc.jpg").putFile(Uri.fromFile(file)) }
    }

    @Test
    fun `uploadImage is false on any failure`() = runTest {
        val file = localImage("abc")
        val codes = listOf(
            StorageException.ERROR_OBJECT_NOT_FOUND, StorageException.ERROR_NOT_AUTHENTICATED,
            StorageException.ERROR_NOT_AUTHORIZED, StorageException.ERROR_RETRY_LIMIT_EXCEEDED,
        )
        codes.forEach {
            failUpload("abc", storageException(it))
            assertFalse(FirebaseStorageManager.uploadImage(file, "abc"))
        }
        failUpload("abc")
        assertFalse(FirebaseStorageManager.uploadImage(file, "abc"))
    }

    @Test
    fun `downloadImage writes into the image cache file and returns it`() = runTest {
        val expected = ImageManager.getImageFile("abc", context)
        assertEquals(expected, FirebaseStorageManager.downloadImage("abc", context))
        verify { ref("grocery_images/abc.jpg").getFile(expected) }
    }

    @Test
    fun `downloadImage returns null on failure`() = runTest {
        failDownload("abc")
        assertNull(FirebaseStorageManager.downloadImage("abc", context))
    }

    // ---------- batch upload ----------

    @Test
    fun `batchUploadImages uploads existing local images of visible groceries and counts successes`() = runTest {
        installGroceries()
        localImage("local")
        localImage("failing")
        localImage("deleted")
        localImage("orphan")
        localImage("deletedSub")
        failUpload("failing")

        // Deleted groceries and groceries inside a deleted category or sub-category are not uploaded.
        assertEquals(1, FirebaseStorageManager.batchUploadImages(context))

        verify(exactly = 1) { ref("grocery_images/local.jpg").putFile(any()) }
        verify(exactly = 1) { ref("grocery_images/failing.jpg").putFile(any()) }
        assertEquals(setOf("grocery_images/local.jpg", "grocery_images/failing.jpg"), refs.keys)
    }

    @Test
    fun `batchUploadImages returns 0 without uploading when the connection test fails`() = runTest {
        installGroceries()
        localImage("local")
        failConnection(RuntimeException("offline"))
        assertEquals(0, FirebaseStorageManager.batchUploadImages(context))
        verify(exactly = 0) { root.child(any()) }
    }

    // ---------- batch download ----------

    @Test
    fun `batchDownloadImages downloads only missing images of visible groceries and counts successes`() = runTest {
        installGroceries()
        localImage("local")
        failDownload("failing")

        assertEquals(1, FirebaseStorageManager.batchDownloadImages(context)) // "missing"

        assertEquals(setOf("grocery_images/missing.jpg", "grocery_images/failing.jpg"), refs.keys)
        verify(exactly = 1) { ref("grocery_images/missing.jpg").getFile(ImageManager.getImageFile("missing", context)) }
    }

    @Test
    fun `batchDownloadImages returns 0 without downloading when the connection test fails`() = runTest {
        installGroceries()
        failConnection(storageException(StorageException.ERROR_BUCKET_NOT_FOUND))
        assertEquals(0, FirebaseStorageManager.batchDownloadImages(context))
        verify(exactly = 0) { root.child(any()) }
    }
}
