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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.ui.theme.SuperCartColors
import kotlinx.coroutines.launch

@Composable
fun HideStoresDialog(
    onDismiss: () -> Unit
) {
    val version = DataManagerObject.version
    val scope = rememberCoroutineScope()
    
    val allStores = remember(version) {
        DataManagerObject.getSortedStores()
    }
    
    val hiddenStoreIds = remember(version) {
        DataManagerObject.hiddenStoreIds.toList()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(stringResource(R.string.show_hide_stores)) 
        },
        text = {
            if (allStores.isEmpty()) {
                Text(
                    stringResource(R.string.no_stores_available),
                    color = SuperCartColors.gray,
                    fontStyle = FontStyle.Italic
                )
            } else {
                Column {
                    Text(
                        stringResource(R.string.unchecked_stores_hidden),
                        fontSize = 12.sp,
                        color = SuperCartColors.gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allStores) { store ->
                            val isVisible = !hiddenStoreIds.contains(store.uuid)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            DataManagerObject.toggleStoreHidden(store.uuid)
                                            DataStoreManager.saveDataGlobally()
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isVisible,
                                    onCheckedChange = { visible ->
                                        scope.launch {
                                            if (visible) {
                                                // Show store (remove from hidden list)
                                                DataManagerObject.hiddenStoreIds.remove(store.uuid)
                                            } else {
                                                // Hide store (add to hidden list)
                                                if (!DataManagerObject.hiddenStoreIds.contains(store.uuid)) {
                                                    DataManagerObject.hiddenStoreIds.add(store.uuid)
                                                }
                                            }
                                            DataManagerObject.updateData()
                                            DataStoreManager.saveDataGlobally()
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SuperCartColors.primaryGreen
                                    )
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = store.name,
                                    color = if (isVisible) {
                                        SuperCartColors.black
                                    } else {
                                        SuperCartColors.gray
                                    },
                                    style = if (isVisible) {
                                        MaterialTheme.typography.bodyMedium
                                    } else {
                                        MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
}
