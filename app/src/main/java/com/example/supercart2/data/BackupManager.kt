package com.example.supercart2.data

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manages backup and restore operations for SuperCart data.
 * Backup files are stored in the Downloads folder as ZIP archives
 * containing data.json + an images/ directory.
 * Legacy .json backups (no images) are still supported for import.
 */
object BackupManager {

    private const val BACKUP_FILENAME_PREFIX = "supercart_backup_"
    private const val BACKUP_FILE_EXTENSION = ".zip"
    private const val LEGACY_EXTENSION = ".json"
    private const val ZIP_DATA_ENTRY = "data.json"
    private const val ZIP_IMAGES_DIR = "images/"

    // LocalDate adapter for Gson
    private class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
            JsonPrimitive(src?.format(formatter))

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate? =
            try {
                json?.asString?.let { LocalDate.parse(it, formatter) }
            } catch (e: Exception) {
                Log.e("BackupManager", "Error deserializing LocalDate: ${json?.asString}", e)
                null
            }
    }

    // LocalDateTime adapter for Gson
    private class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
            JsonPrimitive(src?.format(formatter))

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime =
            try {
                json?.asString?.let { LocalDateTime.parse(it, formatter) } ?: LocalDateTime.now()
            } catch (e: Exception) {
                Log.e("BackupManager", "Error deserializing LocalDateTime: ${json?.asString}", e)
                LocalDateTime.now()
            }
    }

    private val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }

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
    suspend fun createBackup(context: Context): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val backupFile = File(downloadsDir, "$BACKUP_FILENAME_PREFIX${timestamp}$BACKUP_FILE_EXTENSION")

            // Collect flat data
            val (categories, subCategories, groceries) = DataConverter.flattenCategories(
                DataManagerObject.categories,
                includeDeleted = true
            )
            val stores = DataManagerObject.stores.toList()
            val hiddenStoreIds = DataManagerObject.hiddenStoreIds.toList()
            val storeCategoryOrders = DataManagerObject.storeCategoryOrders.toMap()

            val backupData = BackupData(
                categories = categories,
                subCategories = subCategories,
                groceries = groceries,
                stores = stores,
                hiddenStoreIds = hiddenStoreIds,
                storeCategoryOrders = storeCategoryOrders
            )
            val jsonContent = gson.toJson(backupData)

            ZipOutputStream(FileOutputStream(backupFile)).use { zip ->

                // 1. data.json
                zip.putNextEntry(ZipEntry(ZIP_DATA_ENTRY))
                zip.write(jsonContent.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 2. images/
                groceries
                    .filter { !it.deleted && it.imageUUID != null }
                    .forEach { grocery ->
                        val uuid = grocery.imageUUID!!
                        val imageFile = ImageManager.getLocalImageFile(uuid, context)
                        if (imageFile != null && imageFile.exists()) {
                            zip.putNextEntry(ZipEntry("$ZIP_IMAGES_DIR$uuid.jpg"))
                            imageFile.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                            Log.d("BackupManager", "  ↳ packed image $uuid")
                        }
                    }
            }

            Log.d("BackupManager", "✅ ZIP backup created: ${backupFile.absolutePath}")
            backupFile
        } catch (e: Exception) {
            Log.e("BackupManager", "❌ Error creating backup", e)
            null
        }
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
        return if (backupFile.name.endsWith(BACKUP_FILE_EXTENSION)) {
            restoreFromZip(context, backupFile)
        } else {
            restoreFromJson(context, backupFile)
        }
    }

    /** Restore from new ZIP format (data.json + images/). */
    private suspend fun restoreFromZip(context: Context, backupFile: File): Boolean {
        return try {
            var backupData: BackupData? = null
            val imageMap = mutableMapOf<String, ByteArray>() // uuid → bytes

            ZipInputStream(FileInputStream(backupFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == ZIP_DATA_ENTRY -> {
                            val json = zip.readBytes().toString(Charsets.UTF_8)
                            backupData = gson.fromJson(json, BackupData::class.java)
                        }
                        entry.name.startsWith(ZIP_IMAGES_DIR) && !entry.isDirectory -> {
                            val uuid = File(entry.name).nameWithoutExtension
                            imageMap[uuid] = zip.readBytes()
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val data = backupData ?: run {
                Log.e("BackupManager", "❌ data.json not found in ZIP")
                return false
            }

            applyBackupData(context, data)

            // Write extracted images to local cache
            imageMap.forEach { (uuid, bytes) ->
                val dest = ImageManager.getImageFile(uuid, context)
                dest.writeBytes(bytes)
                Log.d("BackupManager", "  ↳ restored image $uuid")
            }

            Log.d("BackupManager", "✅ ZIP restored: ${data.groceries.size} groceries, ${imageMap.size} images")
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "❌ Error restoring ZIP", e)
            false
        }
    }

    /** Restore from legacy JSON format (no images). */
    private suspend fun restoreFromJson(context: Context, backupFile: File): Boolean {
        return try {
            val json = backupFile.readText()
            val data = gson.fromJson(json, BackupData::class.java)
            applyBackupData(context, data)
            Log.d("BackupManager", "✅ Legacy JSON restored: ${data.groceries.size} groceries")
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "❌ Error restoring JSON", e)
            false
        }
    }

    /**
     * Validates and applies a BackupData snapshot to DataManagerObject + DataStore.
     * Shared by both restore paths.
     */
    private suspend fun applyBackupData(context: Context, backupData: BackupData) {
        val now = LocalDateTime.now()

        val updatedCategories    = backupData.categories.map    { it.copy(lastUpdate = now) }
        val updatedSubCategories = backupData.subCategories.map { it.copy(lastUpdate = now) }
        val updatedGroceries     = backupData.groceries.map     { it.copy(lastUpdate = now) }
        val updatedStores        = backupData.stores.map        { it.copy(lastUpdate = now) }

        if (!DataConverter.validateRelationships(updatedCategories, updatedSubCategories, updatedGroceries)) {
            throw IllegalArgumentException("Invalid data relationships in backup file")
        }

        val nested = DataConverter.buildNestedStructure(updatedCategories, updatedSubCategories, updatedGroceries)

        DataManagerObject.categories.clear()
        DataManagerObject.categories.addAll(nested)

        DataManagerObject.stores.clear()
        DataManagerObject.stores.addAll(updatedStores.filter { !it.deleted })

        DataManagerObject.hiddenStoreIds.clear()
        DataManagerObject.hiddenStoreIds.addAll(backupData.hiddenStoreIds)

        DataManagerObject.storeCategoryOrders.clear()
        DataManagerObject.storeCategoryOrders.putAll(backupData.storeCategoryOrders)

        DataStoreManager.saveDataGlobally()
        DataManagerObject.updateData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    fun getBackupDirectory(): File? = try {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (dir.exists() || dir.mkdirs()) dir else null
    } catch (e: Exception) {
        Log.e("BackupManager", "Error accessing Downloads directory", e)
        null
    }

    /**
     * Lists all backup files (both .zip and legacy .json) in Downloads,
     * sorted newest first.
     */
    fun listBackupFiles(): List<File> = try {
        val dir = getBackupDirectory() ?: return emptyList()
        dir.listFiles()
            ?.filter { it.name.startsWith(BACKUP_FILENAME_PREFIX) &&
                       (it.name.endsWith(BACKUP_FILE_EXTENSION) || it.name.endsWith(LEGACY_EXTENSION)) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    } catch (e: Exception) {
        Log.e("BackupManager", "Error listing backup files", e)
        emptyList()
    }
}
