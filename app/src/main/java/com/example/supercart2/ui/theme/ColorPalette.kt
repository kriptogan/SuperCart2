package com.example.supercart2.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Defines the primary and light background colors for the app palette.
 * White (inputs, cards, dropdowns) and black (text) stay the same across palettes.
 */
data class ColorPalette(
    val primary: Color,
    val lightPrimary: Color
)

object AppPalettes {
    val GREEN = ColorPalette(
        primary = SuperCartGreen,
        lightPrimary = SuperCartLightGreen
    )
    val ORANGE = ColorPalette(
        primary = SuperCartOrange,
        lightPrimary = SuperCartLightOrange
    )
    val PURPLE = ColorPalette(
        primary = SuperCartPurple,
        lightPrimary = SuperCartLightPurple
    )
    val BLUE = ColorPalette(
        primary = SuperCartBluePrimary,
        lightPrimary = SuperCartLightBlue
    )
    val PINK = ColorPalette(
        primary = SuperCartPink,
        lightPrimary = SuperCartLightPink
    )

    private val byId = mapOf(
        "green" to GREEN,
        "orange" to ORANGE,
        "purple" to PURPLE,
        "blue" to BLUE,
        "pink" to PINK
    )

    fun get(id: String?): ColorPalette = byId[id?.lowercase()] ?: GREEN
    fun idFor(palette: ColorPalette): String =
        byId.entries.find { it.value.primary == palette.primary }?.key ?: "green"
}
