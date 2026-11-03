package com.rustgram.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.rustgram.app.ui.theme.AvatarGradients
import com.rustgram.app.ui.theme.MotionSprings
import com.rustgram.app.ui.theme.OnlineGreen
import java.io.File

/**
 * Interactive Avatar with M3 Expressive tactile spring physics and real photo support:
 * If photoPath is present and exists on disk, renders the real TDLib profile image via Coil.
 * Otherwise, falls back to the gradient monogram.
 */
@Composable
fun ExpressiveAvatar(
    name: String,
    initials: String,
    tintIndex: Int,
    isOnline: Boolean,
    photoPath: String? = null,
    size: Dp = 54.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = MotionSprings.fastSpatial,
        label = "avatar_scale"
    )

    val cornerRadius by animateFloatAsState(
        targetValue = if (isPressed) 18f else (size.value / 2f),
        animationSpec = MotionSprings.fastSpatial,
        label = "avatar_corner_morph"
    )

    val gradient = AvatarGradients.getOrElse(tintIndex % AvatarGradients.size) { AvatarGradients[0] }
    val displayText = if (initials.isNotBlank()) initials.take(2).uppercase() else name.take(1).uppercase()

    val hasRealPhoto = photoPath != null && photoPath.isNotBlank() && File(photoPath).exists()

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        if (hasRealPhoto) {
            val context = LocalContext.current
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(File(photoPath!!))
                    .crossfade(true)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.30f)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(100))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .size(size * 0.22f)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(100))
                        .background(OnlineGreen)
                )
            }
        }
    }
}
