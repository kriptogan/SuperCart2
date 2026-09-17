package com.example.supercart2.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.visibleGroceriesByUuid
import com.example.supercart2.models.Grocery

/** UI state of the ingredient picker; [excludedIds] are the groceries already in the draft. */
@Stable
internal class MealsScreenIngredientPickerState(private val excludedIds: Set<String>) {
    var query by mutableStateOf("")
    var showGroceryCreation by mutableStateOf(false)
        private set
    private var selectedIds by mutableStateOf(setOf<String>())
    private var createdIds by mutableStateOf(setOf<String>())

    /** Visible groceries not in the draft, sorted by name ignoring case. */
    fun candidates(): List<Grocery> = DataManagerObject.getSortedCategories().visibleGroceriesByUuid().values
        .filterNot { it.uuid in excludedIds }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    /** [candidates] whose name contains the query (ignoring case), plus the groceries created in this picker. */
    fun visibleCandidates(candidates: List<Grocery>): List<Grocery> =
        candidates.filter { it.name.contains(query.trim(), ignoreCase = true) || it.uuid in createdIds }

    fun isSelected(groceryId: String): Boolean = groceryId in selectedIds

    fun toggle(groceryId: String) {
        selectedIds = if (isSelected(groceryId)) selectedIds - groceryId else selectedIds + groceryId
    }

    /** Selected groceries among [candidates], in candidate order. */
    fun confirmedIds(candidates: List<Grocery>): List<String> = candidates.map { it.uuid }.filter(::isSelected)

    fun openGroceryCreation() {
        showGroceryCreation = true
    }

    fun closeGroceryCreation() {
        showGroceryCreation = false
    }

    /** A grocery created from the picker is selected and kept visible. */
    fun onGroceryCreated(grocery: Grocery) {
        selectedIds = selectedIds + grocery.uuid
        createdIds = createdIds + grocery.uuid
    }
}
