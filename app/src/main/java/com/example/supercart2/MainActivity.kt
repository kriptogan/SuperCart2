package com.example.supercart2

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
import com.example.supercart2.ui.screens.ShoppingListScreen
import com.example.supercart2.ui.theme.SuperCart2Theme
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.SettingsManager
import com.example.supercart2.utils.LanguageManager
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
        setContent {
            // Get current language for layout direction
            val context = LocalContext.current
            var currentLanguage by remember {
                mutableStateOf(
                    runBlocking { LanguageManager.getCurrentLanguage(context) }
                )
            }
            
            // Force layout direction based on language
            CompositionLocalProvider(
                LocalLayoutDirection provides currentLanguage.toLayoutDirection()
            ) {
                SuperCart2Theme {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    var currentRoute by remember { mutableStateOf("home") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize data on app startup
    DisposableEffect(Unit) {
        // Set global context for DataStoreManager and SettingsManager
        DataStoreManager.setGlobalContext(context)
        SettingsManager.setGlobalContext(context)
        
        scope.launch {
            DataStoreManager.loadData(context)
        }
        onDispose { }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onRouteChange = { route -> currentRoute = route }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.supercart2.ui.theme.SuperCartColors.lightGreen)
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                "home" -> HomeScreen()
                "shopping_list" -> ShoppingListScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    SuperCart2Theme {
        MainApp()
    }
}