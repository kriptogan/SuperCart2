package com.example.supercart2.data

import android.os.Looper
import com.example.supercart2.TestData
import com.example.supercart2.TestData.OLD
import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import com.example.supercart2.models.Meal
import com.example.supercart2.models.MealIngredient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataManagerObjectMealsTest {

    private val dm = DataManagerObject

    @After
    fun flushMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    @Before
    fun setUp() {
        TestData.installStandardTree()
    }

    private fun meal(uuid: String, name: String = uuid, deleted: Boolean = false, groceryIds: List<String> = emptyList()) =
        Meal(uuid = uuid, name = name, ingredients = groceryIds.map { MealIngredient(it, "q-$it") }, lastUpdate = OLD, deleted = deleted)

    private fun storedMeal(uuid: String) = dm.meals.single { it.uuid == uuid }

    // ---------- getSortedMeals ----------

    @Test
    fun `getSortedMeals hides deleted meals and sorts by name ignoring case`() {
        dm.meals.addAll(listOf(meal("1", "pasta"), meal("2", "Burger"), meal("3", "apple pie", deleted = true), meal("4", "Chili")))
        assertEquals(listOf("Burger", "Chili", "pasta"), dm.getSortedMeals().map { it.name })
    }

    // ---------- add / update / delete ----------

    @Test
    fun `addMeal appends the meal as given and bumps version`() {
        val v = dm.version
        val m = meal("m1", groceryIds = listOf("g1"))
        dm.addMeal(m)
        assertEquals(listOf(m), dm.meals.toList())
        assertTrue(dm.version > v)
    }

    @Test
    fun `updateMeal applies the update, stamps lastUpdate and bumps version`() {
        dm.meals.addAll(listOf(meal("m1"), meal("m2")))
        val v = dm.version
        dm.updateMeal("m2") { it.copy(name = "Soup") }
        assertEquals("Soup", storedMeal("m2").name)
        assertTrue(storedMeal("m2").lastUpdate.isAfter(OLD))
        assertEquals(meal("m1"), storedMeal("m1"))
        assertTrue(dm.version > v)
    }

    @Test
    fun `updateMeal with an unknown uuid changes nothing`() {
        dm.meals.add(meal("m1"))
        val v = dm.version
        dm.updateMeal("nope") { it.copy(name = "x") }
        assertEquals(listOf(meal("m1")), dm.meals.toList())
        assertEquals(v, dm.version)
    }

    @Test
    fun `deleteMeal soft-deletes, stamps lastUpdate and bumps version`() {
        dm.meals.add(meal("m1"))
        val v = dm.version
        dm.deleteMeal("m1")
        assertTrue(storedMeal("m1").deleted)
        assertTrue(storedMeal("m1").lastUpdate.isAfter(OLD))
        assertTrue(dm.getSortedMeals().isEmpty())
        assertTrue(dm.version > v)
    }

    // ---------- mealIngredientRows ----------

    @Test
    fun `mealIngredientRows resolves groceries in ingredient order with their ingredient`() {
        dm.updateGrocery("g3") { it.copy(name = "Fresh g3") }
        val m = meal("m1", groceryIds = listOf("g3", "g1"))
        val rows = dm.mealIngredientRows(m)
        assertEquals(listOf("Fresh g3", "g1"), rows.map { it.first.name })
        assertEquals(m.ingredients, rows.map { it.second })
    }

    @Test
    fun `mealIngredientRows skips missing, deleted and hidden-location groceries`() {
        dm.deleteGrocery("g2")
        dm.categories.add(node(category("dc", deleted = true), subNode(sub("ds", "dc"), grocery("inDeletedCat", "dc", "ds"))))
        dm.categories.add(node(category("c9"), subNode(sub("dsub", "c9", deleted = true), grocery("inDeletedSub", "c9", "dsub"))))
        val m = meal("m1", groceryIds = listOf("missing", "g2", "inDeletedCat", "inDeletedSub", "g1"))
        assertEquals(listOf("g1"), dm.mealIngredientRows(m).map { it.first.uuid })
    }

    // ---------- addGroceriesToShoppingList ----------

    @Test
    fun `addGroceriesToShoppingList lists unlisted visible groceries, stamps lastUpdate and returns the count`() {
        val v = dm.version
        assertEquals(2, dm.addGroceriesToShoppingList(listOf("g1", "g3")))
        listOf("g1", "g3").forEach {
            assertTrue(TestData.findGrocery(it)!!.inShoppingList)
            assertTrue(TestData.findGrocery(it)!!.lastUpdate.isAfter(OLD))
        }
        assertFalse(TestData.findGrocery("g2")!!.inShoppingList)
        assertTrue(dm.version > v)
    }

    @Test
    fun `addGroceriesToShoppingList leaves already listed groceries untouched`() {
        dm.categories[0].subCategories[0].groceries[0] = grocery("g1", "c1", "s1", inShoppingList = true, isBought = true)
        val before = TestData.findGrocery("g1")
        assertEquals(1, dm.addGroceriesToShoppingList(setOf("g1", "g2")))
        assertEquals(before, TestData.findGrocery("g1"))
        assertTrue(TestData.findGrocery("g2")!!.inShoppingList)
    }

    @Test
    fun `addGroceriesToShoppingList ignores missing, deleted, hidden-location and duplicate ids`() {
        dm.categories[0].subCategories[0].groceries[1] = grocery("g2", "c1", "s1", deleted = true)
        dm.categories.add(node(category("dc", deleted = true), subNode(sub("ds", "dc"), grocery("inDeletedCat", "dc", "ds"))))
        assertEquals(1, dm.addGroceriesToShoppingList(listOf("missing", "g2", "inDeletedCat", "g3", "g3")))
        assertFalse(TestData.findGrocery("g2")!!.inShoppingList)
        assertFalse(TestData.findGrocery("inDeletedCat")!!.inShoppingList)
        assertTrue(TestData.findGrocery("g3")!!.inShoppingList)
    }

    @Test
    fun `addGroceriesToShoppingList with nothing to add returns zero`() {
        assertEquals(0, dm.addGroceriesToShoppingList(emptyList()))
    }
}
