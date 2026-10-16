package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.ui.components.common.superCartTextFieldColors
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/**
 * Search field + expand/collapse-all toggle. In buying mode (fixed header) the field is indented and takes 78% of
 * the width; in edit mode (scrolling list) it fills the space left by the toggle.
 */
@Composable
internal fun StoresScreenSearchRow(state: StoresScreenDetailState, buyingLayout: Boolean) {
    val rowModifier = Modifier.fillMaxWidth().padding(bottom = SuperCartSpacing.md)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        StoresScreenSearchInput(state, buyingLayout)
        Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
        StoresScreenExpandToggle(state.isAllExpanded) { state.isAllExpanded = !state.isAllExpanded }
    }
}

/** Buying layout: 8dp indent and 78% of the width. Edit layout: all the space left by the toggle. */
@Composable
private fun RowScope.StoresScreenSearchInput(state: StoresScreenDetailState, buyingLayout: Boolean) {
    if (buyingLayout) Spacer(modifier = Modifier.width(8.dp))
    StoresScreenSearchField(state, if (buyingLayout) Modifier.fillMaxWidth(0.78f) else Modifier.weight(1f))
}

/** The field has no label, so the shared dialog text-field colors render exactly like its former own colors. */
@Composable
private fun StoresScreenSearchField(state: StoresScreenDetailState, modifier: Modifier) {
    OutlinedTextField(
        value = state.searchQuery, onValueChange = { state.searchQuery = it }, modifier = modifier, singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) }, colors = superCartTextFieldColors(),
        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(24.dp), tint = SuperCartColors.gray) },
        trailingIcon = if (state.searchQuery.isEmpty()) null else { { StoresScreenClearSearchButton(state) } },
    )
}

@Composable
private fun StoresScreenClearSearchButton(state: StoresScreenDetailState) {
    IconButton(onClick = { state.searchQuery = "" }, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Default.Clear, null, tint = SuperCartColors.darkGray, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StoresScreenExpandToggle(isAllExpanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.size(56.dp), shape = RoundedCornerShape(12.dp), border = CardDefaults.outlinedCardBorder(),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) { StoresScreenExpandButton(isAllExpanded, onToggle) }
}

@Composable
private fun StoresScreenExpandButton(isAllExpanded: Boolean, onToggle: () -> Unit) {
    val icon = if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    IconButton(onClick = onToggle, modifier = Modifier.fillMaxSize()) {
        Icon(icon, contentDescription = null, tint = SuperCartColors.primaryGreen, modifier = Modifier.size(28.dp))
    }
}
