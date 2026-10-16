package com.example.supercart2.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.supercart2.TestData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

/**
 * FirebaseManager captures `FirebaseFirestore.getInstance()` once per class loader, so every test
 * shares this single mock instance and re-stubs it through [FirestoreFake].
 */
object FirebaseManagerSharedDb {
    val db: FirebaseFirestore = mockk()

    fun install() {
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns db
    }
}

/** A recorded `WriteBatch.set` call: document path ("collection/id"), data and options (null = no options). */
data class FirestoreWrite(val path: String, val data: Any?, val options: SetOptions?)

/** In-memory stand-in for `groups/{group}/...` in Firestore, backed by mockk. */
class FirestoreFake(private val db: FirebaseFirestore, private val group: String) {
    private val docs = mutableMapOf<String, Task<QuerySnapshot>>()
    private val collections = mutableMapOf<String, CollectionReference>()
    val writes = mutableListOf<FirestoreWrite>()
    var commits = 0
    var commitResult: Task<Void> = Tasks.forResult(null)
    var ordersTask: Task<DocumentSnapshot> = Tasks.forResult(snapshot("orders", null))
    val batch: WriteBatch = mockk()

    init {
        clearMocks(db)
        val groups = mockk<CollectionReference>()
        val groupDoc = mockk<DocumentReference>()
        every { db.collection("groups") } returns groups
        every { groups.document(group) } returns groupDoc
        every { groupDoc.collection(any()) } answers { collection(firstArg()) }
        every { db.batch() } returns batch
        stubBatch()
    }

    fun setDocs(collection: String, vararg snapshots: DocumentSnapshot) {
        docs[collection] = Tasks.forResult(querySnapshot(snapshots.toList()))
    }

    fun failGet(collection: String, error: Exception) {
        docs[collection] = Tasks.forException(error)
    }

    fun setOrders(data: Map<String, Any?>?) {
        ordersTask = Tasks.forResult(snapshot("orders", data))
    }

    private fun queryTask(name: String) = docs[name] ?: Tasks.forResult(querySnapshot(emptyList()))

    private fun collection(name: String): CollectionReference = collections.getOrPut(name) {
        val col = mockk<CollectionReference>()
        val limited = mockk<Query>()
        every { col.get() } answers { queryTask(name) }
        every { col.limit(1) } returns limited
        every { limited.get() } answers { queryTask(name) }
        every { col.document(any()) } answers { docRef("$name/${firstArg<String>()}") }
        col
    }

    private fun docRef(path: String): DocumentReference {
        val ref = mockk<DocumentReference>()
        every { ref.path } returns path
        every { ref.get() } answers { ordersTask }
        return ref
    }

    private fun stubBatch() {
        every { batch.set(any<DocumentReference>(), any<Any>()) } answers {
            writes += FirestoreWrite(firstArg<DocumentReference>().path, secondArg(), null)
            batch
        }
        every { batch.set(any<DocumentReference>(), any<Any>(), any<SetOptions>()) } answers {
            writes += FirestoreWrite(firstArg<DocumentReference>().path, secondArg(), thirdArg())
            batch
        }
        every { batch.commit() } answers { commits++; commitResult }
    }

    companion object {
        fun snapshot(id: String, data: Map<String, Any?>?): DocumentSnapshot {
            val doc = mockk<DocumentSnapshot>()
            every { doc.id } returns id
            @Suppress("UNCHECKED_CAST")
            every { doc.data } returns (data?.toMutableMap() as MutableMap<String, Any>?)
            every { doc.exists() } returns (data != null)
            return doc
        }

        fun querySnapshot(documents: List<DocumentSnapshot>): QuerySnapshot {
            val snapshot = mockk<QuerySnapshot>()
            every { snapshot.documents } returns documents
            every { snapshot.isEmpty } returns documents.isEmpty()
            return snapshot
        }
    }
}

/** Shared set-up: mocked Firestore, group code "G" saved, image sync stubbed out. */
abstract class FirebaseManagerTestBase {
    protected lateinit var context: Context
    protected lateinit var fake: FirestoreFake
    protected val db get() = FirebaseManagerSharedDb.db

    @Before
    fun setUpFirebase() = runTest {
        context = ApplicationProvider.getApplicationContext()
        TestData.reset()
        DataStoreManager.saveData(context)
        DataStoreManager.saveGroupCode(context, GROUP)
        DataStoreManager.setGlobalContext(context)
        FirebaseManagerSharedDb.install()
        fake = FirestoreFake(db, GROUP)
        mockkObject(FirebaseStorageManager)
        coEvery { FirebaseStorageManager.batchUploadImages(any()) } returns 0
        coEvery { FirebaseStorageManager.batchDownloadImages(any()) } returns 0
    }

    @After
    fun tearDownFirebase() {
        unmockkObject(FirebaseStorageManager)
    }

    companion object {
        const val GROUP = "G"
    }
}
