package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import com.example.supercart2.data.DataManagerObject

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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(SuperCartSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Welcome to SuperCart!",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.lg)
                    )
                    
                    Text(
                        text = "Categories: ${DataManagerObject.categories.size}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.sm)
                    )
                    
                    Text(
                        text = "Sub-Categories: ${DataManagerObject.categories.sumOf { it.subCategories.size }}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.sm)
                    )
                    
                    Text(
                        text = "Groceries: ${DataManagerObject.categories.sumOf { it.subCategories.sumOf { sub -> sub.groceries.size } }}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                    
                    // Debug info
                    Text(
                        text = "Debug: Categories loaded = ${DataManagerObject.categories.isNotEmpty()}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.Gray,
                        modifier = Modifier.padding(top = SuperCartSpacing.md)
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
