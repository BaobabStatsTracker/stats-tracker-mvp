package com.example.statstracker.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Surface Hierarchy (Tonal Layers) ──
val Surface = Color(0xFFF8F9FB)
val SurfaceContainerLow = Color(0xFFF2F4F6)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerHigh = Color(0xFFE6E8EA)
val SurfaceContainerHighest = Color(0xFFDCDEE0)
val SurfaceBright = Color(0xFFFCFDFE)
val SurfaceVariant = Color(0xFFE0E3E5)

// ── Primary (Blue) ──
val Primary = Color(0xFF004BCA)
val PrimaryContainer = Color(0xFFDBE1FF)
val PrimaryAccent = Color(0xFF0061FF)
val OnPrimary = Color(0xFFFFFFFF)
val OnPrimaryContainer = Color(0xFF001A4D)

// ── Secondary (Blaze Orange) ──
val Secondary = Color(0xFFFE6B00)
val SecondaryContainer = Color(0xFFFFDCC2)
val OnSecondary = Color(0xFFFFFFFF)
val OnSecondaryContainer = Color(0xFF572000)

// ── Tertiary ──
val Tertiary = Color(0xFF006E2C)
val TertiaryContainer = Color(0xFF95F6AA)
val OnTertiaryContainer = Color(0xFF002109)

// ── Text & Content ──
val OnSurface = Color(0xFF191C1E)
val OnSurfaceVariant = Color(0xFF43474E)

// ── Outline ──
val OutlineColor = Color(0xFF73777F)
val OutlineVariantColor = Color(0xFFC2C6D9)

// ── Error ──
val Error = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val OnError = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF410002)

// ── Semantic Game Colors ──
data class AppColors(
    val madeShot: Color = Color(0xFF2E7D32),
    val missedShot: Color = Color(0xFFC62828),
    val timerRunning: Color = Color(0xFF2E7D32),
    val timerPaused: Color = Color(0xFFC62828),
    val twoPointer: Color = Color(0xFF2E7D32),
    val threePointer: Color = Primary,
    val freeThrow: Color = Secondary,
    val playerHighlight: Color = Color(0xFFFFD600),
    val positivePoints: Color = Color(0xFF2E7D32)
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }