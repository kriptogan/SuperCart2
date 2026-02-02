package com.example.supercart2.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import java.time.LocalDate

object DataManagerObject {
    val categories: SnapshotStateList<CategoryWithSubCategories> = mutableStateListOf()
    val stores: SnapshotStateList<Store> = mutableStateListOf()
    val hiddenStoreIds: SnapshotStateList<String> = mutableStateListOf()
    
    // Store-specific category ordering: Map<StoreUUID, List<CategoryUUID>>
    var storeCategoryOrders: MutableMap<String, List<String>> = mutableMapOf()
    
    // Version counter to force recomposition
    private var _version by mutableStateOf(0)
    val version: Int get() = _version
    
    private fun notifyUpdate() {
        _version++
        android.util.Log.d("datastore test", "Data updated, version: $_version")
    }
    
    // Get categories sorted by viewOrder for consistent display order
    fun getSortedCategories(): List<CategoryWithSubCategories> {
        // Filter out deleted categories, subcategories, and groceries
        return categories
            .filter { !it.category.deleted }
            .map { categoryWithSubs ->
                CategoryWithSubCategories(
                    category = categoryWithSubs.category,
                    subCategories = categoryWithSubs.subCategories
                        .filter { !it.subCategory.deleted }
                        .map { subCategoryWithGroceries ->
                            SubCategoryWithGroceries(
                                subCategory = subCategoryWithGroceries.subCategory,
                                groceries = subCategoryWithGroceries.groceries
                                    .filterNot { it.deleted }
                                    .toMutableList()
                            )
                        }
                        .toMutableList()
                )
            }
            .sortedBy { it.category.viewOrder }
    }
    
    // Helper function to update a grocery item
    fun updateGrocery(groceryUuid: String, update: (Grocery) -> Grocery) {
        categories.forEachIndexed { categoryIndex, category ->
            category.subCategories.forEachIndexed { subCategoryIndex, subCategory ->
                val index = subCategory.groceries.indexOfFirst { it.uuid == groceryUuid }
                if (index != -1) {
                    val oldGrocery = subCategory.groceries[index]
                    val updatedGrocery = update(oldGrocery)
                    
                    // Automatically update lastUpdate timestamp
                    val newGrocery = updatedGrocery.copy(
                        lastUpdate = java.time.LocalDateTime.now()
                    )
                    
                    // Create new groceries list with the updated item
                    val updatedGroceries = subCategory.groceries.toMutableList().apply {
                        set(index, newGrocery)
                    }
                    
                    // Create new sub-category with updated groceries list
                    val updatedSubCategory = SubCategoryWithGroceries(
                        subCategory = subCategory.subCategory,
                        groceries = updatedGroceries
                    )
                    
                    // Update sub-categories list
                    val updatedSubCategories = category.subCategories.toMutableList().apply {
                        set(subCategoryIndex, updatedSubCategory)
                    }
                    
                    // Update category with new sub-categories list
                    categories[categoryIndex] = CategoryWithSubCategories(
                        category = category.category,
                        subCategories = updatedSubCategories
                    )
                    
                    updateData()
                    android.util.Log.d("DataManagerObject", "Updated grocery ${newGrocery.name}")
                    return
                }
            }
        }
    }

    // Helper function to toggle shopping list status
    fun toggleShoppingListStatus(groceryUuid: String) {
        updateGrocery(groceryUuid) { grocery ->
            // When toggling shopping list status, also set isBought to false
            grocery.copy(
                inShoppingList = !grocery.inShoppingList,
                isBought = false // Reset bought status whenever shopping list status changes
            )
        }
        android.util.Log.d("DataManagerObject", "Toggled shopping list status and reset bought status")
    }

    // Helper function to toggle bought status (just toggles isBought, doesn't affect buyEvents)
    fun toggleBoughtStatus(groceryUuid: String) {
        updateGrocery(groceryUuid) { grocery ->
            grocery.copy(
                isBought = !grocery.isBought
            )
        }
    }

    // Category Management Helpers
    fun updateCategory(categoryUuid: String, update: (Category) -> Category) {
        val index = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (index != -1) {
            val categoryWithSubs = categories[index]
            val updatedCategory = update(categoryWithSubs.category)
            
            // Automatically update lastUpdate timestamp
            val categoryWithTimestamp = updatedCategory.copy(
                lastUpdate = java.time.LocalDateTime.now()
            )
            
            categories[index] = categoryWithSubs.copy(
                category = categoryWithTimestamp
            )
            updateData()
        }
    }

    fun swapCategoryOrder(category1Uuid: String, category2Uuid: String) {
        val index1 = categories.indexOfFirst { it.category.uuid == category1Uuid }
        val index2 = categories.indexOfFirst { it.category.uuid == category2Uuid }
        
        if (index1 != -1 && index2 != -1) {
            val cat1 = categories[index1]
            val cat2 = categories[index2]
            
            // Swap their view orders
            val tempOrder = cat1.category.viewOrder
            updateCategory(cat1.category.uuid) { it.copy(viewOrder = cat2.category.viewOrder) }
            updateCategory(cat2.category.uuid) { it.copy(viewOrder = tempOrder) }
        }
    }

    fun addCategory(category: Category, subCategories: List<SubCategoryWithGroceries> = emptyList()) {
        categories.add(CategoryWithSubCategories(
            category = category,
            subCategories = subCategories.toMutableList()
        ))
        updateData()
    }

    fun deleteCategory(categoryUuid: String) {
        updateCategory(categoryUuid) { category ->
            // Soft delete - mark as deleted instead of removing
            category.copy(deleted = true)
        }
        
        // Remove category from all store orders
        storeCategoryOrders.forEach { (storeId, categoryIds) ->
            storeCategoryOrders[storeId] = categoryIds.filter { it != categoryUuid }
        }
        
        android.util.Log.d("DataManagerObject", "Soft-deleted category: $categoryUuid")
    }

    // Sub-Category Management Helpers
    fun updateSubCategory(categoryUuid: String, subCategoryUuid: String, update: (SubCategory) -> SubCategory) {
        val categoryIndex = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (categoryIndex != -1) {
            val categoryWithSubs = categories[categoryIndex]
            val subCategoryIndex = categoryWithSubs.subCategories.indexOfFirst { 
                it.subCategory.uuid == subCategoryUuid 
            }
            
            if (subCategoryIndex != -1) {
                val updatedSubCategory = update(categoryWithSubs.subCategories[subCategoryIndex].subCategory)
                
                // Automatically update lastUpdate timestamp
                val subCategoryWithTimestamp = updatedSubCategory.copy(
                    lastUpdate = java.time.LocalDateTime.now()
                )
                
                categoryWithSubs.subCategories[subCategoryIndex] = SubCategoryWithGroceries(
                    subCategory = subCategoryWithTimestamp,
                    groceries = categoryWithSubs.subCategories[subCategoryIndex].groceries
                )
                updateData()
            }
        }
    }

    fun addSubCategory(categoryUuid: String, subCategory: SubCategory) {
        val categoryIndex = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (categoryIndex != -1) {
            val categoryWithSubs = categories[categoryIndex]
            
            // Create new list with the added sub-category
            val updatedSubCategories = categoryWithSubs.subCategories.toMutableList().apply {
                add(SubCategoryWithGroceries(
                    subCategory = subCategory,
                    groceries = mutableListOf()
                ))
            }
            
            // Update the category with the new sub-categories list
            categories[categoryIndex] = CategoryWithSubCategories(
                category = categoryWithSubs.category,
                subCategories = updatedSubCategories
            )
            
            updateData()
            android.util.Log.d("DataManagerObject", "Added sub-category ${subCategory.name} to category $categoryUuid")
        }
    }

    fun deleteSubCategory(categoryUuid: String, subCategoryUuid: String) {
        updateSubCategory(categoryUuid, subCategoryUuid) { subCategory ->
            // Soft delete - mark as deleted instead of removing
            subCategory.copy(deleted = true)
        }
        android.util.Log.d("DataManagerObject", "Soft-deleted sub-category $subCategoryUuid from category $categoryUuid")
    }

    fun moveGroceriesToSubCategory(
        sourceSubCategoryUuid: String,
        targetSubCategoryUuid: String,
        groceryUuids: List<String>
    ) {
        // Find source and target sub-categories
        var sourceGroceries: MutableList<Grocery>? = null
        var targetSubCategory: SubCategoryWithGroceries? = null

        categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                if (subCategory.subCategory.uuid == sourceSubCategoryUuid) {
                    sourceGroceries = subCategory.groceries
                }
                if (subCategory.subCategory.uuid == targetSubCategoryUuid) {
                    targetSubCategory = subCategory
                }
            }
        }

        if (sourceGroceries != null && targetSubCategory != null) {
            // Find groceries to move
            val groceriesToMove = sourceGroceries!!.filter { it.uuid in groceryUuids }
            
            // Update their category and sub-category IDs
            val updatedGroceries = groceriesToMove.map { grocery ->
                grocery.copy(
                    categoryId = targetSubCategory!!.subCategory.categoryId,
                    subCategoryId = targetSubCategory!!.subCategory.uuid,
                    lastUpdate = java.time.LocalDateTime.now()
                )
            }

            // Remove from source and add to target
            sourceGroceries!!.removeAll { it.uuid in groceryUuids }
            targetSubCategory!!.groceries.addAll(updatedGroceries)
            
            updateData()
        }
    }

    // Grocery Management Helpers
    fun addGrocery(grocery: Grocery) {
        categories.forEachIndexed { categoryIndex, category ->
            if (category.category.uuid == grocery.categoryId) {
                category.subCategories.forEachIndexed { subCategoryIndex, subCategory ->
                    if (subCategory.subCategory.uuid == grocery.subCategoryId) {
                        // Create new list with the added grocery
                        val updatedGroceries = subCategory.groceries.toMutableList().apply {
                            add(grocery)
                        }
                        
                        // Create new sub-category with updated groceries list
                        val updatedSubCategory = SubCategoryWithGroceries(
                            subCategory = subCategory.subCategory,
                            groceries = updatedGroceries
                        )
                        
                        // Update sub-categories list
                        val updatedSubCategories = category.subCategories.toMutableList().apply {
                            set(subCategoryIndex, updatedSubCategory)
                        }
                        
                        // Update category with new sub-categories list
                        categories[categoryIndex] = CategoryWithSubCategories(
                            category = category.category,
                            subCategories = updatedSubCategories
                        )
                        
                        updateData()
                        android.util.Log.d("DataManagerObject", "Added grocery ${grocery.name}")
                        return
                    }
                }
            }
        }
    }

    fun updateGroceryLocation(
        groceryUuid: String,
        newCategoryId: String,
        newSubCategoryId: String
    ) {
        // Find source and target locations
        var sourceCategoryIndex = -1
        var sourceSubCategoryIndex = -1
        var targetCategoryIndex = -1
        var targetSubCategoryIndex = -1
        var groceryToMove: Grocery? = null

        // Find the grocery and its current location
        categories.forEachIndexed { catIndex, category ->
            category.subCategories.forEachIndexed { subIndex, subCategory ->
                // Find source location
                val groceryIndex = subCategory.groceries.indexOfFirst { it.uuid == groceryUuid }
                if (groceryIndex != -1) {
                    sourceCategoryIndex = catIndex
                    sourceSubCategoryIndex = subIndex
                    groceryToMove = subCategory.groceries[groceryIndex]
                }
                
                // Find target location
                if (category.category.uuid == newCategoryId && 
                    subCategory.subCategory.uuid == newSubCategoryId) {
                    targetCategoryIndex = catIndex
                    targetSubCategoryIndex = subIndex
                }
            }
        }

        if (groceryToMove != null && 
            sourceCategoryIndex != -1 && 
            sourceSubCategoryIndex != -1 && 
            targetCategoryIndex != -1 && 
            targetSubCategoryIndex != -1) {
            
            // Update source category
            val sourceCategory = categories[sourceCategoryIndex]
            val sourceSubCategory = sourceCategory.subCategories[sourceSubCategoryIndex]
            
            // Create new source groceries list without the moved item
            val updatedSourceGroceries = sourceSubCategory.groceries
                .filterNot { it.uuid == groceryUuid }
                .toMutableList()
            
            // Create new source sub-category
            val updatedSourceSubCategory = SubCategoryWithGroceries(
                subCategory = sourceSubCategory.subCategory,
                groceries = updatedSourceGroceries
            )
            
            // Create new source sub-categories list
            val updatedSourceSubCategories = sourceCategory.subCategories.toMutableList().apply {
                set(sourceSubCategoryIndex, updatedSourceSubCategory)
            }
            
            // Update source category
            categories[sourceCategoryIndex] = CategoryWithSubCategories(
                category = sourceCategory.category,
                subCategories = updatedSourceSubCategories
            )

            // Update target category
            val targetCategory = categories[targetCategoryIndex]
            val targetSubCategory = targetCategory.subCategories[targetSubCategoryIndex]
            
            // Create new target groceries list with the moved item
            val updatedTargetGroceries = targetSubCategory.groceries.toMutableList().apply {
                add(groceryToMove!!.copy(
                    categoryId = newCategoryId,
                    subCategoryId = newSubCategoryId
                ))
            }
            
            // Create new target sub-category
            val updatedTargetSubCategory = SubCategoryWithGroceries(
                subCategory = targetSubCategory.subCategory,
                groceries = updatedTargetGroceries
            )
            
            // Create new target sub-categories list
            val updatedTargetSubCategories = targetCategory.subCategories.toMutableList().apply {
                set(targetSubCategoryIndex, updatedTargetSubCategory)
            }
            
            // Update target category
            categories[targetCategoryIndex] = CategoryWithSubCategories(
                category = targetCategory.category,
                subCategories = updatedTargetSubCategories
            )

            updateData()
            android.util.Log.d("DataManagerObject", "Moved grocery ${groceryToMove.name} to new location")
        }
    }

    fun deleteGrocery(groceryUuid: String) {
        updateGrocery(groceryUuid) { grocery ->
            // Soft delete - mark as deleted instead of removing
            grocery.copy(deleted = true)
        }
        
        // Delete local image if exists
        categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                val grocery = subCategory.groceries.find { it.uuid == groceryUuid }
                grocery?.imageUUID?.let { imageUUID ->
                    val context = DataStoreManager.globalContext
                    if (context != null) {
                        ImageManager.deleteLocalImage(imageUUID, context)
                        android.util.Log.d("DataManagerObject", "Deleted image for grocery: $imageUUID")
                    }
                }
            }
        }
        
        android.util.Log.d("DataManagerObject", "Soft-deleted grocery: $groceryUuid")
    }

    // Calculate average days between buy events
    private fun calculateAverageBuyDays(buyEvents: List<LocalDate>): Int? {
        // Need at least 2 events to calculate average
        if (buyEvents.size < 2) return null
        
        // Take up to last 4 events, sorted newest to oldest
        val events = buyEvents.sortedDescending().take(4)
        
        // Calculate differences between consecutive dates
        val differences = events.zipWithNext { newer, older ->
            newer.toEpochDay() - older.toEpochDay()
        }
        
        // Calculate average and round down
        return differences.average().toInt()
    }

    // Confirms all bought items - adds buyEvent and resets their status
    fun confirmBoughtItems() {
        val currentDate = LocalDate.now()
        
        // Find all bought items and update them
        categories.forEachIndexed { categoryIndex, category ->
            category.subCategories.forEachIndexed { subCategoryIndex, subCategory ->
                // Find any bought items in this sub-category
                val boughtItems = subCategory.groceries.filter { it.isBought }
                if (boughtItems.isNotEmpty()) {
                    // Create new list with updated items
                    val updatedGroceries = subCategory.groceries.map { grocery ->
                        if (grocery.isBought) {
                            // For bought items: add buyEvent (if not already exists for today) and reset status
                            val existingEvents = grocery.buyEvents ?: emptyList()
                            val updatedEvents = if (existingEvents.any { it == currentDate }) {
                                // If today's date already exists, keep the list as is
                                existingEvents
                            } else {
                                // Add today's date only if it doesn't exist
                                existingEvents + currentDate
                            }
                            
                            // Calculate new average buy days
                            val newAverageBuyDays = calculateAverageBuyDays(updatedEvents)
                            android.util.Log.d("DataManagerObject", 
                                "Calculated average buy days for '${grocery.name}': $newAverageBuyDays" +
                                " (from ${updatedEvents.size} events)"
                            )
                            
                            grocery.copy(
                                isBought = false,
                                inShoppingList = false,
                                buyEvents = updatedEvents,
                                imageUUID = grocery.imageUUID,
                                averageBuyDays = newAverageBuyDays,
                                lastUpdate = java.time.LocalDateTime.now()
                            )
                        } else {
                            // For non-bought items: keep as is
                            grocery
                        }
                    }.toMutableList()
                    
                    // Create new sub-category with updated groceries
                    val updatedSubCategory = SubCategoryWithGroceries(
                        subCategory = subCategory.subCategory,
                        groceries = updatedGroceries
                    )
                    
                    // Update sub-categories list
                    val updatedSubCategories = category.subCategories.toMutableList().apply {
                        set(subCategoryIndex, updatedSubCategory)
                    }
                    
                    // Update category with new sub-categories list
                    categories[categoryIndex] = CategoryWithSubCategories(
                        category = category.category,
                        subCategories = updatedSubCategories
                    )
                }
            }
        }
        
        // Log all groceries that were updated with their buyEvents
        categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                subCategory.groceries.forEach { grocery ->
                    if (grocery.buyEvents.isNotEmpty()) {
                        android.util.Log.d("DataManagerObject", 
                            "Grocery '${grocery.name}' buyEvents: ${grocery.buyEvents.joinToString(", ") { 
                                it.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                            }}"
                        )
                    }
                }
            }
        }

        updateData()
        android.util.Log.d("DataManagerObject", "Confirmed bought items and added buyEvents")
    }

    // Store Management Helpers
    fun getSortedStores(): List<Store> {
        return stores.filter { !it.deleted }.sortedBy { it.viewOrder }
    }

    fun addStore(store: Store) {
        stores.add(store)
        updateData()
        android.util.Log.d("DataManagerObject", "Added store ${store.name}")
    }

    fun updateStore(storeUuid: String, update: (Store) -> Store) {
        val index = stores.indexOfFirst { it.uuid == storeUuid }
        if (index != -1) {
            val updatedStore = update(stores[index])
            
            // Automatically update lastUpdate timestamp
            stores[index] = updatedStore.copy(
                lastUpdate = java.time.LocalDateTime.now()
            )
            
            updateData()
            android.util.Log.d("DataManagerObject", "Updated store")
        }
    }

    fun swapStoreOrder(store1Uuid: String, store2Uuid: String) {
        val index1 = stores.indexOfFirst { it.uuid == store1Uuid }
        val index2 = stores.indexOfFirst { it.uuid == store2Uuid }
        
        if (index1 != -1 && index2 != -1) {
            val store1 = stores[index1]
            val store2 = stores[index2]
            
            // Swap their view orders
            val tempOrder = store1.viewOrder
            updateStore(store1.uuid) { it.copy(viewOrder = store2.viewOrder) }
            updateStore(store2.uuid) { it.copy(viewOrder = tempOrder) }
        }
    }

    fun unlinkStoreFromAllGroceries(storeUuid: String) {
        categories.forEach { categoryWithSubs ->
            categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                subCategoryWithGroceries.groceries.forEachIndexed { index, grocery ->
                    if (grocery.storeIds.contains(storeUuid)) {
                        val updatedStoreIds = grocery.storeIds.filter { it != storeUuid }
                        val updatedGrocery = grocery.copy(storeIds = updatedStoreIds)
                        subCategoryWithGroceries.groceries[index] = updatedGrocery
                    }
                }
            }
        }
        updateData()
        android.util.Log.d("DataManagerObject", "Unlinked store from all groceries")
    }

    fun deleteStore(storeUuid: String) {
        // First unlink from all groceries
        unlinkStoreFromAllGroceries(storeUuid)
        
        // Soft delete (for Firebase sync)
        updateStore(storeUuid) { it.copy(deleted = true) }
        
        // Remove from hidden list if present
        hiddenStoreIds.remove(storeUuid)
        
        // Remove store's custom category order
        storeCategoryOrders.remove(storeUuid)
        
        android.util.Log.d("DataManagerObject", "Deleted store")
    }

    fun toggleStoreHidden(storeUuid: String) {
        if (hiddenStoreIds.contains(storeUuid)) {
            hiddenStoreIds.remove(storeUuid)
        } else {
            hiddenStoreIds.add(storeUuid)
        }
        updateData()
        android.util.Log.d("DataManagerObject", "Toggled store visibility")
    }

    fun isStoreHidden(storeUuid: String): Boolean {
        return hiddenStoreIds.contains(storeUuid)
    }

    // ========== Store Category Order Management ==========
    
    /**
     * Get custom category order for a store
     * @param storeId Store UUID
     * @return List of category UUIDs in custom order, or null if no custom order exists
     */
    fun getStoreCategoryOrder(storeId: String): List<String>? {
        return storeCategoryOrders[storeId]
    }
    
    /**
     * Set custom category order for a store
     * @param storeId Store UUID
     * @param categoryIds List of category UUIDs in desired order
     */
    fun setStoreCategoryOrder(storeId: String, categoryIds: List<String>) {
        storeCategoryOrders[storeId] = categoryIds.toList() // Create copy
        notifyUpdate()
        android.util.Log.d("DataManagerObject", "Set category order for store $storeId: ${categoryIds.size} categories")
    }
    
    /**
     * Swap two categories in a store's custom order
     * @param storeId Store UUID
     * @param categoryId1 First category UUID
     * @param categoryId2 Second category UUID
     */
    fun swapStoreCategoryOrder(storeId: String, categoryId1: String, categoryId2: String) {
        val currentOrder = storeCategoryOrders[storeId]?.toMutableList() 
            ?: getSortedCategories().map { it.category.uuid }.toMutableList()
        
        val index1 = currentOrder.indexOf(categoryId1)
        val index2 = currentOrder.indexOf(categoryId2)
        
        if (index1 != -1 && index2 != -1) {
            // Swap
            val temp = currentOrder[index1]
            currentOrder[index1] = currentOrder[index2]
            currentOrder[index2] = temp
            
            // Save updated order
            storeCategoryOrders[storeId] = currentOrder
            notifyUpdate()
            android.util.Log.d("DataManagerObject", "Swapped category order for store $storeId")
        }
    }

    // Group groceries by store for store view mode
    fun groupGroceriesByStore(
        categories: List<CategoryWithSubCategories>
    ): List<com.example.supercart2.ui.components.StoreWithCategories> {
        val result = mutableListOf<com.example.supercart2.ui.components.StoreWithCategories>()
        
        // Group by each store
        getSortedStores().forEach { store ->
            val storeCategories = mutableListOf<CategoryWithSubCategories>()
            
            categories.forEach { categoryWithSubs ->
                val categorySubCategories = mutableListOf<SubCategoryWithGroceries>()
                
                categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                    val groceriesForStore = subCategoryWithGroceries.groceries
                        .filter { grocery ->
                            grocery.storeIds.contains(store.uuid)
                        }
                    
                    if (groceriesForStore.isNotEmpty()) {
                        categorySubCategories.add(
                            SubCategoryWithGroceries(
                                subCategory = subCategoryWithGroceries.subCategory,
                                groceries = groceriesForStore.toMutableList()
                            )
                        )
                    }
                }
                
                if (categorySubCategories.isNotEmpty()) {
                    storeCategories.add(
                        CategoryWithSubCategories(
                            category = categoryWithSubs.category,
                            subCategories = categorySubCategories
                        )
                    )
                }
            }
            
            // Apply custom ordering if exists, otherwise use global order
            val customOrder = getStoreCategoryOrder(store.uuid)
            if (customOrder != null) {
                // Sort by custom order, then by viewOrder for any missing categories
                val orderedCategories = mutableListOf<CategoryWithSubCategories>()
                val unorderedCategories = storeCategories.toMutableList()
                
                // Add categories in custom order
                customOrder.forEach { categoryId ->
                    val category = unorderedCategories.find { it.category.uuid == categoryId }
                    if (category != null) {
                        orderedCategories.add(category)
                        unorderedCategories.remove(category)
                    }
                }
                
                // Add any remaining categories (not in custom order) sorted by viewOrder
                orderedCategories.addAll(
                    unorderedCategories.sortedBy { it.category.viewOrder }
                )
                
                storeCategories.clear()
                storeCategories.addAll(orderedCategories)
            } else {
                // Use global order (viewOrder)
                storeCategories.sortBy { it.category.viewOrder }
            }
            
            // Only add if store has groceries and is not hidden
            if (storeCategories.isNotEmpty() && !isStoreHidden(store.uuid)) {
                result.add(
                    com.example.supercart2.ui.components.StoreWithCategories(
                        store = store, 
                        categories = storeCategories
                    )
                )
            }
        }
        
        // Add "Not Linked to Any Store" section
        val notLinkedCategories = mutableListOf<CategoryWithSubCategories>()
        categories.forEach { categoryWithSubs ->
            val categorySubCategories = mutableListOf<SubCategoryWithGroceries>()
            
            categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                val notLinkedGroceries = subCategoryWithGroceries.groceries
                    .filter { grocery ->
                        grocery.storeIds.isEmpty()
                    }
                
                if (notLinkedGroceries.isNotEmpty()) {
                    categorySubCategories.add(
                        SubCategoryWithGroceries(
                            subCategory = subCategoryWithGroceries.subCategory,
                            groceries = notLinkedGroceries.toMutableList()
                        )
                    )
                }
            }
            
            if (categorySubCategories.isNotEmpty()) {
                notLinkedCategories.add(
                    CategoryWithSubCategories(
                        category = categoryWithSubs.category,
                        subCategories = categorySubCategories
                    )
                )
            }
        }
        
        // Sort not-linked categories by global order
        notLinkedCategories.sortBy { it.category.viewOrder }
        
        if (notLinkedCategories.isNotEmpty()) {
            result.add(
                com.example.supercart2.ui.components.StoreWithCategories(
                    store = null, 
                    categories = notLinkedCategories
                )
            )
        }
        
        return result
    }

    // Call this after any data modification
    fun updateData() {
        notifyUpdate()
    }
}

data class CategoryWithSubCategories(
    val category: Category,
    val subCategories: MutableList<SubCategoryWithGroceries>
) {
    fun getGroceriesCount(): Int = subCategories.sumOf { it.groceries.size }
}

data class SubCategoryWithGroceries(
    val subCategory: SubCategory,
    val groceries: MutableList<Grocery>
)
