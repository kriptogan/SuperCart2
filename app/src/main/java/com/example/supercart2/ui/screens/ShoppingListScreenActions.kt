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

    private fun save(scope: CoroutineScope, tag: String, message: String) {
        scope.launch {
            DataStoreManager.saveDataGlobally()
            Log.d(tag, message)
        }
    }
}
