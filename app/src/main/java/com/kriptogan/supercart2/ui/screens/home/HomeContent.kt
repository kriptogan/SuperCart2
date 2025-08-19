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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.Grocery
import com.kriptogan.supercart2.classes.FirebaseManager
import com.kriptogan.supercart2.classes.LocalStorageManager
import com.kriptogan.supercart2.ui.components.CategoriesList

@Composable
fun HomeContent(
    firebaseManager: FirebaseManager,
    modifier: Modifier,
    categories: List<Category>, // ← Keep the same simple approach - categories passed directly
    groceries: List<Grocery>, // ← Keep the same simple approach - groceries passed directly
    localStorageManager: LocalStorageManager,
    onShowCategoryDialog: () -> Unit,
    onDeleteAllCategories: () -> Unit, // ← Simple callback, no complex state management
    onEditCategory: ((Category) -> Unit)? = null, // ← New callback for editing
    onShowGroceryDialog: () -> Unit,
    onDeleteAllGroceries: () -> Unit,
    onEditGrocery: ((Grocery) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header - Simple and clean
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
                        onClick = onDeleteAllCategories, // ← Simple callback, no complex state management
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEBEE),
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text("Delete All Categories")
                    }
                    
                    Button(
                        onClick = onShowGroceryDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8F5E8),
                            contentColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Text("Create Grocery")
                    }
                    
                    Button(
                        onClick = onDeleteAllGroceries,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFF3E0),
                            contentColor = Color(0xFFF57C00)
                        )
                    ) {
                        Text("Delete All Groceries")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Data Count Display - Shows real-time updates from both categories and groceries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Categories Count
            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Categories",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = "${categories.size} saved",
                        fontSize = 12.sp,
                        color = Color(0xFF424242)
                    )
                }
            }
            
            // Groceries Count
            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E8)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Groceries",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "${groceries.size} saved",
                        fontSize = 12.sp,
                        color = Color(0xFF424242)
                    )
                }
            }
        }
        
        // Categories removed for now - will be called where needed later
    }
}
