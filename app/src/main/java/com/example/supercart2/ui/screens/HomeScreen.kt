package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Grocery
import android.util.Log
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    var showCategoriesManagement by remember { mutableStateOf(false) }
    var showGroceryCreation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isAllExpanded by remember { mutableStateOf(false) }
    var dataRefreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    // Get filtered and expanded data based on search query
    val filteredData = derivedStateOf {
        dataRefreshTrigger // This ensures recomposition when data changes
        if (searchQuery.isBlank()) {
            // No search query - return all data as is
            DataManagerObject.getSortedCategories()
        } else {
            // Filter data based on search query
            filterAndExpandData(DataManagerObject.getSortedCategories(), searchQuery)
        }
    }
    
    // Ensure we always have data to display
    val displayData = derivedStateOf {
        if (filteredData.value.isEmpty() && searchQuery.isBlank()) {
            DataManagerObject.getSortedCategories()
        } else {
            filteredData.value
        }
    }
    
    // Reset collapse/expand all state when search becomes active
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isAllExpanded = true
        } else {
            // When search is cleared, return to collapsed state
            isAllExpanded = false
        }
    }
    
    // Debug logging
    Log.d("HomeScreen", "Search query: '$searchQuery', Filtered data size: ${filteredData.value.size}, Display data size: ${displayData.value.size}")
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content area with burger menu and add grocery button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Take remaining space, leaving room for bottom navigation
                .padding(
                    start = SuperCartSpacing.md,
                    end = SuperCartSpacing.md,
                    bottom = SuperCartSpacing.md,
                    top = SuperCartSpacing.xl // Much more space at the top
                )
        ) {
            // Burger menu and add grocery button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Burger menu with right padding
                Box(
                    modifier = Modifier.padding(start = 5.dp)
                ) {
                    BurgerMenu(
                        onCategoriesManagementClick = {
                            showCategoriesManagement = true
                        }
                    )
                }
                
                // Add Grocery Button (+ icon) with left padding
                Box(
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Card(
                        modifier = Modifier.size(56.dp), // Same size as burger menu
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
                            onClick = { showGroceryCreation = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add New Grocery",
                                tint = SuperCartColors.primaryGreen,
                                modifier = Modifier.size(34.dp) // Slightly larger for better visual balance
                            )
                        }
                    }
                }
            }
            
            // Search bar and controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left spacer to align with burger icon
                Spacer(modifier = Modifier.width(15.dp))
                // Search bar (responsive width to move collapse button left)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search groceries...") },
                    modifier = Modifier.fillMaxWidth(0.78f), // 70% of available width - responsive!
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuperCartColors.primaryGreen,
                        unfocusedBorderColor = SuperCartColors.gray,
                        focusedLabelColor = SuperCartColors.primaryGreen
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SuperCartColors.gray,
                            modifier = Modifier.size(24.dp) // Larger search icon
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(40.dp) // Larger touch target
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search for '$searchQuery'",
                                    tint = SuperCartColors.darkGray,
                                    modifier = Modifier.size(20.dp) // Larger icon
                                )
                            }
                        }
                    },
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.width(SuperCartSpacing.sm)) // Restored original spacing
                
                // Collapse/Expand all toggle
                Card(
                    modifier = Modifier
                        .height(56.dp) // Match search bar height
                        .width(56.dp), // Square aspect ratio
                    shape = RoundedCornerShape(12.dp), // Square with rounded corners
                    colors = CardDefaults.cardColors(
                        containerColor = SuperCartColors.white
                    ),
                    border = CardDefaults.outlinedCardBorder(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    IconButton(
                        onClick = { 
                            isAllExpanded = !isAllExpanded
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isAllExpanded) "Collapse All" else "Expand All",
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(28.dp) // Larger icon
                        )
                    }
                }
            }
            
            // Hierarchical Category Display (takes remaining space)
            HierarchicalCategoryDisplay(
                categories = displayData.value,
                searchQuery = searchQuery,
                isAllExpanded = isAllExpanded
            )
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
                        
                        // Trigger UI refresh
                        dataRefreshTrigger++
                        
                        Log.d("HomeScreen", "New grocery added: ${newGrocery.name}")
                    }
                }
                
                showGroceryCreation = false
            }
        )
    }
}

/**
 * Filters and expands data based on search query
 * - Shows only categories/sub-categories that contain matching groceries
 * - Automatically expands categories/sub-categories with matches
 * - Hides empty categories and sub-categories
 */
private fun filterAndExpandData(
    categories: List<CategoryWithSubCategories>,
    searchQuery: String
): List<CategoryWithSubCategories> {
    val lowerCaseQuery = searchQuery.lowercase()
    
    return categories.mapNotNull { categoryWithSubs ->
        // Filter sub-categories for this category
        val filteredSubCategories = categoryWithSubs.subCategories.mapNotNull { subCategoryWithGroceries ->
            // Filter groceries for this sub-category
            val filteredGroceries = subCategoryWithGroceries.groceries.filter { grocery ->
                grocery.name.lowercase().contains(lowerCaseQuery)
            }
            
            // Only include sub-category if it has matching groceries
            if (filteredGroceries.isNotEmpty()) {
                subCategoryWithGroceries.copy(
                    groceries = mutableStateListOf<Grocery>().apply { addAll(filteredGroceries) }
                )
            } else {
                null
            }
        }
        
        // Only include category if it has sub-categories with matching groceries
        if (filteredSubCategories.isNotEmpty()) {
            categoryWithSubs.copy(
                subCategories = mutableStateListOf<SubCategoryWithGroceries>().apply { addAll(filteredSubCategories) }
            )
        } else {
            null
        }
    }
}
