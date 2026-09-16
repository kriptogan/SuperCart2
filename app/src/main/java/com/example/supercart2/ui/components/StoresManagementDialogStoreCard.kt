package com.example.supercart2.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Store
import com.example.supercart2.ui.components.common.DialogIconButton
import com.example.supercart2.ui.components.common.secondaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.ui.theme.SuperCartSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** One store row: name, then move down / move up / delete (with confirmation) / edit icon buttons. */
@Composable
internal fun StoresManagementDialogStoreCard(
    store: Store, onEnterEditMode: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit,
    canMoveUp: Boolean, canMoveDown: Boolean,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    if (showDeleteConfirmation) StoresManagementDialogDeleteConfirmation(store) { showDeleteConfirmation = false }
    StoresManagementDialogCard(store.name) {
        StoresManagementDialogArrows(canMoveUp, canMoveDown, onMoveUp, onMoveDown)
        StoresManagementDialogDeleteAndEdit(onDelete = { showDeleteConfirmation = true }, onEdit = onEnterEditMode)
    }
}

@Composable
private fun StoresManagementDialogDeleteAndEdit(onDelete: () -> Unit, onEdit: () -> Unit) {
    StoresManagementDialogIconAction(Icons.Default.Delete, R.string.delete_store, Color.Red, onClick = onDelete)
    val editTint = SuperCartColors.primaryGreen
    StoresManagementDialogIconAction(Icons.Default.Edit, R.string.edit_store, editTint, onClick = onEdit)
}

@Composable
private fun StoresManagementDialogCard(name: String, actions: @Composable RowScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = SuperCartSpacing.sm), shape = SuperCartShapes.medium,
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) { Column(modifier = Modifier.padding(SuperCartSpacing.md)) { StoresManagementDialogRow(name, actions) } }
}

@Composable
private fun StoresManagementDialogRow(name: String, actions: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        StoresManagementDialogStoreName(name, Modifier.weight(1f))
        StoresManagementDialogActionsRow(actions)
    }
}

/** Store name on a single line, with ellipsis for long names. */
@Composable
private fun StoresManagementDialogStoreName(name: String, modifier: Modifier) {
    Text(
        text = name, style = MaterialTheme.typography.titleLarge, color = SuperCartColors.black,
        modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StoresManagementDialogActionsRow(actions: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-4).dp), verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = SuperCartSpacing.sm), content = actions,
    )
}

@Composable
private fun StoresManagementDialogArrows(
    canMoveUp: Boolean, canMoveDown: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit,
) {
    val downTint = if (canMoveDown) SuperCartColors.primaryGreen else SuperCartColors.gray
    val down = Icons.Default.KeyboardArrowDown
    StoresManagementDialogIconAction(down, R.string.move_down, downTint, canMoveDown, onMoveDown)
    val upTint = if (canMoveUp) SuperCartColors.primaryGreen else SuperCartColors.gray
    StoresManagementDialogIconAction(Icons.Default.KeyboardArrowUp, R.string.move_up, upTint, canMoveUp, onMoveUp)
}

/** 40dp icon button with a 20dp tinted icon. */
@Composable
private fun StoresManagementDialogIconAction(
    icon: ImageVector, @StringRes description: Int, tint: Color, enabled: Boolean = true, onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        val text = stringResource(description)
        Icon(imageVector = icon, contentDescription = text, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StoresManagementDialogDeleteConfirmation(store: Store, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose, containerColor = SuperCartColors.lightGreen,
        title = { StoresManagementDialogTitle(stringResource(R.string.delete_store)) },
        text = { StoresManagementDialogDeleteMessage(store.name) },
        confirmButton = { StoresManagementDialogDeleteButtons(store.uuid, onClose) },
    )
}

@Composable
private fun StoresManagementDialogDeleteMessage(storeName: String) {
    Text(
        text = stringResource(R.string.delete_store_confirmation, storeName),
        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
    )
}

@Composable
private fun StoresManagementDialogDeleteButtons(storeId: String, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        val cancel = stringResource(R.string.cancel)
        DialogIconButton(Icons.Default.Close, cancel, secondaryButtonColors(), Modifier.weight(1f), onClick = onClose)
        StoresManagementDialogConfirmDeleteButton(storeId, Modifier.weight(1f), onClose)
    }
}

/** Red delete icon button: deletes the store, saves, then closes the confirmation. */
@Composable
private fun StoresManagementDialogConfirmDeleteButton(storeId: String, modifier: Modifier, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
    DialogIconButton(Icons.Default.Delete, stringResource(R.string.delete), colors, modifier) {
        storesManagementDialogDelete(storeId, scope)
        onClose()
    }
}

private fun storesManagementDialogDelete(storeId: String, scope: CoroutineScope) {
    DataManagerObject.deleteStore(storeId)
    scope.launch { DataStoreManager.saveDataGlobally() }
}
