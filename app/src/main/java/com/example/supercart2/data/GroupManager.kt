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
     * Create a new group: generate a UUID and save it as the group code.
     * Errors are logged and rethrown.
     */
    suspend fun createGroup(context: Context): String {
        return withContext(Dispatchers.IO) {
            logAndRethrow("❌ Error creating group") { saveNewGroupCode(context) }
        }
    }

    private suspend fun saveNewGroupCode(context: Context): String {
        val groupCode = generateGroupCode()
        android.util.Log.d("GroupManager", "Creating group with code: $groupCode")
        DataStoreManager.saveGroupCode(context, groupCode)
        android.util.Log.d("GroupManager", "✅ Group created successfully: $groupCode")
        return groupCode
    }

    /**
     * Join an existing group
     * 1. Validate group code format and that the group exists in Firebase
     * 2. Save to DataStore
     * 3. Download group data (will overwrite local)
     * Any error is logged and returns false.
     */
    suspend fun joinGroup(context: Context, groupCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            falseOnError("❌ Error joining group") { joinExistingGroup(context, groupCode) }
        }
    }

    private suspend fun joinExistingGroup(context: Context, groupCode: String): Boolean {
        android.util.Log.d("GroupManager", "Attempting to join group: $groupCode")
        if (!isValidGroupCode(groupCode)) return logFailure("❌ Invalid group code format")
        if (!FirebaseManager.checkGroupExists(groupCode)) return logFailure("❌ Group does not exist: $groupCode")
        DataStoreManager.saveGroupCode(context, groupCode)
        FirebaseManager.downloadData() // overwrites ALL local data
        return logSuccess("✅ Successfully joined group: $groupCode")
    }

    /**
     * Leave the current group: clear the group code, keeping local data intact.
     * Errors are logged and rethrown.
     */
    suspend fun leaveGroup(context: Context) {
        return withContext(Dispatchers.IO) {
            logAndRethrow("❌ Error leaving group") { leaveCurrentGroup(context) }
        }
    }

    private suspend fun leaveCurrentGroup(context: Context) {
        val currentCode = DataStoreManager.loadGroupCode(context)
        android.util.Log.d("GroupManager", "Leaving group: $currentCode")
        DataStoreManager.clearGroupCode(context)
        android.util.Log.d("GroupManager", "✅ Successfully left group (local data preserved)")
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

    private fun logFailure(message: String): Boolean {
        android.util.Log.e("GroupManager", message)
        return false
    }

    private fun logSuccess(message: String): Boolean {
        android.util.Log.d("GroupManager", message)
        return true
    }

    private inline fun falseOnError(message: String, block: () -> Boolean): Boolean = try {
        block()
    } catch (e: Exception) {
        android.util.Log.e("GroupManager", message, e)
        false
    }

    private inline fun <T> logAndRethrow(message: String, block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        android.util.Log.e("GroupManager", message, e)
        throw e
    }
}
