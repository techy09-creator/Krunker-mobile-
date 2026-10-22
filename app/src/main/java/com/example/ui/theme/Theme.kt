package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KrunkerDarkColorScheme = darkColorScheme(
    primary = KrunkerOrange,
    onPrimary = Color.White,
    primaryContainer = KrunkerOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = GamingAccent,
    onSecondary = Color.Black,
    secondaryContainer = GamingSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = KrunkerOrangeLight,
    onTertiary = Color.White,
    background = GamingDark,
    onBackground = TextPrimary,
    surface = GamingSurface,
    onSurface = TextPrimary,
    surfaceVariant = GamingSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun KrunkerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KrunkerDarkColorScheme,
        typography = Typography,
        content = content
    )
}
