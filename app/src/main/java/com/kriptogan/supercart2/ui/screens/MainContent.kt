package com.kriptogan.supercart2.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.classes.SubCategory
import com.kriptogan.supercart2.ui.components.ReusableAlertDialog
import com.kriptogan.supercart2.ui.components.ReusableFullScreenWindow
import com.kriptogan.supercart2.ui.components.CategoryCreationForm
import com.kriptogan.supercart2.ui.screens.home.HomeContent
import com.kriptogan.supercart2.ui.screens.shopping.ShoppingListContent
import com.kriptogan.supercart2.ui.state.rememberCategoryState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    
    // Use the new state manager
    val categoryState = rememberCategoryState(context, firebaseManager)
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
            categories = categoryState.categories,
            localStorageManager = categoryState.localStorageManager,
            onShowCategoryDialog = onShowCategoryDialog,
            onDeleteAllCategories = categoryState.onDeleteAllCategories,
            onEditCategory = categoryState.onShowEditDialog
        )
        "shopping_list" -> ShoppingListContent(
            modifier = modifier
        )
        else -> HomeContent(
            firebaseManager = firebaseManager,
            modifier = modifier,
            categories = categoryState.categories,
            localStorageManager = categoryState.localStorageManager,
            onShowCategoryDialog = onShowCategoryDialog,
            onDeleteAllCategories = categoryState.onDeleteAllCategories,
            onEditCategory = categoryState.onShowEditDialog
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
                        // Save the category first
                        categoryState.onSaveCategory(category)
                        
                        // Automatically create a "General" sub-category for this category
                        val generalSubCategory = SubCategory(
                            categoryId = category.uuid,
                            name = "General"
                        )
                        
                        // Save the general sub-category to local storage
                        categoryState.localStorageManager.addSubCategory(generalSubCategory)
                        
                        // Save the general sub-category to Firebase
                        coroutineScope.launch {
                            try {
                                firebaseManager.saveSubCategory(generalSubCategory)
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
    if (currentRoute == "home" && categoryState.showEditCategoryDialog && categoryState.categoryToEdit != null) {
        ReusableFullScreenWindow(
            isVisible = categoryState.showEditCategoryDialog,
            onDismiss = categoryState.onHideEditDialog,
            title = "Edit Category",
            content = {
                CategoryCreationForm(
                    initialCategory = categoryState.categoryToEdit,
                    onSave = { updatedCategory ->
                        categoryState.onUpdateCategory(updatedCategory)
                        categoryState.onHideEditDialog()
                    },
                    onCancel = categoryState.onHideEditDialog
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
