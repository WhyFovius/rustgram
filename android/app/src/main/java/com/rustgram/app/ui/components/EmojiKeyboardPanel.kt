package com.rustgram.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rustgram.app.ui.theme.MotionSprings

val POPULAR_TELEGRAM_EMOJIS = listOf(
    "👍", "❤️", "🔥", "🥰", "👏", "😁", "🤔", "🤯", "😱", "🤬",
    "😢", "🎉", "🤩", "🤮", "💩", "🙏", "👌", "🕊️", "🤡", "🥱",
    "🥴", "😍", "🐳", "❤️‍🔥", "🌚", "🌭", "💯", "🤣", "⚡", "🍌",
    "🏆", "💔", "🤨", "😐", "🍓", "🍾", "💋", "🖕", "😈", "😴",
    "😭", "🤓", "👻", "👨‍💻", "👀", "🎃", "🙈", "😇", "😨", "🤝",
    "✍️", "🤗", "🫡", "🎅", "🎄", "☃️", "💅", "🤪", "🗿", "🆒"
)

val ANIMAL_FOOD_EMOJIS = listOf(
    "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
    "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🦆", "🦅",
    "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐",
    "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑",
    "🥦", "🥬", "🥒", "🌶️", "🌽", "🥕", "🧄", "🧅", "🥔", "🍠"
)

val OBJECTS_SYMBOLS_EMOJIS = listOf(
    "🚀", "✈️", "🚗", "🚲", "🏎️", "🛵", "🛸", "⛵", "🛳️", "🛰️",
    "💡", "🔦", "📱", "💻", "⌚", "📷", "🎥", "📺", "📻", "🎙️",
    "💎", "🔔", "🔑", "🛡️", "⚔️", "💣", "🪓", "🧭", "📦", "🏷️",
    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "❤️‍🩹",
    "⭐", "🌟", "✨", "⚡", "💥", "🔥", "🌈", "☀️", "🌙", "☁️"
)

/**
 * Native Telegram Emoji & Sticker Bottom Sheet Panel (exteraGram style).
 */
@Composable
fun EmojiKeyboardPanel(
    visible: Boolean,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableIntStateOf(0) }

    val currentEmojis = remember(selectedCategory) {
        when (selectedCategory) {
            0 -> POPULAR_TELEGRAM_EMOJIS
            1 -> ANIMAL_FOOD_EMOJIS
            else -> OBJECTS_SYMBOLS_EMOJIS
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = MotionSprings.defaultSizeSpatial),
        exit = shrinkVertically(animationSpec = MotionSprings.fastSizeSpatial),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Category header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedCategory == 0,
                        onClick = { selectedCategory = 0 },
                        label = { Text("Популярные") },
                        shape = MaterialTheme.shapes.small
                    )
                    FilterChip(
                        selected = selectedCategory == 1,
                        onClick = { selectedCategory = 1 },
                        label = { Text("Природа") },
                        shape = MaterialTheme.shapes.small
                    )
                    FilterChip(
                        selected = selectedCategory == 2,
                        onClick = { selectedCategory = 2 },
                        label = { Text("Символы") },
                        shape = MaterialTheme.shapes.small
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Emoji Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentEmojis) { emoji ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onEmojiSelected(emoji) }
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 26.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
