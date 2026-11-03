package com.rustgram.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rustgram.app.ui.theme.MotionSprings
import com.rustgram.app.ui.theme.TelegramBlue

data class NavigationTabItem(
    val index: Int,
    val icon: ImageVector,
    val label: String
)

/**
 * exteraGram Pill Stack — floating bottom navigation pill.
 * Telegram-blue selected bubble, translucent surface, spring physics.
 */
@Composable
fun FloatingDockBar(
    tabs: List<NavigationTabItem>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = if (dark) Color(0xCC1C2733) else Color(0xCCFFFFFF),
            shadowElevation = 10.dp,
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedIndex == tab.index
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val scale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 0.90f
                            isSelected -> 1.0f
                            else -> 1.0f
                        },
                        animationSpec = MotionSprings.fastSpatial,
                        label = "dock_tab_scale"
                    )

                    val pillColor by animateColorAsState(
                        targetValue = if (isSelected) TelegramBlue else Color.Transparent,
                        label = "dock_tab_color"
                    )

                    Surface(
                        shape = if (isSelected) RoundedCornerShape(20.dp) else CircleShape,
                        color = pillColor,
                        modifier = Modifier
                            .scale(scale)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelectTab(tab.index) }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = if (isSelected) 14.dp else 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
