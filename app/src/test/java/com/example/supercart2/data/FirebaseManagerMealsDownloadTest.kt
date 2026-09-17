package com.example.supercart2.data

import com.example.supercart2.data.FirestoreFake.Companion.snapshot
import com.example.supercart2.models.Meal
import com.example.supercart2.models.MealIngredient
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseManagerMealsDownloadTest : FirebaseManagerTestBase() {

    private val t = LocalDateTime.of(2024, 3, 1, 10, 30, 15, 123_000_000)
    private fun ts(time: LocalDateTime) = Timestamp(Date.from(time.atZone(ZoneId.systemDefault()).toInstant()))

    private fun installCategory() = fake.setDocs("categories", snapshot("c1", mapOf("name" to "c1")))

    @Test
    fun `downloadData maps every meal field, accepting String and Timestamp dates`() = runTest {
        installCategory()
        fake.setDocs(
            "meals",
            snapshot(
                "m1",
                mapOf(
                    "uuid" to "ignored", "name" to "Pasta", "lastUpdate" to "2024-03-01T10:30:15.123", "deleted" to false,
                    "ingredients" to listOf(mapOf("groceryId" to "g1", "quantity" to "500 g"), mapOf("groceryId" to "g2")),
                ),
            ),
            snapshot("m2", mapOf("name" to "Soup", "lastUpdate" to ts(t), "deleted" to true)),
        )

        FirebaseManager.downloadData()

        assertEquals(
            listOf(
                Meal("m1", "Pasta", listOf(MealIngredient("g1", "500 g"), MealIngredient("g2", "")), t),
                Meal("m2", "Soup", emptyList(), t, deleted = true),
            ),
            DataManagerObject.meals.toList(),
        )
    }

    @Test
    fun `downloadData skips malformed ingredients and keeps the valid ones`() = runTest {
        installCategory()
        val ingredients = listOf(
            mapOf("groceryId" to "g1", "quantity" to 2L),
            mapOf("quantity" to "1"),
            mapOf("groceryId" to 5, "quantity" to "1"),
            "g3",
            null,
            mapOf("groceryId" to "g4", "quantity" to "1 kg"),
        )
        fake.setDocs(
            "meals",
            snapshot("m1", mapOf("name" to "Pasta", "lastUpdate" to "2024-03-01T10:30:15.123", "ingredients" to ingredients)),
            snapshot("m2", mapOf("name" to "Soup", "lastUpdate" to "2024-03-01T10:30:15.123", "ingredients" to "g1")),
        )

        FirebaseManager.downloadData()

        assertEquals(
            listOf(listOf(MealIngredient("g1", ""), MealIngredient("g4", "1 kg")), emptyList()),
            DataManagerObject.meals.map { it.ingredients },
        )
    }

    @Test
    fun `downloadData skips malformed meal documents and keeps the rest`() = runTest {
        installCategory()
        fake.setDocs(
            "meals",
            snapshot("bad1", mapOf("ingredients" to emptyList<Any>())),
            snapshot("bad2", mapOf("name" to 5)),
            snapshot("bad3", null),
            snapshot("m1", mapOf("name" to "Pasta")),
        )

        FirebaseManager.downloadData()

        assertEquals(listOf("m1"), DataManagerObject.meals.map { it.uuid })
    }

    @Test
    fun `downloadData replaces local meals, keeping deleted meals and unknown groceries`() = runTest {
        DataManagerObject.meals.add(Meal("local", "Local", lastUpdate = t))
        installCategory()
        fake.setDocs(
            "meals",
            snapshot("m1", mapOf("name" to "Gone", "deleted" to true, "lastUpdate" to "2024-03-01T10:30:15.123")),
            snapshot(
                "m2",
                mapOf("name" to "Pasta", "lastUpdate" to "2024-03-01T10:30:15.123", "ingredients" to listOf(mapOf("groceryId" to "unknown"))),
            ),
        )

        FirebaseManager.downloadData()

        val expected = listOf(
            Meal("m1", "Gone", lastUpdate = t, deleted = true),
            Meal("m2", "Pasta", listOf(MealIngredient("unknown")), t),
        )
        assertEquals(expected, DataManagerObject.meals.toList())
    }

    @Test
    fun `downloadData clears local meals when the group has no meals collection`() = runTest {
        DataManagerObject.meals.add(Meal("local", "Local", lastUpdate = t))
        installCategory()

        FirebaseManager.downloadData()

        assertEquals(emptyList<Meal>(), DataManagerObject.meals.toList())
    }
}
