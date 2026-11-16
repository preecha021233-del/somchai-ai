package com.somchai.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SomchaiDarkColorScheme = darkColorScheme(
    primary = SomchaiPrimary,
    onPrimary = Color.White,
    primaryContainer = SomchaiPrimaryDark,
    onPrimaryContainer = SomchaiTextPrimary,
    secondary = SomchaiPrimaryLight,
    onSecondary = Color.Black,
    background = SomchaiBackground,
    onBackground = SomchaiTextPrimary,
    surface = SomchaiSurface,
    onSurface = SomchaiTextPrimary,
    surfaceVariant = SomchaiSurfaceElevated,
    onSurfaceVariant = SomchaiTextSecondary,
    outline = SomchaiBorder,
    outlineVariant = SomchaiBorderSubtle,
    error = SomchaiCritical,
    onError = Color.White,
    errorContainer = SomchaiCriticalContainer,
    onErrorContainer = SomchaiTextPrimary
)

@Composable
fun SomchaiTheme(
    darkTheme: Boolean = true, // Force dark-first
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SomchaiDarkColorScheme,
        typography = SomchaiTypography,
        content = content
    )
}
