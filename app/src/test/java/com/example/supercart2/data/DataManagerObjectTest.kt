package com.example.supercart2.data

import com.example.supercart2.TestData
import com.example.supercart2.TestData.OLD
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.findGrocery
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.locationOf
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Grocery / category / sub-category mutations on the in-memory tree. */
class DataManagerObjectTest {

    private val dm = DataManagerObject

    @Before
    fun setUp() = TestData.installStandardTree()

    // ---------- getSortedCategories ----------

    @Test
    fun `getSortedCategories sorts by viewOrder`() {
        assertEquals(listOf("c2", "c1"), dm.getSortedCategories().map { it.category.uuid })
    }

    @Test
    fun `getSortedCategories hides deleted categories, sub-categories and groceries`() {
        dm.categories.add(node(category("gone", deleted = true), subNode(sub("sx", "gone"))))
        dm.updateSubCategory("c1", "s2") { it.copy(deleted = true) }
        dm.updateGrocery("g2") { it.copy(deleted = true) }

        val sorted = dm.getSortedCategories()
        assertEquals(listOf("c2", "c1"), sorted.map { it.category.uuid })
        val c1 = sorted.single { it.category.uuid == "c1" }
        assertEquals(listOf("s1"), c1.subCategories.map { it.subCategory.uuid })
        assertEquals(listOf("g1"), c1.subCategories.single().groceries.map { it.uuid })
    }

    @Test
    fun `getSortedCategories returns copies that do not alias the source lists`() {
        dm.getSortedCategories().single { it.category.uuid == "c1" }.subCategories.clear()
        assertEquals(2, dm.categories.single { it.category.uuid == "c1" }.subCategories.size)
    }

    // ---------- groceries ----------

    @Test
    fun `updateGrocery applies the change, stamps lastUpdate and bumps version`() {
        val v = dm.version
        dm.updateGrocery("g3") { it.copy(name = "milk") }

        val g = findGrocery("g3")!!
        assertEquals("milk", g.name)
        assertTrue(g.lastUpdate.isAfter(OLD))
        assertTrue(dm.version > v)
    }

    @Test
    fun `updateGrocery with unknown uuid is a no-op`() {
        val v = dm.version
        dm.updateGrocery("nope") { it.copy(name = "x") }
        assertEquals(v, dm.version)
    }

    @Test
    fun `toggleShoppingListStatus flips inShoppingList and always resets isBought`() {
        dm.updateGrocery("g1") { it.copy(inShoppingList = true, isBought = true) }
        dm.toggleShoppingListStatus("g1")
        assertFalse(findGrocery("g1")!!.inShoppingList)
        assertFalse(findGrocery("g1")!!.isBought)

        dm.updateGrocery("g1") { it.copy(isBought = true) }
        dm.toggleShoppingListStatus("g1")
        assertTrue(findGrocery("g1")!!.inShoppingList)
        assertFalse(findGrocery("g1")!!.isBought)
    }

    @Test
    fun `toggleBoughtStatus only flips isBought`() {
        dm.toggleBoughtStatus("g1")
        val g = findGrocery("g1")!!
        assertTrue(g.isBought)
        assertFalse(g.inShoppingList)
        assertTrue(g.buyEvents.isEmpty())
        dm.toggleBoughtStatus("g1")
        assertFalse(findGrocery("g1")!!.isBought)
    }

    @Test
    fun `addGrocery appends to the matching sub-category`() {
        dm.addGrocery(grocery("g4", "c1", "s2"))
        assertEquals("c1" to "s2", locationOf("g4"))
    }

    @Test
    fun `addGrocery is ignored when category and sub-category do not match`() {
        val v = dm.version
        dm.addGrocery(grocery("g4", "c2", "s2")) // s2 belongs to c1
        dm.addGrocery(grocery("g5", "missing", "s1"))
        assertNull(findGrocery("g4"))
        assertNull(findGrocery("g5"))
        assertEquals(v, dm.version)
    }

    @Test
    fun `updateGroceryLocation moves grocery and rewrites its ids`() {
        dm.updateGroceryLocation("g1", "c2", "s3")
        assertEquals("c2" to "s3", locationOf("g1"))
        val g = findGrocery("g1")!!
        assertEquals("c2", g.categoryId)
        assertEquals("s3", g.subCategoryId)
        assertTrue(g.lastUpdate.isAfter(OLD)) // stamped so the move is uploaded
        assertEquals(OLD, findGrocery("g2")!!.lastUpdate)
    }

    @Test
    fun `updateGroceryLocation within the same sub-category moves it to the end`() {
        dm.updateGroceryLocation("g1", "c1", "s1")
        val groceries = dm.categories.single { it.category.uuid == "c1" }
            .subCategories.single { it.subCategory.uuid == "s1" }.groceries.map { it.uuid }
        assertEquals(listOf("g2", "g1"), groceries)
    }

    @Test
    fun `updateGroceryLocation with invalid target does nothing`() {
        val v = dm.version
        dm.updateGroceryLocation("g1", "c2", "s1") // s1 not under c2
        assertEquals("c1" to "s1", locationOf("g1"))
        assertEquals(v, dm.version)
    }

    @Test
    fun `deleteGrocery soft-deletes`() {
        dm.deleteGrocery("g1")
        val g = findGrocery("g1")!!
        assertTrue(g.deleted)
        assertTrue(g.lastUpdate.isAfter(OLD))
    }

    // ---------- categories ----------

    @Test
    fun `updateCategory stamps lastUpdate`() {
        dm.updateCategory("c1") { it.copy(name = "Dairy") }
        val c = dm.categories.single { it.category.uuid == "c1" }.category
        assertEquals("Dairy", c.name)
        assertTrue(c.lastUpdate.isAfter(OLD))
    }

    @Test
    fun `swapCategoryOrder swaps viewOrder values`() {
        dm.swapCategoryOrder("c1", "c2")
        assertEquals(listOf("c1", "c2"), dm.getSortedCategories().map { it.category.uuid })
        assertEquals(1, dm.categories.single { it.category.uuid == "c1" }.category.viewOrder)
        assertEquals(2, dm.categories.single { it.category.uuid == "c2" }.category.viewOrder)
    }

    @Test
    fun `swapCategoryOrder with unknown uuid does nothing`() {
        dm.swapCategoryOrder("c1", "nope")
        assertEquals(2, dm.categories.single { it.category.uuid == "c1" }.category.viewOrder)
    }

    @Test
    fun `addCategory appends with given sub-categories`() {
        dm.addCategory(category("c3"), listOf(subNode(sub("s9", "c3"))))
        val added = dm.categories.last()
        assertEquals("c3", added.category.uuid)
        assertEquals(listOf("s9"), added.subCategories.map { it.subCategory.uuid })
    }

    @Test
    fun `deleteCategory soft-deletes and strips it from every store category order`() {
        dm.setStoreCategoryOrder("storeA", listOf("c1", "c2"))
        dm.setStoreCategoryOrder("storeB", listOf("c1"))

        dm.deleteCategory("c1")

        assertTrue(dm.categories.single { it.category.uuid == "c1" }.category.deleted)
        assertEquals(listOf("c2"), dm.getStoreCategoryOrder("storeA"))
        assertEquals(emptyList<String>(), dm.getStoreCategoryOrder("storeB"))
    }

    // ---------- sub-categories ----------

    @Test
    fun `updateSubCategory stamps lastUpdate and keeps groceries`() {
        dm.updateSubCategory("c1", "s1") { it.copy(name = "Cheese") }
        val s = dm.categories.single { it.category.uuid == "c1" }.subCategories.single { it.subCategory.uuid == "s1" }
        assertEquals("Cheese", s.subCategory.name)
        assertTrue(s.subCategory.lastUpdate.isAfter(OLD))
        assertEquals(listOf("g1", "g2"), s.groceries.map { it.uuid })
    }

    @Test
    fun `updateSubCategory requires the right parent category`() {
        dm.updateSubCategory("c2", "s1") { it.copy(name = "x") }
        assertEquals("s1", dm.categories.single { it.category.uuid == "c1" }.subCategories[0].subCategory.name)
    }

    @Test
    fun `addSubCategory appends empty sub-category, ignores unknown category`() {
        dm.addSubCategory("c2", sub("s4", "c2"))
        dm.addSubCategory("missing", sub("s5", "missing"))
        val c2 = dm.categories.single { it.category.uuid == "c2" }
        assertEquals(listOf("s3", "s4"), c2.subCategories.map { it.subCategory.uuid })
        assertTrue(c2.subCategories.last().groceries.isEmpty())
        assertTrue(dm.categories.none { c -> c.subCategories.any { it.subCategory.uuid == "s5" } })
    }

    @Test
    fun `deleteSubCategory soft-deletes`() {
        dm.deleteSubCategory("c1", "s2")
        assertTrue(dm.categories.single { it.category.uuid == "c1" }.subCategories[1].subCategory.deleted)
    }

    @Test
    fun `getGroceriesCount counts every grocery including deleted`() {
        dm.deleteGrocery("g1")
        assertEquals(2, dm.categories.single { it.category.uuid == "c1" }.getGroceriesCount())
    }

    @Test
    fun `every mutation bumps version`() {
        val versions = mutableListOf(dm.version)
        dm.addCategory(category("c9"))
        versions += dm.version
        dm.addSubCategory("c9", sub("s9", "c9"))
        versions += dm.version
        dm.addGrocery(grocery("g9", "c9", "s9"))
        versions += dm.version
        dm.confirmBoughtItems()
        versions += dm.version
        assertEquals(versions.distinct(), versions)
        assertNotEquals(versions.first(), versions.last())
    }

    // ---------- extra characterization pins ----------

    private fun c1() = dm.categories.single { it.category.uuid == "c1" }

    @Test
    fun `updateGroceryLocation between sub-categories of the same category keeps both changes`() {
        dm.updateGroceryLocation("g1", "c1", "s2")
        assertEquals("c1" to "s2", locationOf("g1"))
        assertEquals(listOf("g2"), c1().subCategories[0].groceries.map { it.uuid })
        assertEquals(listOf("g1"), c1().subCategories[1].groceries.map { it.uuid })
    }

    @Test
    fun `updateGroceryLocation with unknown grocery does nothing`() {
        val v = dm.version
        dm.updateGroceryLocation("nope", "c2", "s3")
        assertEquals(v, dm.version)
        assertEquals(listOf("g3"), dm.categories[1].subCategories[0].groceries.map { it.uuid })
    }

    @Test
    fun `grocery mutations replace the category node instead of mutating the old one`() {
        val before = c1()
        dm.updateGrocery("g1") { it.copy(name = "milk") }
        dm.addGrocery(grocery("g4", "c1", "s2"))
        assertEquals("g1", before.subCategories[0].groceries[0].name)
        assertTrue(before.subCategories[1].groceries.isEmpty())
        assertEquals("milk", c1().subCategories[0].groceries[0].name)
    }

    @Test
    fun `updateSubCategory mutates the category's sub-category list in place`() {
        val list = c1().subCategories
        val node = c1()
        dm.updateSubCategory("c1", "s2") { it.copy(name = "renamed") }
        assertSame(node, c1())
        assertSame(list, c1().subCategories)
        assertEquals("renamed", list[1].subCategory.name)
    }

    @Test
    fun `update helpers with unknown ids do not bump version`() {
        val v = dm.version
        dm.updateCategory("nope") { it.copy(name = "x") }
        dm.updateSubCategory("c1", "nope") { it.copy(name = "x") }
        dm.updateSubCategory("nope", "s1") { it.copy(name = "x") }
        dm.addSubCategory("nope", sub("s9", "nope"))
        assertEquals(v, dm.version)
    }

    @Test
    fun `deleteGrocery without a global context only soft-deletes`() {
        dm.updateGrocery("g1") { it.copy(imageUUID = "img") }
        dm.deleteGrocery("g1")
        assertTrue(findGrocery("g1")!!.deleted)
        dm.deleteGrocery("nope") // no-op, no crash
    }

    @Test
    fun `getSortedCategories leaves deleted items in the source tree`() {
        dm.deleteGrocery("g1")
        dm.deleteSubCategory("c1", "s2")
        dm.getSortedCategories()
        assertEquals(2, c1().subCategories.size)
        assertEquals(listOf("g1", "g2"), c1().subCategories[0].groceries.map { it.uuid })
    }

    @Test
    fun `addCategory defaults to no sub-categories and deleteCategory of unknown uuid keeps orders`() {
        dm.addCategory(category("c3"))
        assertTrue(dm.categories.last().subCategories.isEmpty())
        dm.setStoreCategoryOrder("A", listOf("c1"))
        dm.deleteCategory("nope")
        assertEquals(listOf("c1"), dm.getStoreCategoryOrder("A"))
    }
}
