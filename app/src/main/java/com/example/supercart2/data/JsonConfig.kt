package com.example.supercart2.data

import com.example.supercart2.models.Category
import com.example.supercart2.models.Grocery
import com.example.supercart2.models.Meal
import com.example.supercart2.models.MealIngredient
import com.example.supercart2.models.Store
import com.example.supercart2.models.SubCategory
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Gson configured with ISO-8601 java.time adapters, shared by local storage and backups.
 * Unparseable dates are logged and become null (LocalDate) or now() (LocalDateTime).
 */
internal fun gsonBuilder(): GsonBuilder = GsonBuilder()
    .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter)
    .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter)

// Gson bypasses Kotlin defaults: properties missing from the JSON are null even when declared non-null.
// The normalized() copies below restore the defaults after deserializing. A missing required field
// (e.g. uuid or name) still makes copy() throw, which callers treat as invalid data.

@Suppress("USELESS_ELVIS")
internal fun Category.normalized(): Category = copy(lastUpdate = lastUpdate ?: LocalDateTime.now())

@Suppress("USELESS_ELVIS")
internal fun SubCategory.normalized(): SubCategory = copy(lastUpdate = lastUpdate ?: LocalDateTime.now())

@Suppress("USELESS_ELVIS")
internal fun Grocery.normalized(): Grocery = copy(
    storeIds = storeIds ?: emptyList(),
    buyEvents = buyEvents ?: emptyList(),
    lastUpdate = lastUpdate ?: LocalDateTime.now(),
)

@Suppress("USELESS_ELVIS")
internal fun Store.normalized(): Store = copy(address = address ?: "", lastUpdate = lastUpdate ?: LocalDateTime.now())

/** Ingredients without a groceryId (or null entries) are dropped. */
@Suppress("USELESS_ELVIS", "UNNECESSARY_SAFE_CALL")
internal fun Meal.normalized(): Meal = copy(
    ingredients = (ingredients ?: emptyList()).mapNotNull { it?.normalizedOrNull() },
    lastUpdate = lastUpdate ?: LocalDateTime.now(),
)

@Suppress("USELESS_ELVIS", "SENSELESS_COMPARISON")
private fun MealIngredient.normalizedOrNull(): MealIngredient? =
    if (groceryId == null) null else copy(quantity = quantity ?: "")

private object LocalDateAdapter: JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
        JsonPrimitive(src?.format(formatter))

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate? =
        try {
            json?.asString?.let { LocalDate.parse(it, formatter) }
        } catch (e: Exception) {
            android.util.Log.e("JsonConfig", "Error deserializing LocalDate: ${json?.asString}", e)
            null
        }
}

private object LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
        JsonPrimitive(src?.format(formatter))

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime =
        try {
            json?.asString?.let { LocalDateTime.parse(it, formatter) } ?: LocalDateTime.now()
        } catch (e: Exception) {
            android.util.Log.e("JsonConfig", "Error deserializing LocalDateTime: ${json?.asString}", e)
            LocalDateTime.now()
        }
}
