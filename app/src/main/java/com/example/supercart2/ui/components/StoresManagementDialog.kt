package com.example.supercart2.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Store
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun StoresManagementDialog(
    onDismiss: () -> Unit,
    onEnterStoreEditMode: (storeId: String) -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val version = DataManagerObject.version
    if (showCreateDialog) {
        CreateStoreDialog(onDismiss = { showCreateDialog = false }, onStoreCreated = { showCreateDialog = false })
    }
    StoresManagementDialogAlert(version, onDismiss, onEnterStoreEditMode, onCreate = { showCreateDialog = true })
}

@Composable
private fun StoresManagementDialogAlert(
    version: Int, onDismiss: () -> Unit, onEnterStoreEditMode: (storeId: String) -> Unit, onCreate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, modifier = Modifier.fillMaxSize(), containerColor = SuperCartColors.lightGreen,
        title = { StoresManagementDialogTitle(stringResource(R.string.stores_management)) },
        text = { StoresManagementDialogBody(version, onDismiss, onEnterStoreEditMode) },
        confirmButton = { StoresManagementDialogButtons(onDismiss, onCreate) },
    )
}

/** Centered headline title (default dialog title color). */
@Composable
internal fun StoresManagementDialogTitle(text: String) {
    Text(
        text = text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StoresManagementDialogBody(
    version: Int, onDismiss: () -> Unit, onEnterStoreEditMode: (storeId: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val sortedStores = remember(version) { DataManagerObject.getSortedStores() }
        if (sortedStores.isEmpty()) StoresManagementDialogEmpty()
        else StoresManagementDialogList(sortedStores, onDismiss, onEnterStoreEditMode)
    }
}

@Composable
private fun StoresManagementDialogEmpty() {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        StoresManagementDialogEmptyText(R.string.no_stores_yet, MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
        val hintStyle = MaterialTheme.typography.bodyMedium
        StoresManagementDialogEmptyText(R.string.no_stores_available_create, hintStyle, TextAlign.Center)
    }
}

@Composable
private fun StoresManagementDialogEmptyText(@StringRes textRes: Int, style: TextStyle, textAlign: TextAlign? = null) {
    Text(text = stringResource(textRes), style = style, color = SuperCartColors.gray, textAlign = textAlign)
}

@Composable
private fun StoresManagementDialogList(
    stores: List<Store>, onDismiss: () -> Unit, onEnterStoreEditMode: (storeId: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(stores) { store ->
            StoresManagementDialogItem(stores, stores.indexOf(store), scope, onDismiss, onEnterStoreEditMode)
        }
    }
}

/** Editing a store closes this dialog first, then enters edit mode for it. */
@Composable
private fun StoresManagementDialogItem(
    stores: List<Store>, index: Int, scope: CoroutineScope,
    onDismiss: () -> Unit, onEnterStoreEditMode: (storeId: String) -> Unit,
) {
    StoresManagementDialogStoreCard(
        store = stores[index], onEnterEditMode = { onDismiss(); onEnterStoreEditMode(stores[index].uuid) },
        onMoveUp = { storesManagementDialogMove(stores, index, -1, scope) },
        onMoveDown = { storesManagementDialogMove(stores, index, 1, scope) },
        canMoveUp = index > 0, canMoveDown = index < stores.size - 1,
    )
}

/** Swaps the viewOrder of the store at [index] with its neighbour at [index] + [delta] and saves. */
private fun storesManagementDialogMove(stores: List<Store>, index: Int, delta: Int, scope: CoroutineScope) {
    val neighbour = stores.getOrNull(index + delta) ?: return
    DataManagerObject.swapStoreOrder(stores[index].uuid, neighbour.uuid)
    scope.launch { DataStoreManager.saveDataGlobally() }
}

@Composable
private fun StoresManagementDialogButtons(onDismiss: () -> Unit, onCreate: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        val cancel = stringResource(R.string.cancel)
        DialogIconButton(Icons.Default.Close, cancel, secondaryButtonColors(), Modifier.weight(1f), onClick = onDismiss)
        val create = stringResource(R.string.create_new_store)
        DialogIconButton(Icons.Default.Add, create, primaryButtonColors(), Modifier.weight(1f), onClick = onCreate)
    }
}
