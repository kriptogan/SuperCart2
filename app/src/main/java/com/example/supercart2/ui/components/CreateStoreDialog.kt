package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Store
import com.example.supercart2.ui.components.common.CancelConfirmButtons
import com.example.supercart2.ui.components.common.DialogTitle
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CreateStoreDialog(onDismiss: () -> Unit, onStoreCreated: (Store) -> Unit = {}) {
    var storeName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val create: () -> Unit = { createStore(storeName, scope)?.let { onStoreCreated(it); onDismiss() } }
    CreateStoreAlert(storeName, { storeName = it }, onDismiss, create)
}

@Composable
private fun CreateStoreAlert(name: String, onNameChange: (String) -> Unit, onDismiss: () -> Unit, onCreate: () -> Unit) {
    val createLabel = stringResource(R.string.create_store)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SuperCartColors.lightGreen,
        title = { DialogTitle(createLabel) }, text = { StoreNameField(name, onNameChange) },
        confirmButton = { CancelConfirmButtons(onDismiss, createLabel, name.isNotBlank(), onCreate) },
    )
}

@Composable
private fun StoreNameField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(stringResource(R.string.store_name)) },
        modifier = Modifier.fillMaxWidth(), singleLine = false, maxLines = 4,
        colors = superCartTextFieldColors(), shape = SuperCartShapes.small,
    )
}

/** Adds a store after the highest existing viewOrder and saves; null when the name is blank. */
private fun createStore(name: String, scope: CoroutineScope): Store? {
    if (name.isBlank()) return null
    val store = Store(name = name.trim(), viewOrder = (DataManagerObject.stores.maxOfOrNull { it.viewOrder } ?: 0) + 1)
    DataManagerObject.addStore(store)
    scope.launch { DataStoreManager.saveDataGlobally() }
    return store
}
