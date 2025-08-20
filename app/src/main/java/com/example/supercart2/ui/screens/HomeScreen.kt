package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.components.BurgerMenu
import com.example.supercart2.ui.components.CategoriesManagementDialog
import com.example.supercart2.ui.theme.SuperCartSpacing

@Composable
fun HomeScreen() {
    var showCategoriesManagement by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with burger menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SuperCartSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BurgerMenu(
                onCategoriesManagementClick = {
                    showCategoriesManagement = true
                }
            )
            
            Text(
                text = "SuperCart",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = SuperCartSpacing.md)
            )
        }
        
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(SuperCartSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome to SuperCart!",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
        }
    }
    
    // Categories Management Dialog
    if (showCategoriesManagement) {
        CategoriesManagementDialog(
            onDismiss = { showCategoriesManagement = false }
        )
    }
}
