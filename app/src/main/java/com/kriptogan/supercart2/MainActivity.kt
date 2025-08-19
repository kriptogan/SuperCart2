package com.kriptogan.supercart2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.ui.components.ReusableAlertDialog
import com.kriptogan.supercart2.ui.components.ReusableFullScreenWindow
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(
                        firebaseManager = firebaseManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(firebaseManager: FirebaseManager, modifier: Modifier = Modifier) {
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
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Firebase Connection Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isConnected) Color.Green else Color.Red,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    
                    Text(
                        text = connectionStatus,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isConnected) Color.Green else Color.Red
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isConnected) 
                        "Your app is successfully connected to Firebase!" 
                    else 
                        "Firebase connection failed. Check your configuration.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
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
        
        // Test Connection Button
        Button(
            onClick = {
                coroutineScope.launch {
                    val connected = firebaseManager.isConnected()
                    isConnected = connected
                    connectionStatus = if (connected) "Connected" else "Disconnected"
                }
            }
        ) {
            Text("Test Connection")
        }
        
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
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { showAlertDialog = true }
                    ) {
                        Text("Test AlertDialog")
                    }
                    
                    Button(
                        onClick = { showFullScreenWindow = true }
                    ) {
                        Text("Test Full Screen")
                    }
                }
            }
        }
    }
    
    // AlertDialog Component
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

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    SuperCart2Theme {
        MainContent(FirebaseManager())
    }
}