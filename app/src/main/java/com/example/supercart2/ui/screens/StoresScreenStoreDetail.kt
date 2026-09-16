package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import java.time.LocalDate

/** A store's groceries in buying mode (to buy / bought) or edit mode (store info + every linked grocery). */
@Composable
internal fun StoresDetailView(storeId: String, storeMode: StoreMode, onExit: () -> Unit, onEnterEditMode: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = remember { StoresScreenDetailState(scope) }
    val model = rememberStoresScreenDetailModel(storeId, storeMode, state.searchQuery)
    LaunchedEffect(model.store) { state.loadStore(model.store) }
    StoresScreenDetailLayout(model, state, onExit, onEnterEditMode)
}

@Composable
private fun rememberStoresScreenDetailModel(storeId: String, mode: StoreMode, query: String): StoresScreenDetailModel {
    val version = DataManagerObject.version
    val store = remember(version) { DataManagerObject.getSortedStores().find { it.uuid == storeId } }
    val toBuy = remember(version, query, mode) { storesScreenStoreToBuy(storeId, mode, query) }
    val bought = remember(version, query, mode) { storesScreenStoreBought(storeId, mode, query) }
    return StoresScreenDetailModel(storeId, mode, store, toBuy, bought)
}

private fun storesScreenStoreToBuy(storeId: String, mode: StoreMode, query: String): List<CategoryWithSubCategories> =
    storesScreenToBuyCategories(DataManagerObject.getSortedCategoriesForStore(storeId), storeId, mode, query)

private fun storesScreenStoreBought(storeId: String, mode: StoreMode, query: String): List<CategoryWithSubCategories> =
    if (mode != StoreMode.Buying) emptyList()
    else storesScreenBoughtCategories(DataManagerObject.getSortedCategoriesForStore(storeId), storeId, query)

@Composable
private fun StoresScreenDetailLayout(
    model: StoresScreenDetailModel, state: StoresScreenDetailState, onExit: () -> Unit, onEnterEditMode: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(SuperCartColors.lightGreen)) {
        StoresScreenDetailHeader(model, state, onExit, onEnterEditMode)
        StoresScreenDetailContent(model, state)
        StoresScreenDetailOverlays(state)
        StoresScreenDetailDialogs(model, state, onExit)
    }
}

@Composable
private fun StoresScreenDetailHeader(
    model: StoresScreenDetailModel, state: StoresScreenDetailState, onExit: () -> Unit, onEnterEditMode: () -> Unit,
) {
    StoresScreenHeaderColumn {
        StoresScreenDetailTitleRow(model, onExit, onEnterEditMode)
        if (model.mode == StoreMode.Buying) StoresScreenBuyingHeader(model, state)
    }
}

@Composable
private fun StoresScreenDetailTitleRow(
    model: StoresScreenDetailModel, onExit: () -> Unit, onEnterEditMode: () -> Unit,
) {
    val title = if (model.mode == StoreMode.Edit) stringResource(R.string.edit_mode) else model.store?.name ?: ""
    StoresScreenTitleRow(bottomPadding = SuperCartSpacing.sm) {
        StoresScreenBackButton(onExit)
        StoresScreenTitle(title, Modifier.weight(1f).padding(start = 8.dp))
        if (model.mode == StoreMode.Buying) StoresScreenEditStoreButton(onEnterEditMode)
    }
}

@Composable
private fun StoresScreenEditStoreButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Edit, stringResource(R.string.edit_store), tint = SuperCartColors.primaryGreen)
    }
}

/** Date, search row and (when something is bought) the finish button, emitted into the header column. */
@Composable
private fun StoresScreenBuyingHeader(model: StoresScreenDetailModel, state: StoresScreenDetailState) {
    StoresScreenDateRow()
    StoresScreenSearchRow(state, buyingLayout = true)
    if (model.boughtCount > 0) StoresScreenFinishButton(model.boughtCount) { state.showFinishConfirmation = true }
}

@Composable
private fun StoresScreenDateRow() {
    val rowModifier = Modifier.fillMaxWidth().padding(bottom = SuperCartSpacing.sm)
    Row(modifier = rowModifier, horizontalArrangement = Arrangement.Center) {
        val style = MaterialTheme.typography.titleMedium
        Text(LocalDate.now().toString(), Modifier.padding(horizontal = 16.dp), color = Color.Black, style = style)
    }
}

@Composable
private fun StoresScreenFinishButton(boughtCount: Int, onClick: () -> Unit) {
    val colors = ButtonDefaults.buttonColors(containerColor = SuperCartColors.primaryGreen)
    val modifier = Modifier.fillMaxWidth().padding(horizontal = SuperCartSpacing.md)
    Button(onClick = onClick, modifier = modifier, colors = colors, shape = RoundedCornerShape(12.dp)) {
        StoresScreenFinishButtonContent(boughtCount)
    }
}

/** Icon, spacer and label, emitted into the button's row. */
@Composable
private fun StoresScreenFinishButtonContent(boughtCount: Int) {
    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = stringResource(R.string.finish_shopping_store, boughtCount))
}

/** "Done" snackbar above the floating add-grocery button. */
@Composable
private fun BoxScope.StoresScreenDetailOverlays(state: StoresScreenDetailState) {
    SnackbarHost(state.snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    val fabArea = Modifier.fillMaxSize().padding(end = 16.dp, bottom = 16.dp)
    Box(modifier = fabArea, contentAlignment = Alignment.BottomEnd) {
        StoresScreenAddGroceryButton(onClick = { state.openGroceryCreation() })
    }
}

@Composable
private fun StoresScreenAddGroceryButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(56.dp), shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.primaryGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) { StoresScreenAddGroceryIconButton(onClick) }
}

@Composable
private fun StoresScreenAddGroceryIconButton(onClick: () -> Unit) {
    val description = stringResource(R.string.add_new_grocery)
    IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
        Icon(Icons.Default.Add, description, tint = SuperCartColors.white, modifier = Modifier.size(28.dp))
    }
}
