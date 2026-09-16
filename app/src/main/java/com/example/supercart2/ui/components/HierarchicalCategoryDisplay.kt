package com.example.supercart2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing
import com.example.supercart2.utils.localizedCategoryDisplayName
import com.example.supercart2.utils.localizedSubCategoryDisplayName

@Composable
fun HierarchicalCategoryDisplay(
    categories: List<CategoryWithSubCategories>,
    searchQuery: String,
    isAllExpanded: Boolean,
    onEditGrocery: (Grocery) -> Unit,
    modifier: Modifier = Modifier,
    useScroll: Boolean = true,
    isShoppingList: Boolean = false,
    showAlerts: Boolean = true,
    onNavigateToStore: (storeId: String) -> Unit = {}
) {
    val options = HierarchicalCategoryDisplayOptions(onEditGrocery, isShoppingList, showAlerts, onNavigateToStore)
    if (useScroll) CategoryLazyColumn(categories, isAllExpanded, options, modifier)
    else CategoryColumn(categories, isAllExpanded, options, modifier)
}

/** Settings shared by every row of the tree (searchQuery is accepted by the public API but unused). */
internal data class HierarchicalCategoryDisplayOptions(
    val onEditGrocery: (Grocery) -> Unit,
    val isShoppingList: Boolean,
    val showAlerts: Boolean,
    val onNavigateToStore: (storeId: String) -> Unit,
)

@Composable
private fun CategoryLazyColumn(
    categories: List<CategoryWithSubCategories>, isAllExpanded: Boolean,
    options: HierarchicalCategoryDisplayOptions, modifier: Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { Spacer(modifier = Modifier.height(SuperCartSpacing.lg)) }
        items(categories) { CategorySectionWithGap(it, isAllExpanded, options) }
    }
}

@Composable
private fun CategoryColumn(
    categories: List<CategoryWithSubCategories>, isAllExpanded: Boolean,
    options: HierarchicalCategoryDisplayOptions, modifier: Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(SuperCartSpacing.lg))
        categories.forEach { CategorySectionWithGap(it, isAllExpanded, options) }
    }
}

@Composable
private fun CategorySectionWithGap(
    categoryWithSubs: CategoryWithSubCategories, isAllExpanded: Boolean, options: HierarchicalCategoryDisplayOptions,
) {
    CategorySection(categoryWithSubs, isAllExpanded, options)
    Spacer(modifier = Modifier.height(8.dp))
}

/** Expansion state of a category card and of the sub-categories it shows. */
private class HierarchicalCategoryDisplayExpansion(initial: Boolean) {
    var isExpanded by mutableStateOf(initial)
    var subCategoriesExpanded by mutableStateOf(initial)

    fun setAll(expanded: Boolean) {
        isExpanded = expanded
        subCategoriesExpanded = expanded
    }

    /** A category with a single sub-category opens it together with the category. */
    fun expandSingleSubCategory(subCategoryCount: Int) {
        if (isExpanded && subCategoryCount == 1) subCategoriesExpanded = true
    }
}

@Composable
private fun rememberExpansion(isAllExpanded: Boolean, subCategoryCount: Int): HierarchicalCategoryDisplayExpansion {
    val expansion = remember { HierarchicalCategoryDisplayExpansion(isAllExpanded) }
    LaunchedEffect(isAllExpanded) { expansion.setAll(isAllExpanded) }
    LaunchedEffect(expansion.isExpanded, subCategoryCount) { expansion.expandSingleSubCategory(subCategoryCount) }
    return expansion
}

@Composable
private fun CategorySection(
    categoryWithSubs: CategoryWithSubCategories, isAllExpanded: Boolean, options: HierarchicalCategoryDisplayOptions,
) {
    val expansion = rememberExpansion(isAllExpanded, categoryWithSubs.subCategories.size)
    CategoryCard {
        CategoryHeader(categoryWithSubs, expansion.isExpanded) { expansion.isExpanded = !expansion.isExpanded }
        if (expansion.isExpanded) SubCategoryList(categoryWithSubs, expansion.subCategoriesExpanded, options)
    }
}

@Composable
private fun CategoryCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) { Column(content = content) }
}

@Composable
private fun CategoryHeader(categoryWithSubs: CategoryWithSubCategories, isExpanded: Boolean, onToggle: () -> Unit) {
    HierarchicalCategoryDisplayHeaderRow(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp)) {
        CategoryTitle(categoryWithSubs)
        HierarchicalCategoryDisplayExpandIcon(isExpanded)
    }
}

@Composable
private fun CategoryTitle(categoryWithSubs: CategoryWithSubCategories) {
    Column {
        val name = localizedCategoryDisplayName(categoryWithSubs.category.name)
        Text(text = name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        val count = "${categoryWithSubs.getGroceriesCount()} ${stringResource(R.string.items)}"
        Text(text = count, fontSize = 12.sp, color = SuperCartColors.gray)
    }
}

/** Full-width header: content spread to both ends, vertically centered. */
@Composable
internal fun HierarchicalCategoryDisplayHeaderRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically, content = content,
    )
}

@Composable
internal fun HierarchicalCategoryDisplayExpandIcon(isExpanded: Boolean) {
    Icon(
        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
        contentDescription = if (isExpanded) "Collapse" else "Expand",
        tint = SuperCartColors.darkGray, modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun SubCategoryList(
    categoryWithSubs: CategoryWithSubCategories, isAllExpanded: Boolean, options: HierarchicalCategoryDisplayOptions,
) {
    categoryWithSubs.subCategories.forEach { SubCategorySection(it, isAllExpanded, options) }
}

@Composable
private fun SubCategorySection(
    subCategory: SubCategoryWithGroceries, isAllExpanded: Boolean, options: HierarchicalCategoryDisplayOptions,
) {
    var isExpanded by remember { mutableStateOf(isAllExpanded) }
    LaunchedEffect(isAllExpanded) { isExpanded = isAllExpanded }
    Column {
        SubCategoryHeader(subCategory, isExpanded) { isExpanded = !isExpanded }
        if (isExpanded) SubCategoryBody(subCategory, options)
    }
}

@Composable
private fun SubCategoryHeader(subCategory: SubCategoryWithGroceries, isExpanded: Boolean, onToggle: () -> Unit) {
    HierarchicalCategoryDisplayHeaderRow(subCategoryHeaderModifier(onToggle)) {
        SubCategoryTitle(subCategory)
        HierarchicalCategoryDisplayExpandIcon(isExpanded)
    }
}

private fun subCategoryHeaderModifier(onToggle: () -> Unit): Modifier = Modifier
    .fillMaxWidth()
    .background(SuperCartColors.lightGray.copy(alpha = 0.2f))
    .clickable(onClick = onToggle)
    .padding(horizontal = 12.dp, vertical = 8.dp)

@Composable
private fun SubCategoryTitle(subCategory: SubCategoryWithGroceries) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val name = localizedSubCategoryDisplayName(subCategory.subCategory.name)
        Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "(${subCategory.groceries.size})", fontSize = 12.sp, color = SuperCartColors.gray)
    }
}

@Composable
private fun SubCategoryBody(subCategory: SubCategoryWithGroceries, options: HierarchicalCategoryDisplayOptions) {
    Divider(color = SuperCartColors.lightGray)
    if (subCategory.groceries.isNotEmpty()) SubCategoryGroceries(subCategory.groceries, options)
    else NoGroceriesPlaceholder()
}

@Composable
private fun SubCategoryGroceries(groceries: List<Grocery>, options: HierarchicalCategoryDisplayOptions) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        groceries.forEach { grocery ->
            key(grocery.uuid) { GroceryWithGap(grocery, options) }
        }
    }
}

@Composable
private fun GroceryWithGap(grocery: Grocery, options: HierarchicalCategoryDisplayOptions) {
    HierarchicalCategoryDisplayGroceryItem(grocery, options = options)
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun NoGroceriesPlaceholder() {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, null, tint = SuperCartColors.gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        val text = stringResource(R.string.no_groceries_yet)
        Text(text = text, fontSize = 12.sp, color = SuperCartColors.gray, fontStyle = FontStyle.Italic)
    }
}
