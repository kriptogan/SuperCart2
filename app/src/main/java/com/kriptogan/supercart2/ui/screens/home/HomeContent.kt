package com.kriptogan.supercart2.ui.screens.home

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.SubCategory
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.classes.LocalStorageManager
import com.kriptogan.supercart2.ui.components.CategoriesList
import com.kriptogan.supercart2.ui.components.SimpleSubCategoriesSection
import com.kriptogan.supercart2.ui.components.ReusableFullScreenWindow
import kotlinx.coroutines.launch

@Composable
fun HomeContent(
    firebaseManager: FirebaseManager,
    modifier: Modifier,
    categories: List<Category>,
    localStorageManager: LocalStorageManager,
    onShowCategoryDialog: () -> Unit,
    onDeleteAllCategories: () -> Unit,
    onEditCategory: ((Category) -> Unit)? = null
) {
    var showSubCategoryDialog by remember { mutableStateOf(false) }
    var showEditSubCategoryDialog by remember { mutableStateOf(false) }
    var subCategoryToEdit by remember { mutableStateOf<SubCategory?>(null) }
    var subCategoryName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Test Components Section - Simple buttons, no complex logic
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
                        onClick = onShowCategoryDialog
                    ) {
                        Text("Create Category")
                    }
                    
                    Button(
                        onClick = onDeleteAllCategories,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEBEE),
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text("Delete All Categories")
                    }
                    
                    // Sub-Categories Management Section
                    if (categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Sub-Categories for: ${categories.first().name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1976D2)
                        )
                        
                        SimpleSubCategoriesSection(
                            category = categories.first(),
                            localStorageManager = localStorageManager,
                            onAddSubCategory = { 
                                subCategoryName = ""
                                showSubCategoryDialog = true
                            },
                            onEditSubCategory = { subCategory -> 
                                subCategoryToEdit = subCategory
                                subCategoryName = subCategory.name
                                showEditSubCategoryDialog = true
                            },
                            onDeleteSubCategory = { subCategory ->
                                coroutineScope.launch {
                                    try {
                                        firebaseManager.deleteSubCategory(subCategory.uuid)
                                    } catch (e: Exception) {
                                        // Firebase failure doesn't affect local state
                                    }
                                }
                                localStorageManager.deleteSubCategory(subCategory.uuid)
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Categories List - Receives categories directly as parameter (same as before)
        CategoriesList(
            firebaseManager = firebaseManager,
            localStorageManager = localStorageManager,
            modifier = Modifier.fillMaxWidth(),
            onCategoryClick = { /* Handle category click */ },
            showTitle = true,
            title = "Your Categories",
            onDataChanged = { /* This will be handled by parent */ },
            categories = categories,
            showEditButton = true,
            onEditCategory = onEditCategory
        )
    }
    
    // Add Sub-Category Dialog
    if (showSubCategoryDialog && categories.isNotEmpty()) {
        ReusableFullScreenWindow(
            isVisible = showSubCategoryDialog,
            onDismiss = { showSubCategoryDialog = false },
            title = "Create New Sub-Category",
            content = {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = subCategoryName,
                        onValueChange = { subCategoryName = it },
                        label = { Text("Sub-Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    )
                    {
                        Button(
                            onClick = { showSubCategoryDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                if (subCategoryName.isNotBlank()) {
                                    val subCategory = SubCategory(
                                        categoryId = categories.first().uuid,
                                        name = subCategoryName.trim()
                                    )
                                    localStorageManager.addSubCategory(subCategory)
                                    
                                    coroutineScope.launch {
                                        try {
                                            firebaseManager.saveSubCategory(subCategory)
                                        } catch (e: Exception) {
                                            // Firebase failure doesn't affect local state
                                        }
                                    }
                                    
                                    showSubCategoryDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = subCategoryName.isNotBlank()
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        )
    }
    
    // Edit Sub-Category Dialog
    if (showEditSubCategoryDialog && subCategoryToEdit != null) {
        ReusableFullScreenWindow(
            isVisible = showEditSubCategoryDialog,
            onDismiss = { showEditSubCategoryDialog = false },
            title = "Edit Sub-Category",
            content = {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = subCategoryName,
                        onValueChange = { subCategoryName = it },
                        label = { Text("Sub-Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showEditSubCategoryDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                if (subCategoryName.isNotBlank()) {
                                    val updatedSubCategory = subCategoryToEdit!!.copy(
                                        name = subCategoryName.trim()
                                    )
                                    localStorageManager.updateSubCategory(updatedSubCategory)
                                    
                                    coroutineScope.launch {
                                        try {
                                            firebaseManager.updateSubCategory(updatedSubCategory)
                                        } catch (e: Exception) {
                                            // Firebase failure doesn't affect local state
                                        }
                                    }
                                    
                                    showEditSubCategoryDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = subCategoryName.isNotBlank()
                        ) {
                            Text("Update")
                        }
                    }
                }
            }
        )
    }
}
