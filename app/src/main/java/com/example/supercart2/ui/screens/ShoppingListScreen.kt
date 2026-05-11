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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R
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
import com.example.supercart2.ui.components.StoresManagementDialog
import com.example.supercart2.ui.components.GroceryCreationDialog
import com.example.supercart2.ui.components.ImportGroceriesDialog
import com.example.supercart2.ui.components.SettingsDialog
import com.example.supercart2.ui.components.GroupManagementDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import com.example.supercart2.ui.components.StoreBasedDisplay
import com.example.supercart2.ui.components.HideStoresDialog
import com.example.supercart2.utils.localizedCategoryDisplayName
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder

enum class ShoppingListViewMode {
    CATEGORY,  // Default hierarchical view
    STORE      // Grouped by stores
}

@Composable
fun ShoppingListScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToStoreEdit: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCategoriesManagement by remember { mutableStateOf(false) }
    var showStoresManagement by remember { mutableStateOf(false) }
    var showGroceryCreation by remember { mutableStateOf(false) }
    var showFinishConfirmation by remember { mutableStateOf(false) }
    var showImportGroceries by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showGroupManagement by remember { mutableStateOf(false) }
    var showHideStoresDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isAllExpanded by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ShoppingListViewMode.CATEGORY) }
    
    // Load saved view mode on screen creation
    LaunchedEffect(Unit) {
        val isStoreView = DataStoreManager.loadStoreViewMode(context)
        viewMode = if (isStoreView) ShoppingListViewMode.STORE else ShoppingListViewMode.CATEGORY
    }
    
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
    
    // Calculate item counts for each section
    val toBuyCount = remember(version, searchQuery) {
        toBuyCategories.sumOf { category ->
            category.subCategories.sumOf { subCategory ->
                subCategory.groceries.size
            }
        }
    }
    
    val boughtCount = remember(version, searchQuery) {
        boughtCategories.sumOf { category ->
            category.subCategories.sumOf { subCategory ->
                subCategory.groceries.size
            }
        }
    }
    
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
            // Burger menu and add grocery button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (when shown as sub-screen inside Stores)
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = SuperCartColors.black
                        )
                    }
                }

                // Burger menu with right padding
                Box(
                    modifier = Modifier.padding(start = if (onBack != null) 0.dp else 5.dp)
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
                                contentDescription = stringResource(R.string.add_new_grocery),
                                tint = SuperCartColors.white, // White icon
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
                    .padding(bottom = SuperCartSpacing.lg), // Increased spacing below search bar
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left spacer to align with burger icon
                Spacer(modifier = Modifier.width(15.dp))
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    modifier = Modifier.fillMaxWidth(0.78f),
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
                            contentDescription = stringResource(R.string.search),
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
                                    contentDescription = stringResource(R.string.clear_search),
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
                            contentDescription = if (isAllExpanded) stringResource(R.string.collapse_all) else stringResource(R.string.expand_all),
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            // View Mode Toggle Button
            Button(
                onClick = { 
                    viewMode = if (viewMode == ShoppingListViewMode.CATEGORY) {
                        ShoppingListViewMode.STORE
                    } else {
                        ShoppingListViewMode.CATEGORY
                    }
                    // Save view mode preference
                    scope.launch {
                        DataStoreManager.saveStoreViewMode(context, viewMode == ShoppingListViewMode.STORE)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SuperCartSpacing.md),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuperCartColors.white,
                    contentColor = SuperCartColors.primaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Icon(
                    imageVector = if (viewMode == ShoppingListViewMode.CATEGORY) {
                        Icons.Default.ShoppingCart
                    } else {
                        Icons.AutoMirrored.Filled.List
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (viewMode == ShoppingListViewMode.CATEGORY) {
                        stringResource(R.string.switch_to_store_view)
                    } else {
                        stringResource(R.string.switch_to_category_view)
                    }
                )
            }
            
            // Hide/Unhide Stores Button (only show in store view mode)
            if (viewMode == ShoppingListViewMode.STORE) {
                val hiddenCount = remember(version) { 
                    DataManagerObject.hiddenStoreIds.size 
                }
                
                Button(
                    onClick = { showHideStoresDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SuperCartSpacing.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen.copy(alpha = 0.1f),
                        contentColor = SuperCartColors.primaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Icon(
                        imageVector = if (hiddenCount > 0) {
                            Icons.Default.FavoriteBorder
                        } else {
                            Icons.Default.Favorite
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hiddenCount > 0) {
                            stringResource(R.string.show_hide_stores_hidden, hiddenCount)
                        } else {
                            stringResource(R.string.show_hide_stores)
                        }
                    )
                }
            }
        }

        // Scrollable content area (bottom padding from Scaffold innerPadding in MainActivity)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (viewMode == ShoppingListViewMode.STORE) 280.dp else 230.dp, // Extra space for hide button in store view
                    bottom = SuperCartSpacing.md,
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SuperCartSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.things_to_buy),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuperCartColors.primaryGreen
                            )
                            Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                            Text(
                                text = "($toBuyCount)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = SuperCartColors.gray
                            )
                        }
                        
                        when (viewMode) {
                            ShoppingListViewMode.CATEGORY -> {
                                HierarchicalCategoryDisplay(
                                    categories = toBuyCategories,
                                    searchQuery = searchQuery,
                                    isAllExpanded = isAllExpanded,
                                    onEditGrocery = { onEditGrocery(it) },
                                    useScroll = false,
                                    isShoppingList = true
                                )
                            }
                            ShoppingListViewMode.STORE -> {
                                StoreBasedDisplay(
                                    categories = toBuyCategories,
                                    searchQuery = searchQuery,
                                    isAllExpanded = isAllExpanded,
                                    onEditGrocery = { onEditGrocery(it) },
                                    useScroll = false
                                )
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
                                text = stringResource(R.string.no_items_to_buy),
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SuperCartSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.already_bought),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuperCartColors.primaryGreen
                            )
                            Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                            Text(
                                text = "($boughtCount)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = SuperCartColors.gray
                            )
                        }
                        // Flat list of bought groceries
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            boughtCategories.forEach { category ->
                                category.subCategories.forEach { subCategory ->
                                    subCategory.groceries.forEach { grocery ->
                                        BoughtGroceryCard(
                                            grocery = grocery,
                                            categoryName = localizedCategoryDisplayName(category.category.name),
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
                                text = stringResource(R.string.no_bought_items),
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
                            onClick = { showFinishConfirmation = true },
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
                                text = stringResource(R.string.finish_shopping_button, boughtCategories.sumOf { it.getGroceriesCount() }),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        // Finish Shopping Confirmation Dialog
        if (showFinishConfirmation) {
            AlertDialog(
                onDismissRequest = { showFinishConfirmation = false },
                title = {
                    Text(
                        text = stringResource(R.string.finish_shopping),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.finish_shopping_confirmation_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
                        Text(
                            text = stringResource(R.string.finish_shopping_bullet_history),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = stringResource(R.string.finish_shopping_bullet_remove),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
                    ) {
                        // Cancel Button (left) - secondary styled
                        Button(
                            onClick = { showFinishConfirmation = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuperCartColors.white,
                                contentColor = SuperCartColors.primaryGreen
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                        
                        // Confirm Button (right) - primary styled
                        Button(
                            onClick = {
                                // Confirm all bought items
                                DataManagerObject.confirmBoughtItems()
                                
                                // Save to DataStore
                                scope.launch {
                                    DataStoreManager.saveDataGlobally()
                                    android.util.Log.d("ShoppingListScreen", "Saved shopping completion to DataStore")
                                }
                                
                                showFinishConfirmation = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuperCartColors.primaryGreen,
                                contentColor = SuperCartColors.white
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.confirm)
                            )
                        }
                    }
                }
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
        
        // Hide/Unhide Stores Dialog
        if (showHideStoresDialog) {
            HideStoresDialog(
                onDismiss = { showHideStoresDialog = false }
            )
        }
        
        // Settings Dialog
        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false }
            )
        }
        
        if (showGroupManagement) {
            GroupManagementDialog(
                onDismiss = { showGroupManagement = false }
            )
        }
        
        // Import Groceries Dialog
        if (showImportGroceries) {
            ImportGroceriesDialog(
                onDismiss = { showImportGroceries = false },
                onImportComplete = {
                    showImportGroceries = false
                }
            )
        }
        
        // Grocery Creation/Edit Dialog
        if (showGroceryCreation) {
            GroceryCreationDialog(
                groceryToEdit = groceryToEdit,
                initialGroceryName = if (isEditMode) "" else searchQuery,
                addToShoppingList = !isEditMode, // Add to shopping list when creating from shopping list screen
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
                    contentDescription = stringResource(R.string.remove_from_list),
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}