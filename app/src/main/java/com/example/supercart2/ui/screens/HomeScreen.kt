package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.components.BurgerMenu
import com.example.supercart2.ui.components.CategoriesManagementDialog
import com.example.supercart2.ui.components.GroceryCreationDialog
import com.example.supercart2.ui.components.HierarchicalCategoryDisplay
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import android.util.Log
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    var showCategoriesManagement by remember { mutableStateOf(false) }
    var showGroceryCreation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with burger menu and add grocery button
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
            
            // Add Grocery Button (+ icon)
            IconButton(
                onClick = { showGroceryCreation = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Grocery",
                    tint = SuperCartColors.primaryGreen
                )
            }
        }
        
                        // Main content - Hierarchical Category Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SuperCartSpacing.md)
                ) {
                    Text(
                        text = "Your Groceries",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = SuperCartSpacing.md)
                    )
                    
                    HierarchicalCategoryDisplay()
                }
    }
    
    // Categories Management Dialog
    if (showCategoriesManagement) {
        CategoriesManagementDialog(
            onDismiss = { showCategoriesManagement = false }
        )
    }
    
    // Grocery Creation Dialog
    if (showGroceryCreation) {
        GroceryCreationDialog(
            onDismiss = { showGroceryCreation = false },
            onGroceryCreated = { newGrocery ->
                // Add the new grocery to the appropriate sub-category in DataManagerObject
                val categoryIndex = DataManagerObject.categories.indexOfFirst { 
                    it.category.uuid == newGrocery.categoryId 
                }
                
                if (categoryIndex != -1) {
                    val categoryWithSubs = DataManagerObject.categories[categoryIndex]
                    val subCategoryIndex = categoryWithSubs.subCategories.indexOfFirst { 
                        it.subCategory.uuid == newGrocery.subCategoryId 
                    }
                    
                    if (subCategoryIndex != -1) {
                        // Add the grocery to the sub-category
                        val updatedSubCategoryWithGroceries = categoryWithSubs.subCategories[subCategoryIndex].copy(
                            groceries = categoryWithSubs.subCategories[subCategoryIndex].groceries.toMutableList().apply {
                                add(newGrocery)
                            }
                        )
                        
                        val updatedCategoryWithSubs = categoryWithSubs.copy(
                            subCategories = categoryWithSubs.subCategories.toMutableList().apply {
                                set(subCategoryIndex, updatedSubCategoryWithGroceries)
                            }
                        )
                        
                        DataManagerObject.categories[categoryIndex] = updatedCategoryWithSubs
                        
                        // Save the updated data to local storage
                        scope.launch {
                            DataStoreManager.saveDataGlobally()
                        }
                        
                        Log.d("HomeScreen", "New grocery added: ${newGrocery.name}")
                    }
                }
                
                showGroceryCreation = false
            }
        )
    }
}
