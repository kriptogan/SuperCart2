package com.kriptogan.supercart2

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.kriptogan.supercart2.ui.components.ReusableAlertDialog
import com.kriptogan.supercart2.ui.components.ReusableFullScreenWindow
import com.kriptogan.supercart2.ui.components.BottomNavigationBar
import com.kriptogan.supercart2.ui.components.CategoryCreationForm
import com.kriptogan.supercart2.classes.LocalStorageManager
import com.kriptogan.supercart2.ui.theme.SuperCart2Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    
    // Route-based content
    when (currentRoute) {
        "home" -> HomeContent(
            firebaseManager = firebaseManager,
            modifier = modifier,
            connectionStatus = connectionStatus,
            isConnected = isConnected,
            showAlertDialog = showAlertDialog,
            showFullScreenWindow = showFullScreenWindow,
            onShowAlertDialog = { showAlertDialog = true },
            onShowFullScreenWindow = { showFullScreenWindow = true },
            onTestConnection = {
                coroutineScope.launch {
                    val connected = firebaseManager.isConnected()
                    isConnected = connected
                    connectionStatus = if (connected) "Connected" else "Disconnected"
                }
            },
            showCategoryDialog = showCategoryDialog,
            onShowCategoryDialog = onShowCategoryDialog
        )
        "shopping_list" -> ShoppingListContent(
            modifier = modifier
        )
        else -> HomeContent(
            firebaseManager = firebaseManager,
            modifier = modifier,
            connectionStatus = connectionStatus,
            isConnected = isConnected,
            showAlertDialog = showAlertDialog,
            showFullScreenWindow = showFullScreenWindow,
            onShowAlertDialog = { showAlertDialog = true },
            onShowFullScreenWindow = { showFullScreenWindow = true },
            onTestConnection = {
                coroutineScope.launch {
                    val connected = firebaseManager.isConnected()
                    isConnected = connected
                    connectionStatus = if (connected) "Connected" else "Disconnected"
                }
            },
            showCategoryDialog = showCategoryDialog,
            onShowCategoryDialog = onShowCategoryDialog
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
                        // Save to local storage first
                        val localStorageManager = LocalStorageManager(context)
                        localStorageManager.addCategory(category)
                        
                        // Then save to Firestore
                        coroutineScope.launch {
                            val success = firebaseManager.saveCategory(category)
                            if (success) {
                                // Show success message or handle success
                            }
                        }
                        
                        onHideCategoryDialog()
                    },
                    onCancel = onHideCategoryDialog
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
fun HomeContent(
    firebaseManager: FirebaseManager,
    modifier: Modifier,
    connectionStatus: String,
    isConnected: Boolean,
    showAlertDialog: Boolean,
    showFullScreenWindow: Boolean,
    onShowAlertDialog: () -> Unit,
    onShowFullScreenWindow: () -> Unit,
    onTestConnection: () -> Unit,
    showCategoryDialog: Boolean = false,
    onShowCategoryDialog: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "SuperCart2",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Grocery Management App",
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Test Components Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Test Components",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onShowAlertDialog
                    ) {
                        Text("Test AlertDialog")
                    }
                    
                    Button(
                        onClick = onShowFullScreenWindow
                    ) {
                        Text("Test Full Screen")
                    }
                    
                    Button(
                        onClick = onShowCategoryDialog
                    ) {
                        Text("Create Category")
                    }
                }
            }
        }
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