package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HashGridColorScheme = lightColorScheme(
    primary = GoldGradientMid,
    onPrimary = ObsidianNavy,
    primaryContainer = GoldLight,
    onPrimaryContainer = GoldGradientEnd,
    secondary = ObsidianNavy,
    onSecondary = Color.White,
    secondaryContainer = SlateNavy,
    onSecondaryContainer = Color.White,
    tertiary = MintDark,
    onTertiary = Color.White,
    background = CanvasBackground,
    onBackground = ObsidianNavy,
    surface = CardWhite,
    onSurface = ObsidianNavy,
    surfaceVariant = CardWarm,
    onSurfaceVariant = SlateGray,
    outline = GoldBorder,
    outlineVariant = BorderSubtle,
    error = CrimsonRed,
    onError = Color.White
)

@Composable
fun HashGridTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HashGridColorScheme,
        typography = Typography,
        content = content
    )
}
