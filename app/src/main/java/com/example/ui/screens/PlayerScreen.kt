package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    
    val parentalEnabled by viewModel.parentalEnabled.collectAsState()
    val isParentalUnlocked by viewModel.isParentalSessionUnlocked.collectAsState()

    var showPinDialogForChannel by remember { mutableStateOf<Channel?>(null) }
    var pinInputValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

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
            viewModel.selectChannel(channels.first { !it.isLocked || !parentalEnabled })
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. VIDEO PLAYER SECTION (TOP)
        val isCurrentRecording = selectedChannel?.let { viewModel.activeRecordingUrls.value.contains(it.streamUrl) } ?: false
        
        VideoPlayer(
            streamUrl = when (playMode) {
                is AppViewModel.PlayMediaMode.RecordingPlay -> (playMode as AppViewModel.PlayMediaMode.RecordingPlay).recording.streamUrl
                is AppViewModel.PlayMediaMode.ArchivePlay -> {
                    // Append archive trigger time parameter typically used in catch-up systems
                    val archiveTs = (playMode as AppViewModel.PlayMediaMode.ArchivePlay).episode.startTimeMs / 1000
                    "${selectedChannel?.streamUrl}?utc=$archiveTs"
                }
                is AppViewModel.PlayMediaMode.DirectLive -> selectedChannel?.streamUrl
            },
            title = selectedChannel?.name ?: "ТВ Плеер",
            mode = playMode,
            isRecording = isCurrentRecording,
            modifier = Modifier
                .fillMaxWidth(),
            onToggleRecording = {
                viewModel.toggleRecordingActiveChannel()
            }
        )

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

        // 3. CATEGORY SELECTOR TABS
        val allCategories = remember(categories) { listOf("Все", "★ Избранные") + categories }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
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

        // 4. SCREEN SPLIT PANEL: Channels Grid + Schedule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // Left block list: Channels Grid
            LazyColumn(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (filteredChannels.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
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
                            // Logo Placeholder or Letter circle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = channel.name.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
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

                            // Badges & Actions (Recording / Lock / Favorite)
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

            Spacer(modifier = Modifier.width(16.dp))

            // Right block list: EPB/Archive TV Guide
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .background(SlateCard, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Архив передач",
                    color = CinemaAmber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (archiveSchedule.isEmpty()) {
                        item {
                            Text(
                                "Телепрограмма недоступна",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                    
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
