package com.example.supercart2.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supercart2.data.GroceryRepository
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.CategoryWithSubCategories

@Composable
fun HomeScreen() {
    // Observe the categories list and force recomposition when it changes
    val categories = remember { GroceryRepository.categories }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Home Screen")
            
            // Buttons in a row with spacing
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Button(
                    onClick = { 
                        GroceryRepository.createMockData()
                    }
                ) {
                    Text("Create Mock Data")
                }
                
                Button(
                    onClick = { 
                        GroceryRepository.clearAllData()
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Clear Data")
                }
            }
            
            // Get the first grocery item from the repository
            categories.firstOrNull()?.let { category ->
                category.subCategories.firstOrNull()?.let { subCategory ->
                    subCategory.groceries.firstOrNull()?.let { grocery ->
                        android.util.Log.d("datastore test", "Displaying grocery: ${grocery.name}")
                        GroceryCard(
                            grocery = grocery,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            } ?: run {
                android.util.Log.d("datastore test", "No grocery found to display")
            }
        }
    }
}

@Composable
fun GroceryCard(
    grocery: Grocery,
    modifier: Modifier = Modifier
) {
    // Observe repository version to trigger recomposition
    val version = GroceryRepository.version
    
    // Get current grocery state
    val currentGrocery = remember(grocery.uuid, version) { 
        android.util.Log.d("datastore test", "Recomputing grocery state, version: $version")
        GroceryRepository.categories
            .asSequence()
            .flatMap { it.subCategories }
            .flatMap { it.groceries }
            .find { it.uuid == grocery.uuid } ?: grocery
    }

    Card(
        modifier = modifier
            .width(300.dp)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentGrocery.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        GroceryRepository.toggleGroceryInShoppingList(
                            groceryId = currentGrocery.uuid,
                            inShoppingList = !currentGrocery.inShoppingList
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = if (currentGrocery.inShoppingList) "Remove from shopping list" else "Add to shopping list",
                        tint = if (currentGrocery.inShoppingList) Color(0xFF4CAF50) else Color.Black
                    )
                }
            }
            // Shopping list status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "In Shopping List: ${currentGrocery.inShoppingList}",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Text("UUID: ${currentGrocery.uuid}")
            Text("SubCategory ID: ${currentGrocery.subCategoryId}")
            Text("Is Bought: ${currentGrocery.isBought}")
            if (currentGrocery.imageId != null) {
                Text("Image ID: ${currentGrocery.imageId}")
            }
        }
    }
}
