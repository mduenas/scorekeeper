package com.markduenas.scorekeeper.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1E3A5F),
    surface = Color(0xFF1A1A2E),
    onSurface = Color(0xFFE0E0E0),
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBBDEFB),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
)

@Composable
fun ScorekeeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
