package com.example.supercart2.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** UI state of the store detail sub-screen, shared by buying and edit mode. Saves run in [scope]. */
@Stable
internal class StoresScreenDetailState(private val scope: CoroutineScope) {
    var searchQuery by mutableStateOf("")
    var isAllExpanded by mutableStateOf(false)
    var showGroceryCreation by mutableStateOf(false)
    var showFinishConfirmation by mutableStateOf(false)
    var showReorderCategories by mutableStateOf(false)
    var groceryToEdit by mutableStateOf<Grocery?>(null)
    var isGroceryEditMode by mutableStateOf(false)
    var editStoreName by mutableStateOf("")
    var editStoreAddress by mutableStateOf("")
    var storeExists by mutableStateOf(false)
        private set
    val snackbarHostState = SnackbarHostState()

    /** Fills the edit fields from [store]; a missing (unknown or deleted) store leaves them empty. */
    fun loadStore(store: Store?) {
        storeExists = store != null
        editStoreName = store?.name ?: ""
        editStoreAddress = store?.address ?: ""
    }

    fun openGroceryCreation() {
        isGroceryEditMode = false
        groceryToEdit = null
        showGroceryCreation = true
    }

    fun openGroceryEdit(grocery: Grocery) {
        groceryToEdit = grocery
        isGroceryEditMode = true
        showGroceryCreation = true
    }

    fun closeGroceryDialog() {
        showGroceryCreation = false
        isGroceryEditMode = false
        groceryToEdit = null
    }

    fun onGroceryCreated(newGrocery: Grocery) {
        val original = groceryToEdit
        if (isGroceryEditMode && original != null) storesScreenApplyGroceryEdit(original, newGrocery)
        else DataManagerObject.addGrocery(newGrocery)
        save()
        closeGroceryDialog()
    }

    /**
     * Saves the trimmed name and address, then shows [savedText]. Ignored when the name is blank or the store is
     * unknown or deleted.
     */
    fun confirmStoreInfo(storeId: String, savedText: String) {
        val trimmedName = editStoreName.trim()
        if (trimmedName.isBlank() || DataManagerObject.getSortedStores().none { it.uuid == storeId }) return
        DataManagerObject.updateStore(storeId) { it.copy(name = trimmedName, address = editStoreAddress.trim()) }
        scope.launch { saveAndShow(savedText) }
    }

    private suspend fun saveAndShow(message: String) {
        DataStoreManager.saveDataGlobally()
        snackbarHostState.showSnackbar(message)
    }

    fun unmarkBought(grocery: Grocery) {
        DataManagerObject.updateGrocery(grocery.uuid) { it.copy(isBought = false) }
        save()
    }

    /** Confirms the store's bought items, closes the confirmation, then calls [onFinished]. */
    fun finishShopping(storeId: String, onFinished: () -> Unit = {}) {
        DataManagerObject.confirmBoughtItemsForStore(storeId)
        save()
        showFinishConfirmation = false
        onFinished()
    }

    private fun save() {
        scope.launch { DataStoreManager.saveDataGlobally() }
    }
}
