package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/**
 * Edit mode: editable store name/address, "Reorder Categories" and "Confirm" (disabled while the name is blank or
 * the store is unknown or deleted).
 */
@Composable
internal fun StoreInfoCard(state: StoresScreenDetailState, onConfirm: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) { StoresScreenStoreInfoColumn(state, onConfirm) }
}

@Composable
private fun StoresScreenStoreInfoColumn(state: StoresScreenDetailState, onConfirm: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().padding(SuperCartSpacing.md)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        StoresScreenStoreInfoFields(state)
        StoresScreenStoreInfoButtons(state, onConfirm)
    }
}

/** Title and both text fields, emitted as separate children of the card column. */
@Composable
private fun StoresScreenStoreInfoFields(state: StoresScreenDetailState) {
    val title = stringResource(R.string.store_info)
    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SuperCartColors.primaryGreen)
    StoresScreenStoreInfoField(state.editStoreName, { state.editStoreName = it }, R.string.store_name)
    StoresScreenStoreInfoField(state.editStoreAddress, { state.editStoreAddress = it }, R.string.store_address)
}

@Composable
private fun StoresScreenStoreInfoField(value: String, onValueChange: (String) -> Unit, labelRes: Int) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(stringResource(labelRes)) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, colors = storesScreenStoreInfoFieldColors(),
    )
}

@Composable
private fun storesScreenStoreInfoFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SuperCartColors.primaryGreen, unfocusedBorderColor = SuperCartColors.gray,
    focusedLabelColor = SuperCartColors.primaryGreen, unfocusedLabelColor = SuperCartColors.gray,
    focusedTextColor = SuperCartColors.black, unfocusedTextColor = SuperCartColors.black,
    focusedContainerColor = SuperCartColors.white, unfocusedContainerColor = SuperCartColors.white,
)

@Composable
private fun StoresScreenStoreInfoButtons(state: StoresScreenDetailState, onConfirm: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
        StoresScreenReorderButton(onClick = { state.showReorderCategories = true })
        StoresScreenConfirmStoreButton(enabled = state.storeExists && state.editStoreName.isNotBlank(), onConfirm)
    }
}

@Composable
private fun RowScope.StoresScreenReorderButton(onClick: () -> Unit) {
    val green = SuperCartColors.primaryGreen
    val colors = ButtonDefaults.buttonColors(containerColor = green.copy(alpha = 0.1f), contentColor = green)
    val elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    Button(onClick, Modifier.weight(1f), colors = colors, shape = RoundedCornerShape(12.dp), elevation = elevation) {
        StoresScreenStoreInfoButtonContent(Icons.Default.Sort, R.string.reorder_categories)
    }
}

@Composable
private fun RowScope.StoresScreenConfirmStoreButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Button(onClick, Modifier.weight(1f), enabled = enabled, colors = primaryButtonColors(), shape = shape) {
        StoresScreenStoreInfoButtonContent(Icons.Default.Check, R.string.confirm)
    }
}

/** Icon, spacer and label, emitted into the button's row. */
@Composable
private fun StoresScreenStoreInfoButtonContent(icon: ImageVector, textRes: Int) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text(text = stringResource(textRes), fontSize = 13.sp)
}
