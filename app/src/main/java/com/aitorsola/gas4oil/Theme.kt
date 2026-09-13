package com.aitorsola.gas4oil

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Orange = Color(0xFFF08A1E)
private val OrangeDark = Color(0xFFFFA94D)

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    secondary = Orange,
    tertiary = Orange
)

private val DarkColors = darkColorScheme(
    primary = OrangeDark,
    onPrimary = Color(0xFF3A1B00),
    secondary = OrangeDark,
    tertiary = OrangeDark
)

val PriceGreen = Color(0xFF34A853)
val PriceRed = Color(0xFFEA4335)
val PriceBlue = Color(0xFF4285F4)
val PriceIndigo = Color(0xFF5C6BC0)
val PriceTeal = Color(0xFF26A69A)
val PriceCyan = Color(0xFF00ACC1)
val PricePurple = Color(0xFF9C27B0)

@Composable
fun Gas4OilTheme(preference: ThemePreference, content: @Composable () -> Unit) {
    val dark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
