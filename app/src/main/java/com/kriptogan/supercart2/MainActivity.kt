package com.kriptogan.supercart2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.ui.components.BottomNavigationBar
import com.kriptogan.supercart2.ui.screens.MainContent
import com.kriptogan.supercart2.ui.theme.SuperCart2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Firebase
        val firebaseManager = FirebaseManager()
        firebaseManager.initialize()
        
        setContent {
            SuperCart2Theme {
                var currentRoute by remember { mutableStateOf("home") }
                var showCategoryDialog by remember { mutableStateOf(false) }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigationBar(
                            currentRoute = currentRoute,
                            onRouteSelected = { route -> currentRoute = route }
                        )
                    }
                ) { innerPadding ->
                    MainContent(
                        firebaseManager = firebaseManager,
                        modifier = Modifier.padding(innerPadding),
                        currentRoute = currentRoute,
                        showCategoryDialog = showCategoryDialog,
                        onShowCategoryDialog = { showCategoryDialog = true },
                        onHideCategoryDialog = { showCategoryDialog = false },
                        context = this@MainActivity
                    )
                }
            }
        }
    }
}