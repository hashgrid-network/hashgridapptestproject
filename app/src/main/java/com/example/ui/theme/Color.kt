package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// HashGrid Luxury Champagne Gold, Warm Cream & Obsidian Palette
val GoldGradientStart = Color(0xFFE6CA65)
val GoldGradientMid = Color(0xFFD4AF37)
val GoldGradientEnd = Color(0xFFB8860B)
val GoldLight = Color(0xFFFDF8EE)
val GoldBorder = Color(0x66D4AF37)

// Subtle warm gold border matching border-amber-200/40
val GoldBorderSubtle = Color(0xFFFDE68A).copy(alpha = 0.40f)

// Soft cream background (#F8F9FA)
val CanvasBackground = Color(0xFFF8F9FA)
val CanvasBackgroundAlt = Color(0xFFF1F3F5)
val CardWhite = Color(0xFFFFFFFF)
val CardWarm = Color(0xFFFCFBF9)

val ObsidianNavy = Color(0xFF0B0E14) // Dark Navy
val SlateNavy = Color(0xFF181F2C)
val SlateGray = Color(0xFF64748B)
val SlateLight = Color(0xFF94A3B8)
val BorderSubtle = Color(0xFFE2E8F0)

val MintGreen = Color(0xFF00FFA3) // Electric Mint
val MintGreenGlow = Color(0x3300FFA3)
val MintDark = Color(0xFF059669)

val CrimsonRed = Color(0xFFFF4D4D)
val CrimsonLight = Color(0xFFFFF0F0)

val GoldBrush = Brush.horizontalGradient(
    listOf(GoldGradientStart, GoldGradientMid, GoldGradientEnd)
)

val GoldVerticalBrush = Brush.verticalGradient(
    listOf(GoldGradientStart, GoldGradientMid, GoldGradientEnd)
)

val ObsidianBrush = Brush.linearGradient(
    listOf(ObsidianNavy, SlateNavy)
)

val ArcticGlowBrush = Brush.linearGradient(
    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
)
