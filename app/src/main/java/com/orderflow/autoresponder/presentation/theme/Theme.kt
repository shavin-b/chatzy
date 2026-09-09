package com.orderflow.autoresponder.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    secondary = BrandAccent,
    background = BrandDarkBackground,
    surface = BrandSurfaceDark,
    surfaceVariant = BrandCardDark,
    onPrimary = BrandTextPrimary,
    onSecondary = BrandTextPrimary,
    onBackground = BrandTextPrimary,
    onSurface = BrandTextPrimary
)

@Composable
fun OrderFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
