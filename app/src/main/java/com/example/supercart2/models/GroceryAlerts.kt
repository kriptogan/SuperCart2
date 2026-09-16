package com.example.supercart2.models

import java.time.LocalDate

/**
 * A grocery needs attention when it expires by tomorrow (or already expired),
 * or when at least [Grocery.averageBuyDays] days have passed since it was last bought.
 */
fun Grocery.hasAlert(today: LocalDate = LocalDate.now()): Boolean {
    val isExpiringSoon = expirationDate?.let { it <= today.plusDays(1) } ?: false
    val needsToBuy = averageBuyDays?.let { avgDays ->
        buyEvents.maxOrNull()?.let { lastBuyDate -> today.toEpochDay() - lastBuyDate.toEpochDay() >= avgDays }
    } ?: false
    return isExpiringSoon || needsToBuy
}
