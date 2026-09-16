package com.example.supercart2

import com.example.supercart2.data.CategoryWithSubCategories
import com.example.supercart2.data.DataManagerObject
import com.example.supercart2.data.DataStoreManager
import com.example.supercart2.data.SubCategoryWithGroceries
import com.example.supercart2.models.Category
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Store
import com.example.supercart2.models.SubCategory
import java.time.LocalDateTime

/** Builders + global-state reset shared by all unit tests. */
object TestData {
    /** A fixed timestamp well in the past, so "was lastUpdate stamped?" is easy to assert. */
    val OLD: LocalDateTime = LocalDateTime.of(2020, 1, 1, 12, 0)

    fun reset() {
        DataStoreManager.globalContext = null
        DataManagerObject.categories.clear()
        DataManagerObject.stores.clear()
        DataManagerObject.hiddenStoreIds.clear()
        DataManagerObject.storeCategoryOrders.clear()
        DataManagerObject.storeSubCategoryOrders.clear()
    }

    fun category(uuid: String, viewOrder: Int = 0, deleted: Boolean = false, name: String = uuid) =
        Category(uuid = uuid, name = name, viewOrder = viewOrder, deleted = deleted, lastUpdate = OLD)

    fun sub(uuid: String, categoryId: String, deleted: Boolean = false, name: String = uuid) =
        SubCategory(uuid = uuid, categoryId = categoryId, name = name, deleted = deleted, lastUpdate = OLD)

    fun grocery(
        uuid: String,
        categoryId: String,
        subCategoryId: String,
        storeIds: List<String> = emptyList(),
        inShoppingList: Boolean = false,
        isBought: Boolean = false,
        deleted: Boolean = false,
    ) = Grocery(
        uuid = uuid,
        name = uuid,
        categoryId = categoryId,
        subCategoryId = subCategoryId,
        storeIds = storeIds,
        inShoppingList = inShoppingList,
        isBought = isBought,
        deleted = deleted,
        lastUpdate = OLD,
    )

    fun store(uuid: String, viewOrder: Int = 0, deleted: Boolean = false) =
        Store(uuid = uuid, name = uuid, viewOrder = viewOrder, deleted = deleted, lastUpdate = OLD)

    fun node(category: Category, vararg subs: SubCategoryWithGroceries) =
        CategoryWithSubCategories(category, subs.toMutableList())

    fun subNode(sub: SubCategory, vararg groceries: Grocery) =
        SubCategoryWithGroceries(sub, groceries.toMutableList())

    /**
     * Installs a tree: categories c1 (viewOrder 2) and c2 (viewOrder 1);
     * c1 has s1 (g1, g2) and s2 (empty); c2 has s3 (g3).
     */
    fun installStandardTree() {
        reset()
        DataManagerObject.categories.addAll(
            listOf(
                node(
                    category("c1", viewOrder = 2),
                    subNode(sub("s1", "c1"), grocery("g1", "c1", "s1"), grocery("g2", "c1", "s1")),
                    subNode(sub("s2", "c1")),
                ),
                node(category("c2", viewOrder = 1), subNode(sub("s3", "c2"), grocery("g3", "c2", "s3"))),
            )
        )
    }

    fun findGrocery(uuid: String): Grocery? = DataManagerObject.categories
        .flatMap { it.subCategories }
        .flatMap { it.groceries }
        .find { it.uuid == uuid }

    fun locationOf(uuid: String): Pair<String, String>? {
        DataManagerObject.categories.forEach { c ->
            c.subCategories.forEach { s ->
                if (s.groceries.any { it.uuid == uuid }) return c.category.uuid to s.subCategory.uuid
            }
        }
        return null
    }
}
