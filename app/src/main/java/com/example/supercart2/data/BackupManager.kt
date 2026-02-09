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
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Manages backup and restore operations for SuperCart data.
 * Backup files are stored in the Downloads folder.
 */
object BackupManager {
    
    private const val BACKUP_FILENAME_PREFIX = "supercart_backup_"
    private const val BACKUP_FILE_EXTENSION = ".json"
    
    // LocalDate adapter for Gson
    private class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        
        override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter))
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate? {
            return try {
                json?.asString?.let { LocalDate.parse(it, formatter) }
            } catch (e: Exception) {
                Log.e("BackupManager", "Error deserializing LocalDate: ${json?.asString}", e)
                null
            }
        }
    }
    
    // LocalDateTime adapter for Gson
    private class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter))
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime {
            return try {
                json?.asString?.let { LocalDateTime.parse(it, formatter) }
                    ?: LocalDateTime.now()
            } catch (e: Exception) {
                Log.e("BackupManager", "Error deserializing LocalDateTime: ${json?.asString}", e)
                LocalDateTime.now()
            }
        }
    }
    
    // Configured Gson instance with LocalDate and LocalDateTime support
    private val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }
    
    /**
     * Data structure for backup file
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
    
    /**
     * Creates a backup file in the Downloads folder.
     * @return The File object if successful, null otherwise
     */
    suspend fun createBackup(context: Context): File? {
        return try {
            // Get Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            // Create backup filename with timestamp
            val timestamp = System.currentTimeMillis()
            val backupFile = File(downloadsDir, "${BACKUP_FILENAME_PREFIX}${timestamp}${BACKUP_FILE_EXTENSION}")
            
            // Get all data (including deleted items for complete backup)
            val (categories, subCategories, groceries) = DataConverter.flattenCategories(
                DataManagerObject.categories,
                includeDeleted = true
            )
            val stores = DataManagerObject.stores.toList()
            val hiddenStoreIds = DataManagerObject.hiddenStoreIds.toList()
            val storeCategoryOrders = DataManagerObject.storeCategoryOrders.toMap()
            
            // Create backup data structure
            val backupData = BackupData(
                categories = categories,
                subCategories = subCategories,
                groceries = groceries,
                stores = stores,
                hiddenStoreIds = hiddenStoreIds,
                storeCategoryOrders = storeCategoryOrders
            )
            
            // Write to file
            FileWriter(backupFile).use { writer ->
                gson.toJson(backupData, writer)
            }
            
            Log.d("BackupManager", "✅ Backup created successfully: ${backupFile.absolutePath}")
            backupFile
        } catch (e: Exception) {
            Log.e("BackupManager", "❌ Error creating backup", e)
            null
        }
    }
    
    /**
     * Restores data from a backup file.
     * Sets lastUpdate to now() for all restored objects.
     * @param backupFile The backup file to restore from
     * @return true if successful, false otherwise
     */
    suspend fun restoreFromBackup(context: Context, backupFile: File): Boolean {
        return try {
            if (!backupFile.exists() || !backupFile.canRead()) {
                Log.e("BackupManager", "❌ Backup file does not exist or cannot be read: ${backupFile.absolutePath}")
                return false
            }
            
            // Read backup file
            val jsonContent = backupFile.readText()
            val backupData = gson.fromJson(jsonContent, BackupData::class.java)
            
            // Update lastUpdate to now() for all objects
            val now = LocalDateTime.now()
            val updatedCategories = backupData.categories.map { 
                it.copy(lastUpdate = now) 
            }
            val updatedSubCategories = backupData.subCategories.map { 
                it.copy(lastUpdate = now) 
            }
            val updatedGroceries = backupData.groceries.map { 
                it.copy(lastUpdate = now) 
            }
            val updatedStores = backupData.stores.map { 
                it.copy(lastUpdate = now) 
            }
            
            // Validate relationships
            if (!DataConverter.validateRelationships(updatedCategories, updatedSubCategories, updatedGroceries)) {
                Log.e("BackupManager", "❌ Invalid data relationships in backup file")
                return false
            }
            
            // Convert to nested structure
            val nestedData = DataConverter.buildNestedStructure(
                updatedCategories,
                updatedSubCategories,
                updatedGroceries
            )
            
            // Restore data to DataManagerObject
            DataManagerObject.categories.clear()
            DataManagerObject.categories.addAll(nestedData)
            
            // Restore stores (filter out deleted ones)
            DataManagerObject.stores.clear()
            DataManagerObject.stores.addAll(updatedStores.filter { !it.deleted })
            
            // Restore hidden store IDs
            DataManagerObject.hiddenStoreIds.clear()
            DataManagerObject.hiddenStoreIds.addAll(backupData.hiddenStoreIds)
            
            // Restore store category orders
            DataManagerObject.storeCategoryOrders.clear()
            DataManagerObject.storeCategoryOrders.putAll(backupData.storeCategoryOrders)
            
            // Save to DataStore
            DataStoreManager.saveDataGlobally()
            
            Log.d("BackupManager", "✅ Backup restored successfully. Categories: ${updatedCategories.size}, SubCategories: ${updatedSubCategories.size}, Groceries: ${updatedGroceries.size}, Stores: ${updatedStores.size}")
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "❌ Error restoring backup", e)
            false
        }
    }
    
    /**
     * Gets the default backup file location (Downloads folder).
     * Returns null if Downloads folder is not accessible.
     */
    fun getBackupDirectory(): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                downloadsDir
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Error accessing Downloads directory", e)
            null
        }
    }
    
    /**
     * Lists all backup files in the Downloads folder.
     * @return List of backup files, sorted by modification date (newest first)
     */
    fun listBackupFiles(): List<File> {
        return try {
            val downloadsDir = getBackupDirectory() ?: return emptyList()
            downloadsDir.listFiles()
                ?.filter { it.name.startsWith(BACKUP_FILENAME_PREFIX) && it.name.endsWith(BACKUP_FILE_EXTENSION) }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("BackupManager", "Error listing backup files", e)
            emptyList()
        }
    }
}
