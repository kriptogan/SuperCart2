package com.example.supercart2.data

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Meal
import com.example.supercart2.models.Store
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

/**
 * Manages backup and restore operations for SuperCart data.
 * Backup files are stored in the Downloads folder as ZIP archives
 * containing data.json + an images/ directory (see BackupManagerZip.kt).
 * Legacy .json backups (no images) are still supported for import.
 */
object BackupManager {

    private const val BACKUP_FILENAME_PREFIX = "supercart_backup_"
    private const val BACKUP_FILE_EXTENSION = ".zip"
    private const val LEGACY_EXTENSION = ".json"

    internal val gson: Gson by lazy { gsonBuilder().setPrettyPrinting().create() }

    /**
     * Data structure written to data.json inside the ZIP.
     */
    data class BackupData(
        val categories: List<Category>,
        val subCategories: List<SubCategory>,
        val groceries: List<Grocery>,
        val stores: List<Store>,
        val hiddenStoreIds: List<String>,
        val storeCategoryOrders: Map<String, List<String>>,
        val storeSubCategoryOrders: Map<String, Map<String, List<String>>> = emptyMap(),
        val meals: List<Meal> = emptyList(),
        val backupDate: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Export
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a ZIP backup in the Downloads folder containing:
     *  - data.json  : all app data
     *  - images/<uuid>.jpg : every grocery image that exists locally
     *
     * @return The created File, or null on failure.
     */
    suspend fun createBackup(context: Context): File? = logErrors("❌ Error creating backup", fallback = null) {
        val backupFile = newBackupFile()
        writeBackupZip(backupFile, currentBackupData(), context)
        Log.d("BackupManager", "✅ ZIP backup created: ${backupFile.absolutePath}")
        backupFile
    }

    private fun newBackupFile(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        return File(downloadsDir, "$BACKUP_FILENAME_PREFIX${System.currentTimeMillis()}$BACKUP_FILE_EXTENSION")
    }

    // All in-memory data, including deleted items
    private fun currentBackupData(): BackupData {
        val dm = DataManagerObject
        val (categories, subCategories, groceries) = DataConverter.flattenCategories(dm.categories, includeDeleted = true)
        return BackupData(
            categories, subCategories, groceries, dm.stores.toList(), dm.hiddenStoreIds.toList(),
            dm.storeCategoryOrders.toMap(), dm.storeSubCategoryOrders.toMap(), dm.meals.toList(),
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Import
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Restores data from a backup file.
     * Supports both the new .zip format (with images) and legacy .json files.
     *
     * @return true if successful, false otherwise.
     */
    suspend fun restoreFromBackup(context: Context, backupFile: File): Boolean {
        if (!backupFile.exists() || !backupFile.canRead()) {
            Log.e("BackupManager", "❌ File not accessible: ${backupFile.absolutePath}")
            return false
        }
        val isZip = backupFile.name.endsWith(BACKUP_FILE_EXTENSION)
        return if (isZip) restoreFromZip(context, backupFile) else restoreFromJson(context, backupFile)
    }

    /** Restore from new ZIP format (data.json + images/). */
    private suspend fun restoreFromZip(context: Context, backupFile: File): Boolean =
        logErrors("❌ Error restoring ZIP", fallback = false) {
            val contents = ZipInputStream(FileInputStream(backupFile)).use { readBackupZip(it) }
            applyZipContents(context, contents)
        }

    private suspend fun applyZipContents(context: Context, contents: BackupZipContents): Boolean {
        val data = contents.data ?: return logMissingData()
        applyBackupData(context, data)
        restoreImages(contents.images, context)
        Log.d("BackupManager", "✅ ZIP restored: ${data.groceries.size} groceries, ${contents.images.size} images")
        return true
    }

    private fun logMissingData(): Boolean {
        Log.e("BackupManager", "❌ data.json not found in ZIP")
        return false
    }

    // Write extracted images to local cache
    private fun restoreImages(images: Map<String, ByteArray>, context: Context) {
        images.forEach { (uuid, bytes) ->
            ImageManager.getImageFile(uuid, context).writeBytes(bytes)
            Log.d("BackupManager", "  ↳ restored image $uuid")
        }
    }

    /** Restore from legacy JSON format (no images). */
    private suspend fun restoreFromJson(context: Context, backupFile: File): Boolean =
        logErrors("❌ Error restoring JSON", fallback = false) {
            val data = gson.fromJson(backupFile.readText(), BackupData::class.java)
            applyBackupData(context, data)
            Log.d("BackupManager", "✅ Legacy JSON restored: ${data.groceries.size} groceries")
            true
        }

    /**
     * Validates and applies a BackupData snapshot to DataManagerObject + DataStore.
     * Shared by both restore paths. Every lastUpdate is re-stamped so the restore wins the next upload.
     * Atomic: everything is normalized and validated before any in-memory data is replaced.
     */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun applyBackupData(context: Context, backupData: BackupData) {
        val data = RestoredData(backupData, LocalDateTime.now())
        requireValidRelationships(data)
        data.replaceInMemory()
        DataStoreManager.saveDataGlobally()
        DataManagerObject.updateData()
    }

    private fun requireValidRelationships(data: RestoredData) =
        require(DataConverter.validateRelationships(data.categories, data.subCategories, data.groceries)) {
            "Invalid data relationships in backup file"
        }

    /**
     * The backup normalized (Gson leaves missing fields null) with every lastUpdate set to [now].
     * Construction throws when the category lists or a required entity field are missing.
     * Missing stores, hidden store ids, per-store orders and meals become empty (older backups lack some of them).
     */
    private class RestoredData(backupData: BackupData, now: LocalDateTime) {
        val categories = backupData.categories.required("categories").map { it.normalized().copy(lastUpdate = now) }
        val subCategories = backupData.subCategories.required("subCategories").map { it.normalized().copy(lastUpdate = now) }
        val groceries = backupData.groceries.required("groceries").map { it.normalized().copy(lastUpdate = now) }
        val stores = backupData.stores.orEmpty().map { it.normalized().copy(lastUpdate = now) }
        val hiddenStoreIds = backupData.hiddenStoreIds.orEmpty()
        val storeCategoryOrders = backupData.storeCategoryOrders.orEmpty()
        val storeSubCategoryOrders = backupData.storeSubCategoryOrders.orEmpty()
        val meals = backupData.meals.orEmpty().map { it.normalized().copy(lastUpdate = now) }

        fun replaceInMemory() {
            DataManagerObject.replaceCategoriesAndStores(categories, subCategories, groceries, stores)
            replaceOrders(hiddenStoreIds, storeCategoryOrders, storeSubCategoryOrders)
            replaceMeals(meals)
        }
    }

    private fun <T> List<T>?.required(name: String): List<T> = this ?: throw IllegalArgumentException("Backup has no $name")

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    fun getBackupDirectory(): File? = logErrors("Error accessing Downloads directory", fallback = null) {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (dir.exists() || dir.mkdirs()) dir else null
    }

    /**
     * Lists all backup files (both .zip and legacy .json) in Downloads,
     * sorted newest first.
     */
    fun listBackupFiles(): List<File> = logErrors("Error listing backup files", fallback = emptyList()) {
        val dir = getBackupDirectory() ?: return emptyList()
        dir.listFiles()?.filter { it.isBackupFile() }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    private fun File.isBackupFile() = name.startsWith(BACKUP_FILENAME_PREFIX) &&
        (name.endsWith(BACKUP_FILE_EXTENSION) || name.endsWith(LEGACY_EXTENSION))

    /** Runs [block]; any Exception is logged with [message] and [fallback] is returned instead. */
    private inline fun <T> logErrors(message: String, fallback: T, block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        Log.e("BackupManager", message, e)
        fallback
    }
}
