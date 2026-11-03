package com.rustgram.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rustgram.app.ui.theme.MotionSprings

/**
 * An M3 Expressive Morphing Button that seamlessly transforms into a Loading Hexagon / Pill
 * on click with organic spatial spring physics.
 */
@Composable
fun MorphingExpressiveButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spatial Spring scale bounce
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading && enabled) 0.95f else 1.0f,
        animationSpec = MotionSprings.fastSpatial,
        label = "btn_scale"
    )

    // Corner radius morphing (changes curvature when loading)
    val cornerRadius by animateDpAsState(
        targetValue = if (isLoading) (height / 2) else 16.dp,
        animationSpec = MotionSprings.fastDpSpatial,
        label = "btn_corner"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.4f),
        contentColor = contentColor,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn(animationSpec = MotionSprings.fastEffects),
                exit = fadeOut(animationSpec = MotionSprings.fastEffects)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(animationSpec = MotionSprings.fastEffects),
                exit = fadeOut(animationSpec = MotionSprings.fastEffects)
            ) {
                M3HexagonLoadingIndicator(
                    size = 36.dp,
                    contained = false,
                    color = contentColor
                )
            }
        }
    }
}
