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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rustgram.app.NativeBridge
import com.rustgram.app.ui.components.ExpressiveAvatar
import com.rustgram.app.ui.components.expressiveBouncyClick

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

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = { Text("Информация", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Share, contentDescription = "Поделиться")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Ещё")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Large Hero Avatar
            ExpressiveAvatar(
                name = chat.name,
                initials = chat.initials,
                tintIndex = chat.tint,
                isOnline = chat.online,
                photoPath = chat.photoPath,
                size = 110.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = chat.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (chat.online) "в сети" else "был(а) недавно",
                style = MaterialTheme.typography.bodyMedium,
                color = if (chat.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Action Buttons (Call, Mute, Search, More)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileActionButton(icon = Icons.Outlined.Call, label = "Звонок") {}
                ProfileActionButton(icon = Icons.Outlined.Videocam, label = "Видео") {}
                ProfileActionButton(
                    icon = if (isNotificationsEnabled) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                    label = if (isNotificationsEnabled) "Звук" else "Без звука"
                ) {
                    isNotificationsEnabled = !isNotificationsEnabled
                }
                ProfileActionButton(icon = Icons.Outlined.Search, label = "Поиск") {}
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Detailed Info Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "О пользователе",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        ProfileInfoRow(
                            title = "+7 (900) 123-45-67",
                            subtitle = "Телефон"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        ProfileInfoRow(
                            title = "@${chat.name.filter { it.isLetterOrDigit() }.lowercase()}",
                            subtitle = "Имя пользователя"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        ProfileInfoRow(
                            title = "Hey there! I am using Rustgram on official TDLib 🦀⚡",
                            subtitle = "О себе"
                        )
                    }
                }

                // Shared Media Tabs
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text("Медиа", fontWeight = FontWeight.SemiBold) },
                            trailingContent = { Text("24", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text("Файлы", fontWeight = FontWeight.SemiBold) },
                            trailingContent = { Text("8", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text("Ссылки", fontWeight = FontWeight.SemiBold) },
                            trailingContent = { Text("15", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .expressiveBouncyClick(pressedScale = 0.90f) { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProfileInfoRow(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
