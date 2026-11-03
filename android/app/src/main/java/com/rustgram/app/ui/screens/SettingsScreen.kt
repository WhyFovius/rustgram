package com.rustgram.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rustgram.app.ui.components.ExpressiveAvatar
import com.rustgram.app.ui.components.expressiveBouncyClick
import java.io.File

enum class SettingsSubscreen {
    Main,
    Appearance,
    Storage,
    Privacy,
    Notifications
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    useDynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    cacheDir: File?
) {
    var currentSubscreen by remember { mutableStateOf(SettingsSubscreen.Main) }
    var cacheClearedMessage by remember { mutableStateOf(false) }

    when (currentSubscreen) {
        SettingsSubscreen.Appearance -> {
            AppearanceSubscreen(
                onBack = { currentSubscreen = SettingsSubscreen.Main },
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                useDynamicColor = useDynamicColor,
                onToggleDynamicColor = onToggleDynamicColor
            )
        }
        SettingsSubscreen.Storage -> {
            StorageSubscreen(
                onBack = { currentSubscreen = SettingsSubscreen.Main },
                cacheDir = cacheDir,
                onCacheCleared = { cacheClearedMessage = true }
            )
        }
        SettingsSubscreen.Privacy -> {
            SimpleSettingsSubscreen(
                title = "Конфиденциальность",
                description = "Шифрование сессий: MTProto 2.0\nДвухфакторная аутентификация: Управляется через ядро Telegram\nЛокальное хранилище: Защищено шифрованием базы данных TDLib",
                onBack = { currentSubscreen = SettingsSubscreen.Main }
            )
        }
        SettingsSubscreen.Notifications -> {
            SimpleSettingsSubscreen(
                title = "Уведомления",
                description = "Push-first архитектура Telegram активна.\nУведомления личных чатов: Включены\nУведомления каналов: Включены\nЗвуки и вибрация: M3 Haptic Feedback",
                onBack = { currentSubscreen = SettingsSubscreen.Main }
            )
        }
        SettingsSubscreen.Main -> {
            MainSettingsList(
                onNavigate = { currentSubscreen = it },
                onLogout = onLogout
            )
        }
    }
}

@Composable
fun MainSettingsList(
    onNavigate: (SettingsSubscreen) -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveAvatar(
                        name = "Telegram User",
                        initials = "TG",
                        tintIndex = 0,
                        isOnline = true,
                        size = 60.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Telegram Аккаунт",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "TDLib 1.8.61 Native Core",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            "Сессия активна",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.Palette,
                title = "Оформление (M3 Expressive)",
                subtitle = "Светлая/Тёмная тема, Material You палитра",
                onClick = { onNavigate(SettingsSubscreen.Appearance) }
            )
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.DataUsage,
                title = "Данные и память",
                subtitle = "Кэш TDLib, загруженные медиа и очистка",
                onClick = { onNavigate(SettingsSubscreen.Storage) }
            )
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.Lock,
                title = "Конфиденциальность и безопасность",
                subtitle = "Пароль 2FA, шифрование MTProto",
                onClick = { onNavigate(SettingsSubscreen.Privacy) }
            )
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.Notifications,
                title = "Уведомления и звуки",
                subtitle = "Push-first фон, виброотклик",
                onClick = { onNavigate(SettingsSubscreen.Notifications) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .expressiveBouncyClick(pressedScale = 0.96f) { onLogout() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выйти из аккаунта", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .expressiveBouncyClick(pressedScale = 0.98f) { onClick() }
    ) {
        ListItem(
            leadingContent = {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            headlineContent = {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            },
            supportingContent = {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingContent = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSubscreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    useDynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = { Text("Оформление", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                ListItem(
                    headlineContent = { Text("Тёмная тема", fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Переключение темы приложения") },
                    trailingContent = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onToggleDarkTheme
                        )
                    }
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                ListItem(
                    headlineContent = { Text("Material You (Dynamic Color)", fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Адаптировать цвета под обои устройства Android") },
                    trailingContent = {
                        Switch(
                            checked = useDynamicColor,
                            onCheckedChange = onToggleDynamicColor
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSubscreen(
    onBack: () -> Unit,
    cacheDir: File?,
    onCacheCleared: () -> Unit
) {
    var cacheSizeMb by remember {
        mutableStateOf(
            cacheDir?.walkTopDown()?.filter { it.isFile }?.map { it.length() }?.sum()?.let {
                it / (1024 * 1024)
            } ?: 0L
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = { Text("Данные и память", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Использование диска", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Локальный кэш файлов TDLib: $cacheSizeMb МБ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    cacheDir?.listFiles()?.forEach { file ->
                        if (file.name != "tdlib.binlog") {
                            file.deleteRecursively()
                        }
                    }
                    cacheSizeMb = 0L
                    onCacheCleared()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Очистить кэш медиа", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSettingsSubscreen(
    title: String,
    description: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = { Text(title, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}
