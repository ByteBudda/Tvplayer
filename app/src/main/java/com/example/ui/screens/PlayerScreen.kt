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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.view.KeyEvent
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
    val activeRecordingUrls by viewModel.activeRecordingUrls.collectAsState()
    val playMode by viewModel.playMode.collectAsState()
    val resizeMode by viewModel.videoResizeMode.collectAsState()
    
    val parentalEnabled by viewModel.parentalEnabled.collectAsState()
    val isParentalUnlocked by viewModel.isParentalSessionUnlocked.collectAsState()

    var showPinDialogForChannel by remember { mutableStateOf<Channel?>(null) }
    var pinInputValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Full-screen toggle state
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    // Local visibility for fullscreen overlays
    var showFullscreenEpg by remember { mutableStateOf(false) }
    var showFullscreenChannels by remember { mutableStateOf(false) }
    // EPG Accordion/Spoiler toggle state
    var isEpgExpanded by remember { mutableStateOf(false) }

    // Sync local fullscreen state to ViewModel for platform-level system bar control
    LaunchedEffect(isFullscreen) {
        viewModel.setFullscreen(isFullscreen)
    }

    // Filter channels based on chosen category
    val filteredChannels = remember(channels, selectedCategory) {
        when (selectedCategory) {
            "Все" -> channels
            "★ Избранные" -> channels.filter { it.isFavorite }
            else -> channels.filter { it.category == selectedCategory }
        }
    }

    // Auto-select first channel on launch if nothing is selected
    LaunchedEffect(channels) {
        if (selectedChannel == null && channels.isNotEmpty()) {
            val nonLockedChannel = channels.firstOrNull { !it.isLocked || !parentalEnabled }
            viewModel.selectChannel(nonLockedChannel ?: channels.first())
        }
    }

    // Fast Channel switching lambda callbacks
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

    // Handle back press to exit overlays or fullscreen
    BackHandler(enabled = isFullscreen) {
        if (showFullscreenEpg) {
            showFullscreenEpg = false
        } else if (showFullscreenChannels) {
            showFullscreenChannels = false
        } else {
            viewModel.setFullscreen(false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background)
        ) {
            // SHARED VIDEO PLAYER SECTION
            val isCurrentRecording = selectedChannel?.let { viewModel.activeRecordingUrls.value.contains(it.streamUrl) } ?: false
            
            VideoPlayer(
                streamUrl = when (playMode) {
                    is AppViewModel.PlayMediaMode.RecordingPlay -> (playMode as AppViewModel.PlayMediaMode.RecordingPlay).recording.streamUrl
                    is AppViewModel.PlayMediaMode.ArchivePlay -> {
                        val base = selectedChannel?.streamUrl
                        if (base != null) {
                            val archiveTs = (playMode as AppViewModel.PlayMediaMode.ArchivePlay).episode.startTimeMs / 1000
                            "$base?utc=$archiveTs"
                        } else {
                            null
                        }
                    }
                    is AppViewModel.PlayMediaMode.DirectLive -> selectedChannel?.streamUrl
                },
                title = selectedChannel?.name ?: "ТВ Плеер",
                logoUrl = selectedChannel?.logoUrl,
                mode = playMode,
                isRecording = isCurrentRecording,
                isFullscreen = isFullscreen,
                resizeMode = resizeMode,
                onToggleFullscreen = { viewModel.setFullscreen(!isFullscreen) },
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
                modifier = if (isFullscreen) {
                    Modifier.weight(1f)
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -20) { // Swipe Up
                                    showFullscreenChannels = true
                                    showFullscreenEpg = false
                                } else if (dragAmount > 20) { // Swipe Down
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
                                        } else {
                                            false
                                        }
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .focusable()
                } else {
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                },
                onToggleRecording = { viewModel.toggleRecordingActiveChannel() }
            )

            if (!isFullscreen) {
                // NORMAL VIEW CONTENTS
                // 2. TIMESIFT ARCHIVE BANNER
                if (playMode is AppViewModel.PlayMediaMode.ArchivePlay) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = "Архив",
                                    tint = SkyBlue
                                )
                                Text(
                                    text = "Вы смотрите запись передачи из архива",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Button(
                                onClick = { viewModel.switchBackToLive() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LiveRed,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Вернуться в эфир", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // 3. EXPANDABLE SPOILER (EPG / TV ARCHIVE GUIDE)
                Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("epg_spoiler_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = SlateCard
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column {
                                    // Spoiler Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isEpgExpanded = !isEpgExpanded }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CalendarMonth,
                                                contentDescription = null,
                                                tint = CinemaAmber,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = selectedChannel?.let { "Телепрограмма и архив: ${it.name}" } ?: "Телепрограмма и архив передач",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isEpgExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = if (isEpgExpanded) "Свернуть" else "Развернуть",
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }

                                    // Expanded EPG list
                                    AnimatedVisibility(
                                        visible = isEpgExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 220.dp)
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (archiveSchedule.isEmpty()) {
                                                    item {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 16.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                "Телепрограмма недоступна",
                                                                color = Color.Gray,
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    items(archiveSchedule) { program ->
                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable(enabled = program.isArchive) {
                                                                    viewModel.selectArchiveEpisode(program)
                                                                },
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (program.isArchive) SlateFocus else Color.Transparent
                                                            )
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(8.dp)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = "${program.startTimeString} - ${program.endTimeString}",
                                                                        color = if (program.isArchive) SkyBlue else Color.White.copy(alpha = 0.5f),
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    
                                                                    if (program.isArchive) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(4.dp))
                                                                                .background(SkyBlue.copy(alpha = 0.15f))
                                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                                        ) {
                                                                            Text(
                                                                                "ЗАПИСЬ ТВ",
                                                                                color = SkyBlue,
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                fontWeight = FontWeight.Bold
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                
                                                                Text(
                                                                    text = program.title,
                                                                    color = Color.White,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis
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

                // 4. CATEGORY SELECTOR TABS
                val allCategories = remember(categories) { listOf("Все", "★ Избранные") + categories }
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCategories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else SlateCard)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                    } else {
                                        Modifier.background(SlateCard).border(
                                            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                            RoundedCornerShape(16.dp)
                                        )
                                    }
                                )
                                .clickable { viewModel.selectCategory(category) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 5. CHANNELS LISTING
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (filteredChannels.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 42.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TvOff,
                                    contentDescription = "Пусто",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Нет каналов в данной категории",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    items(filteredChannels) { channel ->
                        val isSelected = selectedChannel?.id == channel.id
                        val isRecording = activeRecordingUrls.contains(channel.streamUrl)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (channel.isLocked && parentalEnabled && !isParentalUnlocked) {
                                            showPinDialogForChannel = channel
                                            pinInputValue = ""
                                            pinError = false
                                        } else {
                                            viewModel.selectChannel(channel)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleChannelLock(channel)
                                    }
                                )
                                .testTag("channel_card_${channel.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SlateFocus else SlateCard
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, CinemaAmber)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Logo AsyncImage with Letter Circle fallback
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!channel.logoUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = channel.logoUrl,
                                            contentDescription = channel.name,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = channel.name.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = channel.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = channel.category ?: "Эфир",
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Badges
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isRecording) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(LiveRed)
                                        )
                                    }
                                    
                                    if (channel.isLocked && parentalEnabled) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = "Родительский контроль",
                                            tint = LiveRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(channel) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (channel.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                            contentDescription = "В избранное",
                                            tint = if (channel.isFavorite) CinemaAmber else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FULLSCREEN OVERLAYS
        if (isFullscreen) {
            // OVERLAY: Program Info (EPG)
            AnimatedVisibility(
                visible = showFullscreenEpg,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { showFullscreenEpg = false }
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Программа передач: ${selectedChannel?.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = CinemaAmber,
                            fontWeight = FontWeight.Bold
                        )
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(archiveSchedule) { program ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = program.startTimeString,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SkyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = program.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // OVERLAY: Channel List
            AnimatedVisibility(
                visible = showFullscreenChannels,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(bottom = 16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Выбор канала", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { showFullscreenChannels = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                            }
                        }
                        
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredChannels) { channel ->
                                val isNowSelected = selectedChannel?.id == channel.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.selectChannel(channel)
                                            showFullscreenChannels = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isNowSelected) SlateFocus else SlateCard.copy(alpha = 0.6f)
                                    ),
                                    border = BorderStroke(1.dp, if (isNowSelected) CinemaAmber else Color.White.copy(alpha = 0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = channel.logoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = channel.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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

    // Parental PIN Entry Dialog before playing channel
    if (showPinDialogForChannel != null) {
        AlertDialog(
            onDismissRequest = { showPinDialogForChannel = null },
            title = { Text("Родительский контроль") },
            text = {
                Column {
                    Text("Этот канал защищен PIN-кодом. Введите код для разблокировки:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInputValue,
                        onValueChange = { 
                            pinInputValue = it
                            pinError = false
                        },
                        label = { Text("Ведите PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth().testTag("parental_pin_field")
                    )
                    if (pinError) {
                        Text(
                            text = "Неверный PIN-код",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val correct = viewModel.unlockParentalSession(pinInputValue)
                        if (correct) {
                            val ch = showPinDialogForChannel
                            if (ch != null) {
                                viewModel.selectChannel(ch)
                            }
                            showPinDialogForChannel = null
                        } else {
                            pinError = true
                        }
                    },
                    modifier = Modifier.testTag("parental_pin_confirm")
                ) {
                    Text("Войти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialogForChannel = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
