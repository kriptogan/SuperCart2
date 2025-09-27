package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.data.FirebaseManager
import kotlinx.coroutines.launch

@Composable
fun BurgerMenu(
    onCategoriesManagementClick: () -> Unit,
    onImportGroceriesClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier
            .size(72.dp) // Smaller circle size
            .padding(SuperCartSpacing.sm),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.white
        ),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                modifier = Modifier.size(36.dp) // Exact size requested
            )
        }
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Categories Management") },
            onClick = {
                onCategoriesManagementClick()
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text("Import Groceries") },
            onClick = {
                onImportGroceriesClick()
                expanded = false
            }
        )
        // Firebase Upload Option
        DropdownMenuItem(
            text = { Text("Upload to Cloud") },
            onClick = {
                scope.launch {
                    try {
                        FirebaseManager.uploadData()
                    } catch (e: Exception) {
                        // Handle error - in a real app, you'd want to show a proper error dialog
                        android.util.Log.e("BurgerMenu", "Upload failed", e)
                    }
                }
                expanded = false
            }
        )
        // Firebase Download Option
        DropdownMenuItem(
            text = { Text("Download from Cloud") },
            onClick = {
                scope.launch {
                    try {
                        FirebaseManager.downloadData()
                    } catch (e: Exception) {
                        // Handle error - in a real app, you'd want to show a proper error dialog
                        android.util.Log.e("BurgerMenu", "Download failed", e)
                    }
                }
                expanded = false
            }
        )
    }
}