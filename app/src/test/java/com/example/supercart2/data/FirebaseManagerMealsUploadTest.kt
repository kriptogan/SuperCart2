package com.example.supercart2.data

import com.example.supercart2.data.FirestoreFake.Companion.snapshot
import com.example.supercart2.models.Meal
import com.example.supercart2.models.MealIngredient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseManagerMealsUploadTest : FirebaseManagerTestBase() {

    private val t = LocalDateTime.of(2024, 3, 1, 10, 30, 15, 123_000_000)

    private fun remote(id: String, lastUpdate: Any?) = snapshot(id, mapOf("name" to id, "lastUpdate" to lastUpdate))

    @Test
    fun `uploadData writes a meal as a map with its ingredients and an ISO lastUpdate`() = runTest {
        val ingredients = listOf(MealIngredient("g1", "500 g"), MealIngredient("g2"))
        DataManagerObject.meals.add(Meal("m1", "Pasta", ingredients, lastUpdate = t))

        FirebaseManager.uploadData()

        val write = fake.writes.single { it.path == "meals/m1" }
        assertEquals(
            mapOf(
                "uuid" to "m1", "name" to "Pasta",
                "ingredients" to listOf(
                    mapOf("groceryId" to "g1", "quantity" to "500 g"), mapOf("groceryId" to "g2", "quantity" to ""),
                ),
                "lastUpdate" to "2024-03-01T10:30:15.123", "deleted" to false,
            ),
            write.data,
        )
        assertNull(write.options)
        assertEquals(1, fake.commits)
    }

    @Test
    fun `uploadData writes a meal without ingredients as an empty list`() = runTest {
        DataManagerObject.meals.add(Meal("m1", "Empty", lastUpdate = t))

        FirebaseManager.uploadData()

        val data = fake.writes.single { it.path == "meals/m1" }.data as Map<*, *>
        assertEquals(emptyList<Any>(), data["ingredients"])
    }

    @Test
    fun `uploadData uploads deleted meals so the deletion syncs`() = runTest {
        DataManagerObject.meals.add(Meal("m1", "Gone", lastUpdate = t, deleted = true))

        FirebaseManager.uploadData()

        val data = fake.writes.single { it.path == "meals/m1" }.data as Map<*, *>
        assertEquals(true, data["deleted"])
    }

    @Test
    fun `uploadData only writes meals that are new or strictly newer than Firebase`() = runTest {
        listOf("new", "older", "same", "newer").forEach { DataManagerObject.meals.add(Meal("m$it", it, lastUpdate = t)) }
        fake.setDocs(
            "meals",
            remote("molder", "2024-03-01T10:30:15.122"), remote("msame", "2024-03-01T10:30:15.123"),
            remote("mnewer", "2024-03-01T10:30:16"), remote("unrelated", "2024-03-01T10:30:16"),
        )

        FirebaseManager.uploadData()

        assertEquals(
            setOf("meals/mnew", "meals/molder", "storeCategoryOrders/orders"),
            fake.writes.map { it.path }.toSet(),
        )
        assertEquals(1, fake.commits)
    }

    @Test
    fun `uploadData treats a malformed Firebase meal as missing and overwrites it`() = runTest {
        DataManagerObject.meals.add(Meal("m1", "Pasta", lastUpdate = t))
        fake.setDocs("meals", snapshot("m1", mapOf("lastUpdate" to "2024-03-01T10:30:16")))

        FirebaseManager.uploadData()

        assertEquals(listOf("meals/m1"), fake.writes.map { it.path }.filter { it.startsWith("meals/") })
    }
}
