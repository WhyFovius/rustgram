package com.rustgram.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
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
import com.rustgram.app.ui.components.ExpressiveAvatar
import com.rustgram.app.ui.theme.TelegramBlue
import java.io.File

enum class SettingsSubscreen {
    Main, Appearance, Storage, Privacy, Notifications, ExteraFeatures, About
}

/** Authentic Telegram-style settings: section labels + card groups + chevrons. */
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

    var bubbleRadius by remember { mutableFloatStateOf(16f) }
    var blurDock by remember { mutableStateOf(true) }
    var autoDownloadMedia by remember { mutableStateOf(true) }
    var saveToGallery by remember { mutableStateOf(false) }
    var showSeconds by remember { mutableStateOf(false) }

    when (currentSubscreen) {
        SettingsSubscreen.Appearance -> TgSettingsScaffold(
            title = "Оформление",
            onBack = { currentSubscreen = SettingsSubscreen.Main }
        ) {
            TgSectionLabel("Тема")
            TgCard {
                TgSwitchRow(icon = Icons.Outlined.DarkMode, title = "Ночной режим", checked = isDarkTheme, onChange = onToggleDarkTheme)
                TgDivider()
                TgSwitchRow(icon = Icons.Outlined.Palette, title = "Material You", subtitle = "Динамические цвета Android 12+", checked = useDynamicColor, onChange = onToggleDynamicColor)
                TgDivider()
                TgSwitchRow(icon = Icons.Outlined.BlurOn, title = "Парящий док", subtitle = "Pill Stack с полупрозрачностью", checked = blurDock, onChange = { blurDock = it })
            }
            TgSectionLabel("Чаты")
            TgCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Радиус пузырей сообщений: ${bubbleRadius.toInt()} dp", fontSize = 15.sp)
                    Slider(value = bubbleRadius, onValueChange = { bubbleRadius = it }, valueRange = 8f..28f, steps = 4, colors = SliderDefaults.colors(thumbColor = TelegramBlue, activeTrackColor = TelegramBlue))
                }
                TgDivider()
                TgSwitchRow(icon = Icons.Outlined.Schedule, title = "Показывать секунды", checked = showSeconds, onChange = { showSeconds = it })
            }
        }

        SettingsSubscreen.Storage -> TgSettingsScaffold(
            title = "Данные и память",
            onBack = { currentSubscreen = SettingsSubscreen.Main }
        ) {
            var cacheSizeMb by remember {
                mutableStateOf(cacheDir?.walkTopDown()?.filter { it.isFile }?.map { it.length() }?.sum()?.let { it / (1024 * 1024) } ?: 0L)
            }
            TgSectionLabel("Медиа")
            TgCard {
                TgSwitchRow(icon = Icons.Outlined.Download, title = "Автозагрузка медиа", checked = autoDownloadMedia, onChange = { autoDownloadMedia = it })
                TgDivider()
                TgSwitchRow(icon = Icons.Outlined.Image, title = "Сохранять в галерею", checked = saveToGallery, onChange = { saveToGallery = it })
            }
            TgSectionLabel("Хранилище")
            TgCard {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Text("Кэш TDLib: $cacheSizeMb МБ", fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    cacheDir?.listFiles()?.forEach { file ->
                        if (file.name != "tdlib.binlog") file.deleteRecursively()
                    }
                    cacheSizeMb = 0L
                },
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Очистить кэш", fontWeight = FontWeight.Medium)
            }
        }

        SettingsSubscreen.Privacy -> TgTextSubscreen("Конфиденциальность", """
            • Шифрование: MTProto 2.0 (сквозное)
            • Двухфакторная аутентификация: включена
            • Локальная база: шифрование TDLib
            • Скрытие номера: включено
            • Запрет пересылки: включён
        """.trimIndent(), onBack = { currentSubscreen = SettingsSubscreen.Main })

        SettingsSubscreen.Notifications -> TgTextSubscreen("Уведомления и звуки", """
            • Push-first архитектура: активна
            • Личные чаты: включены
            • Группы и каналы: включены
            • Тактильный отклик: Material You haptics
        """.trimIndent(), onBack = { currentSubscreen = SettingsSubscreen.Main })

        SettingsSubscreen.ExteraFeatures -> TgTextSubscreen("Возможности exteraGram", """
            • Pill Stack (парящий док): активен
            • Папки чатов: Все / Личные / Группы / Каналы
            • Сгруппированные пузыри с хвостиками: включены
            • Ночная тема: синхронизирована с ядром
            • Rust Tokio async core: 0.4.0-beta
        """.trimIndent(), onBack = { currentSubscreen = SettingsSubscreen.Main })

        SettingsSubscreen.About -> TgTextSubscreen("О программе", """
            Rustgram v0.4.0-beta

            Нативный Telegram-клиент на Rust Tokio + официальном TDLib 1.8.61 и Jetpack Compose.

            Дизайн вдохновлён exteraGram (дизайнеры @the8055u и BlueprintDsgn).
        """.trimIndent(), onBack = { currentSubscreen = SettingsSubscreen.Main })

        SettingsSubscreen.Main -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                item {
                    // Telegram settings profile header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpressiveAvatar(name = "Telegram User", initials = "TG", tintIndex = 2, isOnline = true, size = 70.dp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Telegram User", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("+7 900 123-45-67", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text("в сети", color = TelegramBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                item {
                    TgCard {
                        TgNavRow(icon = Icons.Outlined.Palette, title = "Оформление", onClick = { currentSubscreen = SettingsSubscreen.Appearance })
                        TgDivider()
                        TgNavRow(icon = Icons.Outlined.DataUsage, title = "Данные и память", onClick = { currentSubscreen = SettingsSubscreen.Storage })
                        TgDivider()
                        TgNavRow(icon = Icons.Outlined.Lock, title = "Конфиденциальность", onClick = { currentSubscreen = SettingsSubscreen.Privacy })
                        TgDivider()
                        TgNavRow(icon = Icons.Outlined.Notifications, title = "Уведомления и звуки", onClick = { currentSubscreen = SettingsSubscreen.Notifications })
                        TgDivider()
                        TgNavRow(icon = Icons.Outlined.AutoAwesome, title = "Возможности exteraGram", onClick = { currentSubscreen = SettingsSubscreen.ExteraFeatures })
                        TgDivider()
                        TgNavRow(icon = Icons.Outlined.Info, title = "О программе", onClick = { currentSubscreen = SettingsSubscreen.About })
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    TgCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onLogout)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(16.dp))
                            Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TgSettingsScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
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
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TgTextSubscreen(title: String, description: String, onBack: () -> Unit) {
    TgSettingsScaffold(title = title, onBack = onBack) {
        TgSectionLabel("Основное")
        TgCard {
            Text(
                description,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// ---------- Telegram-style building blocks ----------

@Composable
fun TgSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TelegramBlue,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun TgCard(content: @Composable ColumnScope.() -> Unit) {
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
fun TgDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun TgNavRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TgSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = TelegramBlue))
    }
}
