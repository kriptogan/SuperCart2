package com.example.supercart2.ui.screens

import android.util.Log
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Data mutations triggered from [ShoppingListScreen]; each one persists through DataStoreManager. */
internal object ShoppingListScreenActions {
    private const val TAG = "ShoppingListScreen"

    fun unmarkBought(grocery: Grocery, scope: CoroutineScope) {
        DataManagerObject.updateGrocery(grocery.uuid) { it.copy(isBought = false) }
        save(scope, "datastore test", "Saved removal to DataStore")
    }

    fun finishShopping(state: ShoppingListScreenState, scope: CoroutineScope) {
        DataManagerObject.confirmBoughtItems()
        save(scope, TAG, "Saved shopping completion to DataStore")
        state.showFinishConfirmation = false
    }

    /**
     * Handler passed as GroceryCreationDialog.onGroceryCreated. The dialog currently never calls it
     * (it saves by itself), so this is unreachable from the UI; kept to preserve the screen's contract.
     */
    fun onGroceryCreated(state: ShoppingListScreenState, newGrocery: Grocery, scope: CoroutineScope) {
        val original = state.groceryToEdit
        if (state.isEditMode && original != null) update(original, newGrocery, scope) else add(newGrocery, scope)
        scope.launch { DataStoreManager.saveDataGlobally() }
        state.showGroceryCreation = false
    }

    private fun update(original: Grocery, newGrocery: Grocery, scope: CoroutineScope) {
        if (original.categoryId != newGrocery.categoryId || original.subCategoryId != newGrocery.subCategoryId) {
            DataManagerObject.updateGroceryLocation(original.uuid, newGrocery.categoryId, newGrocery.subCategoryId)
        }
        DataManagerObject.updateGrocery(original.uuid) { newGrocery }
        save(scope, TAG, "Saved grocery update to DataStore")
        Log.d(TAG, "Grocery updated: ${newGrocery.name}")
    }

    private fun add(newGrocery: Grocery, scope: CoroutineScope) {
        DataManagerObject.addGrocery(newGrocery)
        save(scope, TAG, "Saved new grocery to DataStore")
        Log.d(TAG, "New grocery added: ${newGrocery.name}")
    }

    private fun save(scope: CoroutineScope, tag: String, message: String) {
        scope.launch {
            DataStoreManager.saveDataGlobally()
            Log.d(tag, message)
        }
    }
}
