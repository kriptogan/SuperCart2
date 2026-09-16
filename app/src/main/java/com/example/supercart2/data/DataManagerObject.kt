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
import com.example.supercart2.ui.components.StoreWithCategories
import java.time.LocalDate
import java.time.LocalDateTime

object DataManagerObject {
    val categories: SnapshotStateList<CategoryWithSubCategories> = mutableStateListOf()
    val stores: SnapshotStateList<Store> = mutableStateListOf()
    val hiddenStoreIds: SnapshotStateList<String> = mutableStateListOf()

    // Store-specific category ordering: Map<StoreUUID, List<CategoryUUID>>
    var storeCategoryOrders: MutableMap<String, List<String>> = mutableMapOf()

    // Store-specific sub-category ordering: Map<StoreUUID, Map<CategoryUUID, List<SubCategoryUUID>>>
    var storeSubCategoryOrders: MutableMap<String, Map<String, List<String>>> = mutableMapOf()

    // Version counter to force recomposition
    private var _version by mutableStateOf(0)
    val version: Int get() = _version

    // Call this after any data modification
    fun updateData() {
        _version++
    }

    // Get categories sorted by viewOrder, without deleted categories, sub-categories and groceries
    fun getSortedCategories(): List<CategoryWithSubCategories> = categories
        .filter { !it.category.deleted }
        .map { categoryWithSubs ->
            categoryWithSubs.copy(
                subCategories = categoryWithSubs.subCategories
                    .filter { !it.subCategory.deleted }
                    .map { it.copy(groceries = it.groceries.filterNot { grocery -> grocery.deleted }.toMutableList()) }
                    .toMutableList()
            )
        }
        .sortedBy { it.category.viewOrder }

    /** Replaces the groceries of one sub-category, rebuilding its (current) category immutably. */
    private fun setGroceries(categoryIndex: Int, subCategoryIndex: Int, groceries: MutableList<Grocery>) {
        val category = categories[categoryIndex]
        val subCategories = category.subCategories.toMutableList()
        subCategories[subCategoryIndex] = subCategories[subCategoryIndex].copy(groceries = groceries)
        categories[categoryIndex] = category.copy(subCategories = subCategories)
    }

    // ========== Groceries ==========

    fun updateGrocery(groceryUuid: String, update: (Grocery) -> Grocery) {
        categories.forEachIndexed { categoryIndex, category ->
            category.subCategories.forEachIndexed { subCategoryIndex, subCategory ->
                val index = subCategory.groceries.indexOfFirst { it.uuid == groceryUuid }
                if (index != -1) {
                    val groceries = subCategory.groceries.toMutableList()
                    groceries[index] = update(groceries[index]).copy(lastUpdate = LocalDateTime.now())
                    setGroceries(categoryIndex, subCategoryIndex, groceries)
                    updateData()
                    return
                }
            }
        }
    }

    // Toggling shopping list status always resets bought status
    fun toggleShoppingListStatus(groceryUuid: String) {
        updateGrocery(groceryUuid) { it.copy(inShoppingList = !it.inShoppingList, isBought = false) }
    }

    // Just toggles isBought, doesn't affect buyEvents
    fun toggleBoughtStatus(groceryUuid: String) {
        updateGrocery(groceryUuid) { it.copy(isBought = !it.isBought) }
    }

    fun addGrocery(grocery: Grocery) {
        val categoryIndex = categories.indexOfFirst { it.category.uuid == grocery.categoryId }
        if (categoryIndex == -1) return
        val subCategories = categories[categoryIndex].subCategories
        val subCategoryIndex = subCategories.indexOfFirst { it.subCategory.uuid == grocery.subCategoryId }
        if (subCategoryIndex == -1) return

        setGroceries(categoryIndex, subCategoryIndex, (subCategories[subCategoryIndex].groceries + grocery).toMutableList())
        updateData()
    }

    // Moves a grocery to another category/sub-category. Note: does not stamp lastUpdate.
    fun updateGroceryLocation(groceryUuid: String, newCategoryId: String, newSubCategoryId: String) {
        var source: Pair<Int, Int>? = null
        var target: Pair<Int, Int>? = null
        var groceryToMove: Grocery? = null

        categories.forEachIndexed { catIndex, category ->
            category.subCategories.forEachIndexed { subIndex, subCategory ->
                subCategory.groceries.find { it.uuid == groceryUuid }?.let {
                    source = catIndex to subIndex
                    groceryToMove = it
                }
                if (category.category.uuid == newCategoryId && subCategory.subCategory.uuid == newSubCategoryId) {
                    target = catIndex to subIndex
                }
            }
        }

        val (sourceCat, sourceSub) = source ?: return
        val (targetCat, targetSub) = target ?: return
        val moved = groceryToMove ?: return

        val sourceGroceries = categories[sourceCat].subCategories[sourceSub].groceries
        setGroceries(sourceCat, sourceSub, sourceGroceries.filterNot { it.uuid == groceryUuid }.toMutableList())

        // Re-read the target: it may be the category (or sub-category) just rebuilt above
        val targetGroceries = categories[targetCat].subCategories[targetSub].groceries
        setGroceries(
            targetCat, targetSub,
            (targetGroceries + moved.copy(categoryId = newCategoryId, subCategoryId = newSubCategoryId)).toMutableList()
        )
        updateData()
    }

    fun deleteGrocery(groceryUuid: String) {
        // Soft delete - mark as deleted instead of removing
        updateGrocery(groceryUuid) { it.copy(deleted = true) }

        // Delete local image if exists
        val context = DataStoreManager.globalContext ?: return
        categories.forEach { category ->
            category.subCategories.forEach { subCategory ->
                subCategory.groceries.find { it.uuid == groceryUuid }?.imageUUID?.let { imageUUID ->
                    ImageManager.deleteLocalImage(imageUUID, context)
                }
            }
        }
    }

    // ========== Categories ==========

    fun updateCategory(categoryUuid: String, update: (Category) -> Category) {
        val index = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (index == -1) return
        val categoryWithSubs = categories[index]
        categories[index] = categoryWithSubs.copy(
            category = update(categoryWithSubs.category).copy(lastUpdate = LocalDateTime.now())
        )
        updateData()
    }

    fun swapCategoryOrder(category1Uuid: String, category2Uuid: String) {
        val cat1 = categories.find { it.category.uuid == category1Uuid }?.category ?: return
        val cat2 = categories.find { it.category.uuid == category2Uuid }?.category ?: return
        updateCategory(cat1.uuid) { it.copy(viewOrder = cat2.viewOrder) }
        updateCategory(cat2.uuid) { it.copy(viewOrder = cat1.viewOrder) }
    }

    fun addCategory(category: Category, subCategories: List<SubCategoryWithGroceries> = emptyList()) {
        categories.add(CategoryWithSubCategories(category, subCategories.toMutableList()))
        updateData()
    }

    fun deleteCategory(categoryUuid: String) {
        // Soft delete - mark as deleted instead of removing
        updateCategory(categoryUuid) { it.copy(deleted = true) }

        // Remove category from all store orders
        storeCategoryOrders.forEach { (storeId, categoryIds) ->
            storeCategoryOrders[storeId] = categoryIds.filter { it != categoryUuid }
        }
    }

    // ========== Sub-categories ==========

    fun updateSubCategory(categoryUuid: String, subCategoryUuid: String, update: (SubCategory) -> SubCategory) {
        val subCategories = categories.find { it.category.uuid == categoryUuid }?.subCategories ?: return
        val index = subCategories.indexOfFirst { it.subCategory.uuid == subCategoryUuid }
        if (index == -1) return
        // Updated in place (the category's list is mutated, not replaced)
        subCategories[index] = subCategories[index].copy(
            subCategory = update(subCategories[index].subCategory).copy(lastUpdate = LocalDateTime.now())
        )
        updateData()
    }

    fun addSubCategory(categoryUuid: String, subCategory: SubCategory) {
        val index = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (index == -1) return
        val categoryWithSubs = categories[index]
        categories[index] = categoryWithSubs.copy(
            subCategories = (categoryWithSubs.subCategories + SubCategoryWithGroceries(subCategory, mutableListOf()))
                .toMutableList()
        )
        updateData()
    }

    fun deleteSubCategory(categoryUuid: String, subCategoryUuid: String) {
        // Soft delete - mark as deleted instead of removing
        updateSubCategory(categoryUuid, subCategoryUuid) { it.copy(deleted = true) }
    }

    // ========== Confirm bought ==========

    // Average days between the newest (up to 4) buy events, rounded down; null with fewer than 2 events
    private fun calculateAverageBuyDays(buyEvents: List<LocalDate>): Int? {
        if (buyEvents.size < 2) return null
        return buyEvents.sortedDescending().take(4)
            .zipWithNext { newer, older -> newer.toEpochDay() - older.toEpochDay() }
            .average().toInt()
    }

    // Adds today's buy event (once per day), recomputes the average and takes the item off the list
    private fun confirmBought(grocery: Grocery, today: LocalDate): Grocery {
        @Suppress("USELESS_ELVIS") // Gson can leave buyEvents null for old data
        val existingEvents = grocery.buyEvents ?: emptyList()
        val updatedEvents = if (today in existingEvents) existingEvents else existingEvents + today
        return grocery.copy(
            isBought = false,
            inShoppingList = false,
            buyEvents = updatedEvents,
            averageBuyDays = calculateAverageBuyDays(updatedEvents),
            lastUpdate = LocalDateTime.now()
        )
    }

    // NOTE (known bug, pinned by tests): both confirm functions rebuild each category from `category`,
    // the snapshot taken at the start of the outer loop, so within one category only the last updated
    // sub-category survives.

    // Confirms all bought items - adds buyEvent and resets their status
    fun confirmBoughtItems() {
        val today = LocalDate.now()
        categories.forEachIndexed { categoryIndex, category ->
            category.subCategories.forEachIndexed { subCategoryIndex, subCategory ->
                if (subCategory.groceries.any { it.isBought }) {
                    val updatedGroceries = subCategory.groceries
                        .map { if (it.isBought) confirmBought(it, today) else it }
                        .toMutableList()
                    categories[categoryIndex] = category.withSubCategory(subCategoryIndex, updatedGroceries)
                }
            }
        }
        updateData()
    }

    // Confirms bought items that are linked to a specific store
    fun confirmBoughtItemsForStore(storeId: String) {
        val today = LocalDate.now()
        categories.forEachIndexed { categoryIndex, category ->
            category.subCategories.forEachIndexed { subCategoryIndex, subCategory ->
                val updatedGroceries = subCategory.groceries
                    .map { if (it.isBought && storeId in it.storeIds) confirmBought(it, today) else it }
                    .toMutableList()
                if (updatedGroceries != subCategory.groceries) {
                    categories[categoryIndex] = category.withSubCategory(subCategoryIndex, updatedGroceries)
                }
            }
        }
        updateData()
    }

    private fun CategoryWithSubCategories.withSubCategory(index: Int, groceries: MutableList<Grocery>) =
        copy(subCategories = subCategories.toMutableList().apply { set(index, get(index).copy(groceries = groceries)) })

    // ========== Stores ==========

    fun getSortedStores(): List<Store> = stores.filter { !it.deleted }.sortedBy { it.viewOrder }

    fun addStore(store: Store) {
        stores.add(store)
        updateData()
    }

    fun updateStore(storeUuid: String, update: (Store) -> Store) {
        val index = stores.indexOfFirst { it.uuid == storeUuid }
        if (index == -1) return
        stores[index] = update(stores[index]).copy(lastUpdate = LocalDateTime.now())
        updateData()
    }

    fun swapStoreOrder(store1Uuid: String, store2Uuid: String) {
        val store1 = stores.find { it.uuid == store1Uuid } ?: return
        val store2 = stores.find { it.uuid == store2Uuid } ?: return
        updateStore(store1.uuid) { it.copy(viewOrder = store2.viewOrder) }
        updateStore(store2.uuid) { it.copy(viewOrder = store1.viewOrder) }
    }

    // Note: edits grocery lists in place and does not stamp lastUpdate
    fun unlinkStoreFromAllGroceries(storeUuid: String) {
        categories.forEach { categoryWithSubs ->
            categoryWithSubs.subCategories.forEach { subCategoryWithGroceries ->
                val groceries = subCategoryWithGroceries.groceries
                groceries.forEachIndexed { index, grocery ->
                    if (storeUuid in grocery.storeIds) {
                        groceries[index] = grocery.copy(storeIds = grocery.storeIds.filter { it != storeUuid })
                    }
                }
            }
        }
        updateData()
    }

    fun deleteStore(storeUuid: String) {
        unlinkStoreFromAllGroceries(storeUuid)
        // Soft delete (for Firebase sync)
        updateStore(storeUuid) { it.copy(deleted = true) }
        hiddenStoreIds.remove(storeUuid)
        storeCategoryOrders.remove(storeUuid)
        storeSubCategoryOrders.remove(storeUuid)
    }

    fun toggleStoreHidden(storeUuid: String) {
        if (!hiddenStoreIds.remove(storeUuid)) hiddenStoreIds.add(storeUuid)
        updateData()
    }

    fun isStoreHidden(storeUuid: String): Boolean = storeUuid in hiddenStoreIds

    // ========== Per-store ordering ==========

    /** Custom category order (category UUIDs) for a store, or null if none. */
    fun getStoreCategoryOrder(storeId: String): List<String>? = storeCategoryOrders[storeId]

    fun setStoreCategoryOrder(storeId: String, categoryIds: List<String>) {
        storeCategoryOrders[storeId] = categoryIds.toList() // Create copy
        updateData()
    }

    fun getStoreSubCategoryOrder(storeId: String, categoryId: String): List<String>? =
        storeSubCategoryOrders[storeId]?.get(categoryId)

    fun setStoreSubCategoryOrders(storeId: String, orders: Map<String, List<String>>) {
        storeSubCategoryOrders[storeId] = orders.toMap()
        updateData()
    }

    /** Items whose id appears in [ids] first (in that order), then the rest via [sortRest]. */
    private fun <T> List<T>.orderedBy(
        ids: List<String>,
        idOf: (T) -> String,
        sortRest: (List<T>) -> List<T> = { it },
    ): List<T> {
        val remaining = toMutableList()
        val ordered = ids.mapNotNull { id -> remaining.find { idOf(it) == id }?.also { remaining.remove(it) } }
        return ordered + sortRest(remaining)
    }

    // Store's custom category order first, remaining categories by global viewOrder
    private fun List<CategoryWithSubCategories>.inStoreCategoryOrder(storeId: String) =
        orderedBy(getStoreCategoryOrder(storeId).orEmpty(), { it.category.uuid }) { rest ->
            rest.sortedBy { it.category.viewOrder }
        }

    /**
     * Returns categories sorted by store-specific category order,
     * with sub-categories sorted by store-specific sub-category order.
     */
    fun getSortedCategoriesForStore(storeId: String): List<CategoryWithSubCategories> =
        getSortedCategories()
            .map { catWithSubs ->
                val subOrder = getStoreSubCategoryOrder(storeId, catWithSubs.category.uuid)
                    ?: return@map catWithSubs
                CategoryWithSubCategories(
                    catWithSubs.category,
                    catWithSubs.subCategories.orderedBy(subOrder, { it.subCategory.uuid }).toMutableList()
                )
            }
            .inStoreCategoryOrder(storeId)

    /**
     * Groups groceries by visible store (in store order, each with its category order),
     * followed by a `store = null` group for groceries not linked to any store.
     */
    fun groupGroceriesByStore(categories: List<CategoryWithSubCategories>): List<StoreWithCategories> {
        val byStore = getSortedStores()
            .filterNot { isStoreHidden(it.uuid) }
            .mapNotNull { store ->
                categories.filterGroceries { store.uuid in it.storeIds }
                    .inStoreCategoryOrder(store.uuid)
                    .takeIf { it.isNotEmpty() }
                    ?.let { StoreWithCategories(store = store, categories = it) }
            }
        val notLinked = categories.filterGroceries { it.storeIds.isEmpty() }.sortedBy { it.category.viewOrder }
        return if (notLinked.isEmpty()) byStore else byStore + StoreWithCategories(store = null, categories = notLinked)
    }
}

/**
 * Keeps only groceries matching [predicate], dropping sub-categories and categories left empty.
 * Returns new (mutable) lists; the source tree is not modified.
 */
fun List<CategoryWithSubCategories>.filterGroceries(predicate: (Grocery) -> Boolean): List<CategoryWithSubCategories> =
    mapNotNull { categoryWithSubs ->
        val subCategories = categoryWithSubs.subCategories.mapNotNull { subCategoryWithGroceries ->
            subCategoryWithGroceries.groceries.filter(predicate)
                .takeIf { it.isNotEmpty() }
                ?.let { SubCategoryWithGroceries(subCategoryWithGroceries.subCategory, it.toMutableList()) }
        }
        subCategories.takeIf { it.isNotEmpty() }
            ?.let { CategoryWithSubCategories(categoryWithSubs.category, it.toMutableList()) }
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
