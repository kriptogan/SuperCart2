package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.components.BurgerMenu
import com.example.supercart2.ui.components.CategoriesManagementDialog
import com.example.supercart2.ui.components.GroceryCreationDialog
import com.example.supercart2.ui.components.GroupManagementDialog
import com.example.supercart2.ui.components.HierarchicalCategoryDisplay
import com.example.supercart2.ui.components.ImportGroceriesDialog
import com.example.supercart2.ui.components.SettingsDialog
import com.example.supercart2.ui.components.StoresManagementDialog
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName
import kotlinx.coroutines.launch

// Sub-screen navigation sealed class
private sealed class StoresSubScreen {
    object Landing : StoresSubScreen()
    object ViewGroceries : StoresSubScreen()
    object SelectStore : StoresSubScreen()
    data class StoreDetail(val storeId: String) : StoresSubScreen()
}

private data class StoreCardData(
    val storeId: String,
    val storeName: String,
    val itemCount: Int,
    val uniqueCount: Int
)

@Composable
fun StoresScreen() {
    var subScreen by remember { mutableStateOf<StoresSubScreen>(StoresSubScreen.Landing) }

    when (val screen = subScreen) {
        is StoresSubScreen.Landing -> StoresLandingView(
            onViewGroceries = { subScreen = StoresSubScreen.ViewGroceries },
            onSelectStore = { subScreen = StoresSubScreen.SelectStore }
        )
        is StoresSubScreen.ViewGroceries -> StoresViewGroceriesView(
            onSwitchToStoreView = { subScreen = StoresSubScreen.SelectStore },
            onBack = { subScreen = StoresSubScreen.Landing }
        )
        is StoresSubScreen.SelectStore -> StoresSelectStoreView(
            onStoreSelected = { storeId -> subScreen = StoresSubScreen.StoreDetail(storeId) },
            onBack = { subScreen = StoresSubScreen.Landing }
        )
        is StoresSubScreen.StoreDetail -> StoresDetailView(
            storeId = screen.storeId,
            onExit = { subScreen = StoresSubScreen.SelectStore }
        )
    }
}

// ─── Landing ────────────────────────────────────────────────────────────────

@Composable
private fun StoresLandingView(
    onViewGroceries: () -> Unit,
    onSelectStore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SuperCartColors.lightGreen)
            .padding(horizontal = SuperCartSpacing.lg, vertical = SuperCartSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.lg)
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = SuperCartColors.primaryGreen,
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = stringResource(R.string.nav_stores),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SuperCartColors.primaryGreen
            )

            Spacer(modifier = Modifier.height(SuperCartSpacing.md))

            // View Groceries button
            LandingOptionCard(
                titleRes = R.string.view_groceries,
                descRes = R.string.view_groceries_desc,
                onClick = onViewGroceries
            )

            // Select a Store button
            LandingOptionCard(
                titleRes = R.string.select_a_store,
                descRes = R.string.select_a_store_desc,
                onClick = onSelectStore
            )
        }
    }
}

@Composable
private fun LandingOptionCard(
    titleRes: Int,
    descRes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SuperCartSpacing.lg, vertical = SuperCartSpacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleRes),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SuperCartColors.black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(descRes),
                    fontSize = 13.sp,
                    color = SuperCartColors.gray
                )
            }
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = SuperCartColors.primaryGreen,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ─── View Groceries ─────────────────────────────────────────────────────────

@Composable
private fun StoresViewGroceriesView(
    onSwitchToStoreView: () -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val version = DataManagerObject.version

    var showCategoriesManagement by remember { mutableStateOf(false) }
    var showStoresManagement by remember { mutableStateOf(false) }
    var showGroceryCreation by remember { mutableStateOf(false) }
    var showImportGroceries by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showGroupManagement by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isAllExpanded by remember { mutableStateOf(false) }
    var groceryToEdit by remember { mutableStateOf<Grocery?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

    val toBuyCategories = remember(version, searchQuery) {
        DataManagerObject.getSortedCategories().mapNotNull { catWithSubs ->
            val filteredSubs = catWithSubs.subCategories.mapNotNull { subWithGroceries ->
                val filtered = subWithGroceries.groceries.filter { g ->
                    g.inShoppingList && !g.isBought &&
                    (searchQuery.isEmpty() || g.name.contains(searchQuery, ignoreCase = true))
                }
                if (filtered.isNotEmpty()) SubCategoryWithGroceries(subWithGroceries.subCategory, filtered.toMutableList()) else null
            }
            if (filteredSubs.isNotEmpty()) CategoryWithSubCategories(catWithSubs.category, filteredSubs.toMutableList()) else null
        }
    }

    val toBuyCount = remember(toBuyCategories) {
        toBuyCategories.sumOf { c -> c.subCategories.sumOf { s -> s.groceries.size } }
    }

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
        // Fixed header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SuperCartColors.lightGreen)
                .padding(start = SuperCartSpacing.md, end = SuperCartSpacing.md, top = SuperCartSpacing.xl)
        ) {
            // Burger + date + create row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.padding(start = 5.dp)) {
                    BurgerMenu(
                        onCategoriesManagementClick = { showCategoriesManagement = true },
                        onManageStoresClick = { showStoresManagement = true },
                        onManageGroupClick = { showGroupManagement = true },
                        onImportGroceriesClick = { showImportGroceries = true },
                        onSettingsClick = { showSettings = true }
                    )
                }
                Text(
                    text = java.time.LocalDate.now().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Box(modifier = Modifier.padding(end = 12.dp)) {
                    Card(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = SuperCartColors.primaryGreen),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isEditMode = false
                                groceryToEdit = null
                                showGroceryCreation = true
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_new_grocery),
                                tint = SuperCartColors.white,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }

            // Search + collapse row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(15.dp))
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
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search), tint = SuperCartColors.darkGray, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else null
                )
                Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                Card(
                    modifier = Modifier.height(56.dp).width(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
                    border = CardDefaults.outlinedCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    IconButton(onClick = { isAllExpanded = !isAllExpanded }, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Switch to Store View button
            Button(
                onClick = onSwitchToStoreView,
                modifier = Modifier.fillMaxWidth().padding(horizontal = SuperCartSpacing.md),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuperCartColors.white,
                    contentColor = SuperCartColors.primaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(imageVector = Icons.Default.Store, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.change_to_store_view))
            }
        }

        // Scrollable content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 268.dp, bottom = SuperCartSpacing.md, start = SuperCartSpacing.md, end = SuperCartSpacing.md)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    if (toBuyCategories.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.things_to_buy),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuperCartColors.primaryGreen
                            )
                            Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                            Text(text = "($toBuyCount)", fontSize = 16.sp, color = SuperCartColors.gray)
                        }
                        HierarchicalCategoryDisplay(
                            categories = toBuyCategories,
                            searchQuery = searchQuery,
                            isAllExpanded = isAllExpanded,
                            onEditGrocery = { onEditGrocery(it) },
                            useScroll = false,
                            isShoppingList = false
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm),
                            colors = CardDefaults.cardColors(containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = stringResource(R.string.no_items_to_buy),
                                modifier = Modifier.padding(SuperCartSpacing.md),
                                color = SuperCartColors.gray
                            )
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showCategoriesManagement) CategoriesManagementDialog(onDismiss = { showCategoriesManagement = false })
        if (showStoresManagement) StoresManagementDialog(onDismiss = { showStoresManagement = false })
        if (showSettings) SettingsDialog(onDismiss = { showSettings = false })
        if (showGroupManagement) GroupManagementDialog(onDismiss = { showGroupManagement = false })
        if (showImportGroceries) ImportGroceriesDialog(onDismiss = { showImportGroceries = false }, onImportComplete = { showImportGroceries = false })
        if (showGroceryCreation) {
            GroceryCreationDialog(
                groceryToEdit = groceryToEdit,
                initialGroceryName = if (isEditMode) "" else searchQuery,
                addToShoppingList = !isEditMode,
                onDismiss = {
                    showGroceryCreation = false
                    isEditMode = false
                    groceryToEdit = null
                },
                onGroceryCreated = { newGrocery ->
                    if (isEditMode && groceryToEdit != null) {
                        val original = groceryToEdit!!
                        if (original.categoryId != newGrocery.categoryId || original.subCategoryId != newGrocery.subCategoryId) {
                            DataManagerObject.updateGroceryLocation(original.uuid, newGrocery.categoryId, newGrocery.subCategoryId)
                        }
                        DataManagerObject.updateGrocery(original.uuid) { newGrocery }
                    } else {
                        DataManagerObject.addGrocery(newGrocery)
                    }
                    scope.launch { DataStoreManager.saveDataGlobally() }
                    showGroceryCreation = false
                }
            )
        }
    }
}

// ─── Select a Store ──────────────────────────────────────────────────────────

@Composable
private fun StoresSelectStoreView(
    onStoreSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val version = DataManagerObject.version

    val activeStores = remember(version) {
        val shoppingGroceries = DataManagerObject.categories
            .flatMap { it.subCategories }
            .flatMap { it.groceries }
            .filter { it.inShoppingList && !it.deleted }

        DataManagerObject.getSortedStores()
            .filter { store -> shoppingGroceries.any { g -> store.uuid in g.storeIds } }
            .map { store ->
                val storeGroceries = shoppingGroceries.filter { g -> store.uuid in g.storeIds }
                StoreCardData(
                    storeId = store.uuid,
                    storeName = store.name,
                    itemCount = storeGroceries.size,
                    uniqueCount = storeGroceries.count { it.storeIds.size == 1 }
                )
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SuperCartColors.lightGreen)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SuperCartColors.lightGreen)
                .padding(start = SuperCartSpacing.md, end = SuperCartSpacing.md, top = SuperCartSpacing.xl)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = SuperCartColors.black
                    )
                }
                Text(
                    text = stringResource(R.string.select_a_store),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuperCartColors.primaryGreen,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Store list
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp, bottom = SuperCartSpacing.md, start = SuperCartSpacing.md, end = SuperCartSpacing.md)
        ) {
            if (activeStores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_stores_with_items),
                        color = SuperCartColors.gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(SuperCartSpacing.xl)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.md)
                ) {
                    items(activeStores.size) { idx ->
                        val storeData = activeStores[idx]
                        StoreSelectionCard(
                            storeData = storeData,
                            onClick = { onStoreSelected(storeData.storeId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreSelectionCard(
    storeData: StoreCardData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SuperCartSpacing.lg, vertical = SuperCartSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Store icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(SuperCartColors.primaryGreen.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = SuperCartColors.primaryGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(SuperCartSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = storeData.storeName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SuperCartColors.black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StoreStatChip(label = stringResource(R.string.items_count, storeData.itemCount))
                    StoreStatChip(label = stringResource(R.string.unique_items_count, storeData.uniqueCount))
                }
            }

            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = SuperCartColors.primaryGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun StoreStatChip(label: String) {
    Box(
        modifier = Modifier
            .background(SuperCartColors.primaryGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = SuperCartColors.primaryGreen, fontWeight = FontWeight.Medium)
    }
}

// ─── Store Detail ────────────────────────────────────────────────────────────

@Composable
private fun StoresDetailView(
    storeId: String,
    onExit: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val version = DataManagerObject.version

    var searchQuery by remember { mutableStateOf("") }
    var isAllExpanded by remember { mutableStateOf(false) }
    var showGroceryCreation by remember { mutableStateOf(false) }
    var showFinishConfirmation by remember { mutableStateOf(false) }
    var groceryToEdit by remember { mutableStateOf<Grocery?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

    val store = remember(version) { DataManagerObject.getSortedStores().find { it.uuid == storeId } }
    val storeName = store?.name ?: ""

    val toBuyCategories = remember(version, searchQuery) {
        DataManagerObject.getSortedCategories().mapNotNull { catWithSubs ->
            val filteredSubs = catWithSubs.subCategories.mapNotNull { subWithGroceries ->
                val filtered = subWithGroceries.groceries.filter { g ->
                    g.inShoppingList && !g.isBought && storeId in g.storeIds &&
                    (searchQuery.isEmpty() || g.name.contains(searchQuery, ignoreCase = true))
                }
                if (filtered.isNotEmpty()) SubCategoryWithGroceries(subWithGroceries.subCategory, filtered.toMutableList()) else null
            }
            if (filteredSubs.isNotEmpty()) CategoryWithSubCategories(catWithSubs.category, filteredSubs.toMutableList()) else null
        }
    }

    val boughtCategories = remember(version, searchQuery) {
        DataManagerObject.getSortedCategories().mapNotNull { catWithSubs ->
            val filteredSubs = catWithSubs.subCategories.mapNotNull { subWithGroceries ->
                val filtered = subWithGroceries.groceries.filter { g ->
                    g.inShoppingList && g.isBought && storeId in g.storeIds &&
                    (searchQuery.isEmpty() || g.name.contains(searchQuery, ignoreCase = true))
                }
                if (filtered.isNotEmpty()) SubCategoryWithGroceries(subWithGroceries.subCategory, filtered.toMutableList()) else null
            }
            if (filteredSubs.isNotEmpty()) CategoryWithSubCategories(catWithSubs.category, filteredSubs.toMutableList()) else null
        }
    }

    val toBuyCount = remember(toBuyCategories) { toBuyCategories.sumOf { c -> c.subCategories.sumOf { s -> s.groceries.size } } }
    val boughtCount = remember(boughtCategories) { boughtCategories.sumOf { c -> c.subCategories.sumOf { s -> s.groceries.size } } }

    val headerTopPadding = if (boughtCount > 0) 262.dp else 210.dp

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
        // Fixed header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SuperCartColors.lightGreen)
                .padding(start = SuperCartSpacing.md, end = SuperCartSpacing.md, top = SuperCartSpacing.xl)
        ) {
            // Back button + store name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExit) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = SuperCartColors.black
                    )
                }
                Text(
                    text = storeName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuperCartColors.primaryGreen,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Search + collapse row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SuperCartSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
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
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SuperCartColors.gray, modifier = Modifier.size(24.dp))
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = SuperCartColors.darkGray, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else null
                )
                Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                Card(
                    modifier = Modifier.height(56.dp).width(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
                    border = CardDefaults.outlinedCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    IconButton(onClick = { isAllExpanded = !isAllExpanded }, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = SuperCartColors.primaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Finish Shopping button (only if bought items exist)
            if (boughtCount > 0) {
                Button(
                    onClick = { showFinishConfirmation = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SuperCartSpacing.md),
                    colors = ButtonDefaults.buttonColors(containerColor = SuperCartColors.primaryGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.finish_shopping_store, boughtCount))
                }
            }
        }

        // Scrollable content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = headerTopPadding, bottom = SuperCartSpacing.md, start = SuperCartSpacing.md, end = SuperCartSpacing.md)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // To Buy section
                item {
                    if (toBuyCategories.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.things_to_buy), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SuperCartColors.primaryGreen)
                            Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                            Text(text = "($toBuyCount)", fontSize = 16.sp, color = SuperCartColors.gray)
                        }
                        HierarchicalCategoryDisplay(
                            categories = toBuyCategories,
                            searchQuery = searchQuery,
                            isAllExpanded = isAllExpanded,
                            onEditGrocery = { onEditGrocery(it) },
                            useScroll = false,
                            isShoppingList = true
                        )
                    } else if (boughtCategories.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm),
                            colors = CardDefaults.cardColors(containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = stringResource(R.string.no_items_to_buy),
                                modifier = Modifier.padding(SuperCartSpacing.md),
                                color = SuperCartColors.gray
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(SuperCartSpacing.lg)) }

                // Already Bought section
                item {
                    if (boughtCategories.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = SuperCartSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.already_bought), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SuperCartColors.primaryGreen)
                            Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                            Text(text = "($boughtCount)", fontSize = 16.sp, color = SuperCartColors.gray)
                        }
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            boughtCategories.forEach { category ->
                                category.subCategories.forEach { subCategory ->
                                    subCategory.groceries.forEach { grocery ->
                                        DetailBoughtGroceryCard(
                                            grocery = grocery,
                                            categoryName = localizedCategoryDisplayName(category.category.name),
                                            onRemove = {
                                                DataManagerObject.updateGrocery(grocery.uuid) { it.copy(isBought = false) }
                                                scope.launch { DataStoreManager.saveDataGlobally() }
                                            }
                                        )
                                    }
                                }
                            }
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
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.finish_shopping_confirmation_intro), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
                        Text(text = stringResource(R.string.finish_shopping_bullet_history), style = MaterialTheme.typography.bodyMedium)
                        Text(text = stringResource(R.string.finish_shopping_bullet_remove), style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)) {
                        Button(
                            onClick = { showFinishConfirmation = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SuperCartColors.white, contentColor = SuperCartColors.primaryGreen),
                            modifier = Modifier.weight(1f)
                        ) { Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cancel)) }

                        Button(
                            onClick = {
                                DataManagerObject.confirmBoughtItemsForStore(storeId)
                                scope.launch { DataStoreManager.saveDataGlobally() }
                                showFinishConfirmation = false
                                onExit()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuperCartColors.primaryGreen, contentColor = SuperCartColors.white),
                            modifier = Modifier.weight(1f)
                        ) { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = stringResource(R.string.confirm)) }
                    }
                }
            )
        }

        // Grocery edit dialog
        if (showGroceryCreation) {
            GroceryCreationDialog(
                groceryToEdit = groceryToEdit,
                initialGroceryName = "",
                addToShoppingList = false,
                onDismiss = {
                    showGroceryCreation = false
                    isEditMode = false
                    groceryToEdit = null
                },
                onGroceryCreated = { newGrocery ->
                    if (isEditMode && groceryToEdit != null) {
                        val original = groceryToEdit!!
                        if (original.categoryId != newGrocery.categoryId || original.subCategoryId != newGrocery.subCategoryId) {
                            DataManagerObject.updateGroceryLocation(original.uuid, newGrocery.categoryId, newGrocery.subCategoryId)
                        }
                        DataManagerObject.updateGrocery(original.uuid) { newGrocery }
                        scope.launch { DataStoreManager.saveDataGlobally() }
                    }
                    showGroceryCreation = false
                }
            )
        }
    }
}

@Composable
private fun DetailBoughtGroceryCard(
    grocery: Grocery,
    categoryName: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = grocery.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = categoryName, fontSize = 12.sp, color = SuperCartColors.gray)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.remove_from_list), tint = Color.Red, modifier = Modifier.size(20.dp))
            }
        }
    }
}
