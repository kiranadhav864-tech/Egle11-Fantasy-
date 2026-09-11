package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EgleColorScheme = darkColorScheme(
    primary = EgleGoldPrimary,
    onPrimary = Color(0xFF1C1300),
    primaryContainer = EgleGoldDark,
    onPrimaryContainer = EgleGoldLight,
    secondary = EgleCyanAccent,
    onSecondary = Color(0xFF002026),
    tertiary = EgleGreenSuccess,
    onTertiary = Color.White,
    background = EgleNavyDark,
    onBackground = EgleTextPrimary,
    surface = EgleNavySurface,
    onSurface = EgleTextPrimary,
    surfaceVariant = EgleNavyCard,
    onSurfaceVariant = EgleTextSecondary,
    outline = EgleNavyBorder,
    error = EgleRedAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EgleColorScheme,
        typography = Typography,
        content = content
    )
}
