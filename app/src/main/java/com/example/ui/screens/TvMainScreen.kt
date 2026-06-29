package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.data.Playlist
import com.example.data.ProgramEpisode
import com.example.ui.AppViewModel
import com.example.ui.components.VideoPlayer
import com.example.ui.components.glassmorphism
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateFocus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TvScreen {
    PLAYER, PLAYLISTS, PARENTAL, ABOUT
}

@Composable
fun TvMainScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var currentTvScreen by remember { mutableStateOf(TvScreen.PLAYER) }
    var isSidebarFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Key handlers for global TV Dpad Navigation
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121214))
    ) {
        // 1. COLLAPSIBLE TV NAVIGATION SIDEBAR
        TvSidebar(
            selectedScreen = currentTvScreen,
            isFocused = isSidebarFocused,
            onFocusChange = { isSidebarFocused = it },
            onScreenSelect = { currentTvScreen = it }
        )

        // 2. MAIN CONTENT VIEW
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            when (currentTvScreen) {
                TvScreen.PLAYER -> TvPlayerSection(viewModel)
                TvScreen.PLAYLISTS -> TvPlaylistsSection(viewModel)
                TvScreen.PARENTAL -> TvParentalSection(viewModel)
                TvScreen.ABOUT -> TvAboutSection()
            }
        }
    }
}

@Composable
fun TvSidebar(
    selectedScreen: TvScreen,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onScreenSelect: (TvScreen) -> Unit
) {
    val width by animateDpAsState(
        targetValue = if (isFocused) 220.dp else 72.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "sidebar_width"
    )

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(Color(0xFF19181D))
            .onFocusChanged { onFocusChange(it.hasFocus) }
            .padding(vertical = 24.dp, horizontal = 12.dp),
        horizontalAlignment = if (isFocused) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Logo Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CinemaAmber.copy(alpha = 0.2f))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = "Logo",
                tint = CinemaAmber,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Items
        val items = listOf(
            Triple(TvScreen.PLAYER, "Телевизор", Icons.Default.PlayArrow),
            Triple(TvScreen.PLAYLISTS, "Плейлисты", Icons.Default.PlaylistPlay),
            Triple(TvScreen.PARENTAL, "Родительский", Icons.Default.Security),
            Triple(TvScreen.ABOUT, "О программе", Icons.Default.Info)
        )

        items.forEach { (screen, label, icon) ->
            val isSelected = selectedScreen == screen
            var isItemFocused by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isItemFocused -> CinemaAmber
                            isSelected -> Color.White.copy(alpha = 0.1f)
                            else -> Color.Transparent
                        }
                    )
                    .onFocusChanged { isItemFocused = it.isFocused }
                    .clickable { onScreenSelect(screen) }
                    .focusable()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isItemFocused) Color.Black else if (isSelected) CinemaAmber else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )

                if (isFocused) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = label,
                        color = if (isItemFocused) Color.Black else if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Simple Footer indicating TV Mode
        if (isFocused) {
            Text(
                text = "TV EDITION",
                color = CinemaAmber.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun TvPlayerSection(viewModel: AppViewModel) {
    val channels by viewModel.allChannels.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val archiveSchedule by viewModel.archiveSchedule.collectAsState()
    val playMode by viewModel.playMode.collectAsState()
    val resizeMode by viewModel.videoResizeMode.collectAsState()
    val parentalEnabled by viewModel.parentalEnabled.collectAsState()
    val isParentalUnlocked by viewModel.isParentalSessionUnlocked.collectAsState()

    var showDrawer by remember { mutableStateOf(false) }
    var showPinDialogForChannel by remember { mutableStateOf<Channel?>(null) }
    var pinInputValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val drawerFocusRequester = remember { FocusRequester() }

    val allCategories = remember(categories) { listOf("Все", "★ Избранные") + categories }
    val filteredChannels = remember(channels, selectedCategory) {
        when (selectedCategory) {
            "Все" -> channels
            "★ Избранные" -> channels.filter { it.isFavorite }
            else -> channels.filter { it.category == selectedCategory }
        }
    }

    LaunchedEffect(showDrawer) {
        if (showDrawer) {
            delay(100)
            try {
                drawerFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    val onPreviousChannel = {
        if (filteredChannels.isNotEmpty()) {
            val index = filteredChannels.indexOfFirst { it.id == selectedChannel?.id }
            if (index != -1) {
                val prevChannel = filteredChannels[(index - 1 + filteredChannels.size) % filteredChannels.size]
                viewModel.selectChannel(prevChannel)
            }
        }
    }

    val onNextChannel = {
        if (filteredChannels.isNotEmpty()) {
            val index = filteredChannels.indexOfFirst { it.id == selectedChannel?.id }
            if (index != -1) {
                val nextChannel = filteredChannels[(index + 1) % filteredChannels.size]
                viewModel.selectChannel(nextChannel)
            }
        }
    }

    val streamUrl = remember(playMode, selectedChannel) {
        when (val mode = playMode) {
            is AppViewModel.PlayMediaMode.ArchivePlay -> {
                val base = selectedChannel?.streamUrl
                if (base != null) {
                    val archiveTs = mode.episode.startTimeMs / 1000
                    "$base?utc=$archiveTs"
                } else null
            }
            is AppViewModel.PlayMediaMode.DirectLive -> selectedChannel?.streamUrl
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!showDrawer) {
                                showDrawer = true
                                true
                            } else false
                        }
                        android.view.KeyEvent.KEYCODE_BACK -> {
                            if (showDrawer) {
                                showDrawer = false
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. BACKGROUND FULL-SCREEN PLAYER
        VideoPlayer(
            streamUrl = streamUrl,
            title = selectedChannel?.name ?: "ТВ Плеер",
            logoUrl = selectedChannel?.logoUrl,
            mode = playMode,
            isFullscreen = true,
            resizeMode = resizeMode,
            onToggleFullscreen = { /* Already fullscreen */ },
            onToggleResizeMode = {
                val next = when (resizeMode) {
                    0 -> 3
                    3 -> 4
                    else -> 0
                }
                viewModel.setVideoResizeMode(next)
            },
            onPreviousChannel = onPreviousChannel,
            onNextChannel = onNextChannel,
            modifier = Modifier.fillMaxSize()
        )

        // 2. SLIDEOUT SIDE CHANNEL DRAWER (Overlay on Left Side)
        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            TvChannelDrawer(
                categories = allCategories,
                selectedCategory = selectedCategory,
                channels = filteredChannels,
                selectedChannel = selectedChannel,
                drawerFocusRequester = drawerFocusRequester,
                onCategorySelect = { viewModel.selectCategory(it) },
                onChannelSelect = { channel ->
                    if (channel.isLocked && parentalEnabled && !isParentalUnlocked) {
                        showPinDialogForChannel = channel
                        pinInputValue = ""
                        pinError = false
                    } else {
                        viewModel.selectChannel(channel)
                        showDrawer = false
                    }
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onClose = { showDrawer = false }
            )
        }

        // 3. ARCHIVE BANNER ON TOP IF IN TIMESHIFT
        val currentPlayMode = playMode
        if (currentPlayMode is AppViewModel.PlayMediaMode.ArchivePlay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, SkyBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = SkyBlue)
                        Text(
                            "Архив: ${currentPlayMode.episode.title} (${currentPlayMode.episode.startTimeString})",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    var isBackFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = { viewModel.switchBackToLive() },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                        modifier = Modifier
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .border(if (isBackFocused) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(4.dp))
                            .focusable()
                    ) {
                        Text("В прямой эфир", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // PIN unlock popup
        if (showPinDialogForChannel != null) {
            TvPinCodeDialog(
                onDismiss = { showPinDialogForChannel = null },
                onConfirm = { pin ->
                    if (viewModel.unlockParentalSession(pin)) {
                        showPinDialogForChannel?.let { viewModel.selectChannel(it) }
                        showPinDialogForChannel = null
                    } else {
                        pinError = true
                    }
                }
            )
        }
    }
}

@Composable
fun TvChannelDrawer(
    categories: List<String>,
    selectedCategory: String,
    channels: List<Channel>,
    selectedChannel: Channel?,
    drawerFocusRequester: FocusRequester,
    onCategorySelect: (String) -> Unit,
    onChannelSelect: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(420.dp)
            .background(Color(0xE6141318))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(0.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Category List (Left Column of drawer)
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            ) {
                Text(
                    "Категории",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { category ->
                        val isSel = selectedCategory == category
                        var isFocused by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isFocused -> CinemaAmber
                                        isSel -> Color.White.copy(alpha = 0.12f)
                                        else -> Color.Transparent
                                    }
                                )
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable { onCategorySelect(category) }
                                .focusable()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = category,
                                color = if (isFocused) Color.Black else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Channels List (Right Column of drawer)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            ) {
                Text(
                    "Телеканалы",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    itemsIndexed(channels) { index, channel ->
                        val isSel = selectedChannel?.id == channel.id
                        var isFocused by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(drawerFocusRequester) else Modifier)
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable { onChannelSelect(channel) }
                                .focusable(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFocused) CinemaAmber else if (isSel) Color.White.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (isFocused) Color.White else Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        modifier = Modifier.fillMaxSize().padding(2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = channel.name,
                                    color = if (isFocused) Color.Black else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (channel.isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Заблокирован",
                                        tint = if (isFocused) Color.Black else LiveRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Favorite Star
                                var isStarFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .onFocusChanged { isStarFocused = it.isFocused }
                                        .clickable { onToggleFavorite(channel) }
                                        .focusable(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Избранное",
                                        tint = if (isFocused) Color.Black else CinemaAmber,
                                        modifier = Modifier.size(16.dp)
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

@Composable
fun TvPlaylistsSection(viewModel: AppViewModel) {
    val playlists by viewModel.playlists.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()

    var selectedPlaylistForDelete by remember { mutableStateOf<Playlist?>(null) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Плейлисты",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Всего каналов: ${allChannels.size}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                var isRefreshAllFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = { 
                        playlists.forEach { viewModel.refreshPlaylist(it.id) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaAmber),
                    modifier = Modifier
                        .onFocusChanged { isRefreshAllFocused = it.isFocused }
                        .border(if (isRefreshAllFocused) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(8.dp))
                        .focusable(),
                    enabled = !isRefreshing
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Обновить все", color = Color.Black)
                }

                var isAddFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        playlistName = ""
                        playlistUrl = ""
                        showUrlDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                    modifier = Modifier
                        .onFocusChanged { isAddFocused = it.isFocused }
                        .border(if (isAddFocused) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(8.dp))
                        .focusable()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Добавить", color = Color.Black)
                }
            }
        }

        if (isRefreshing) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = CinemaAmber)
                    Text("Загрузка и парсинг плейлистов...", color = Color.White.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (playlists.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("Плейлисты не добавлены. Добавьте M3U ссылку сверху.", color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }

                items(playlists) { playlist ->
                    var isFocused by remember { mutableStateOf(false) }
                    val pChannels = allChannels.filter { it.playlistId == playlist.id }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFocused) SlateFocus else Color.White.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, if (isFocused) CinemaAmber else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Ссылка: ${playlist.url}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Каналов: ${pChannels.size}", color = CinemaAmber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                var isRefFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { viewModel.refreshPlaylist(playlist.id) },
                                    modifier = Modifier
                                        .onFocusChanged { isRefFocused = it.isFocused }
                                        .background(if (isRefFocused) CinemaAmber else Color.White.copy(alpha = 0.1f), CircleShape)
                                        .focusable()
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Обновить", tint = if (isRefFocused) Color.Black else Color.White)
                                }

                                var isDelFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { selectedPlaylistForDelete = playlist },
                                    modifier = Modifier
                                        .onFocusChanged { isDelFocused = it.isFocused }
                                        .background(if (isDelFocused) LiveRed else Color.White.copy(alpha = 0.1f), CircleShape)
                                        .focusable()
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Playlist dialog
        if (showUrlDialog) {
            AlertDialog(
                onDismissRequest = { showUrlDialog = false },
                title = { Text("Добавить плейлист (URL)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Название") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = playlistUrl,
                            onValueChange = { playlistUrl = it },
                            label = { Text("M3U Ссылка") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (playlistName.isNotBlank() && playlistUrl.isNotBlank()) {
                                viewModel.addPlaylist(playlistName, playlistUrl, "m3u")
                                showUrlDialog = false
                            }
                        }
                    ) {
                        Text("ОК")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUrlDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Delete confirmation dialog
        if (selectedPlaylistForDelete != null) {
            AlertDialog(
                onDismissRequest = { selectedPlaylistForDelete = null },
                title = { Text("Удалить плейлист?") },
                text = { Text("Вы действительно хотите удалить плейлист '${selectedPlaylistForDelete?.name}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedPlaylistForDelete?.let { viewModel.deletePlaylist(it.id) }
                            selectedPlaylistForDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedPlaylistForDelete = null }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
fun TvParentalSection(viewModel: AppViewModel) {
    val parentalEnabled by viewModel.parentalEnabled.collectAsState()
    val parentalPin by viewModel.parentalPin.collectAsState()

    var tempPin by remember { mutableStateOf("") }
    var showSetPinMode by remember { mutableStateOf(false) }
    var pinMessage by remember { mutableStateOf("Введите PIN-код для изменения настроек") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Родительский контроль (TV)",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left block: Status and Controls
            Column(
                modifier = Modifier.weight(0.5f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Текущий статус", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (parentalEnabled) LiveRed else Color.Gray)
                            )
                            Text(
                                text = if (parentalEnabled) "АКТИВЕН" else "ОТКЛЮЧЕН",
                                color = if (parentalEnabled) LiveRed else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Text(
                    text = pinMessage,
                    color = CinemaAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // PIN value indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    repeat(4) { index ->
                        val hasChar = tempPin.length > index
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (hasChar) CinemaAmber else Color.White.copy(alpha = 0.15f))
                                .border(1.dp, if (hasChar) Color.White else Color.Transparent, CircleShape)
                        )
                    }
                }

                // Status Switch
                var isSwitchFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        if (tempPin == parentalPin) {
                            viewModel.setParentalLockState(!parentalEnabled, parentalPin)
                            tempPin = ""
                            pinMessage = "Статус изменен!"
                        } else {
                            pinMessage = "Неверный PIN-код!"
                            tempPin = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (parentalEnabled) Color.Gray else LiveRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isSwitchFocused = it.isFocused }
                        .border(if (isSwitchFocused) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(8.dp))
                        .focusable()
                ) {
                    Text(
                        text = if (parentalEnabled) "Отключить родительский контроль" else "Включить родительский контроль",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Change PIN code
                var isChangeFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        if (!showSetPinMode) {
                            if (tempPin == parentalPin) {
                                showSetPinMode = true
                                tempPin = ""
                                pinMessage = "Введите НОВЫЙ PIN-код (4 цифры)"
                            } else {
                                pinMessage = "Неверный текущий PIN!"
                                tempPin = ""
                            }
                        } else {
                            if (tempPin.length == 4) {
                                viewModel.setParentalLockState(parentalEnabled, tempPin)
                                showSetPinMode = false
                                tempPin = ""
                                pinMessage = "Новый PIN успешно сохранен!"
                            } else {
                                pinMessage = "Введите ровно 4 цифры!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isChangeFocused = it.isFocused }
                        .border(if (isChangeFocused) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(8.dp))
                        .focusable()
                ) {
                    Text(
                        text = if (showSetPinMode) "Сохранить новый PIN" else "Сбросить/Изменить PIN-код",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right block: Premium TV Virtual numeric keypad
            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val numGrid = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "OK")
                )

                numGrid.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { char ->
                            var isKeyFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isKeyFocused) CinemaAmber else Color.White.copy(alpha = 0.08f))
                                    .onFocusChanged { isKeyFocused = it.isFocused }
                                    .clickable {
                                        when (char) {
                                            "C" -> {
                                                tempPin = ""
                                                pinMessage = "Ввод очищен"
                                            }
                                            "OK" -> {
                                                // Trigger equivalent button action
                                            }
                                            else -> {
                                                if (tempPin.length < 4) {
                                                    tempPin += char
                                                }
                                            }
                                        }
                                    }
                                    .focusable(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    color = if (isKeyFocused) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvAboutSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("О программе", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Smart ТВ Плеер v2.0 - Специальное издание для Android TV",
                    color = CinemaAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    "Данная версия полностью переработана для максимально удобного управления с помощью стандартного пульта дистанционного управления (D-pad).",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp
                )

                Text(
                    "Ключевые преимущества:\n" +
                    "• Умная навигация пультом (DPAD Left - вызов списка каналов, OK - пауза, Aspect Ratio)\n" +
                    "• Быстрое и надежное кеширование значков каналов во внутреннюю память\n" +
                    "• Автоочистка нерабочих ссылок\n" +
                    "• Полноэкранный 10-foot плеер с нативной аппаратной поддержкой",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                Text("Автор: bytebudda\nЛицензия: MIT", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TvPinCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pinValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Заблокировано родительским контролем", color = Color.White) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Введите 4-значный PIN-код доступа к каналу", color = Color.White.copy(alpha = 0.8f))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (pinValue.length > i) CinemaAmber else Color.Gray)
                        )
                    }
                }

                // Keyboard
                val numpad = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.width(220.dp).height(100.dp)
                ) {
                    itemsIndexed(numpad) { _, num ->
                        var isFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isFocused) CinemaAmber else Color.DarkGray)
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable {
                                    if (pinValue.length < 4) {
                                        pinValue += num
                                        if (pinValue.length == 4) {
                                            onConfirm(pinValue)
                                        }
                                    }
                                }
                                .focusable(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(num, color = if (isFocused) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { pinValue = "" }) {
                Text("Очистить", color = CinemaAmber)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1C22)
    )
}
