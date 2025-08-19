package com.kriptogan.supercart2

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.ui.components.ReusableAlertDialog
import com.kriptogan.supercart2.ui.components.ReusableFullScreenWindow
import com.kriptogan.supercart2.ui.components.BottomNavigationBar
import com.kriptogan.supercart2.ui.components.CategoryCreationForm
import com.kriptogan.supercart2.ui.components.CategoriesList
import com.kriptogan.supercart2.classes.LocalStorageManager
import com.kriptogan.supercart2.ui.theme.SuperCart2Theme
import com.kriptogan.supercart2.ui.screens.home.HomeContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

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

@Composable
fun MainContent(
    firebaseManager: FirebaseManager, 
    modifier: Modifier = Modifier,
    currentRoute: String = "home",
    showCategoryDialog: Boolean = false,
    onShowCategoryDialog: () -> Unit = {},
    onHideCategoryDialog: () -> Unit = {},
    context: Context
) {
    var connectionStatus by remember { mutableStateOf("Checking...") }
    var isConnected by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var showFullScreenWindow by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    
    // Single source of truth for categories - EXACTLY as it was working before
    val localStorageManager = LocalStorageManager(context)
    var categories by remember { mutableStateOf(localStorageManager.getCategories()) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Check connection status every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            val connected = firebaseManager.isConnected()
            isConnected = connected
            connectionStatus = if (connected) "Connected" else "Disconnected"
            delay(2000)
        }
    }
    
    // Default categories initialization removed
    
    // Route-based content
    when (currentRoute) {
        "home" -> HomeContent(
            firebaseManager = firebaseManager,
            modifier = modifier,
            categories = categories, // ← Pass categories directly (same as before)
            localStorageManager = localStorageManager,
            onShowCategoryDialog = onShowCategoryDialog,
            onDeleteAllCategories = {
                // Delete all categories - EXACTLY as it was working before
                coroutineScope.launch {
                    try {
                        val existingCategories = localStorageManager.getCategories()
                        existingCategories.forEach { category ->
                            firebaseManager.deleteCategory(category.uuid)
                        }
                        localStorageManager.clearAllData()
                        categories = emptyList() // ← Direct state update (same as before)
                    } catch (e: Exception) {
                        // Handle error if needed
                    }
                }
            },
            onEditCategory = { category ->
                // Handle edit category - EXACTLY as it was working before
                categoryToEdit = category
                showEditCategoryDialog = true
            }
        )
        "shopping_list" -> ShoppingListContent(
            modifier = modifier
        )
        else -> HomeContent(
            firebaseManager = firebaseManager,
            modifier = modifier,
            categories = categories, // ← Pass categories directly (same as before)
            localStorageManager = localStorageManager,
            onShowCategoryDialog = onShowCategoryDialog,
            onDeleteAllCategories = {
                // Delete all categories - EXACTLY as it was working before
                coroutineScope.launch {
                    try {
                        val existingCategories = localStorageManager.getCategories()
                        existingCategories.forEach { category ->
                            firebaseManager.deleteCategory(category.uuid)
                        }
                        localStorageManager.clearAllData()
                        categories = emptyList() // ← Direct state update (same as before)
                    } catch (e: Exception) {
                        // Handle error if needed
                    }
                }
            },
            onEditCategory = { category ->
                // Handle edit category - EXACTLY as it was working before
                categoryToEdit = category
                showEditCategoryDialog = true
            }
        )
    }
    
    // Category Creation Dialog
    if (currentRoute == "home" && showCategoryDialog) {
        ReusableFullScreenWindow(
            isVisible = showCategoryDialog,
            onDismiss = onHideCategoryDialog,
            title = "Create New Category",
            content = {
                CategoryCreationForm(
                    onSave = { category ->
                        // Save category - EXACTLY as it was working before
                        localStorageManager.addCategory(category)
                        
                        // Update state directly - EXACTLY as it was working before
                        categories = localStorageManager.getCategories()
                        
                        // Save to Firebase asynchronously (don't wait)
                        coroutineScope.launch {
                            try {
                                firebaseManager.saveCategory(category)
                            } catch (e: Exception) {
                                // Firebase failure doesn't affect local state
                            }
                        }
                        
                        onHideCategoryDialog()
                    },
                    onCancel = onHideCategoryDialog
                )
            }
        )
    }
    
    // Category Edit Dialog
    if (currentRoute == "home" && showEditCategoryDialog && categoryToEdit != null) {
        ReusableFullScreenWindow(
            isVisible = showEditCategoryDialog,
            onDismiss = { 
                showEditCategoryDialog = false
                categoryToEdit = null
            },
            title = "Edit Category",
            content = {
                CategoryCreationForm(
                    initialCategory = categoryToEdit, // ← Pass existing category for editing
                    onSave = { updatedCategory ->
                        // Update category - EXACTLY as it was working before
                        localStorageManager.updateCategory(updatedCategory)
                        
                        // Update state directly - EXACTLY as it was working before
                        categories = localStorageManager.getCategories()
                        
                        // Save to Firebase asynchronously (don't wait)
                        coroutineScope.launch {
                            try {
                                firebaseManager.saveCategory(updatedCategory)
                            } catch (e: Exception) {
                                // Firebase failure doesn't affect local state
                            }
                        }
                        
                        showEditCategoryDialog = false
                        categoryToEdit = null
                    },
                    onCancel = { 
                        showEditCategoryDialog = false
                        categoryToEdit = null
                    }
                )
            }
        )
    }
    
    // AlertDialog Component
    if (currentRoute == "home") {
        ReusableAlertDialog(
            isVisible = showAlertDialog,
            onDismiss = { showAlertDialog = false },
            title = "Test AlertDialog",
            content = "This is a test of the reusable AlertDialog component. It can be customized with different content and actions.",
            confirmText = "OK",
            dismissText = "Cancel",
            onConfirm = { showAlertDialog = false }
        )
        
        // Full Screen Window Component
        ReusableFullScreenWindow(
            isVisible = showFullScreenWindow,
            onDismiss = { showFullScreenWindow = false },
            title = "Test Full Screen Window",
            content = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "This is a test of the reusable Full Screen Window component.",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "You can put any content here and customize it as needed.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { showFullScreenWindow = false }
                    ) {
                        Text("Close Window")
                    }
                }
            }
        )
    }
}



@Composable
fun ShoppingListContent(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Shopping List",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your shopping list will appear here",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Shopping Cart",
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    SuperCart2Theme {
        MainContent(
            firebaseManager = FirebaseManager(),
            context = LocalContext.current
        )
    }
}