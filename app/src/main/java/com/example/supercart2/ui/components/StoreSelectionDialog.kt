package com.example.supercart2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.models.Store
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

@Composable
fun StoreSelectionDialog(
    selectedStoreIds: List<String>,
    onDismiss: () -> Unit,
    onStoresSelected: (List<String>) -> Unit
) {
    val allStores = remember(DataManagerObject.version) { DataManagerObject.getSortedStores() }
    val state = remember { StoreSelectionState(selectedStoreIds) }
    StoreSelectionAlert(allStores, state, onClose = { state.close(onStoresSelected, onDismiss) })
    if (state.showCreateStore) {
        CreateStoreDialog(onDismiss = { state.showCreateStore = false }, onStoreCreated = state::onStoreCreated)
    }
}

/** Selected store ids and whether the nested create-store dialog is open. */
private class StoreSelectionState(initialIds: List<String>) {
    var selectedIds by mutableStateOf(initialIds.toMutableSet())
    var showCreateStore by mutableStateOf(false)

    fun isSelected(storeId: String) = selectedIds.contains(storeId)

    fun toggle(storeId: String) = setSelected(storeId, !isSelected(storeId))

    fun setSelected(storeId: String, selected: Boolean) {
        selectedIds = selectedIds.toMutableSet().apply { if (selected) add(storeId) else remove(storeId) }
    }

    /**
     * Closing (button or outside tap) always reports the current selection first: only existing non-deleted
     * stores, in display order.
     */
    fun close(onStoresSelected: (List<String>) -> Unit, onDismiss: () -> Unit) {
        onStoresSelected(DataManagerObject.getSortedStores().map { it.uuid }.filter { it in selectedIds })
        onDismiss()
    }

    fun onStoreCreated(store: Store) {
        setSelected(store.uuid, true)
        showCreateStore = false
    }
}

@Composable
private fun StoreSelectionAlert(stores: List<Store>, state: StoreSelectionState, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.select_stores), color = SuperCartColors.black) },
        text = { StoreSelectionContent(stores, state) },
        confirmButton = { StoreSelectionButtons(onClose, onCreateStore = { state.showCreateStore = true }) },
    )
}

/** Empty while the create-store dialog is open. */
@Composable
private fun StoreSelectionContent(stores: List<Store>, state: StoreSelectionState) {
    when {
        state.showCreateStore -> {}
        stores.isEmpty() -> NoStoresToSelectText()
        else -> LazyColumn { items(stores) { store -> SelectableStoreRow(store, state) } }
    }
}

@Composable
private fun NoStoresToSelectText() {
    Text(
        stringResource(R.string.no_stores_available_create), color = SuperCartColors.gray, fontStyle = FontStyle.Italic,
    )
}

@Composable
private fun SelectableStoreRow(store: Store, state: StoreSelectionState) {
    val rowModifier = Modifier.fillMaxWidth().clickable { state.toggle(store.uuid) }.padding(vertical = 8.dp)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        StoreSelectionCheckbox(state.isSelected(store.uuid)) { checked -> state.setSelected(store.uuid, checked) }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = store.name)
    }
}

@Composable
private fun StoreSelectionCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = CheckboxDefaults.colors(checkedColor = SuperCartColors.primaryGreen)
    Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = colors)
}

/** Close (✕, left, reports the selection) and create store (+, right). */
@Composable
private fun StoreSelectionButtons(onClose: () -> Unit, onCreateStore: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        DialogIconButton(Icons.Default.Close, stringResource(R.string.close), secondaryButtonColors(),
            Modifier.weight(1f), onClick = onClose)
        DialogIconButton(Icons.Default.Add, stringResource(R.string.create_store), primaryButtonColors(),
            Modifier.weight(1f), onClick = onCreateStore)
    }
}
