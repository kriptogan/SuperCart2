package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sort
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
import com.example.supercart2.ui.components.GroceryCreationDialog
import com.example.supercart2.ui.components.HierarchicalCategoryDisplay
import com.example.supercart2.ui.components.StoreCategoryOrderDialog
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName
import kotlinx.coroutines.launch

// ─── Store Mode ─────────────────────────────────────────────────────────────

enum class StoreMode { Buying, Edit }

// ─── Sub-screen navigation sealed class ─────────────────────────────────────

private sealed class StoresSubScreen {
    object Landing : StoresSubScreen()
    object ShoppingList : StoresSubScreen()
    object SelectStore : StoresSubScreen()
    data class StoreDetail(val storeId: String, val mode: StoreMode = StoreMode.Buying, val returnToBuying: Boolean = false) : StoresSubScreen()
}

private data class StoreCardData(
    val storeId: String,
    val storeName: String,
    val itemCount: Int,
    val uniqueCount: Int
)

@Composable
fun StoresScreen(
    editModeStoreId: String? = null,
    onEditModeStoreConsumed: () -> Unit = {}
) {
    var subScreen by remember { mutableStateOf<StoresSubScreen>(StoresSubScreen.Landing) }

    LaunchedEffect(editModeStoreId) {
        if (editModeStoreId != null) {
            subScreen = StoresSubScreen.StoreDetail(editModeStoreId, StoreMode.Edit)
            onEditModeStoreConsumed()
        }
    }

    when (val screen = subScreen) {
        is StoresSubScreen.Landing -> StoresLandingView(
            onViewGroceries = { subScreen = StoresSubScreen.ShoppingList },
            onSelectStore = { subScreen = StoresSubScreen.SelectStore }
        )
        is StoresSubScreen.ShoppingList -> ShoppingListScreen(
            onBack = { subScreen = StoresSubScreen.Landing },
            onNavigateToStoreEdit = { storeId ->
                subScreen = StoresSubScreen.StoreDetail(storeId, StoreMode.Edit)
            }
        )
        is StoresSubScreen.SelectStore -> StoresSelectStoreView(
            onStoreSelected = { storeId -> subScreen = StoresSubScreen.StoreDetail(storeId, StoreMode.Buying) },
            onBack = { subScreen = StoresSubScreen.Landing }
        )
        is StoresSubScreen.StoreDetail -> StoresDetailView(
            storeId = screen.storeId,
            storeMode = screen.mode,
            onExit = {
                if (screen.mode == StoreMode.Edit && screen.returnToBuying) {
                    subScreen = StoresSubScreen.StoreDetail(screen.storeId, StoreMode.Buying)
                } else {
                    subScreen = StoresSubScreen.SelectStore
                }
            },
            onEnterEditMode = { subScreen = StoresSubScreen.StoreDetail(screen.storeId, StoreMode.Edit, returnToBuying = true) }
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

            LandingOptionCard(
                titleRes = R.string.view_groceries,
                descRes = R.string.view_groceries_desc,
                onClick = onViewGroceries
            )

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp, start = SuperCartSpacing.md, end = SuperCartSpacing.md)
        ) {
            if (activeStores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_stores_yet),
                        color = SuperCartColors.gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
                ) {
                    items(activeStores.size) { index ->
                        StoreSelectionCard(
                            storeData = activeStores[index],
                            onClick = { onStoreSelected(activeStores[index].storeId) }
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
    storeMode: StoreMode,
    onExit: () -> Unit,
    onEnterEditMode: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val version = DataManagerObject.version

    var searchQuery by remember { mutableStateOf("") }
    var isAllExpanded by remember { mutableStateOf(false) }
    var showGroceryCreation by remember { mutableStateOf(false) }
    var showFinishConfirmation by remember { mutableStateOf(false) }
    var showReorderCategories by remember { mutableStateOf(false) }
    var groceryToEdit by remember { mutableStateOf<Grocery?>(null) }
    var isGroceryEditMode by remember { mutableStateOf(false) }

    // Edit mode store info fields
    var editStoreName by remember { mutableStateOf("") }
    var editStoreAddress by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val savedText = stringResource(R.string.done)

    val store = remember(version) { DataManagerObject.getSortedStores().find { it.uuid == storeId } }
    val storeName = store?.name ?: ""

    LaunchedEffect(store) {
        editStoreName = store?.name ?: ""
        editStoreAddress = store?.address ?: ""
    }

    // Use store-specific ordering
    val toBuyCategories = remember(version, searchQuery, storeMode) {
        DataManagerObject.getSortedCategoriesForStore(storeId).mapNotNull { catWithSubs ->
            val filteredSubs = catWithSubs.subCategories.mapNotNull { subWithGroceries ->
                val filtered = if (storeMode == StoreMode.Buying) {
                    subWithGroceries.groceries.filter { g ->
                        g.inShoppingList && !g.isBought && storeId in g.storeIds &&
                        (searchQuery.isEmpty() || g.name.contains(searchQuery, ignoreCase = true))
                    }
                } else {
                    subWithGroceries.groceries.filter { g ->
                        storeId in g.storeIds &&
                        (searchQuery.isEmpty() || g.name.contains(searchQuery, ignoreCase = true))
                    }
                }
                if (filtered.isNotEmpty()) SubCategoryWithGroceries(subWithGroceries.subCategory, filtered.toMutableList()) else null
            }
            if (filteredSubs.isNotEmpty()) CategoryWithSubCategories(catWithSubs.category, filteredSubs.toMutableList()) else null
        }
    }

    val boughtCategories = remember(version, searchQuery, storeMode) {
        if (storeMode == StoreMode.Buying) {
            DataManagerObject.getSortedCategoriesForStore(storeId).mapNotNull { catWithSubs ->
                val filteredSubs = catWithSubs.subCategories.mapNotNull { subWithGroceries ->
                    val filtered = subWithGroceries.groceries.filter { g ->
                        g.inShoppingList && g.isBought && storeId in g.storeIds &&
                        (searchQuery.isEmpty() || g.name.contains(searchQuery, ignoreCase = true))
                    }
                    if (filtered.isNotEmpty()) SubCategoryWithGroceries(subWithGroceries.subCategory, filtered.toMutableList()) else null
                }
                if (filteredSubs.isNotEmpty()) CategoryWithSubCategories(catWithSubs.category, filteredSubs.toMutableList()) else null
            }
        } else emptyList()
    }

    val toBuyCount = remember(toBuyCategories) { toBuyCategories.sumOf { c -> c.subCategories.sumOf { s -> s.groceries.size } } }
    val boughtCount = remember(boughtCategories) { boughtCategories.sumOf { c -> c.subCategories.sumOf { s -> s.groceries.size } } }

    val headerTopPadding = when {
        storeMode == StoreMode.Edit -> 100.dp
        boughtCount > 0 -> 248.dp
        else -> 196.dp
    }

    fun onEditGrocery(grocery: Grocery) {
        groceryToEdit = grocery
        isGroceryEditMode = true
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
            // Title row
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
                    text = if (storeMode == StoreMode.Edit) stringResource(R.string.edit_mode) else storeName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuperCartColors.primaryGreen,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                // Edit mode entry icon (buying mode only)
                if (storeMode == StoreMode.Buying) {
                    IconButton(onClick = onEnterEditMode) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_store),
                            tint = SuperCartColors.primaryGreen
                        )
                    }
                }
            }

            // Date row (buying mode only)
            if (storeMode == StoreMode.Buying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SuperCartSpacing.sm),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = java.time.LocalDate.now().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Search + collapse row (buying mode only — in edit mode it lives in the scroll area)
            if (storeMode == StoreMode.Buying) {
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
                        modifier = Modifier
                            .height(56.dp)
                            .width(56.dp),
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
            }

            // Finish Shopping button (buying mode only, when bought items exist)
            if (storeMode == StoreMode.Buying && boughtCount > 0) {
                Button(
                    onClick = { showFinishConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SuperCartSpacing.md),
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
                // Edit mode: Store Info card (first), then search row below it
                if (storeMode == StoreMode.Edit) {
                    item {
                        StoreInfoCard(
                            storeName = editStoreName,
                            storeAddress = editStoreAddress,
                            onStoreNameChange = { editStoreName = it },
                            onStoreAddressChange = { editStoreAddress = it },
                            onReorderCategories = { showReorderCategories = true },
                            onConfirm = {
                                val trimmedName = editStoreName.trim()
                                if (trimmedName.isNotBlank()) {
                                    DataManagerObject.updateStore(storeId) { s ->
                                        s.copy(name = trimmedName, address = editStoreAddress.trim())
                                    }
                                    scope.launch {
                                        DataStoreManager.saveDataGlobally()
                                        snackbarHostState.showSnackbar(savedText)
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
                    }
                    // Search + expand row below the store info card
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = SuperCartSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.search_hint)) },
                                modifier = Modifier.weight(1f),
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
                    }
                }

                item {
                    // Items list
                    if (toBuyCategories.isNotEmpty()) {
                        // "Things to Buy" label (buying mode only)
                        if (storeMode == StoreMode.Buying) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = SuperCartSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = stringResource(R.string.things_to_buy), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SuperCartColors.primaryGreen)
                                Spacer(modifier = Modifier.width(SuperCartSpacing.sm))
                                Text(text = "($toBuyCount)", fontSize = 16.sp, color = SuperCartColors.gray)
                            }
                        }
                        HierarchicalCategoryDisplay(
                            categories = toBuyCategories,
                            searchQuery = searchQuery,
                            isAllExpanded = isAllExpanded,
                            onEditGrocery = { onEditGrocery(it) },
                            useScroll = false,
                            isShoppingList = storeMode == StoreMode.Buying
                        )
                    } else if (boughtCategories.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SuperCartSpacing.sm),
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

                // Already Bought section (buying mode only)
                item {
                    if (storeMode == StoreMode.Buying && boughtCategories.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SuperCartSpacing.sm),
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

                // Extra space at bottom for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Snackbar host (appears above the FAB)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Floating Add Grocery button (both modes)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Card(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = SuperCartColors.primaryGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                IconButton(
                    onClick = {
                        isGroceryEditMode = false
                        groceryToEdit = null
                        showGroceryCreation = true
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_new_grocery),
                        tint = SuperCartColors.white,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Finish Shopping Confirmation Dialog (buying mode)
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

        // Reorder Categories Dialog (edit mode)
        if (showReorderCategories && store != null) {
            StoreCategoryOrderDialog(
                store = store,
                onDismiss = { showReorderCategories = false },
                onOrderUpdated = { showReorderCategories = false }
            )
        }

        // Grocery creation / edit dialog
        if (showGroceryCreation) {
            GroceryCreationDialog(
                groceryToEdit = if (isGroceryEditMode) groceryToEdit else null,
                initialGroceryName = "",
                initialStoreId = if (!isGroceryEditMode) storeId else null,
                addToShoppingList = storeMode == StoreMode.Buying && !isGroceryEditMode,
                onDismiss = {
                    showGroceryCreation = false
                    isGroceryEditMode = false
                    groceryToEdit = null
                },
                onGroceryCreated = { newGrocery ->
                    if (isGroceryEditMode && groceryToEdit != null) {
                        val original = groceryToEdit!!
                        if (original.categoryId != newGrocery.categoryId || original.subCategoryId != newGrocery.subCategoryId) {
                            DataManagerObject.updateGroceryLocation(original.uuid, newGrocery.categoryId, newGrocery.subCategoryId)
                        }
                        DataManagerObject.updateGrocery(original.uuid) { newGrocery }
                        scope.launch { DataStoreManager.saveDataGlobally() }
                    } else {
                        DataManagerObject.addGrocery(newGrocery)
                        scope.launch { DataStoreManager.saveDataGlobally() }
                    }
                    showGroceryCreation = false
                    isGroceryEditMode = false
                    groceryToEdit = null
                }
            )
        }
    }
}

@Composable
private fun StoreInfoCard(
    storeName: String,
    storeAddress: String,
    onStoreNameChange: (String) -> Unit,
    onStoreAddressChange: (String) -> Unit,
    onReorderCategories: () -> Unit,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SuperCartSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.store_info),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SuperCartColors.primaryGreen
            )

            OutlinedTextField(
                value = storeName,
                onValueChange = onStoreNameChange,
                label = { Text(stringResource(R.string.store_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SuperCartColors.primaryGreen,
                    unfocusedBorderColor = SuperCartColors.gray,
                    focusedLabelColor = SuperCartColors.primaryGreen,
                    unfocusedLabelColor = SuperCartColors.gray,
                    focusedTextColor = SuperCartColors.black,
                    unfocusedTextColor = SuperCartColors.black,
                    focusedContainerColor = SuperCartColors.white,
                    unfocusedContainerColor = SuperCartColors.white
                )
            )

            OutlinedTextField(
                value = storeAddress,
                onValueChange = onStoreAddressChange,
                label = { Text(stringResource(R.string.store_address)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SuperCartColors.primaryGreen,
                    unfocusedBorderColor = SuperCartColors.gray,
                    focusedLabelColor = SuperCartColors.primaryGreen,
                    unfocusedLabelColor = SuperCartColors.gray,
                    focusedTextColor = SuperCartColors.black,
                    unfocusedTextColor = SuperCartColors.black,
                    focusedContainerColor = SuperCartColors.white,
                    unfocusedContainerColor = SuperCartColors.white
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
            ) {
                Button(
                    onClick = onReorderCategories,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen.copy(alpha = 0.1f),
                        contentColor = SuperCartColors.primaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.reorder_categories), fontSize = 13.sp)
                }

                Button(
                    onClick = onConfirm,
                    enabled = storeName.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.confirm), fontSize = 13.sp)
                }
            }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = grocery.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = categoryName, fontSize = 12.sp, color = SuperCartColors.gray)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
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
