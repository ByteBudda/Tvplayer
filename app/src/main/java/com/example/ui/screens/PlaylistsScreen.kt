package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.example.data.Playlist
import com.example.data.Channel
import com.example.ui.AppViewModel
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SlateCard

@Composable
fun PlaylistsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var playlistType by remember { mutableStateOf("m3u") } // "m3u" or "xml"
    var isInputError by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var playlistToEdit by remember { mutableStateOf<Playlist?>(null) }
    var editName by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("m3u") }

    var playlistToManageChannels by remember { mutableStateOf<Playlist?>(null) }
    var showAddChannelDialog by remember { mutableStateOf(false) }
    var channelName by remember { mutableStateOf("") }
    var channelUrl by remember { mutableStateOf("") }
    var channelCategory by remember { mutableStateOf("") }
    var channelLogo by remember { mutableStateOf("") }
    var isChannelInputError by remember { mutableStateOf(false) }

    var channelToEdit by remember { mutableStateOf<Channel?>(null) }
    var showEditChannelDialog by remember { mutableStateOf(false) }
    var editChannelName by remember { mutableStateOf("") }
    var editChannelUrl by remember { mutableStateOf("") }
    var editChannelCategory by remember { mutableStateOf("") }
    var editChannelLogo by remember { mutableStateOf("") }
    var isEditChannelInputError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Плейлисты IPTV",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Добавление файлов вещаний M3U и XML",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Add FAB
            FloatingActionButton(
                onClick = {
                    playlistName = ""
                    playlistUrl = ""
                    playlistType = "m3u"
                    isInputError = false
                    showAddDialog = true
                },
                containerColor = CinemaAmber,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_playlist_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Добавить")
            }
        }

        if (isRefreshing) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CinemaAmber)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (playlists.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistRemove,
                                contentDescription = "Пусто",
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Плейлисты не загружены",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Добавьте свой собственный список или подождите загрузки исходного",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                items(playlists) { playlist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_card_${playlist.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = SlateCard
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (playlist.type == "m3u") Icons.Filled.FeaturedPlayList else Icons.Filled.Code,
                                contentDescription = "Тип плейлиста",
                                tint = if (playlist.isBuiltIn) CinemaAmber else SkyBlue,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = playlist.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (playlist.isBuiltIn) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(CinemaAmber.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "АКТИВЕН",
                                                color = CinemaAmber,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                Text(
                                    text = playlist.url,
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            // Actions: Manage, Edit, Refresh, Delete
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Manage channels button
                                IconButton(
                                    onClick = {
                                        playlistToManageChannels = playlist
                                        viewModel.clearCleanResult()
                                    },
                                    modifier = Modifier.testTag("manage_channels_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.List,
                                        contentDescription = "Каналы",
                                        tint = SkyBlue
                                    )
                                }

                                // Edit playlist button
                                IconButton(
                                    onClick = {
                                        playlistToEdit = playlist
                                        editName = playlist.name
                                        editUrl = playlist.url
                                        editType = playlist.type
                                        showEditDialog = true
                                    },
                                    modifier = Modifier.testTag("edit_playlist_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Редактировать плейлист",
                                        tint = CinemaAmber
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.refreshPlaylist(playlist.id) },
                                    modifier = Modifier.testTag("refresh_playlist_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Обновить записи",
                                        tint = Color.White
                                    )
                                }

                                if (!playlist.isBuiltIn) {
                                    IconButton(
                                        onClick = { viewModel.deletePlaylist(playlist.id) },
                                        modifier = Modifier.testTag("delete_playlist_${playlist.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Удалить плейлист",
                                            tint = LiveRed
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

    // Modal Add Playlist Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Добавить плейлист (M3U / XML)") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("playlist_name_field")
                    )

                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = { playlistUrl = it },
                        label = { Text("Ссылка (URL) на список") },
                        singleLine = true,
                        isError = isInputError,
                        modifier = Modifier.fillMaxWidth().testTag("playlist_url_field")
                    )

                    Text(
                        text = "Формат данных:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = playlistType == "m3u",
                                onClick = { playlistType = "m3u" },
                                modifier = Modifier.testTag("radio_m3u")
                            )
                            Text("M3U/M3U8")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = playlistType == "xml",
                                onClick = { playlistType = "xml" },
                                modifier = Modifier.testTag("radio_xml")
                            )
                            Text("XML Playlist")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank() && playlistUrl.isNotBlank()) {
                            viewModel.addPlaylist(playlistName, playlistUrl, playlistType)
                            showAddDialog = false
                        } else {
                            isInputError = true
                        }
                    },
                    modifier = Modifier.testTag("playlist_add_confirm")
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Modal Edit Playlist Dialog
    if (showEditDialog && playlistToEdit != null) {
        val current = playlistToEdit!!
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать плейлист") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_playlist_name_field")
                    )

                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("Ссылка (URL) на список") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_playlist_url_field")
                    )

                    Text(
                        text = "Формат данных:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = editType == "m3u",
                                onClick = { editType = "m3u" }
                            )
                            Text("M3U/M3U8")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = editType == "xml",
                                onClick = { editType = "xml" }
                            )
                            Text("XML Playlist")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editUrl.isNotBlank()) {
                            viewModel.editPlaylist(
                                current.copy(
                                    name = editName,
                                    url = editUrl,
                                    type = editType
                                )
                            )
                            showEditDialog = false
                        }
                    },
                    modifier = Modifier.testTag("playlist_edit_confirm")
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Manage Channels Dialog
    if (playlistToManageChannels != null) {
        val playlist = playlistToManageChannels!!
        val allChannels by viewModel.allChannels.collectAsState()
        val playlistChannels = allChannels.filter { it.playlistId == playlist.id }
        val cleanResult by viewModel.cleanResultCount.collectAsState()

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { playlistToManageChannels = null }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF14171E)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Каналы: ${playlist.name}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Всего каналов: ${playlistChannels.size}",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        IconButton(onClick = { playlistToManageChannels = null }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Закрыть",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Add Channel Button
                        Button(
                            onClick = {
                                channelName = ""
                                channelUrl = ""
                                channelCategory = "Общие"
                                channelLogo = ""
                                isChannelInputError = false
                                showAddChannelDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Добавить", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                        }

                        // Auto-clean Button
                        Button(
                            onClick = {
                                viewModel.cleanUnavailableChannels(playlist.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isRefreshing
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoMode,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Автоочистка", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Display Clean Result if any
                    if (cleanResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinemaAmber.copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Очистка завершена! Удалено каналов: $cleanResult",
                                    color = CinemaAmber,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "ОК",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { viewModel.clearCleanResult() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isRefreshing) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CinemaAmber)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Сканирование и пинг каналов...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        // Channels List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (playlistChannels.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Каналы отсутствуют",
                                            color = Color.White.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            items(playlistChannels) { channel ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Visual Channel Logo with fallback
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!channel.logoUrl.isNullOrEmpty()) {
                                                coil.compose.AsyncImage(
                                                    model = channel.logoUrl,
                                                    contentDescription = channel.name,
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(3.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = channel.name.take(1).uppercase(),
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = channel.name,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!channel.category.isNullOrBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color.White.copy(alpha = 0.15f))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            channel.category,
                                                            color = Color.White.copy(alpha = 0.7f),
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = channel.streamUrl,
                                                color = Color.White.copy(alpha = 0.4f),
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Edit Button
                                            IconButton(
                                                onClick = {
                                                    channelToEdit = channel
                                                    editChannelName = channel.name
                                                    editChannelUrl = channel.streamUrl
                                                    editChannelCategory = channel.category ?: "Общие"
                                                    editChannelLogo = channel.logoUrl ?: ""
                                                    isEditChannelInputError = false
                                                    showEditChannelDialog = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Правка",
                                                    tint = SkyBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Delete Button
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteChannel(channel.id)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Удалить",
                                                    tint = LiveRed,
                                                    modifier = Modifier.size(18.dp)
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
    }

    // Sub-dialog: Add Channel
    if (showAddChannelDialog && playlistToManageChannels != null) {
        val playlist = playlistToManageChannels!!
        AlertDialog(
            onDismissRequest = { showAddChannelDialog = false },
            title = { Text("Добавить канал") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = channelName,
                        onValueChange = { channelName = it },
                        label = { Text("Название канала") },
                        singleLine = true,
                        isError = isChannelInputError && channelName.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = channelUrl,
                        onValueChange = { channelUrl = it },
                        label = { Text("Ссылка вещания (URL)") },
                        singleLine = true,
                        isError = isChannelInputError && channelUrl.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                                value = channelCategory,
                                onValueChange = { channelCategory = it },
                                label = { Text("Категория") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = channelLogo,
                                onValueChange = { channelLogo = it },
                                label = { Text("Ссылка на логотип (URL) (необязательно)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (channelName.isNotBlank() && channelUrl.isNotBlank()) {
                                    viewModel.addChannel(
                                        Channel(
                                            playlistId = playlist.id,
                                            name = channelName,
                                            streamUrl = channelUrl,
                                            logoUrl = if (channelLogo.isBlank()) null else channelLogo,
                                            category = if (channelCategory.isBlank()) "Общие" else channelCategory
                                        )
                                    )
                                    showAddChannelDialog = false
                        } else {
                            isChannelInputError = true
                        }
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChannelDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Sub-dialog: Edit Channel
    if (showEditChannelDialog && channelToEdit != null) {
        val current = channelToEdit!!
        AlertDialog(
            onDismissRequest = { showEditChannelDialog = false },
            title = { Text("Редактировать канал") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editChannelName,
                        onValueChange = { editChannelName = it },
                        label = { Text("Название канала") },
                        singleLine = true,
                        isError = isEditChannelInputError && editChannelName.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editChannelUrl,
                        onValueChange = { editChannelUrl = it },
                        label = { Text("Ссылка вещания (URL)") },
                        singleLine = true,
                        isError = isEditChannelInputError && editChannelUrl.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editChannelCategory,
                        onValueChange = { editChannelCategory = it },
                        label = { Text("Категория") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editChannelLogo,
                        onValueChange = { editChannelLogo = it },
                        label = { Text("Ссылка на логотип (URL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editChannelName.isNotBlank() && editChannelUrl.isNotBlank()) {
                            viewModel.updateChannel(
                                current.copy(
                                    name = editChannelName,
                                    streamUrl = editChannelUrl,
                                    logoUrl = if (editChannelLogo.isBlank()) null else editChannelLogo,
                                    category = if (editChannelCategory.isBlank()) "Общие" else editChannelCategory
                                )
                            )
                            showEditChannelDialog = false
                        } else {
                            isEditChannelInputError = true
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditChannelDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
