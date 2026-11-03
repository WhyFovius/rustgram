package com.rustgram.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rustgram.app.NativeBridge
import com.rustgram.app.ui.components.*
import com.rustgram.app.ui.theme.MotionSprings
import org.json.JSONArray
import java.io.File

data class ChatItem(
    val id: Long,
    val name: String,
    val initials: String = "",
    val preview: String = "",
    val time: String = "",
    val unread: Int = 0,
    val tint: Int = 0,
    val category: String = "Personal",
    val online: Boolean = false,
    val photoPath: String? = null
)

data class MessageItem(
    val body: String,
    val time: String,
    val outgoing: Boolean,
    val read: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    useDynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    cacheDir: File?
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeChat by remember { mutableStateOf<ChatItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Dialogs loaded exclusively from real TDLib / MTProto backend
    var chats by remember { mutableStateOf<List<ChatItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Category filter: 0 = All, 1 = Personal, 2 = Groups
    var selectedCategoryFilter by remember { mutableIntStateOf(0) }

    // Trigger load of real chats upon entering screen
    LaunchedEffect(Unit) {
        NativeBridge.loadChats(100)
    }

    // Polling backend dialogs
    LaunchedEffect(Unit) {
        while (true) {
            val jsonStr = NativeBridge.getDialogs()
            try {
                val array = JSONArray(jsonStr)
                val list = ArrayList<ChatItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ChatItem(
                            id = obj.optLong("id"),
                            name = obj.optString("name", "Telegram"),
                            initials = obj.optString("initials", ""),
                            preview = obj.optString("preview", ""),
                            time = obj.optString("time", ""),
                            unread = obj.optInt("unread", 0),
                            tint = obj.optInt("tint", 0),
                            category = obj.optString("category", "Personal"),
                            online = obj.optBoolean("online", false),
                            photoPath = if (obj.has("photo_path") && !obj.isNull("photo_path")) obj.getString("photo_path") else null
                        )
                    )
                }
                chats = list
                isLoading = false
            } catch (e: Exception) {
                // Keep state
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    val filteredChats = remember(chats, searchQuery, selectedCategoryFilter) {
        chats.filter { chat ->
            val matchesCategory = when (selectedCategoryFilter) {
                1 -> chat.category.equals("Personal", ignoreCase = true)
                2 -> chat.category.equals("Group", ignoreCase = true)
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    chat.name.contains(searchQuery, ignoreCase = true) ||
                    chat.preview.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    if (activeChat != null) {
        ChatDetailScreen(
            chat = activeChat!!,
            onBack = {
                activeChat = null
            }
        )
        return
    }

    val navTabs = listOf(
        NavigationTabItem(0, Icons.Outlined.ChatBubbleOutline, "Чаты"),
        NavigationTabItem(1, Icons.Outlined.Call, "Звонки"),
        NavigationTabItem(2, Icons.Outlined.PersonOutline, "Контакты"),
        NavigationTabItem(3, Icons.Outlined.Settings, "Настройки")
    )

    Scaffold(
        topBar = {
            // Floating style Top App Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск чатов и сообщений...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = when (selectedTab) {
                                0 -> "Чаты"
                                1 -> "Звонки"
                                2 -> "Контакты"
                                else -> "Настройки"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }
                    ) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Outlined.Search,
                            contentDescription = "Поиск"
                        )
                    }
                    IconButton(
                        onClick = {
                            NativeBridge.loadChats(100)
                        }
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Обновить")
                    }
                }
            }
        },
        bottomBar = {
            // Floating Dock Navigation Bar
            FloatingDockBar(
                tabs = navTabs,
                selectedIndex = selectedTab,
                onSelectTab = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategoryFilter == 0,
                                onClick = { selectedCategoryFilter = 0 },
                                label = { Text("Все (${chats.size})") },
                                shape = MaterialTheme.shapes.small
                            )
                            FilterChip(
                                selected = selectedCategoryFilter == 1,
                                onClick = { selectedCategoryFilter = 1 },
                                label = { Text("Личные") },
                                shape = MaterialTheme.shapes.small
                            )
                            FilterChip(
                                selected = selectedCategoryFilter == 2,
                                onClick = { selectedCategoryFilter = 2 },
                                label = { Text("Группы и каналы") },
                                shape = MaterialTheme.shapes.small
                            )
                        }

                        if (isLoading && chats.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    M3HexagonLoadingIndicator(size = 54.dp, contained = true)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Загрузка диалогов из TDLib MTProto...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (filteredChats.isEmpty()) {
                            EmptyStateTab(
                                icon = Icons.Outlined.ChatBubbleOutline,
                                title = "Нет сообщений",
                                subtitle = "В вашем Telegram аккаунте нет активных диалогов по выбранному фильтру"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                items(filteredChats, key = { it.id }) { chat ->
                                    ChatRowItem(
                                        chat = chat,
                                        onClick = {
                                            NativeBridge.markAsRead(chat.id)
                                            activeChat = chat
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> EmptyStateTab(
                    icon = Icons.Outlined.Call,
                    title = "Журнал звонков пуст",
                    subtitle = "Звонки Telegram через MTProto шифрование будут отображаться здесь"
                )
                2 -> EmptyStateTab(
                    icon = Icons.Outlined.PeopleOutline,
                    title = "Контакты Telegram",
                    subtitle = "Контакты загружаются синхронно с серверов Telegram"
                )
                3 -> SettingsTab(
                    onLogout = onLogout,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    useDynamicColor = useDynamicColor,
                    onToggleDynamicColor = onToggleDynamicColor,
                    cacheDir = cacheDir
                )
            }
        }
    }
}

@Composable
fun ChatRowItem(
    chat: ChatItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .expressiveBouncyClick(pressedScale = 0.98f) { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveAvatar(
                name = chat.name,
                initials = chat.initials,
                tintIndex = chat.tint,
                isOnline = chat.online,
                photoPath = chat.photoPath,
                size = 54.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = chat.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.preview.ifBlank { "Сообщений пока нет" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (chat.unread > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.animateContentSize(animationSpec = MotionSprings.fastSizeSpatial)
                        ) {
                            Text(chat.unread.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateTab(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(92.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: ChatItem,
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf<List<MessageItem>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }

    LaunchedEffect(chat.id) {
        NativeBridge.loadHistory(chat.id)
    }

    LaunchedEffect(chat.id) {
        while (true) {
            val jsonStr = NativeBridge.getMessages(chat.id)
            try {
                val array = JSONArray(jsonStr)
                val list = ArrayList<MessageItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        MessageItem(
                            body = obj.optString("body", ""),
                            time = obj.optString("time", ""),
                            outgoing = obj.optBoolean("outgoing", false),
                            read = obj.optBoolean("read", false)
                        )
                    )
                }
                messages = list
            } catch (e: Exception) {
                // Keep
            }
            kotlinx.coroutines.delay(800)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                    ExpressiveAvatar(
                        name = chat.name,
                        initials = chat.initials,
                        tintIndex = chat.tint,
                        isOnline = chat.online,
                        photoPath = chat.photoPath,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            chat.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (chat.online) "в сети" else "был(а) недавно",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (chat.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Call, contentDescription = "Звонок")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Опции")
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = "Прикрепить")
                    }

                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Сообщение...") },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = MaterialTheme.shapes.large
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                NativeBridge.sendMessage(chat.id, inputText)
                                inputText = ""
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.expressiveBouncyClick(pressedScale = 0.88f) {
                                NativeBridge.sendMessage(chat.id, inputText)
                                inputText = ""
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                isRecordingVoice = !isRecordingVoice
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isRecordingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isRecordingVoice) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.expressiveBouncyClick(pressedScale = 0.88f) {
                                isRecordingVoice = !isRecordingVoice
                            }
                        ) {
                            Icon(Icons.Outlined.Mic, contentDescription = "Голосовое сообщение")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    M3HexagonLoadingIndicator(size = 48.dp, contained = true)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Загрузка истории диалога...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isOut = msg.outgoing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOut) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isOut) 16.dp else 4.dp,
                                bottomEnd = if (isOut) 4.dp else 16.dp
                            ),
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .animateContentSize(animationSpec = MotionSprings.fastSizeSpatial)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = msg.body,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isOut) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg.time,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    if (isOut) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (msg.read) "✓✓" else "✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.read) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
