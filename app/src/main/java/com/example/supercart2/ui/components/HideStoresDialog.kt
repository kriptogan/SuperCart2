package com.example.supercart2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Store
import com.example.supercart2.ui.theme.SuperCartColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HideStoresDialog(
    onDismiss: () -> Unit
) {
    val version = DataManagerObject.version
    val scope = rememberCoroutineScope()
    val allStores = remember(version) { DataManagerObject.getSortedStores() }
    val hiddenStoreIds = remember(version) { DataManagerObject.hiddenStoreIds.toList() }
    HideStoresAlert(allStores, hiddenStoreIds, StoreVisibilityActions(scope), onDismiss)
}

/** Visibility changes are applied and saved in a coroutine; each bumps the data version. */
private class StoreVisibilityActions(private val scope: CoroutineScope) {
    fun toggle(storeId: String) {
        scope.launch {
            DataManagerObject.toggleStoreHidden(storeId)
            DataStoreManager.saveDataGlobally()
        }
    }

    fun setVisible(storeId: String, visible: Boolean) {
        scope.launch {
            applyStoreVisibility(storeId, visible)
            DataManagerObject.updateData()
            DataStoreManager.saveDataGlobally()
        }
    }
}

private fun applyStoreVisibility(storeId: String, visible: Boolean) {
    val hidden = DataManagerObject.hiddenStoreIds
    when {
        visible -> hidden.remove(storeId)
        !hidden.contains(storeId) -> hidden.add(storeId)
    }
}

@Composable
private fun HideStoresAlert(
    stores: List<Store>, hiddenIds: List<String>, actions: StoreVisibilityActions, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.show_hide_stores), color = SuperCartColors.black) },
        text = { if (stores.isEmpty()) NoStoresToHideText() else StoreVisibilityList(stores, hiddenIds, actions) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun NoStoresToHideText() {
    Text(stringResource(R.string.no_stores_available), color = SuperCartColors.gray, fontStyle = FontStyle.Italic)
}

@Composable
private fun StoreVisibilityList(stores: List<Store>, hiddenIds: List<String>, actions: StoreVisibilityActions) {
    Column {
        HiddenStoresHint()
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(stores) { store -> StoreVisibilityRow(store, isVisible = !hiddenIds.contains(store.uuid), actions) }
        }
    }
}

@Composable
private fun HiddenStoresHint() {
    Text(
        stringResource(R.string.unchecked_stores_hidden), fontSize = 12.sp, color = SuperCartColors.gray,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun StoreVisibilityRow(store: Store, isVisible: Boolean, actions: StoreVisibilityActions) {
    val rowModifier = Modifier.fillMaxWidth().clickable { actions.toggle(store.uuid) }.padding(vertical = 8.dp)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        StoreVisibilityCheckbox(isVisible) { visible -> actions.setVisible(store.uuid, visible) }
        Spacer(modifier = Modifier.width(8.dp))
        StoreVisibilityName(store.name, isVisible)
    }
}

@Composable
private fun StoreVisibilityCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = CheckboxDefaults.colors(checkedColor = SuperCartColors.primaryGreen)
    Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = colors)
}

/** Visible stores in black; hidden ones grayed out and struck through. */
@Composable
private fun StoreVisibilityName(name: String, isVisible: Boolean) {
    val base = MaterialTheme.typography.bodyMedium
    val style = if (isVisible) base else base.copy(textDecoration = TextDecoration.LineThrough)
    Text(text = name, color = if (isVisible) SuperCartColors.black else SuperCartColors.gray, style = style)
}
