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
    Notifications,
    ExteraFeatures,
    About
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

    // Advanced exteraGram style customization states
    var bubbleCornersRadius by remember { mutableFloatStateOf(16f) }
    var showSecondsInTime by remember { mutableStateOf(false) }
    var blurFloatingDock by remember { mutableStateOf(true) }
    var autoDownloadMedia by remember { mutableStateOf(true) }
    var saveToGallery by remember { mutableStateOf(false) }

    when (currentSubscreen) {
        SettingsSubscreen.Appearance -> {
            AppearanceSubscreen(
                onBack = { currentSubscreen = SettingsSubscreen.Main },
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                useDynamicColor = useDynamicColor,
                onToggleDynamicColor = onToggleDynamicColor,
                bubbleRadius = bubbleCornersRadius,
                onBubbleRadiusChange = { bubbleCornersRadius = it },
                blurFloating = blurFloatingDock,
                onToggleBlurFloating = { blurFloatingDock = it }
            )
        }
        SettingsSubscreen.Storage -> {
            StorageSubscreen(
                onBack = { currentSubscreen = SettingsSubscreen.Main },
                cacheDir = cacheDir,
                autoDownload = autoDownloadMedia,
                onToggleAutoDownload = { autoDownloadMedia = it },
                saveGallery = saveToGallery,
                onToggleSaveGallery = { saveToGallery = it },
                onCacheCleared = { cacheClearedMessage = true }
            )
        }
        SettingsSubscreen.Privacy -> {
            SimpleSettingsSubscreen(
                title = "Конфиденциальность",
                description = "• Сквозное шифрование MTProto 2.0 активно\n• Двухфакторная аутентификация (2FA): Поддерживается\n• Локальная база данных: Защищена SQLite cipher TDLib\n• Скрытие номера: По умолчанию включено для незнакомых контактов\n• Защита от пересылки сообщений: Включена",
                onBack = { currentSubscreen = SettingsSubscreen.Main }
            )
        }
        SettingsSubscreen.Notifications -> {
            SimpleSettingsSubscreen(
                title = "Уведомления и звуки",
                description = "• Push-first архитектура Telegram активна\n• Уведомления личных чатов: Включены\n• Уведомления каналов и групп: Включены\n• Тактильный отклик (Haptics): M3 Expressive Spring tactile feedback",
                onBack = { currentSubscreen = SettingsSubscreen.Main }
            )
        }
        SettingsSubscreen.ExteraFeatures -> {
            SimpleSettingsSubscreen(
                title = "Функции exteraGram",
                description = "• Парящий Dock (Floating Pill Stack): Включен\n• M3 Hexagon анимация загрузки: Активна\n• Моне-палитра (Android 12+ Monet): Активна\n• Поддержка расширенных эмодзи и стикеров: Включена\n• Rust Tokio Async Core: 0.3.1",
                onBack = { currentSubscreen = SettingsSubscreen.Main }
            )
        }
        SettingsSubscreen.About -> {
            SimpleSettingsSubscreen(
                title = "О программе Rustgram",
                description = "Rustgram v0.3.1-beta\n\nВысокопроизводительный нативный Android-клиент Telegram, разработанный на связке Rust Tokio Core, официального TDLib 1.8.61 и Jetpack Compose Material 3 Expressive.\n\nДизайн вдохновлен exteraGram, BlueprintDsgn и Material You.",
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
                        size = 64.dp
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
                            "Сессия защищена MTProto",
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
                title = "Оформление (exteraGram & M3)",
                subtitle = "Темы, скругления пузырей, Material You",
                onClick = { onNavigate(SettingsSubscreen.Appearance) }
            )
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.DataUsage,
                title = "Данные и память",
                subtitle = "Автозагрузка, кэш TDLib и медиафайлы",
                onClick = { onNavigate(SettingsSubscreen.Storage) }
            )
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.Lock,
                title = "Конфиденциальность и безопасность",
                subtitle = "Пароль 2FA, скрытие номера, шифрование",
                onClick = { onNavigate(SettingsSubscreen.Privacy) }
            )
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.Notifications,
                title = "Уведомления и звуки",
                subtitle = "Push-уведомления, тактильный отклик",
                onClick = { onNavigate(SettingsSubscreen.Notifications) }
            )
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.AutoAwesome,
                title = "Возможности exteraGram",
                subtitle = "Pill Stack, плавающий док, кастомные иконки",
                onClick = { onNavigate(SettingsSubscreen.ExteraFeatures) }
            )
        }

        item {
            SettingsCard(
                icon = Icons.Outlined.Info,
                title = "О программе",
                subtitle = "Версия 0.3.1-beta, Rust Tokio ядро",
                onClick = { onNavigate(SettingsSubscreen.About) }
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
            Spacer(modifier = Modifier.height(80.dp))
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
    onToggleDynamicColor: (Boolean) -> Unit,
    bubbleRadius: Float,
    onBubbleRadiusChange: (Float) -> Unit,
    blurFloating: Boolean,
    onToggleBlurFloating: (Boolean) -> Unit
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
                Column {
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ListItem(
                        headlineContent = { Text("Парящий Dock (Floating Pill)", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Полупрозрачный M3 эффект для нижней панели") },
                        trailingContent = {
                            Switch(
                                checked = blurFloating,
                                onCheckedChange = onToggleBlurFloating
                            )
                        }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Скругление сообщений: ${bubbleRadius.toInt()} dp", fontWeight = FontWeight.Medium)
                    Slider(
                        value = bubbleRadius,
                        onValueChange = onBubbleRadiusChange,
                        valueRange = 8f..28f,
                        steps = 5
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSubscreen(
    onBack: () -> Unit,
    cacheDir: File?,
    autoDownload: Boolean,
    onToggleAutoDownload: (Boolean) -> Unit,
    saveGallery: Boolean,
    onToggleSaveGallery: (Boolean) -> Unit,
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
                Column {
                    ListItem(
                        headlineContent = { Text("Автозагрузка медиа", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Загружать фото и файлы автоматически") },
                        trailingContent = {
                            Switch(
                                checked = autoDownload,
                                onCheckedChange = onToggleAutoDownload
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ListItem(
                        headlineContent = { Text("Сохранять в галерею", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Копировать загруженные фото в хранилище устройства") },
                        trailingContent = {
                            Switch(
                                checked = saveGallery,
                                onCheckedChange = onToggleSaveGallery
                            )
                        }
                    )
                }
            }

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
