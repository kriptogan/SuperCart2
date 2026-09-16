package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors

/*
 * Building blocks shared by CategorySelectionDialog and SubCategorySelectionDialog:
 * a full-screen list alert with Close / New buttons, and a selectable row.
 */

/** Full-screen light-green alert listing [list] items, with Close (dismiss) and "+" ([onNew]) buttons. */
@Composable
internal fun CategorySelectionListAlert(
    title: String, newDescription: String, onDismiss: () -> Unit, onNew: () -> Unit,
    list: LazyListScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, modifier = Modifier.fillMaxSize(), containerColor = SuperCartColors.lightGreen,
        title = { Text(text = title, modifier = Modifier.fillMaxWidth()) },
        text = { SelectionListColumn(list) },
        confirmButton = { SelectionListButtons(newDescription, onDismiss, onNew) },
    )
}

@Composable
private fun SelectionListColumn(list: LazyListScope.() -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = list)
}

@Composable
private fun SelectionListButtons(newDescription: String, onClose: () -> Unit, onNew: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectionListIconButton(Icons.Default.Close, stringResource(R.string.close), Modifier.weight(1f), onClose)
        SelectionListIconButton(Icons.Default.Add, newDescription, Modifier.weight(1f), onNew)
    }
}

@Composable
private fun SelectionListIconButton(icon: ImageVector, description: String, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = description, tint = Color.White)
    }
}

/** Gray rounded row with a green check (or equivalent indent) before the name; the click area excludes the padding. */
@Composable
internal fun CategorySelectionListRow(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val rowModifier = Modifier.fillMaxWidth().background(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp))
        .padding(16.dp).clickable { onClick() }
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        if (isSelected) SelectedCheckIcon() else Spacer(modifier = Modifier.width(28.dp))
        Text(text = text, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectedCheckIcon() {
    Icon(
        imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Green,
        modifier = Modifier.padding(end = 12.dp),
    )
}
