package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.ui.theme.SuperCartColors

enum class ShoppingListViewMode {
    CATEGORY,  // Default hierarchical view
    STORE      // Grouped by stores
}

/** Shopping list: to-buy and bought groceries, per category or per store. Reached from StoresScreen. */
@Composable
fun ShoppingListScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToStoreEdit: (String) -> Unit = {}
) {
    val state = ShoppingListScreenRememberState()
    Box(modifier = Modifier.fillMaxSize().background(SuperCartColors.lightGreen)) {
        ShoppingListScreenHeader(state, onBack)
        ShoppingListScreenContent(state, onNavigateToStoreEdit)
        ShoppingListScreenDialogs(state, onNavigateToStoreEdit)
    }
}

/** Remembers the screen state and loads the saved view mode once. */
@Composable
private fun ShoppingListScreenRememberState(): ShoppingListScreenState {
    val context = LocalContext.current
    val state = remember { ShoppingListScreenState() }
    LaunchedEffect(Unit) {
        state.viewMode = ShoppingListScreenState.viewModeFor(DataStoreManager.loadStoreViewMode(context))
    }
    return state
}
