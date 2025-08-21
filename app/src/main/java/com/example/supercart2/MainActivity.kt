package com.example.supercart2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.supercart2.ui.components.BottomNavigationBar
import com.example.supercart2.ui.screens.HomeScreen
import com.example.supercart2.ui.screens.ShoppingListScreen
import com.example.supercart2.ui.theme.SuperCart2Theme
import com.example.supercart2.data.DataStoreManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuperCart2Theme {
                MainApp()
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
        // Set global context for DataStoreManager
        DataStoreManager.setGlobalContext(context)
        
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
        when (currentRoute) {
            "home" -> HomeScreen()
            "shopping_list" -> ShoppingListScreen()
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