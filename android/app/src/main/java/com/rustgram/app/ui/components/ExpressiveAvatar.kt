package com.rustgram.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.rustgram.app.ui.theme.MotionSprings
import com.rustgram.app.ui.theme.OnlineGreen
import com.rustgram.app.ui.theme.TelegramAvatarColors
import java.io.File

/**
 * Authentic Telegram-style avatar: round, solid monogram colors,
 * real photo from TDLib cache when available.
 * Springy press reaction (exteraGram tactile feel).
 */
@Composable
fun ExpressiveAvatar(
    name: String,
    initials: String,
    tintIndex: Int,
    isOnline: Boolean = false,
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

    val color = TelegramAvatarColors[
        ((if (tintIndex < 0) -tintIndex else tintIndex)) % TelegramAvatarColors.size
    ]
    val displayText = if (initials.isNotBlank()) initials.take(2).uppercase() else name.take(1).uppercase()
    val hasRealPhoto = !photoPath.isNullOrBlank() && File(photoPath).exists()

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
                    .data(File(photoPath))
                    .crossfade(true)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayText,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = (size.value * 0.40f).sp
                )
            }
        }

        if (isOnline) {
            // Telegram online indicator: white ring + green dot
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * 0.06f)
                        .clip(CircleShape)
                        .background(OnlineGreen)
                )
            }
        }
    }
}
