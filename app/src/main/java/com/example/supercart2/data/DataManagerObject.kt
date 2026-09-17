package com.example.supercart2.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Meal
import com.example.supercart2.models.Store
import com.example.supercart2.ui.components.StoreWithCategories
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The app state. Tree mutations are implemented by the helpers in DataManagerObjectGroceries.kt,
 * DataManagerObjectCategories.kt, DataManagerObjectConfirmBought.kt and DataManagerObjectStores.kt.
 */
object DataManagerObject {
    val categories: SnapshotStateList<CategoryWithSubCategories> = mutableStateListOf()
    val stores: SnapshotStateList<Store> = mutableStateListOf()
    val hiddenStoreIds: SnapshotStateList<String> = mutableStateListOf()

    // Meals (including soft-deleted ones, so deletions can sync)
    val meals: SnapshotStateList<Meal> = mutableStateListOf()

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
        .map { it.withoutDeletedChildren() }
        .sortedBy { it.category.viewOrder }

    // ========== Groceries ==========

    fun updateGrocery(groceryUuid: String, update: (Grocery) -> Grocery) {
        val (categoryIndex, subCategoryIndex, groceryIndex) = categories.findGroceryPath(groceryUuid) ?: return
        val groceries = categories[categoryIndex].subCategories[subCategoryIndex].groceries.toMutableList()
        groceries[groceryIndex] = update(groceries[groceryIndex]).copy(lastUpdate = LocalDateTime.now())
        categories.setGroceries(categoryIndex, subCategoryIndex, groceries)
        updateData()
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
        val (categoryIndex, subCategoryIndex) =
            categories.findSubCategoryPath(grocery.categoryId, grocery.subCategoryId) ?: return
        categories.appendGrocery(categoryIndex, subCategoryIndex, grocery)
        updateData()
    }

    // Moves a grocery to another category/sub-category, stamping lastUpdate
    fun updateGroceryLocation(groceryUuid: String, newCategoryId: String, newSubCategoryId: String) {
        val source = categories.lastSubCategoryPathContaining(groceryUuid) ?: return
        val target = categories.lastSubCategoryPath(newCategoryId, newSubCategoryId) ?: return
        categories.moveGrocery(groceryUuid, source, target) { grocery ->
            grocery.copy(categoryId = newCategoryId, subCategoryId = newSubCategoryId, lastUpdate = LocalDateTime.now())
        }
        updateData()
    }

    fun deleteGrocery(groceryUuid: String) {
        // Soft delete - mark as deleted instead of removing
        updateGrocery(groceryUuid) { it.copy(deleted = true) }

        // Delete local image if exists
        val context = DataStoreManager.globalContext ?: return
        categories.imageUuidsOf(groceryUuid).forEach { ImageManager.deleteLocalImage(it, context) }
    }

    // ========== Categories ==========

    fun updateCategory(categoryUuid: String, update: (Category) -> Category) {
        val index = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (index == -1) return
        categories[index] = categories[index].withUpdatedCategory(update)
        updateData()
    }

    // Swaps two non-deleted categories in display order, renumbering them 1..n (see changedViewOrders)
    fun swapCategoryOrder(category1Uuid: String, category2Uuid: String) {
        val sorted = categories.map { it.category }.filterNot { it.deleted }.sortedBy { it.viewOrder }
        changedViewOrders(sorted.map { it.uuid to it.viewOrder }, category1Uuid, category2Uuid)
            .forEach { (uuid, order) -> updateCategory(uuid) { it.copy(viewOrder = order) } }
    }

    fun addCategory(category: Category, subCategories: List<SubCategoryWithGroceries> = emptyList()) {
        categories.add(CategoryWithSubCategories(category, subCategories.toMutableList()))
        updateData()
    }

    fun deleteCategory(categoryUuid: String) {
        // Soft delete - mark as deleted instead of removing
        updateCategory(categoryUuid) { it.copy(deleted = true) }
        storeCategoryOrders.removeCategoryFromAll(categoryUuid)
    }

    // ========== Sub-categories ==========

    fun updateSubCategory(categoryUuid: String, subCategoryUuid: String, update: (SubCategory) -> SubCategory) {
        val subCategories = categories.find { it.category.uuid == categoryUuid }?.subCategories ?: return
        val index = subCategories.indexOfFirst { it.subCategory.uuid == subCategoryUuid }
        if (index == -1) return
        // Updated in place (the category's list is mutated, not replaced)
        subCategories[index] = subCategories[index].withUpdatedSubCategory(update)
        updateData()
    }

    fun addSubCategory(categoryUuid: String, subCategory: SubCategory) {
        val index = categories.indexOfFirst { it.category.uuid == categoryUuid }
        if (index == -1) return
        categories[index] = categories[index].withAddedSubCategory(subCategory)
        updateData()
    }

    fun deleteSubCategory(categoryUuid: String, subCategoryUuid: String) {
        // Soft delete - mark as deleted instead of removing
        updateSubCategory(categoryUuid, subCategoryUuid) { it.copy(deleted = true) }
    }

    // ========== Confirm bought ==========

    // Confirms all bought items - adds buyEvent and resets their status
    fun confirmBoughtItems() {
        categories.confirmBought(LocalDate.now()) { it.isBought }
        updateData()
    }

    // Confirms bought items that are linked to a specific store
    fun confirmBoughtItemsForStore(storeId: String) {
        categories.confirmBought(LocalDate.now()) { it.isBought && storeId in it.storeIds }
        updateData()
    }

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

    // Swaps two non-deleted stores in display order, renumbering them 1..n (see changedViewOrders)
    fun swapStoreOrder(store1Uuid: String, store2Uuid: String) {
        changedViewOrders(getSortedStores().map { it.uuid to it.viewOrder }, store1Uuid, store2Uuid)
            .forEach { (uuid, order) -> updateStore(uuid) { it.copy(viewOrder = order) } }
    }

    // Edits grocery lists in place, stamping lastUpdate on the groceries that were linked
    fun unlinkStoreFromAllGroceries(storeUuid: String) {
        categories.forEach { category -> category.subCategories.forEach { it.groceries.unlinkStore(storeUuid) } }
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

    /**
     * Returns categories sorted by store-specific category order,
     * with sub-categories sorted by store-specific sub-category order.
     */
    fun getSortedCategoriesForStore(storeId: String): List<CategoryWithSubCategories> =
        getSortedCategories()
            .map { it.inStoreSubCategoryOrder(storeId) }
            .inStoreCategoryOrder(storeId)

    /**
     * Groups groceries by visible store (in store order, each with its category order),
     * followed by a `store = null` group for groceries not linked to any store.
     */
    fun groupGroceriesByStore(categories: List<CategoryWithSubCategories>): List<StoreWithCategories> {
        val byStore = getSortedStores().filterNot { isStoreHidden(it.uuid) }.mapNotNull { categories.groupForStore(it) }
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
        categoryWithSubs.subCategories.mapNotNull { it.filterGroceries(predicate) }
            .takeIf { it.isNotEmpty() }
            ?.let { CategoryWithSubCategories(categoryWithSubs.category, it.toMutableList()) }
    }

/** A copy with only the groceries matching [predicate], or null when none match. */
private fun SubCategoryWithGroceries.filterGroceries(predicate: (Grocery) -> Boolean): SubCategoryWithGroceries? =
    groceries.filter(predicate)
        .takeIf { it.isNotEmpty() }
        ?.let { SubCategoryWithGroceries(subCategory, it.toMutableList()) }

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
