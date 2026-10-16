package com.example.supercart2.ui.components

import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Category
import com.example.supercart2.models.SubCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Callbacks CategoriesManagementDialog hands to EditCategoryDialog for the category [categoryUuid].
 * EditCategoryDialog currently only calls [categoryDeleted]; the others are kept as they were.
 */
internal class CategoriesManagementDialogEditHandlers(
    private val categoryUuid: String,
    private val scope: CoroutineScope,
    private val onClose: () -> Unit,
) {
    fun categoryUpdated(updated: Category) {
        DataManagerObject.updateCategory(updated.uuid) { updated }
        save()
        onClose()
    }

    /** Soft-deletes and saves a non-protected category (protected ones are only logged), then closes. */
    fun categoryDeleted(category: Category) {
        if (category.protected) warnProtected("category", category.name) else deleteAndSave(category)
        onClose()
    }

    private fun deleteAndSave(category: Category) {
        DataManagerObject.deleteCategory(category.uuid)
        save()
    }

    fun subCategoryCreated(subCategory: SubCategory) =
        editSubCategories { it.add(SubCategoryWithGroceries(subCategory, mutableListOf())) }

    fun subCategoryUpdated(updated: SubCategory) = editSubCategories { subs ->
        val index = subs.indexOfFirst { it.subCategory.uuid == updated.uuid }
        if (index != -1) subs[index] = subs[index].copy(subCategory = updated)
        index != -1
    }

    /** Soft-deletes a known, non-protected sub-category (via DataManagerObject.deleteSubCategory) and saves. */
    fun subCategoryDeleted(subCategory: SubCategory) {
        if (subCategory.protected) return warnProtected("sub-category", subCategory.name)
        val category = DataManagerObject.categories.find { it.category.uuid == categoryUuid } ?: return
        if (category.subCategories.none { it.subCategory.uuid == subCategory.uuid }) return
        DataManagerObject.deleteSubCategory(categoryUuid, subCategory.uuid)
        save()
    }

    /** Replaces the category by a copy with the edited sub-category list and saves, unless [edit] returns false. */
    private fun editSubCategories(edit: (MutableList<SubCategoryWithGroceries>) -> Boolean) {
        val index = DataManagerObject.categories.indexOfFirst { it.category.uuid == categoryUuid }
        val categoryWithSubs = DataManagerObject.categories.getOrNull(index) ?: return
        val subCategories = categoryWithSubs.subCategories.toMutableList()
        if (!edit(subCategories)) return
        DataManagerObject.categories[index] = categoryWithSubs.copy(subCategories = subCategories)
        save()
    }

    private fun save() {
        scope.launch { DataStoreManager.saveDataGlobally() }
    }

    private fun warnProtected(kind: String, name: String) {
        android.util.Log.w("CategoriesManagementDialog", "Attempted to delete protected $kind: $name")
    }
}
