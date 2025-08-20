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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
    onEditCategory: ((Category) -> Unit)? = null
) {
    var subCategories by remember { mutableStateOf<List<SubCategory>>(emptyList()) }
    var groceries by remember { mutableStateOf<List<Grocery>>(emptyList()) }
    var showEditForm by remember { mutableStateOf(false) }
    var groceryToEdit by remember { mutableStateOf<Grocery?>(null) }
    var isAllExpanded by remember { mutableStateOf(true) }
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
            onGroceryCreated = onGroceryCreated,
            isAllExpanded = isAllExpanded,
            onToggleAll = { isAllExpanded = !isAllExpanded }
        )
        
        // Temporary test button to create categories
        Button(
            onClick = onShowCategoryDialog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Category (Test)")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Categories list with collapsible sections
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                val categorySubCategories = subCategories.filter { it.categoryId == category.uuid }
                CollapsibleCategorySection(
                    category = category,
                    subCategories = categorySubCategories,
                    groceries = groceries,
                    onEditGrocery = { grocery ->
                        groceryToEdit = grocery
                        showEditForm = true
                    },
                    isExpandedGlobal = isAllExpanded
                )
            }
        }
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
