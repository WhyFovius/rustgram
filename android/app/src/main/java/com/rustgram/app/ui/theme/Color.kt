package com.rustgram.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================
// Authentic Telegram / exteraGram palette (design by exteraGram)
// ============================================================

// ---------- Light (Telegram "Day") ----------
val TelegramBlue = Color(0xFF3390EC)
val PrimaryLight = TelegramBlue
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF3390EC)
val OnPrimaryContainerLight = Color(0xFFFFFFFF)

val SecondaryLight = Color(0xFF4FAE4E) // Telegram green checks
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE3F2FD)
val OnSecondaryContainerLight = Color(0xFF0D47A1)

val TertiaryLight = Color(0xFF3390EC)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFE3F2FD)
val OnTertiaryContainerLight = Color(0xFF0D47A1)

val ErrorLight = Color(0xFFE53935)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFEBEE)
val OnErrorContainerLight = Color(0xFFB71C1C)

val BackgroundLight = Color(0xFFFFFFFF)
val OnBackgroundLight = Color(0xFF000000)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF000000)
val SurfaceVariantLight = Color(0xFFF2F4F7)
val OnSurfaceVariantLight = Color(0xFF999999)

val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFFAFAFA)
val SurfaceContainerLight = Color(0xFFF2F4F7)
val SurfaceContainerHighLight = Color(0xFFEBEBEB)
val SurfaceContainerHighestLight = Color(0xFFE6E6E6)

val OutlineLight = Color(0xFFC8C8C8)
val OutlineVariantLight = Color(0xFFE6E6E6)

// Telegram Day chat wallpaper tone
val ChatWallpaperLight = Color(0xFFD6E3F0)
val IncomingBubbleLight = Color(0xFFFFFFFF)
val OutgoingBubbleLight = Color(0xFFEFFDDE)
val OutgoingCheckLight = Color(0xFF4FAE4E)
val MutedBadgeLight = Color(0xFFC6C8CA)

// ---------- Dark (Telegram "Night") ----------
val PrimaryDark = Color(0xFF3390EC)
val OnPrimaryDark = Color(0xFFFFFFFF)
val PrimaryContainerDark = Color(0xFF2B5278)
val OnPrimaryContainerDark = Color(0xFFFFFFFF)

val SecondaryDark = Color(0xFF66BB6A)
val OnSecondaryDark = Color(0xFF000000)
val SecondaryContainerDark = Color(0xFF1C2B35)
val OnSecondaryContainerDark = Color(0xFF8CB8D8)

val TertiaryDark = Color(0xFF3390EC)
val OnTertiaryDark = Color(0xFFFFFFFF)
val TertiaryContainerDark = Color(0xFF1C2B35)
val OnTertiaryContainerDark = Color(0xFF8CB8D8)

val ErrorDark = Color(0xFFEF5350)
val OnErrorDark = Color(0xFFFFFFFF)
val ErrorContainerDark = Color(0xFF3A1C1E)
val OnErrorContainerDark = Color(0xFFFFCDD2)

val BackgroundDark = Color(0xFF17212B)
val OnBackgroundDark = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF17212B)
val OnSurfaceDark = Color(0xFFFFFFFF)
val SurfaceVariantDark = Color(0xFF232E3C)
val OnSurfaceVariantDark = Color(0xFF74808F)

val SurfaceContainerLowestDark = Color(0xFF0E1621)
val SurfaceContainerLowDark = Color(0xFF17212B)
val SurfaceContainerDark = Color(0xFF1C2733)
val SurfaceContainerHighDark = Color(0xFF232E3C)
val SurfaceContainerHighestDark = Color(0xFF2B3742)

val OutlineDark = Color(0xFF334455)
val OutlineVariantDark = Color(0xFF232E3C)

// Telegram Night chat tones
val ChatWallpaperDark = Color(0xFF0E1621)
val IncomingBubbleDark = Color(0xFF182533)
val OutgoingBubbleDark = Color(0xFF2B5278)
val MutedBadgeDark = Color(0xFF4A5B67)

// Online Indicator Green (Telegram exact)
val OnlineGreen = Color(0xFF2ECC71)
val UnreadBadge = TelegramBlue

// ---------- Authentic Telegram avatar palette ----------
// Solid monogram colors exactly like Telegram clients
val TelegramAvatarColors = listOf(
    Color(0xFFE17076), // red
    Color(0xFF7BC862), // green
    Color(0xFF65AADD), // blue
    Color(0xFFA695E7), // violet
    Color(0xFFEE7B6E), // coral (exteraGram accent)
    Color(0xFFFFA726), // orange
    Color(0xFFF598A7), // pink
    Color(0xFF6EC9CB)  // cyan
)

// Kept for compatibility with ExpressiveAvatar (solid Telegram colors)
val AvatarGradients = TelegramAvatarColors.map { Brush.linearGradient(listOf(it, it)) }

// exteraGram brand gradient for accents
val ExteraGradient = Brush.horizontalGradient(
    listOf(Color(0xFF3390EC), Color(0xFF5AA9F5))
)
