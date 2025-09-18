package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.ui.components.HierarchicalCategoryDisplay
import com.example.supercart2.ui.components.BurgerMenu
import com.example.supercart2.ui.components.CategoriesManagementDialog
import com.example.supercart2.ui.components.GroceryCreationDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ShoppingListScreen() {
    var showCategoriesManagement by remember { mutableStateOf(false) }
    var showGroceryCreation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isAllExpanded by remember { mutableStateOf(false) }
    
    // Edit mode state
    var groceryToEdit by remember { mutableStateOf<Grocery?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Observe version to trigger recomposition
    val version = DataManagerObject.version
    
    // Filter categories to only include those with shopping list items
    val toBuyCategories = remember(version, searchQuery) {
        DataManagerObject.getSortedCategories().mapNotNull { categoryWithSubs ->
            // Filter subcategories
            val filteredSubCategories = categoryWithSubs.subCategories.mapNotNull { subCategoryWithGroceries ->
                // Filter groceries in shopping list and not bought
                val filteredGroceries = subCategoryWithGroceries.groceries
                    .filter { grocery -> 
                        grocery.inShoppingList && 
                        !grocery.isBought &&
                        (searchQuery.isEmpty() || grocery.name.contains(searchQuery, ignoreCase = true))
                    }
                
                if (filteredGroceries.isNotEmpty()) {
                    // Keep subcategory with filtered groceries
                    SubCategoryWithGroceries(
                        subCategory = subCategoryWithGroceries.subCategory,
                        groceries = mutableListOf<Grocery>().apply { addAll(filteredGroceries) }
                    )
                } else null
            }
            
            if (filteredSubCategories.isNotEmpty()) {
                // Keep category with filtered subcategories
                CategoryWithSubCategories(
                    category = categoryWithSubs.category,
                    subCategories = mutableListOf<SubCategoryWithGroceries>().apply { addAll(filteredSubCategories) }
                )
            } else null
        }
    }
    
    // Filter categories to only include those with bought items
    val boughtCategories = remember(version, searchQuery) {
        DataManagerObject.getSortedCategories().mapNotNull { categoryWithSubs ->
            // Filter subcategories
            val filteredSubCategories = categoryWithSubs.subCategories.mapNotNull { subCategoryWithGroceries ->
                // Filter groceries in shopping list and bought
                val filteredGroceries = subCategoryWithGroceries.groceries
                    .filter { grocery -> 
                        grocery.inShoppingList && 
                        grocery.isBought &&
                        (searchQuery.isEmpty() || grocery.name.contains(searchQuery, ignoreCase = true))
                    }
                
                if (filteredGroceries.isNotEmpty()) {
                    // Keep subcategory with filtered groceries
                    SubCategoryWithGroceries(
                        subCategory = subCategoryWithGroceries.subCategory,
                        groceries = mutableListOf<Grocery>().apply { addAll(filteredGroceries) }
                    )
                } else null
            }
            
            if (filteredSubCategories.isNotEmpty()) {
                // Keep category with filtered subcategories
                CategoryWithSubCategories(
                    category = categoryWithSubs.category,
                    subCategories = mutableListOf<SubCategoryWithGroceries>().apply { addAll(filteredSubCategories) }
                )
            } else null
        }
    }
    
    // Function to handle editing a grocery
    fun onEditGrocery(grocery: Grocery) {
        groceryToEdit = grocery
        isEditMode = true
        showGroceryCreation = true
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fixed top bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SuperCartColors.white)
                .padding(
                    start = SuperCartSpacing.md,
                    end = SuperCartSpacing.md,
                    top = SuperCartSpacing.xl
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

                // Current date display
                Text(
                    text = java.time.LocalDate.now().toString(), // Format: yyyy-MM-dd
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Add Grocery Button (+ icon) with left padding
                Box(
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Card(
                        modifier = Modifier.size(56.dp),
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
                                modifier = Modifier.size(34.dp)
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
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search groceries...") },
                    modifier = Modifier.fillMaxWidth(0.78f),
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
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = SuperCartColors.darkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null
                )
                
                Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                
                // Collapse/Expand all toggle
                Card(
                    modifier = Modifier
                        .height(56.dp)
                        .width(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SuperCartColors.white
                    ),
                    border = CardDefaults.outlinedCardBorder(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    IconButton(
                        onClick = { isAllExpanded = !isAllExpanded },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isAllExpanded) "Collapse All" else "Expand All",
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Scrollable content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 180.dp, // Height of the top bar + extra gap
                    bottom = 100.dp, // Height of the bottom navigation bar + extra gap
                    start = SuperCartSpacing.md,
                    end = SuperCartSpacing.md
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // To Buy Section
                item {
                    if (toBuyCategories.isNotEmpty()) {
                        Text(
                            text = "Things to Buy",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuperCartColors.primaryGreen,
                            modifier = Modifier.padding(vertical = SuperCartSpacing.sm)
                        )
                        HierarchicalCategoryDisplay(
                            categories = toBuyCategories,
                            searchQuery = searchQuery,
                            isAllExpanded = isAllExpanded,
                            onEditGrocery = { onEditGrocery(it) },
                            useScroll = false,
                            isShoppingList = true
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SuperCartSpacing.sm),
                            colors = CardDefaults.cardColors(
                                containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = "No items to buy",
                                modifier = Modifier.padding(SuperCartSpacing.md),
                                color = SuperCartColors.gray
                            )
                        }
                    }
                }

                // Spacer between sections
                item {
                    Spacer(modifier = Modifier.height(SuperCartSpacing.lg))
                }

                // Bought Section
                item {
                    if (boughtCategories.isNotEmpty()) {
                        Text(
                            text = "Already Bought",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuperCartColors.primaryGreen,
                            modifier = Modifier.padding(vertical = SuperCartSpacing.sm)
                        )
                        // Flat list of bought groceries
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            boughtCategories.forEach { category ->
                                category.subCategories.forEach { subCategory ->
                                    subCategory.groceries.forEach { grocery ->
                                        BoughtGroceryCard(
                                            grocery = grocery,
                                            categoryName = category.category.name,
                                            onRemove = {
                                                // Update grocery using DataManagerObject helper
                                                DataManagerObject.updateGrocery(grocery.uuid) { 
                                                    it.copy(isBought = false)
                                                }
                                                
                                                // Save to DataStore
                                                scope.launch {
                                                    DataStoreManager.saveDataGlobally()
                                                    android.util.Log.d("datastore test", "Saved removal to DataStore")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SuperCartSpacing.sm),
                            colors = CardDefaults.cardColors(
                                containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = "No bought items",
                                modifier = Modifier.padding(SuperCartSpacing.md),
                                color = SuperCartColors.gray
                            )
                        }
                    }
                }

                // Finish Shopping button
                if (boughtCategories.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(SuperCartSpacing.lg))
                        Button(
                            onClick = { /* TODO: Implement finish shopping action */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SuperCartSpacing.md)
                                .padding(bottom = SuperCartSpacing.xxl),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuperCartColors.primaryGreen
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Finish Shopping (${boughtCategories.sumOf { it.getGroceriesCount() }} items)",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        // Categories Management Dialog
        if (showCategoriesManagement) {
            CategoriesManagementDialog(
                onDismiss = { showCategoriesManagement = false }
            )
        }
        
        // Grocery Creation/Edit Dialog
        if (showGroceryCreation) {
            GroceryCreationDialog(
                groceryToEdit = groceryToEdit,
                onDismiss = { 
                    showGroceryCreation = false
                    isEditMode = false
                    groceryToEdit = null
                },
                onGroceryCreated = { newGrocery ->
                    if (isEditMode && groceryToEdit != null) {
                        // Edit mode - update existing grocery
                        val originalGrocery = groceryToEdit!!
                        
                        // Check if the grocery is moving to a different sub-category
                        if (originalGrocery.categoryId != newGrocery.categoryId || 
                            originalGrocery.subCategoryId != newGrocery.subCategoryId) {
                            // Update location if category or sub-category changed
                            DataManagerObject.updateGroceryLocation(
                                originalGrocery.uuid,
                                newGrocery.categoryId,
                                newGrocery.subCategoryId
                            )
                        }
                        
                        // Update other properties
                        DataManagerObject.updateGrocery(originalGrocery.uuid) { newGrocery }
                        
                        // Save to DataStore
                        scope.launch {
                            DataStoreManager.saveDataGlobally()
                            android.util.Log.d("ShoppingListScreen", "Saved grocery update to DataStore")
                        }
                        
                        android.util.Log.d("ShoppingListScreen", "Grocery updated: ${newGrocery.name}")
                    } else {
                        // Create mode - add new grocery
                        DataManagerObject.addGrocery(newGrocery)
                        
                        // Save to DataStore
                        scope.launch {
                            DataStoreManager.saveDataGlobally()
                            android.util.Log.d("ShoppingListScreen", "Saved new grocery to DataStore")
                        }
                        android.util.Log.d("ShoppingListScreen", "New grocery added: ${newGrocery.name}")
                    }
                    
                    // Save the updated data to local storage
                    scope.launch {
                        DataStoreManager.saveDataGlobally()
                    }
                    
                    showGroceryCreation = false
                }
            )
        }
    }
}

@Composable
private fun BoughtGroceryCard(
    grocery: Grocery,
    categoryName: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                SuperCartColors.lightGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grocery name and category
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = grocery.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = categoryName,
                    fontSize = 12.sp,
                    color = SuperCartColors.gray
                )
            }
            
            // Remove icon
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from list",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}