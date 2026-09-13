package com.aitorsola.gas4oil

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val LightColors = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDEDED),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF111111),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E6E6),
    onSecondaryContainer = Color(0xFF111111),
    tertiary = Color(0xFF111111),
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF5F5F5F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFECECEC),
    surfaceContainerHighest = Color(0xFFE6E6E6),
    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFFDDDDDD)
)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3A3A3C),
    onSecondaryContainer = Color.White,
    tertiary = Color.White,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFF3A3A3C)
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
