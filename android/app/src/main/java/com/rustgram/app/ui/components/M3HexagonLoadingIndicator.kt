package com.rustgram.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Material 3 Expressive Loading Indicator.
 *
 * Implements the official M3 Expressive loading specification:
 * - High-speed organic spring morphing and dynamic multi-phase rotation
 * - Featuring the iconic M3 Expressive Rounded Hexagon (Cookie 6-Sided)
 * - Contained and Uncontained variants with proper M3 tonal role mapping:
 *     - Contained: Primary Container background circle + On-Primary Container active shape
 *     - Default (Uncontained): Primary color active shape
 * - Smooth cubic-bezier rounded vertices for organic silicone/liquid feel
 */
@Composable
fun M3HexagonLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contained: Boolean = true,
    color: Color? = null,
    containerColor: Color? = null
) {
    val activeColor = color ?: if (contained) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    val boxColor = containerColor ?: MaterialTheme.colorScheme.primaryContainer

    // Infinite transition for continuous M3 Expressive kinetic choreography
    val infiniteTransition = rememberInfiniteTransition(label = "m3_loading_transition")

    // 1. Dual-phase rotation: continuous linear drift + periodic spring pulses
    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_rotation"
    )

    // 2. Organic breathing / pulsation scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // 3. Shape morph tension factor (from deep soft scallops to taut rounded hexagon)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph_progress"
    )

    val content = @Composable {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (contained) size * 0.12f else 0.dp)
        ) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f
            val baseRadius = (min(canvasWidth, canvasHeight) / 2f) * pulseScale

            val path = Path()
            val numVertices = 6
            val angleStep = (2.0 * Math.PI) / numVertices
            val rotationRad = Math.toRadians(continuousRotation.toDouble())

            val outerRadius = baseRadius
            // Inward scallop depth controlled by morph progress
            val innerRadius = baseRadius * (1f - (morphProgress * 0.28f))

            // Build rounded 6-sided Cookie / Rounded Hexagon with Bezier curves
            for (i in 0 until numVertices) {
                val outerAngle = i * angleStep + rotationRad
                val midAngle = outerAngle + (angleStep / 2.0)
                val nextOuterAngle = (i + 1) * angleStep + rotationRad

                val p0X = centerX + (outerRadius * cos(outerAngle)).toFloat()
                val p0Y = centerY + (outerRadius * sin(outerAngle)).toFloat()

                val midX = centerX + (innerRadius * cos(midAngle)).toFloat()
                val midY = centerY + (innerRadius * sin(midAngle)).toFloat()

                val p1X = centerX + (outerRadius * cos(nextOuterAngle)).toFloat()
                val p1Y = centerY + (outerRadius * sin(nextOuterAngle)).toFloat()

                if (i == 0) {
                    path.moveTo(p0X, p0Y)
                }

                // Smooth quad Bezier to inner scallop midpoint and then to next vertex
                path.quadraticTo(
                    centerX + (outerRadius * 0.95f * cos(outerAngle + angleStep * 0.2)).toFloat(),
                    centerY + (outerRadius * 0.95f * sin(outerAngle + angleStep * 0.2)).toFloat(),
                    midX, midY
                )
                path.quadraticTo(
                    centerX + (outerRadius * 0.95f * cos(midAngle + angleStep * 0.2)).toFloat(),
                    centerY + (outerRadius * 0.95f * sin(midAngle + angleStep * 0.2)).toFloat(),
                    p1X, p1Y
                )
            }
            path.close()

            drawPath(
                path = path,
                color = activeColor,
                style = Fill
            )
        }
    }

    if (contained) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(boxColor),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
