package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BengalGreenPrimary,
    secondary = BengalTeal,
    tertiary = BengalGold,
    background = BengalGreenDark,
    surface = BengalSurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = BengalSurfaceCard,
    onSurfaceVariant = TextLight,
    error = BengalCrimson
)

private val LightColorScheme = lightColorScheme(
    primary = BengalPrimaryLight,
    secondary = BengalTeal,
    tertiary = BengalGold,
    background = BengalSurfaceLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = BengalCardLight,
    onSurfaceVariant = TextDark,
    error = BengalAccentLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We intentionally ignore OS dynamicColor to guarantee our gorgeous custom Bengali brand style!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
