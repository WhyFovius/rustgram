package com.rustgram.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

// M3 Expressive Motion Specs
object MotionSprings {
    // Spatial (Float properties: scale, progress, alpha-bounce)
    val fastSpatial = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 800f
    )
    val defaultSpatial = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 380f
    )
    val slowSpatial = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 200f
    )

    // IntOffset Spatial (for slideIn / slideOut navigation transitions)
    val fastOffsetSpatial = spring<IntOffset>(
        dampingRatio = 0.6f,
        stiffness = 800f
    )
    val defaultOffsetSpatial = spring<IntOffset>(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

    // IntSize Spatial (for animateContentSize)
    val fastSizeSpatial = spring<IntSize>(
        dampingRatio = 0.6f,
        stiffness = 800f
    )
    val defaultSizeSpatial = spring<IntSize>(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

    // Effects (Color, Opacity, Alpha) -> No overshoot, damping = 1.0f
    val fastEffects = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 3800f
    )
    val defaultEffects = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1600f
    )

    // Dp Spatial (for animateDpAsState)
    val fastDpSpatial = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.6f,
        stiffness = 800f
    )
    val defaultDpSpatial = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.8f,
        stiffness = 380f
    )
}
