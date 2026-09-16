package com.example.supercart2.ui.screens

import android.util.Log
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.models.Grocery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// VERBATIM EXTRACTION (temporary): HomeScreen's onGroceryCreated body, to be pinned before refactoring.
internal fun homeScreenApplyGroceryResult(groceryToEdit: Grocery?, newGrocery: Grocery, scope: CoroutineScope) {
                if (groceryToEdit != null) {
                    // Edit mode - update existing grocery
                    // Store reference to avoid smart cast issues with delegated property
                    val originalGrocery = groceryToEdit

                    // Check if the grocery is moving to a different sub-category
                    val isMovingToDifferentSubCategory = originalGrocery.subCategoryId != newGrocery.subCategoryId

                    if (isMovingToDifferentSubCategory) {
                        // Grocery is moving to a different sub-category - remove from old, add to new
                        val originalCategoryIndex = DataManagerObject.categories.indexOfFirst {
                            it.category.uuid == originalGrocery.categoryId
                        }

                        if (originalCategoryIndex != -1) {
                            val originalCategoryWithSubs = DataManagerObject.categories[originalCategoryIndex]
                            val originalSubCategoryIndex = originalCategoryWithSubs.subCategories.indexOfFirst {
                                it.subCategory.uuid == originalGrocery.subCategoryId
                            }

                            if (originalSubCategoryIndex != -1) {
                                // Remove from original sub-category
                                val updatedOriginalSubCategory = originalCategoryWithSubs.subCategories[originalSubCategoryIndex].copy(
                                    groceries = originalCategoryWithSubs.subCategories[originalSubCategoryIndex].groceries.toMutableList().apply {
                                        removeAll { it.uuid == originalGrocery.uuid }
                                    }
                                )

                                val updatedOriginalCategory = originalCategoryWithSubs.copy(
                                    subCategories = originalCategoryWithSubs.subCategories.toMutableList().apply {
                                        set(originalSubCategoryIndex, updatedOriginalSubCategory)
                                    }
                                )

                                DataManagerObject.categories[originalCategoryIndex] = updatedOriginalCategory
                            }
                        }

                        // Add to new sub-category at the end
                        val newCategoryIndex = DataManagerObject.categories.indexOfFirst {
                            it.category.uuid == newGrocery.categoryId
                        }

                        if (newCategoryIndex != -1) {
                            val newCategoryWithSubs = DataManagerObject.categories[newCategoryIndex]
                            val newSubCategoryIndex = newCategoryWithSubs.subCategories.indexOfFirst {
                                it.subCategory.uuid == newGrocery.subCategoryId
                            }

                            if (newSubCategoryIndex != -1) {
                                val updatedNewSubCategory = newCategoryWithSubs.subCategories[newSubCategoryIndex].copy(
                                    groceries = newCategoryWithSubs.subCategories[newSubCategoryIndex].groceries.toMutableList().apply {
                                        add(newGrocery)
                                    }
                                )

                                val updatedNewCategory = newCategoryWithSubs.copy(
                                    subCategories = newCategoryWithSubs.subCategories.toMutableList().apply {
                                        set(newSubCategoryIndex, updatedNewSubCategory)
                                    }
                                )

                                DataManagerObject.categories[newCategoryIndex] = updatedNewCategory
                            }
                        }

                        Log.d("HomeScreen", "Grocery moved: ${newGrocery.name} - from ${originalGrocery.subCategoryId} to ${newGrocery.subCategoryId}")
                    } else {
                        // Grocery is staying in the same sub-category - just update in place
                        val categoryIndex = DataManagerObject.categories.indexOfFirst {
                            it.category.uuid == newGrocery.categoryId
                        }

                        if (categoryIndex != -1) {
                            val categoryWithSubs = DataManagerObject.categories[categoryIndex]
                            val subCategoryIndex = categoryWithSubs.subCategories.indexOfFirst {
                                it.subCategory.uuid == newGrocery.subCategoryId
                            }

                            if (subCategoryIndex != -1) {
                                val subCategory = categoryWithSubs.subCategories[subCategoryIndex]
                                val groceryIndex = subCategory.groceries.indexOfFirst { it.uuid == originalGrocery.uuid }

                                if (groceryIndex != -1) {
                                    // Update the grocery in its original position
                                    val updatedGroceries = subCategory.groceries.toMutableList().apply {
                                        set(groceryIndex, newGrocery)
                                    }

                                    val updatedSubCategory = subCategory.copy(groceries = updatedGroceries)
                                    val updatedCategory = categoryWithSubs.copy(
                                        subCategories = categoryWithSubs.subCategories.toMutableList().apply {
                                            set(subCategoryIndex, updatedSubCategory)
                                        }
                                    )

                                DataManagerObject.categories[categoryIndex] = updatedCategory
                                DataManagerObject.updateData()
                                Log.d("HomeScreen", "Grocery updated in place: ${newGrocery.name} at position $groceryIndex")
                                }
                            }
                        }
                    }

                    // Save the updated data to local storage
                    scope.launch {
                        DataStoreManager.saveDataGlobally()
                    }
                } else {
                    // Create mode - add new grocery
                    val categoryIndex = DataManagerObject.categories.indexOfFirst {
                        it.category.uuid == newGrocery.categoryId
                    }

                    if (categoryIndex != -1) {
                        val categoryWithSubs = DataManagerObject.categories[categoryIndex]
                        val subCategoryIndex = categoryWithSubs.subCategories.indexOfFirst {
                            it.subCategory.uuid == newGrocery.subCategoryId
                        }

                        if (subCategoryIndex != -1) {
                            // Add the grocery using DataManagerObject helper
                            DataManagerObject.addGrocery(newGrocery)

                            // Save to DataStore
                            scope.launch {
                                DataStoreManager.saveDataGlobally()
                                android.util.Log.d("HomeScreen", "Saved new grocery to DataStore")
                            }

                            Log.d("HomeScreen", "New grocery added: ${newGrocery.name}")
                        }
                    }
                }
}
