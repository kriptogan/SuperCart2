package com.example.supercart2.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object GroupManager {
    
    /**
     * Generate a new group code (UUID)
     */
    fun generateGroupCode(): String {
        val uuid = UUID.randomUUID().toString()
        android.util.Log.d("GroupManager", "Generated group code: $uuid")
        return uuid
    }
    
    /**
     * Create a new group
     * 1. Generate UUID
     * 2. Save to DataStore
     * 3. Populate groupId in all local data
     * 4. Trigger upload to Firebase
     */
    suspend fun createGroup(context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                // Generate group code
                val groupCode = generateGroupCode()
                android.util.Log.d("GroupManager", "Creating group with code: $groupCode")
                
                // Save to DataStore
                DataStoreManager.saveGroupCode(context, groupCode)
                
                android.util.Log.d("GroupManager", "✅ Group created successfully: $groupCode")
                groupCode
            } catch (e: Exception) {
                android.util.Log.e("GroupManager", "❌ Error creating group", e)
                throw e
            }
        }
    }
    
    /**
     * Join an existing group
     * 1. Validate group code exists in Firebase
     * 2. Save to DataStore
     * 3. Download group data (will overwrite local)
     */
    suspend fun joinGroup(context: Context, groupCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("GroupManager", "Attempting to join group: $groupCode")
                
                // Validate UUID format
                if (!isValidGroupCode(groupCode)) {
                    android.util.Log.e("GroupManager", "❌ Invalid group code format")
                    return@withContext false
                }
                
                // Check if group exists in Firebase
                val groupExists = FirebaseManager.checkGroupExists(groupCode)
                if (!groupExists) {
                    android.util.Log.e("GroupManager", "❌ Group does not exist: $groupCode")
                    return@withContext false
                }
                
                // Save group code
                DataStoreManager.saveGroupCode(context, groupCode)
                
                // Download group data (will overwrite ALL local data)
                FirebaseManager.downloadData()
                
                android.util.Log.d("GroupManager", "✅ Successfully joined group: $groupCode")
                true
            } catch (e: Exception) {
                android.util.Log.e("GroupManager", "❌ Error joining group", e)
                false
            }
        }
    }
    
    /**
     * Leave the current group
     * 1. Clear group code from DataStore
     * 2. Clear groupId from all local data
     * 3. Keep local data intact
     */
    suspend fun leaveGroup(context: Context) {
        return withContext(Dispatchers.IO) {
            try {
                val currentCode = DataStoreManager.loadGroupCode(context)
                android.util.Log.d("GroupManager", "Leaving group: $currentCode")
                
                // Clear group code (local data stays intact)
                DataStoreManager.clearGroupCode(context)
                
                android.util.Log.d("GroupManager", "✅ Successfully left group (local data preserved)")
            } catch (e: Exception) {
                android.util.Log.e("GroupManager", "❌ Error leaving group", e)
                throw e
            }
        }
    }
    
    /**
     * Get current group code
     */
    suspend fun getCurrentGroupCode(context: Context): String? {
        return DataStoreManager.loadGroupCode(context)
    }
    
    /**
     * Check if user is in a group
     */
    suspend fun isInGroup(context: Context): Boolean {
        return DataStoreManager.hasGroupCode(context)
    }
    
    
    /**
     * Validate group code format (UUID)
     */
    private fun isValidGroupCode(code: String): Boolean {
        return try {
            UUID.fromString(code)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
