package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kriptogan.supercart2.classes.CategoryWithSubCategories
import com.kriptogan.supercart2.classes.Grocery

@Composable
fun SearchFilteredCategories(
    categories: List<CategoryWithSubCategories>,
    expandedCategories: Set<String>,
    expandedSubCategories: Set<String>,
    isAllExpanded: Boolean,
    currentSearchQuery: String,
    onEditGrocery: (Grocery) -> Unit,
    modifier: Modifier = Modifier
) {
    val relevantCategories = if (expandedCategories.isNotEmpty()) {
        categories.filter { it.category.uuid in expandedCategories }
    } else {
        categories
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(relevantCategories) { categoryWithSubs ->
            CollapsibleCategorySection(
                category = categoryWithSubs.category,
                subCategories = categoryWithSubs.subCategories.map { it.subCategory },
                groceries = categoryWithSubs.subCategories.flatMap { it.groceries },
                expandedCategories = expandedCategories,
                expandedSubCategories = expandedSubCategories,
                isAllExpanded = isAllExpanded,
                searchQuery = currentSearchQuery,
                onEditGrocery = onEditGrocery
            )
        }
    }
}
