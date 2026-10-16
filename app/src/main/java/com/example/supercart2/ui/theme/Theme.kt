package com.example.supercart2.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.supercart2.data.SettingsManager

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun SuperCart2Theme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(context) { followColorPalette(context) }
    MaterialTheme(
        colorScheme = colorScheme(darkTheme, dynamicColor, context), typography = Typography, content = content
    )
}

// Makes the settings readable, then applies the saved color palette and follows later changes
private suspend fun followColorPalette(context: Context) {
    SettingsManager.setGlobalContext(context)
    SettingsManager.colorPalette.collect { id -> SuperCartColors.updatePalette(AppPalettes.get(id)) }
}

private fun colorScheme(darkTheme: Boolean, dynamicColor: Boolean, context: Context): ColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
