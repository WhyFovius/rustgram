package com.rustgram.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rustgram.app.NativeBridge
import com.rustgram.app.ui.components.*
import com.rustgram.app.ui.theme.*
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
    val photoPath: String? = null,
    val isMuted: Boolean = false
)

data class MessageItem(
    val body: String,
    val time: String,
    val outgoing: Boolean,
    val read: Boolean
)

// exteraGram folder tabs
enum class TgFolder(val title: String, val category: String?) {
    ALL("Все", null),
    PERSONAL("Личные", "Personal"),
    GROUPS("Группы", "Group"),
    CHANNELS("Каналы", "Channel")
}

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
    var viewingProfileChat by remember { mutableStateOf<ChatItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }

    var chats by remember { mutableStateOf<List<ChatItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFolder by remember { mutableStateOf(TgFolder.ALL) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { NativeBridge.loadChats(100) }

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
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(1000)
        }
    }

    val filteredChats = remember(chats, searchQuery, selectedFolder) {
        chats.filter { chat ->
            val matchesFolder = selectedFolder.category == null ||
                    chat.category.equals(selectedFolder.category, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    chat.name.contains(searchQuery, ignoreCase = true) ||
                    chat.preview.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }

    // Hide top bar when scrolled away, show at top (Telegram behavior)
    var isTopBarVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index -> isTopBarVisible = index == 0 }
    }

    if (viewingProfileChat != null) {
        ProfileScreen(chat = viewingProfileChat!!, onBack = { viewingProfileChat = null })
        return
    }

    if (activeChat != null) {
        ChatDetailScreen(
            chat = activeChat!!,
            onBack = { activeChat = null },
            onOpenProfile = { viewingProfileChat = activeChat }
        )
        return
    }

    val navTabs = listOf(
        NavigationTabItem(0, Icons.Outlined.ChatBubbleOutline, "Чаты"),
        NavigationTabItem(1, Icons.Outlined.Call, "Звонки"),
        NavigationTabItem(2, Icons.Outlined.PersonOutline, "Контакты"),
        NavigationTabItem(3, Icons.Outlined.Settings, "Настройки")
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                AnimatedVisibility(
                    visible = isTopBarVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        // Telegram top bar
                        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSearchActive) {
                                    IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                    }
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Поиск", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                                    IconButton(onClick = { isDrawerOpen = true }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                                    }
                                    Text(
                                        text = if (selectedTab == 0) "Rustgram" else when (selectedTab) {
                                            1 -> "Звонки"; 2 -> "Контакты"; else -> "Настройки"
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Outlined.Search, contentDescription = "Поиск")
                                    }
                                }
                            }
                        }
                        // Telegram folder tabs (underline style)
                        if (selectedTab == 0 && !isSearchActive) {
                            Surface(color = MaterialTheme.colorScheme.surface) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TgFolder.entries.forEach { folder ->
                                        val selected = selectedFolder == folder
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { selectedFolder = folder }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = folder.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(if (selected) 28.dp else 0.dp)
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(TelegramBlue)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
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
                        if (isLoading && chats.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    M3HexagonLoadingIndicator(size = 54.dp, contained = true)
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "Загрузка диалогов...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (filteredChats.isEmpty()) {
                            EmptyStateTab(
                                icon = Icons.Outlined.ChatBubbleOutline,
                                title = "Нет сообщений",
                                subtitle = "Нет активных диалогов по выбранной папке"
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                items(filteredChats, key = { it.id }) { chat ->
                                    TelegramChatRow(
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
                    1 -> EmptyStateTab(
                        icon = Icons.Outlined.Call,
                        title = "Журнал звонков пуст",
                        subtitle = "Звонки с MTProto шифрованием"
                    )
                    2 -> EmptyStateTab(
                        icon = Icons.Outlined.PeopleOutline,
                        title = "Контакты",
                        subtitle = "Синхронизируются с серверами Telegram"
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

        // ============ exteraGram Drawer Menu ============
        TelegramDrawer(
            isOpen = isDrawerOpen,
            isDarkTheme = isDarkTheme,
            onToggleDark = onToggleDarkTheme,
            onClose = { isDrawerOpen = false },
            onContacts = { isDrawerOpen = false; selectedTab = 2 },
            onCalls = { isDrawerOpen = false; selectedTab = 1 },
            onSettings = { isDrawerOpen = false; selectedTab = 3 },
            onSaved = {}
        )
    }
}

/** Authentic Telegram chat list row. */
@Composable
fun TelegramChatRow(
    chat: ChatItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 7.dp),
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

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = chat.time,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.preview.ifBlank { "Нет сообщений" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (chat.unread > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(UnreadBadge)
                    ) {
                        Text(
                            chat.unread.toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (chat.isMuted) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Outlined.NotificationsOff,
                        contentDescription = "Без звука",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/** exteraGram slide-out drawer. */
@Composable
fun TelegramDrawer(
    isOpen: Boolean,
    isDarkTheme: Boolean,
    onToggleDark: (Boolean) -> Unit,
    onClose: () -> Unit,
    onContacts: () -> Unit,
    onCalls: () -> Unit,
    onSettings: () -> Unit,
    onSaved: () -> Unit
) {
    Scrim(visible = isOpen, onDismiss = onClose)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Drawer header: profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpressiveAvatar(
                            name = "Telegram User",
                            initials = "TG",
                            tintIndex = 2,
                            isOnline = true,
                            size = 64.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Telegram User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "+7 900 123-45-67",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    DrawerItem(Icons.Outlined.PersonOutline, "Контакты", onContacts)
                    DrawerItem(Icons.Outlined.Call, "Звонки", onCalls)
                    DrawerItem(Icons.Outlined.BookmarkBorder, "Избранное", onSaved)
                    DrawerItem(Icons.Outlined.Settings, "Настройки", onSettings)
                    DrawerItem(Icons.Outlined.GroupAdd, "Пригласить друзей", {})

                    Spacer(Modifier.weight(1f))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Night mode toggle (Telegram drawer classic)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDark(!isDarkTheme) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                            contentDescription = null,
                            tint = TelegramBlue
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("Ночной режим", color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onToggleDark,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = TelegramBlue
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = TelegramBlue)
        Spacer(Modifier.width(16.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
    }
}

@Composable
private fun Scrim(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss)
        )
    }
}

@Composable
fun EmptyStateTab(icon: ImageVector, title: String, subtitle: String) {
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
                Icon(icon, contentDescription = null, modifier = Modifier.size(42.dp), tint = TelegramBlue)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// Chat screen — authentic Telegram bubbles, wallpaper,
// date separators, grouped messages, scroll-hide toolbar
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: ChatItem,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit
) {
    var messages by remember { mutableStateOf<List<MessageItem>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isEmojiOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(chat.id) { NativeBridge.loadHistory(chat.id) }

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
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(800)
        }
    }

    // Auto-scroll to bottom when new messages arrive (Telegram behavior)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Hide toolbar when scrolled into history, show at bottom (Telegram behavior)
    var isTopBarVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index -> isTopBarVisible = index == 0 }
    }

    Scaffold(
        containerColor = chatWallpaper(),
        topBar = {
            AnimatedVisibility(visible = isTopBarVisible, enter = expandVertically(), exit = shrinkVertically()) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
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
                            size = 40.dp,
                            onClick = onOpenProfile
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenProfile() }
                        ) {
                            Text(
                                chat.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (chat.online) "в сети" else "был(а) недавно",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 12.sp,
                                color = if (chat.online) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.Call, contentDescription = "Звонок")
                        }
                        IconButton(onClick = onOpenProfile) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Ещё")
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().imePadding()) {
                // Telegram input bar
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Attach paperclip (Telegram style)
                            IconButton(onClick = {}) {
                                Icon(
                                    Icons.Outlined.AttachFile,
                                    contentDescription = "Прикрепить",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Сообщение", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = 2.dp),
                                minLines = 1,
                                maxLines = 4,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    cursorColor = TelegramBlue
                                )
                            )

                            IconButton(onClick = { isEmojiOpen = !isEmojiOpen }) {
                                Icon(
                                    Icons.Outlined.EmojiEmotions,
                                    contentDescription = "Эмодзи",
                                    tint = if (isEmojiOpen) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Send / Mic morph (Telegram)
                            val showSend = inputText.isNotBlank()
                            AnimatedContent(
                                targetState = showSend,
                                transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() },
                                label = "send_morph"
                            ) { send ->
                                if (send) {
                                    IconButton(
                                        onClick = {
                                            NativeBridge.sendMessage(chat.id, inputText)
                                            inputText = ""
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = TelegramBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .expressiveBouncyClick(pressedScale = 0.88f) {
                                                NativeBridge.sendMessage(chat.id, inputText)
                                                inputText = ""
                                            }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                                    }
                                } else {
                                    IconButton(
                                        onClick = {},
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = TelegramBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                    ) {
                                        Icon(Icons.Outlined.Mic, contentDescription = "Голосовое")
                                    }
                                }
                            }
                        }

                        EmojiKeyboardPanel(
                            visible = isEmojiOpen,
                            onEmojiSelected = { emoji -> inputText += emoji }
                        )
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
                    Spacer(Modifier.height(14.dp))
                    Text("Загрузка истории...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 10.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(messages) { index, msg ->
                    val groupedWithPrev = index > 0 && messages[index - 1].outgoing == msg.outgoing
                    val groupedWithNext = index < messages.size - 1 &&
                            messages[index + 1].outgoing == msg.outgoing
                    // Date separator every ~30 messages (Telegram-like rhythm)
                    if (index % 30 == 0) {
                        DateSeparator(date = "3 ноября")
                    }
                    TelegramMessageBubble(
                        msg = msg,
                        groupedWithPrev = groupedWithPrev,
                        groupedWithNext = groupedWithNext
                    )
                }
            }
        }
    }
}

/** Telegram-style message bubble with tail & grouping. */
@Composable
fun TelegramMessageBubble(
    msg: MessageItem,
    groupedWithPrev: Boolean,
    groupedWithNext: Boolean
) {
    val isOut = msg.outgoing
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bubbleColor = when {
        isOut && dark -> OutgoingBubbleDark
        isOut && !dark -> OutgoingBubbleLight
        !isOut && dark -> IncomingBubbleDark
        else -> IncomingBubbleLight
    }
    val textColor = if (isOut && !dark) Color(0xFF000000) else Color.White
    val timeColor = if (isOut) {
        if (dark) Color(0xFF98B1C8) else Color(0xFF5FBB5F)
    } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (groupedWithPrev) 1.dp else 5.dp,
                bottom = if (groupedWithNext) 1.dp else 5.dp
            ),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start
    ) {
        // Bubble shape: Telegram tail on group start, flat corners when grouped
        val shape = when {
            isOut -> RoundedCornerShape(
                topStart = 16.dp,
                topEnd = if (groupedWithPrev) 16.dp else 4.dp,
                bottomStart = 16.dp,
                bottomEnd = if (groupedWithNext) 16.dp else 4.dp
            )
            else -> RoundedCornerShape(
                topStart = if (groupedWithPrev) 16.dp else 4.dp,
                topEnd = 16.dp,
                bottomStart = if (groupedWithNext) 16.dp else 4.dp,
                bottomEnd = 16.dp
            )
        }

        Surface(
            shape = shape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = msg.body,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp,
                    color = textColor,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(msg.time, fontSize = 12.sp, color = timeColor)
                    if (isOut) {
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = if (msg.read) "✓✓" else "✓",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.read) if (dark) SecondaryDark else OutgoingCheckLight else timeColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateSeparator(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x99000000)
        ) {
            Text(
                text = date,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}

/** Chat wallpaper depending on active theme (light/day vs dark/night). */
@Composable
fun chatWallpaper(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) ChatWallpaperDark else ChatWallpaperLight
