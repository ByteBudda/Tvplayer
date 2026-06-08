package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.components.glassmorphism
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.data.ProgramEpisode
import com.example.ui.AppViewModel
import com.example.ui.components.VideoPlayer
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateFocus

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.allChannels.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val archiveSchedule by viewModel.archiveSchedule.collectAsState()
    val playMode by viewModel.playMode.collectAsState()
    val resizeMode by viewModel.videoResizeMode.collectAsState()
    
    val parentalEnabled by viewModel.parentalEnabled.collectAsState()
    val isParentalUnlocked by viewModel.isParentalSessionUnlocked.collectAsState()

    var showPinDialogForChannel by remember { mutableStateOf<Channel?>(null) }
    var pinInputValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val isFullscreen by viewModel.isFullscreen.collectAsState()
    var showFullscreenEpg by remember { mutableStateOf(false) }
    var showFullscreenChannels by remember { mutableStateOf(false) }
    var isEpgExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isFullscreen) {
        viewModel.setFullscreen(isFullscreen)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val filteredChannels = remember(channels, selectedCategory) {
        when (selectedCategory) {
            "Все" -> channels
            "★ Избранные" -> channels.filter { it.isFavorite }
            else -> channels.filter { it.category == selectedCategory }
        }
    }

    LaunchedEffect(channels) {
        if (selectedChannel == null && channels.isNotEmpty()) {
            val nonLockedChannel = channels.firstOrNull { !it.isLocked || !parentalEnabled }
            viewModel.selectChannel(nonLockedChannel ?: channels.first())
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

    BackHandler(enabled = isFullscreen) {
        if (showFullscreenEpg) {
            showFullscreenEpg = false
        } else if (showFullscreenChannels) {
            showFullscreenChannels = false
        } else {
            viewModel.setFullscreen(false)
        }
    }

    val allCategories = remember(categories) { listOf("Все", "★ Избранные") + categories }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape && !isFullscreen) {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(modifier = Modifier.weight(0.6f)) {
                    VideoPlayer(
                        streamUrl = getStreamUrl(playMode, selectedChannel),
                        title = selectedChannel?.name ?: "ТВ Плеер",
                        logoUrl = selectedChannel?.logoUrl,
                        mode = playMode,
                        isFullscreen = isFullscreen,
                        resizeMode = resizeMode,
                        onToggleFullscreen = { viewModel.setFullscreen(!isFullscreen) },
                        onToggleResizeMode = { toggleResizeMode(resizeMode, viewModel) },
                        onPreviousChannel = onPreviousChannel,
                        onNextChannel = onNextChannel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .padding(top = 8.dp)
                ) {
                    CategorySelector(allCategories, selectedCategory, viewModel)
                    ChannelsList(filteredChannels, selectedChannel, parentalEnabled, isParentalUnlocked, viewModel) { ch ->
                        showPinDialogForChannel = ch
                        pinInputValue = ""
                        pinError = false
                    }
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background)
            ) {
                VideoPlayer(
                    streamUrl = getStreamUrl(playMode, selectedChannel),
                    title = selectedChannel?.name ?: "ТВ Плеер",
                    logoUrl = selectedChannel?.logoUrl,
                    mode = playMode,
                    isFullscreen = isFullscreen,
                    resizeMode = resizeMode,
                    onToggleFullscreen = { viewModel.setFullscreen(!isFullscreen) },
                    onToggleResizeMode = { toggleResizeMode(resizeMode, viewModel) },
                    onPreviousChannel = onPreviousChannel,
                    onNextChannel = onNextChannel,
                    modifier = if (isFullscreen) {
                        Modifier.weight(1f)
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount < -20) {
                                        showFullscreenChannels = true
                                        showFullscreenEpg = false
                                    } else if (dragAmount > 20) {
                                        showFullscreenEpg = true
                                        showFullscreenChannels = false
                                    }
                                }
                            }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            showFullscreenEpg = true
                                            showFullscreenChannels = false
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            showFullscreenChannels = true
                                            showFullscreenEpg = false
                                            true
                                        }
                                        KeyEvent.KEYCODE_BACK -> {
                                            if (showFullscreenEpg || showFullscreenChannels) {
                                                showFullscreenEpg = false
                                                showFullscreenChannels = false
                                                true
                                            } else false
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .focusable()
                    } else {
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    }
                )

                if (!isFullscreen) {
                    if (playMode is AppViewModel.PlayMediaMode.ArchivePlay) {
                        ArchiveBanner(viewModel)
                    }
                    EPGSpoiler(selectedChannel, archiveSchedule, isEpgExpanded, { isEpgExpanded = it }, viewModel)
                    CategorySelector(allCategories, selectedCategory, viewModel)
                    ChannelsList(filteredChannels, selectedChannel, parentalEnabled, isParentalUnlocked, viewModel) { ch ->
                        showPinDialogForChannel = ch
                        pinInputValue = ""
                        pinError = false
                    }
                }
            }
        }

        if (isFullscreen) {
            FullscreenOverlays(showFullscreenEpg, showFullscreenChannels, selectedChannel, archiveSchedule, filteredChannels, { showFullscreenEpg = it }, { showFullscreenChannels = it }, viewModel)
        }

        if (showPinDialogForChannel != null) {
            PinDialog(showPinDialogForChannel, pinInputValue, pinError, { pinInputValue = it; pinError = false }, { showPinDialogForChannel = null }, viewModel)
        }
    }
}

private fun getStreamUrl(playMode: AppViewModel.PlayMediaMode, selectedChannel: Channel?): String? {
    return when (playMode) {
        is AppViewModel.PlayMediaMode.ArchivePlay -> {
            val base = selectedChannel?.streamUrl
            if (base != null) {
                val archiveTs = playMode.episode.startTimeMs / 1000
                "$base?utc=$archiveTs"
            } else null
        }
        is AppViewModel.PlayMediaMode.DirectLive -> selectedChannel?.streamUrl
    }
}

private fun toggleResizeMode(current: Int, viewModel: AppViewModel) {
    val next = when (current) {
        0 -> 3
        3 -> 4
        else -> 0
    }
    viewModel.setVideoResizeMode(next)
}

@Composable
private fun ArchiveBanner(viewModel: AppViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyBlue.copy(alpha = 0.2f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Filled.History, contentDescription = null, tint = SkyBlue)
                Text("Вы смотрите запись из архива", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = { viewModel.switchBackToLive() },
                colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("В эфир", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EPGSpoiler(selectedChannel: Channel?, archiveSchedule: List<ProgramEpisode>, isExpanded: Boolean, onToggle: (Boolean) -> Unit, viewModel: AppViewModel) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .glassmorphism(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle(!isExpanded) }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedChannel?.let { "EPG: ${it.name}" } ?: "Программа передач", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Icon(imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            AnimatedVisibility(visible = isExpanded) {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(archiveSchedule) { program ->
                        var isItemFocused by remember { mutableStateOf(false) }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isItemFocused = it.isFocused }
                                .clickable(enabled = program.isArchive) { viewModel.selectArchiveEpisode(program) }
                                .focusable(),
                            color = if (isItemFocused) SlateFocus else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${program.startTimeString} - ${program.title}", 
                                color = if (program.isArchive) SkyBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), 
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySelector(categories: List<String>, selected: String, viewModel: AppViewModel) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { category ->
            val isSel = selected == category
            var isFoc by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .onFocusChanged { isFoc = it.isFocused }
                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(if (isFoc) 2.dp else 1.dp, if (isFoc) MaterialTheme.colorScheme.onBackground else Color.Transparent), RoundedCornerShape(16.dp))
                    .clickable { viewModel.selectCategory(category) }
                    .focusable()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(category, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelsList(channels: List<Channel>, selected: Channel?, parental: Boolean, unlocked: Boolean, viewModel: AppViewModel, onPin: (Channel) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .glassmorphism(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(channels) { channel ->
            val isSel = selected?.id == channel.id
            var isFoc by remember { mutableStateOf(false) }
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val highlightColor = if (isDark) SlateFocus else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFoc = it.isFocused }
                    .combinedClickable(
                        onClick = { if (channel.isLocked && parental && !unlocked) onPin(channel) else viewModel.selectChannel(channel) },
                        onLongClick = { viewModel.toggleChannelLock(channel) }
                    )
                    .focusable(),
                colors = CardDefaults.cardColors(containerColor = if (isSel || isFoc) highlightColor else Color.Transparent),
                border = BorderStroke(2.dp, if (isFoc) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) else if (isSel) CinemaAmber else Color.Transparent)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        AsyncImage(model = channel.logoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(channel.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (channel.isLocked) Icon(Icons.Default.Lock, contentDescription = null, tint = LiveRed, modifier = Modifier.size(16.dp))
                    IconButton(onClick = { viewModel.toggleFavorite(channel) }) {
                        Icon(if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null, tint = CinemaAmber)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.FullscreenOverlays(showEpg: Boolean, showChannels: Boolean, selected: Channel?, episodes: List<ProgramEpisode>, channels: List<Channel>, onEpg: (Boolean) -> Unit, onChannels: (Boolean) -> Unit, viewModel: AppViewModel) {
    AnimatedVisibility(visible = showEpg, enter = slideInVertically { -it }, exit = slideOutVertically { -it }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .glassmorphism(
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    backgroundColor = Color.Black.copy(alpha = 0.5f),
                    borderColor = Color.White.copy(alpha = 0.1f)
                )
                .padding(24.dp)
                .clickable { onEpg(false) }
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(episodes) { ep ->
                    var isItemFocused by remember { mutableStateOf(false) }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isItemFocused = it.isFocused }
                            .clickable(enabled = ep.isArchive) { viewModel.selectArchiveEpisode(ep) }
                            .focusable(),
                        color = if (isItemFocused) SlateFocus else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${ep.startTimeString} ${ep.title}", 
                            color = if (isItemFocused) Color.White else Color.White.copy(alpha = 0.8f), 
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
    AnimatedVisibility(visible = showChannels, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter)) {
        Box(                
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .glassmorphism(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    backgroundColor = Color.Black.copy(alpha = 0.5f),
                    borderColor = Color.White.copy(alpha = 0.1f)
                )
                .padding(16.dp)) {
            LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(channels) { ch ->
                    var isItemFocused by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .onFocusChanged { isItemFocused = it.isFocused }
                            .clickable { viewModel.selectChannel(ch); onChannels(false) }
                            .focusable(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isItemFocused) CinemaAmber else Color.Transparent
                        ),
                        border = if (isItemFocused) BorderStroke(2.dp, Color.White) else null
                    ) {
                        Text(
                            ch.name, 
                            color = if (isItemFocused) Color.Black else Color.White, 
                            modifier = Modifier.padding(12.dp), 
                            maxLines = 1,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDialog(channel: Channel?, pin: String, error: Boolean, onPinChange: (String) -> Unit, onDismiss: () -> Unit, viewModel: AppViewModel) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN") },
        text = {
            Column {
                OutlinedTextField(value = pin, onValueChange = onPinChange, visualTransformation = PasswordVisualTransformation(), isError = error)
                if (error) Text("Error", color = Color.Red)
            }
        },
        confirmButton = {
            Button(onClick = { if (viewModel.unlockParentalSession(pin)) { channel?.let { viewModel.selectChannel(it) }; onDismiss() } }) { Text("OK") }
        }
    )
}
