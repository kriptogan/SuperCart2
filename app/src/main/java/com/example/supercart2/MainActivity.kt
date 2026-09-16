package com.example.supercart2

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import com.example.supercart2.ui.components.BottomNavigationBar
import com.example.supercart2.ui.screens.HomeScreen
import com.example.supercart2.ui.screens.StoresScreen
import com.example.supercart2.ui.theme.SuperCart2Theme
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.SettingsManager
import com.example.supercart2.utils.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Force app language (ignore device OS language)
        val language = runBlocking {
            LanguageManager.getCurrentLanguage(newBase)
        }
        val context = LanguageManager.applyLanguage(newBase, language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LocalizedMainApp() }
    }
}

/** MainApp with the layout direction forced by the saved app language. */
@Composable
private fun LocalizedMainApp() {
    val context = LocalContext.current
    val currentLanguage by remember { mutableStateOf(runBlocking { LanguageManager.getCurrentLanguage(context) }) }
    CompositionLocalProvider(LocalLayoutDirection provides currentLanguage.toLayoutDirection()) {
        SuperCart2Theme { MainApp() }
    }
}

/** Navigation state of MainApp: the current route ("home" | "stores") and a store to open in edit mode. */
private class MainAppState {
    var currentRoute by mutableStateOf("home")
    var pendingEditStoreId by mutableStateOf<String?>(null)

    fun navigateToStoreEdit(storeId: String) {
        currentRoute = "stores"
        pendingEditStoreId = storeId
    }

    fun consumePendingEditStore() {
        pendingEditStoreId = null
    }
}

@Composable
fun MainApp() {
    val state = remember { MainAppState() }
    InitializeDataOnStart()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNavigationBar(state.currentRoute, onRouteChange = { state.currentRoute = it }) }
    ) { innerPadding -> MainAppContent(state, innerPadding) }
}

// Sets the global contexts for DataStoreManager and SettingsManager, then loads data
@Composable
private fun InitializeDataOnStart() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        initializeData(context, scope)
        onDispose { }
    }
}

private fun initializeData(context: Context, scope: CoroutineScope) {
    DataStoreManager.setGlobalContext(context)
    SettingsManager.setGlobalContext(context)
    scope.launch { DataStoreManager.loadData(context) }
}

@Composable
private fun MainAppContent(state: MainAppState, innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SuperCartColors.lightGreen)
            .padding(innerPadding)
    ) { CurrentRouteScreen(state) }
}

@Composable
private fun CurrentRouteScreen(state: MainAppState) {
    when (state.currentRoute) {
        "home" -> HomeScreen(onNavigateToStoreEdit = state::navigateToStoreEdit)
        "stores" -> StoresScreen(state.pendingEditStoreId, onEditModeStoreConsumed = state::consumePendingEditStore)
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    SuperCart2Theme {
        MainApp()
    }
}
