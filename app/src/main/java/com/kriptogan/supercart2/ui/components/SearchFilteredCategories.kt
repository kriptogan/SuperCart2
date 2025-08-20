package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kriptogan.supercart2.classes.Category
import com.kriptogan.supercart2.classes.SubCategory
import com.kriptogan.supercart2.classes.Grocery

@Composable
fun SearchFilteredCategories(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    groceries: List<Grocery>,
    expandedCategories: Set<String>,
    expandedSubCategories: Set<String>,
    isAllExpanded: Boolean,
    currentSearchQuery: String,
    onEditGrocery: (Grocery) -> Unit,
    modifier: Modifier = Modifier
) {
    val relevantCategories = if (expandedCategories.isNotEmpty()) {
        categories.filter { it.uuid in expandedCategories }
    } else {
        categories
    }
    
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(relevantCategories) { category ->
            val categorySubCategories = subCategories.filter { it.categoryId == category.uuid }
            val relevantSubCategories = if (expandedSubCategories.isNotEmpty()) {
                categorySubCategories.filter { it.uuid in expandedSubCategories }
            } else {
                categorySubCategories
            }
            
            CollapsibleCategorySection(
                category = category,
                subCategories = relevantSubCategories,
                groceries = groceries,
                onEditGrocery = onEditGrocery,
                isExpandedGlobal = isAllExpanded,
                isExpandedIndividually = expandedCategories.contains(category.uuid),
                expandedSubCategories = expandedSubCategories,
                searchQuery = currentSearchQuery
            )
        }
    }
}
