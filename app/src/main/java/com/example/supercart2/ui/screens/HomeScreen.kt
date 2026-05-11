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
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
import com.example.supercart2.ui.components.BurgerMenu
import com.example.supercart2.ui.components.CategoriesManagementDialog
import com.example.supercart2.ui.components.StoresManagementDialog
import com.example.supercart2.ui.components.GroupManagementDialog
import com.example.supercart2.ui.components.GroceryCreationDialog
import com.example.supercart2.ui.components.ImportGroceriesDialog
import com.example.supercart2.ui.components.HierarchicalCategoryDisplay
import com.example.supercart2.ui.components.SettingsDialog
import com.example.supercart2.data.SettingsManager
import androidx.compose.runtime.collectAsState
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
fun HomeScreen(onNavigateToStoreEdit: (String) -> Unit = {}) {
    var showCategoriesManagement by remember { mutableStateOf(false) }
    var showStoresManagement by remember { mutableStateOf(false) }
    var showGroupManagement by remember { mutableStateOf(false) }
    var showGroceryCreation by remember { mutableStateOf(false) }
    var showImportGroceries by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isAllExpanded by remember { mutableStateOf(false) }
    var isAlertFilterActive by remember { mutableStateOf(false) }
    var dataRefreshTrigger by remember { mutableStateOf(0) }
    
    // Edit mode state
    var groceryToEdit by remember { mutableStateOf<Grocery?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Observe settings
    val showAlerts by SettingsManager.showAlerts.collectAsState(initial = true)
    
    // Disable alert filter if alerts are disabled
    LaunchedEffect(showAlerts) {
        if (!showAlerts && isAlertFilterActive) {
            isAlertFilterActive = false
        }
    }
    
    // Observe data version to trigger recomposition
    val dataVersion = DataManagerObject.version
    
    // Check if there are any items that would match the alert filter (only if alerts are enabled)
    val hasAlertItems = derivedStateOf {
        if (!showAlerts) {
            false
        } else {
            filterAlertData(DataManagerObject.getSortedCategories()).isNotEmpty()
        }
    }
    
    // Get filtered and expanded data based on search query and alert filter
    val filteredData = derivedStateOf {
        // Use version to trigger recomposition
        android.util.Log.d("datastore test", "Computing filtered data, version: $dataVersion")
        var data = DataManagerObject.getSortedCategories()
        
        // Apply filters in sequence
        if (isAlertFilterActive && showAlerts) {
            // Apply alert filter first if active and alerts are enabled
            data = filterAlertData(data)
            // Force expansion when alert filter is active
            isAllExpanded = true
        }
        
        // Then apply search filter if query exists
        if (searchQuery.isNotBlank()) {
            data = filterAndExpandData(data, searchQuery)
        }
        
        data
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
    
    // Function to handle editing a grocery
    fun onEditGrocery(grocery: Grocery) {
        groceryToEdit = grocery
        isEditMode = true
        showGroceryCreation = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SuperCartColors.lightGreen)
    ) {
        // Fixed top bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SuperCartColors.lightGreen)
                .padding(
                    start = SuperCartSpacing.md,
                    end = SuperCartSpacing.md,
                    top = SuperCartSpacing.xl
                )
        ) {
            // Burger menu and add grocery button row (same layout as ShoppingListScreen: burger left, center, plus right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Burger menu
                Box(
                    modifier = Modifier.padding(start = 5.dp)
                ) {
                    BurgerMenu(
                        onCategoriesManagementClick = {
                            showCategoriesManagement = true
                        },
                        onManageStoresClick = {
                            showStoresManagement = true
                        },
                        onManageGroupClick = {
                            showGroupManagement = true
                        },
                        onImportGroceriesClick = {
                            showImportGroceries = true
                        },
                        onSettingsClick = {
                            showSettings = true
                        }
                    )
                }

                // Alert Button (center) - only shown if there are items to alert about
                if (hasAlertItems.value) {
                    Card(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = SuperCartColors.orange // Orange background
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        IconButton(
                            onClick = { isAlertFilterActive = !isAlertFilterActive },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts",
                                tint = SuperCartColors.white, // White icon
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                } else {
                    // Placeholder to maintain layout spacing
                    Spacer(modifier = Modifier.size(56.dp))
                }
                
                // Add Grocery Button (+ icon) with left padding
                Box(
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Card(
                        modifier = Modifier.size(56.dp), // Same size as burger menu
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = SuperCartColors.primaryGreen // Green background
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        IconButton(
                            onClick = { 
                                // Reset edit mode when creating new grocery
                                isEditMode = false
                                groceryToEdit = null
                                showGroceryCreation = true 
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add New Grocery",
                                tint = SuperCartColors.white, // White icon
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
                    .padding(bottom = SuperCartSpacing.lg), // Increased spacing below search bar
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left spacer to align with burger icon
                Spacer(modifier = Modifier.width(15.dp))
                // Search bar (responsive width to move collapse button left)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    modifier = Modifier.fillMaxWidth(0.78f), // 70% of available width - responsive!
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuperCartColors.primaryGreen,
                        unfocusedBorderColor = SuperCartColors.gray,
                        focusedLabelColor = SuperCartColors.primaryGreen,
                        focusedTextColor = SuperCartColors.black,
                        unfocusedTextColor = SuperCartColors.black,
                        focusedContainerColor = SuperCartColors.white,
                        unfocusedContainerColor = SuperCartColors.white
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
            
            }
        }

        // Scrollable content (bottom padding from Scaffold innerPadding in MainActivity)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 180.dp, // Height of the top bar + extra gap
                    bottom = SuperCartSpacing.md,
                    start = SuperCartSpacing.md,
                    end = SuperCartSpacing.md
                )
        ) {
            HierarchicalCategoryDisplay(
                categories = displayData.value,
                searchQuery = searchQuery,
                isAllExpanded = isAllExpanded,
                onEditGrocery = { grocery -> onEditGrocery(grocery) },
                showAlerts = showAlerts,
                onNavigateToStore = onNavigateToStoreEdit
            )
        }

    
    // Categories Management Dialog
    if (showCategoriesManagement) {
        CategoriesManagementDialog(
            onDismiss = { showCategoriesManagement = false }
        )
    }
    
    // Stores Management Dialog
    if (showStoresManagement) {
        StoresManagementDialog(
            onDismiss = { showStoresManagement = false },
            onEnterStoreEditMode = { storeId ->
                showStoresManagement = false
                onNavigateToStoreEdit(storeId)
            }
        )
    }
    
    if (showGroupManagement) {
        GroupManagementDialog(
            onDismiss = { showGroupManagement = false }
        )
    }
    
    // Settings Dialog
    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false }
        )
    }
    
    // Import Groceries Dialog
    if (showImportGroceries) {
        ImportGroceriesDialog(
            onDismiss = { showImportGroceries = false },
            onImportComplete = {
                showImportGroceries = false
                // Trigger UI refresh
                dataRefreshTrigger++
            }
        )
    }
    
    // Grocery Creation/Edit Dialog
    if (showGroceryCreation) {
        GroceryCreationDialog(
            groceryToEdit = groceryToEdit,
            initialGroceryName = if (isEditMode) "" else searchQuery,
            onDismiss = { 
                showGroceryCreation = false
                // Reset edit mode when dialog is dismissed
                isEditMode = false
                groceryToEdit = null
            },
            onGroceryCreated = { newGrocery ->
                if (isEditMode && groceryToEdit != null) {
                    // Edit mode - update existing grocery
                    // Store reference to avoid smart cast issues with delegated property
                    val originalGrocery = groceryToEdit!!
                    
                    // Check if the grocery is moving to a different sub-category
                    val isMovingToDifferentSubCategory = originalGrocery.subCategoryId != newGrocery.subCategoryId
                    
                    if (isMovingToDifferentSubCategory) {
                        // Grocery is moving to a different sub-category - remove from old, add to new
                        val originalCategoryIndex = DataManagerObject.categories.indexOfFirst { 
                            it.category.uuid == originalGrocery.categoryId 
                        }
                        
                        if (originalCategoryIndex != -1) {
                            val originalCategoryWithSubs = DataManagerObject.categories[originalCategoryIndex]
                            val originalSubCategoryIndex = originalCategoryWithSubs.subCategories.indexOfFirst { 
                                it.subCategory.uuid == originalGrocery.subCategoryId 
                            }
                            
                            if (originalSubCategoryIndex != -1) {
                                // Remove from original sub-category
                                val updatedOriginalSubCategory = originalCategoryWithSubs.subCategories[originalSubCategoryIndex].copy(
                                    groceries = originalCategoryWithSubs.subCategories[originalSubCategoryIndex].groceries.toMutableList().apply {
                                        removeAll { it.uuid == originalGrocery.uuid }
                                    }
                                )
                                
                                val updatedOriginalCategory = originalCategoryWithSubs.copy(
                                    subCategories = originalCategoryWithSubs.subCategories.toMutableList().apply {
                                        set(originalSubCategoryIndex, updatedOriginalSubCategory)
                                    }
                                )
                                
                                DataManagerObject.categories[originalCategoryIndex] = updatedOriginalCategory
                            }
                        }
                        
                        // Add to new sub-category at the end
                        val newCategoryIndex = DataManagerObject.categories.indexOfFirst { 
                            it.category.uuid == newGrocery.categoryId 
                        }
                        
                        if (newCategoryIndex != -1) {
                            val newCategoryWithSubs = DataManagerObject.categories[newCategoryIndex]
                            val newSubCategoryIndex = newCategoryWithSubs.subCategories.indexOfFirst { 
                                it.subCategory.uuid == newGrocery.subCategoryId 
                            }
                            
                            if (newSubCategoryIndex != -1) {
                                val updatedNewSubCategory = newCategoryWithSubs.subCategories[newSubCategoryIndex].copy(
                                    groceries = newCategoryWithSubs.subCategories[newSubCategoryIndex].groceries.toMutableList().apply {
                                        add(newGrocery)
                                    }
                                )
                                
                                val updatedNewCategory = newCategoryWithSubs.copy(
                                    subCategories = newCategoryWithSubs.subCategories.toMutableList().apply {
                                        set(newSubCategoryIndex, updatedNewSubCategory)
                                    }
                                )
                                
                                DataManagerObject.categories[newCategoryIndex] = updatedNewCategory
                            }
                        }
                        
                        Log.d("HomeScreen", "Grocery moved: ${newGrocery.name} - from ${originalGrocery.subCategoryId} to ${newGrocery.subCategoryId}")
                    } else {
                        // Grocery is staying in the same sub-category - just update in place
                        val categoryIndex = DataManagerObject.categories.indexOfFirst { 
                            it.category.uuid == newGrocery.categoryId 
                        }
                        
                        if (categoryIndex != -1) {
                            val categoryWithSubs = DataManagerObject.categories[categoryIndex]
                            val subCategoryIndex = categoryWithSubs.subCategories.indexOfFirst { 
                                it.subCategory.uuid == newGrocery.subCategoryId 
                            }
                            
                            if (subCategoryIndex != -1) {
                                val subCategory = categoryWithSubs.subCategories[subCategoryIndex]
                                val groceryIndex = subCategory.groceries.indexOfFirst { it.uuid == originalGrocery.uuid }
                                
                                if (groceryIndex != -1) {
                                    // Update the grocery in its original position
                                    val updatedGroceries = subCategory.groceries.toMutableList().apply {
                                        set(groceryIndex, newGrocery)
                                    }
                                    
                                    val updatedSubCategory = subCategory.copy(groceries = updatedGroceries)
                                    val updatedCategory = categoryWithSubs.copy(
                                        subCategories = categoryWithSubs.subCategories.toMutableList().apply {
                                            set(subCategoryIndex, updatedSubCategory)
                                        }
                                    )
                                    
                                DataManagerObject.categories[categoryIndex] = updatedCategory
                                DataManagerObject.updateData()
                                Log.d("HomeScreen", "Grocery updated in place: ${newGrocery.name} at position $groceryIndex")
                                }
                            }
                        }
                    }
                    
                    // Save the updated data to local storage
                    scope.launch {
                        DataStoreManager.saveDataGlobally()
                    }
                    
                    // Trigger UI refresh
                    dataRefreshTrigger++
                } else {
                    // Create mode - add new grocery
                    val categoryIndex = DataManagerObject.categories.indexOfFirst { 
                        it.category.uuid == newGrocery.categoryId 
                    }
                    
                    if (categoryIndex != -1) {
                        val categoryWithSubs = DataManagerObject.categories[categoryIndex]
                        val subCategoryIndex = categoryWithSubs.subCategories.indexOfFirst { 
                            it.subCategory.uuid == newGrocery.subCategoryId 
                        }
                        
                        if (subCategoryIndex != -1) {
                            // Add the grocery using DataManagerObject helper
                            DataManagerObject.addGrocery(newGrocery)
                            
                            // Save to DataStore
                            scope.launch {
                                DataStoreManager.saveDataGlobally()
                                android.util.Log.d("HomeScreen", "Saved new grocery to DataStore")
                            }
                            
                            Log.d("HomeScreen", "New grocery added: ${newGrocery.name}")
                        }
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
// Filter data based on expiration dates and buy patterns
private fun filterAlertData(categories: List<CategoryWithSubCategories>): List<CategoryWithSubCategories> {
    val today = java.time.LocalDate.now()
    val tomorrow = today.plusDays(1)
    
    return categories.mapNotNull { categoryWithSubs ->
        // Filter sub-categories
        val filteredSubCategories = categoryWithSubs.subCategories.mapNotNull { subCategoryWithGroceries ->
            // Filter groceries based on alert conditions
            val filteredGroceries = subCategoryWithGroceries.groceries.filter { grocery ->
                // Check expiration date condition
                val isExpiringSoon = grocery.expirationDate?.let { expDate ->
                    expDate <= tomorrow // Due tomorrow or already passed
                } ?: false
                
                // Check buy pattern condition
                val needsToBuy = grocery.averageBuyDays?.let { avgDays ->
                    grocery.buyEvents.maxOrNull()?.let { lastBuyDate ->
                        val daysSinceLastBuy = today.toEpochDay() - lastBuyDate.toEpochDay()
                        daysSinceLastBuy >= avgDays
                    }
                } ?: false
                
                // Show if either condition is met
                isExpiringSoon || needsToBuy
            }
            
            // Only include sub-category if it has matching groceries
            if (filteredGroceries.isNotEmpty()) {
                SubCategoryWithGroceries(
                    subCategory = subCategoryWithGroceries.subCategory,
                    groceries = mutableListOf<Grocery>().apply { addAll(filteredGroceries) }
                )
            } else null
        }
        
        // Only include category if it has sub-categories with matching groceries
        if (filteredSubCategories.isNotEmpty()) {
            CategoryWithSubCategories(
                category = categoryWithSubs.category,
                subCategories = mutableListOf<SubCategoryWithGroceries>().apply { addAll(filteredSubCategories) }
            )
        } else null
    }
}

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
