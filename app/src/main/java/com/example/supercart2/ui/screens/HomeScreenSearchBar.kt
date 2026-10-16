package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

private val homeScreenSearchRowModifier = Modifier.fillMaxWidth().padding(bottom = SuperCartSpacing.lg)

/** Search field plus the expand/collapse-all toggle. */
@Composable
internal fun HomeScreenSearchRow(state: HomeScreenState) {
    Row(modifier = homeScreenSearchRowModifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(15.dp))
        HomeScreenSearchField(state.searchQuery) { state.searchQuery = it }
        Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
        HomeScreenExpandToggle(state.isAllExpanded) { state.isAllExpanded = !state.isAllExpanded }
    }
}

@Composable
private fun HomeScreenSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query, onValueChange = onQueryChange, placeholder = { Text(stringResource(R.string.search_hint)) },
        modifier = Modifier.fillMaxWidth(0.78f), colors = homeScreenSearchFieldColors(), singleLine = true,
        leadingIcon = { HomeScreenSearchIcon() }, textStyle = MaterialTheme.typography.bodyMedium,
        trailingIcon = { if (query.isNotEmpty()) HomeScreenClearSearchButton(query) { onQueryChange("") } },
    )
}

@Composable
private fun HomeScreenSearchIcon() {
    Icon(Icons.Default.Search, stringResource(R.string.search), Modifier.size(24.dp), tint = SuperCartColors.gray)
}

@Composable
private fun homeScreenSearchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SuperCartColors.primaryGreen, unfocusedBorderColor = SuperCartColors.gray,
    focusedLabelColor = SuperCartColors.primaryGreen,
    focusedTextColor = SuperCartColors.black, unfocusedTextColor = SuperCartColors.black,
    focusedContainerColor = SuperCartColors.white, unfocusedContainerColor = SuperCartColors.white,
)

@Composable
private fun HomeScreenClearSearchButton(query: String, onClear: () -> Unit) {
    IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
        val description = stringResource(R.string.clear_search_for, query)
        Icon(Icons.Default.Clear, description, Modifier.size(20.dp), tint = SuperCartColors.darkGray)
    }
}

@Composable
private fun HomeScreenExpandToggle(isAllExpanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.height(56.dp).width(56.dp), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        border = CardDefaults.outlinedCardBorder(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) { IconButton(onClick = onToggle, modifier = Modifier.fillMaxSize()) { HomeScreenExpandIcon(isAllExpanded) } }
}

@Composable
private fun HomeScreenExpandIcon(isAllExpanded: Boolean) {
    val icon = if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    val description = stringResource(if (isAllExpanded) R.string.collapse_all else R.string.expand_all)
    Icon(icon, description, Modifier.size(28.dp), tint = SuperCartColors.primaryGreen)
}
