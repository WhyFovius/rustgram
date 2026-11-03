package com.rustgram.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rustgram.app.NativeBridge
import com.rustgram.app.ui.components.ExpressiveAvatar
import com.rustgram.app.ui.theme.TelegramBlue

/**
 * Authentic Telegram profile screen (exteraGram layout):
 * plain surface, hero avatar, bold name, quick action circle buttons,
 * grouped info sections, divider rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    chat: ChatItem,
    onBack: () -> Unit
) {
    LaunchedEffect(chat.id) {
        NativeBridge.getUser(chat.id)
        NativeBridge.getChat(chat.id)
    }

    var isNotificationsEnabled by remember { mutableStateOf(true) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Изменить")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Hero avatar
            ExpressiveAvatar(
                name = chat.name,
                initials = chat.initials,
                tintIndex = chat.tint,
                isOnline = chat.online,
                photoPath = chat.photoPath,
                size = 120.dp
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = chat.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (chat.online) "в сети" else "был(а) недавно",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = if (chat.online) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Telegram quick action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TgProfileAction(icon = Icons.Outlined.ChatBubbleOutline, label = "Сообщение") { onBack() }
                TgProfileAction(icon = Icons.Outlined.Call, label = "Звонок") {}
                TgProfileAction(icon = Icons.Outlined.Videocam, label = "Видео") {}
                TgProfileAction(
                    icon = if (isNotificationsEnabled) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                    label = if (isNotificationsEnabled) "Звук" else "Без звука"
                ) { isNotificationsEnabled = !isNotificationsEnabled }
            }

            Spacer(Modifier.height(28.dp))

            // Info section
            Text(
                "Информация",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TelegramBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )

            TgInfoCard {
                TgInfoRow(
                    icon = Icons.Outlined.Call,
                    title = "+7 900 123-45-67",
                    subtitle = "Мобильный"
                )
                Divider()
                TgInfoRow(
                    icon = Icons.Outlined.AlternateEmail,
                    title = "@${chat.name.filter { it.isLetterOrDigit() }.lowercase().take(16)}",
                    subtitle = "Имя пользователя"
                )
                Divider()
                TgInfoRow(
                    icon = Icons.Outlined.Info,
                    title = "Использую Rustgram на TDLib 🦀",
                    subtitle = "О себе"
                )
            }

            Spacer(Modifier.height(16.dp))

            // Shared media
            Text(
                "Медиа",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TelegramBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )

            TgInfoCard {
                TgInfoRow(icon = Icons.Outlined.Image, title = "Медиа и файлы", trailing = "24")
                Divider()
                TgInfoRow(icon = Icons.Outlined.FavoriteBorder, title = "Реакции", trailing = "6")
                Divider()
                TgInfoRow(icon = Icons.Outlined.Link, title = "Ссылки", trailing = "15")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TgProfileAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = TelegramBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = TelegramBlue, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontSize = 12.sp)
    }
}

@Composable
private fun TgInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun TgInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        if (trailing != null) {
            Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
