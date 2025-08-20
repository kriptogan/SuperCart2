package com.kriptogan.supercart2.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.SubCategory
import com.kriptogan.supercart2.classes.Grocery
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.classes.LocalStorageManager
import com.kriptogan.supercart2.ui.components.CollapsibleCategorySection
import com.kriptogan.supercart2.ui.components.AppHeader
import com.kriptogan.supercart2.ui.components.ReusableFullScreenWindow
import com.kriptogan.supercart2.ui.components.GroceryCreationForm
import com.kriptogan.supercart2.ui.components.SearchFilteredCategories
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun HomeContent(
    firebaseManager: FirebaseManager,
    modifier: Modifier,
    categories: List<Category>,
    localStorageManager: LocalStorageManager,
    onShowCategoryDialog: () -> Unit,
    onDeleteAllCategories: () -> Unit,
    onEditCategory: ((Category) -> Unit)? = null,
    onCategoryUpdated: (Category, String, Int) -> Unit = { _, _, _ -> }
) {
    var subCategories by remember { mutableStateOf<List<SubCategory>>(emptyList()) }
    var groceries by remember { mutableStateOf<List<Grocery>>(emptyList()) }
    var showEditForm by remember { mutableStateOf(false) }
    var groceryToEdit by remember { mutableStateOf<Grocery?>(null) }
    var isAllExpanded by remember { mutableStateOf(true) }
    var expandedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedSubCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var currentSearchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    // Load sub-categories and groceries when component is created AND when categories change
    LaunchedEffect(categories.size) {
        subCategories = localStorageManager.getSubCategories()
        groceries = localStorageManager.getGroceries()
    }
    
    // Handle grocery creation
    val onGroceryCreated = { name: String, subCategoryId: String, expirationDate: String? ->
        val grocery = Grocery(
            name = name,
            subCategoryId = subCategoryId,
            expirationDate = expirationDate
        )
        
        // Save to local storage
        localStorageManager.addGrocery(grocery)
        
        // Update groceries list to reflect changes immediately
        groceries = localStorageManager.getGroceries()
        
        // Save to Firebase
        coroutineScope.launch {
            try {
                firebaseManager.saveGrocery(grocery)
            } catch (e: Exception) {
                // Firebase failure doesn't affect local state
            }
        }
        
        // Return Unit explicitly
        Unit
    }
    
    // Handle grocery updates
    val onGroceryUpdated = { name: String, subCategoryId: String, expirationDate: String? ->
        groceryToEdit?.let { originalGrocery ->
            val updatedGrocery = originalGrocery.copy(
                name = name,
                subCategoryId = subCategoryId,
                expirationDate = expirationDate
            )
            
            // Update in local storage
            localStorageManager.updateGrocery(updatedGrocery)
            
            // Update groceries list to reflect changes immediately
            groceries = localStorageManager.getGroceries()
            
            // Update in Firebase
            coroutineScope.launch {
                try {
                    firebaseManager.updateGrocery(updatedGrocery)
                } catch (e: Exception) {
                    // Firebase failure doesn't affect local state
                }
            }
        }
        
        // Close the edit form
        showEditForm = false
        groceryToEdit = null
    }
    
    // Handle search functionality
    val handleSearch = { query: String ->
        currentSearchQuery = query
        
        if (query.isBlank()) {
            // If search is empty, expand all
            isAllExpanded = true
            expandedCategories = emptySet()
            expandedSubCategories = emptySet()
        } else {
            // Search for groceries matching the query
            val matchingGroceries = groceries.filter { 
                it.name.contains(query, ignoreCase = true) 
            }
            
            if (matchingGroceries.isNotEmpty()) {
                // Find the categories and sub-categories that contain matching groceries
                val relevantSubCategoryIds = matchingGroceries.map { it.subCategoryId }.toSet()
                val relevantCategories = subCategories
                    .filter { it.uuid in relevantSubCategoryIds }
                    .map { it.categoryId }
                    .toSet()
                
                // Expand only the relevant categories and sub-categories
                isAllExpanded = false
                expandedCategories = relevantCategories
                expandedSubCategories = relevantSubCategoryIds
            } else {
                // No matches found, collapse everything
                isAllExpanded = false
                expandedCategories = emptySet()
                expandedSubCategories = emptySet()
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header with search bar and functional plus button
        AppHeader(
            categories = categories,
            subCategories = subCategories,
            groceries = groceries,
            onGroceryCreated = onGroceryCreated,
            onCategoryUpdated = onCategoryUpdated,
            isAllExpanded = isAllExpanded,
            onToggleAll = { isAllExpanded = !isAllExpanded },
            onSearch = { query -> handleSearch(query) }
        )
        
        // Categories list with collapsible sections
        SearchFilteredCategories(
            categories = categories,
            subCategories = subCategories,
            groceries = groceries,
            expandedCategories = expandedCategories,
            expandedSubCategories = expandedSubCategories,
            isAllExpanded = isAllExpanded,
            currentSearchQuery = currentSearchQuery,
            onEditGrocery = { grocery ->
                groceryToEdit = grocery
                showEditForm = true
            }
        )
    }
    
    // Grocery edit form dialog
    if (showEditForm && groceryToEdit != null) {
        ReusableFullScreenWindow(
            isVisible = showEditForm,
            onDismiss = { 
                showEditForm = false
                groceryToEdit = null
            },
            title = "Edit Grocery",
            content = {
                GroceryCreationForm(
                    categories = categories,
                    subCategories = subCategories,
                    onSave = onGroceryUpdated,
                    onCancel = { 
                        showEditForm = false
                        groceryToEdit = null
                    },
                    groceryToEdit = groceryToEdit
                )
            }
        )
    }
}
