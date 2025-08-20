package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.models.Category

@Composable
fun CategoriesManagementDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(), // This makes it full-screen
        title = { 
            Text(
                text = "Categories Management",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Categories List
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(DataManagerObject.categories) { categoryWithSubs ->
                        CategoryCard(
                            category = categoryWithSubs.category,
                            subCategoriesCount = categoryWithSubs.subCategories.size,
                            groceriesCount = categoryWithSubs.getGroceriesCount()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SuperCartSpacing.sm)
            ) {
                // Cancel Button (left) - secondary styled
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.white,
                        contentColor = SuperCartColors.primaryGreen
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel"
                    )
                }
                
                // Accept Button (right) - primary styled
                Button(
                    onClick = { /* TODO: Create new category */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuperCartColors.primaryGreen,
                        contentColor = SuperCartColors.white
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create New Category"
                    )
                }
            }
        }
    )
}

@Composable
private fun CategoryCard(
    category: Category,
    subCategoriesCount: Int,
    groceriesCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SuperCartSpacing.sm),
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.white
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = SuperCartShapes.medium
    ) {
        Column(
            modifier = Modifier.padding(SuperCartSpacing.md)
        ) {
            Text(
                text = category.name,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = SuperCartColors.black
            )
            
            Spacer(modifier = Modifier.height(SuperCartSpacing.sm))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sub-Categories: $subCategoriesCount",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = SuperCartColors.darkGray
                )
                
                Text(
                    text = "Groceries: $groceriesCount",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = SuperCartColors.darkGray
                )
            }
        }
    }
}
