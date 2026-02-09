package com.example.supercart2.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// SuperCart Design System - Spacing
object SuperCartSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

// SuperCart Design System - Shapes
object SuperCartShapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val circle = CircleShape
}

// SuperCart Design System - Colors. Primary and light background come from selected palette.
object SuperCartColors {
    private val paletteState = mutableStateOf(AppPalettes.GREEN)

    fun updatePalette(palette: ColorPalette) {
        paletteState.value = palette
    }

    val primaryGreen: Color get() = paletteState.value.primary
    val lightGreen: Color get() = paletteState.value.lightPrimary
    val lightGray: Color get() = paletteState.value.lightPrimary
    val blue = SuperCartBlue
    val gray = SuperCartGray
    val darkGray = SuperCartDarkGray
    val white = SuperCartWhite
    val black = SuperCartBlack
    val orange = SuperCartOrange
}
