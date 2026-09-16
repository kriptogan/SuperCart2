package com.example.supercart2.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.supercart2.ui.screens.StoreMode.Buying
import com.example.supercart2.ui.screens.StoreMode.Edit
import com.example.supercart2.ui.screens.StoresSubScreen.Landing
import com.example.supercart2.ui.screens.StoresSubScreen.SelectStore
import com.example.supercart2.ui.screens.StoresSubScreen.ShoppingList
import com.example.supercart2.ui.screens.StoresSubScreen.StoreDetail

enum class StoreMode { Buying, Edit }

/** Sub-screens of the Stores tab: landing → select store → store detail (buying | edit), or the shopping list. */
internal sealed class StoresSubScreen {
    object Landing : StoresSubScreen()
    object ShoppingList : StoresSubScreen()
    object SelectStore : StoresSubScreen()
    data class StoreDetail(
        val storeId: String,
        val mode: StoreMode = Buying,
        val returnToBuying: Boolean = false,
    ) : StoresSubScreen()
}

/** Back from edit mode returns to buying mode only when edit mode was entered from buying mode. */
internal fun StoreDetail.storesScreenExitTarget(): StoresSubScreen =
    if (mode == Edit && returnToBuying) StoreDetail(storeId, Buying) else SelectStore

internal fun StoreDetail.storesScreenEditTarget(): StoreDetail = StoreDetail(storeId, Edit, returnToBuying = true)

/** Builds navigation callbacks that switch the current sub-screen through [go]. */
private class StoresScreenNavigator(val go: (StoresSubScreen) -> Unit) {
    fun to(target: StoresSubScreen): () -> Unit = { go(target) }
    fun toStore(mode: StoreMode): (String) -> Unit = { storeId -> go(StoreDetail(storeId, mode)) }
}

@Composable
fun StoresScreen(
    editModeStoreId: String? = null,
    onEditModeStoreConsumed: () -> Unit = {}
) {
    var subScreen by remember { mutableStateOf<StoresSubScreen>(Landing) }
    val navigator = StoresScreenNavigator { subScreen = it }
    StoresScreenEditModeEntry(editModeStoreId, onEditModeStoreConsumed, navigator)
    StoresScreenSubScreen(subScreen, navigator)
}

/** Opens [editModeStoreId] in edit mode when it is requested from outside (e.g. a store chip on Home). */
@Composable
private fun StoresScreenEditModeEntry(
    editModeStoreId: String?, onConsumed: () -> Unit, navigator: StoresScreenNavigator,
) {
    LaunchedEffect(editModeStoreId) {
        if (editModeStoreId == null) return@LaunchedEffect
        navigator.toStore(Edit)(editModeStoreId)
        onConsumed()
    }
}

@Composable
private fun StoresScreenSubScreen(screen: StoresSubScreen, nav: StoresScreenNavigator) {
    when (screen) {
        is Landing -> StoresLandingView(onViewGroceries = nav.to(ShoppingList), onSelectStore = nav.to(SelectStore))
        is ShoppingList -> ShoppingListScreen(onBack = nav.to(Landing), onNavigateToStoreEdit = nav.toStore(Edit))
        is SelectStore -> StoresSelectStoreView(onStoreSelected = nav.toStore(Buying), onBack = nav.to(Landing))
        is StoreDetail -> StoresScreenStoreDetailRoute(screen, nav)
    }
}

@Composable
private fun StoresScreenStoreDetailRoute(screen: StoreDetail, nav: StoresScreenNavigator) {
    StoresDetailView(
        storeId = screen.storeId, storeMode = screen.mode,
        onExit = nav.to(screen.storesScreenExitTarget()), onEnterEditMode = nav.to(screen.storesScreenEditTarget()),
    )
}
