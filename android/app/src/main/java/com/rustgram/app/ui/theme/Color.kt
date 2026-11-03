package com.rustgram.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Material 3 Expressive Tonal Palettes
val PrimaryLight = Color(0xFF00639B)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFCEE5FF)
val OnPrimaryContainerLight = Color(0xFF001D33)

val SecondaryLight = Color(0xFF51606F)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD4E4F6)
val OnSecondaryContainerLight = Color(0xFF0D1D2A)

val TertiaryLight = Color(0xFF67587A)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFEEDBFF)
val OnTertiaryContainerLight = Color(0xFF221533)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFF7F9FF)
val OnBackgroundLight = Color(0xFF181C20)
val SurfaceLight = Color(0xFFF7F9FF)
val OnSurfaceLight = Color(0xFF181C20)
val SurfaceVariantLight = Color(0xFFDEE3EB)
val OnSurfaceVariantLight = Color(0xFF42474E)

val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF1F4FA)
val SurfaceContainerLight = Color(0xFFEBEFF4)
val SurfaceContainerHighLight = Color(0xFFE5E9EF)
val SurfaceContainerHighestLight = Color(0xFFDFE3E9)

val OutlineLight = Color(0xFF72777F)
val OutlineVariantLight = Color(0xFFC2C7CF)

// Dark Theme Palettes
val PrimaryDark = Color(0xFF96CCFF)
val OnPrimaryDark = Color(0xFF003353)
val PrimaryContainerDark = Color(0xFF004B76)
val OnPrimaryContainerDark = Color(0xFFCEE5FF)

val SecondaryDark = Color(0xFFB8C8DA)
val OnSecondaryDark = Color(0xFF233240)
val SecondaryContainerDark = Color(0xFF394957)
val OnSecondaryContainerDark = Color(0xFFD4E4F6)

val TertiaryDark = Color(0xFFD2BFE6)
val OnTertiaryDark = Color(0xFF382A49)
val TertiaryContainerDark = Color(0xFF4F4061)
val OnTertiaryContainerDark = Color(0xFFEEDBFF)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF101418)
val OnBackgroundDark = Color(0xFFDFE3E9)
val SurfaceDark = Color(0xFF101418)
val OnSurfaceDark = Color(0xFFDFE3E9)
val SurfaceVariantDark = Color(0xFF42474E)
val OnSurfaceVariantDark = Color(0xFFC2C7CF)

val SurfaceContainerLowestDark = Color(0xFF0B0E12)
val SurfaceContainerLowDark = Color(0xFF181C20)
val SurfaceContainerDark = Color(0xFF1C2024)
val SurfaceContainerHighDark = Color(0xFF272A2E)
val SurfaceContainerHighestDark = Color(0xFF31353A)

val OutlineDark = Color(0xFF8C9199)
val OutlineVariantDark = Color(0xFF42474E)

// Online Indicator Green
val OnlineGreen = Color(0xFF2ECC71)

// Expressive Gradient Accents for Avatar Tints (0 to 5)
val AvatarGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF1565C0))), // 0: Blue Ocean
    Brush.linearGradient(listOf(Color(0xFF26A69A), Color(0xFF00897B))), // 1: Emerald Mint
    Brush.linearGradient(listOf(Color(0xFFFFA726), Color(0xFFEF6C00))), // 2: Sunset Amber
    Brush.linearGradient(listOf(Color(0xFFAB47BC), Color(0xFF7B1FA2))), // 3: Amethyst Violet
    Brush.linearGradient(listOf(Color(0xFFEC407A), Color(0xFFC2185B))), // 4: Rose Coral
    Brush.linearGradient(listOf(Color(0xFF26C6DA), Color(0xFF0097A7)))  // 5: Cyan Lagoon
)
