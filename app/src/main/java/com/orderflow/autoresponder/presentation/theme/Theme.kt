package com.orderflow.autoresponder.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    secondary = BrandAccent,
    background = BrandDarkBackground,
    surface = BrandSurfaceDark,
    surfaceVariant = BrandCardDark,
    secondaryContainer = Color(0xFF103A2C),
    onSecondaryContainer = BrandGreen,
    onPrimary = BrandDarkBackground,
    onSecondary = BrandDarkBackground,
    onBackground = BrandTextPrimary,
    onSurface = BrandTextPrimary,
    onSurfaceVariant = BrandTextSecondary,
    outline = Color(0xFF53636D),
    outlineVariant = Color(0xFF202C33)
)

@Composable
fun OrderFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
