package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.components.glassmorphism
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.SlateFocus
import java.io.BufferedReader
import java.io.InputStreamReader

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.EpgSource
import com.example.ui.theme.SlateFocus

@Composable
fun PlaylistsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var playlistType by remember { mutableStateOf("m3u") } // "m3u" or "xml"
    var isInputError by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Playlists, 1: EPG
    val epgSources by viewModel.epgSources.collectAsState()

    var showAddEpgDialog by remember { mutableStateOf(false) }
    var epgName by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var isEpgInputError by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                inputStream?.close()
                
                // Use the filename or a generic name
                val fileName = "Импорт ${System.currentTimeMillis() % 10000}"
                viewModel.importPlaylist(fileName, content, "m3u")
            } catch (e: Exception) {
                // Error handling could be added here (e.g., a snackbar)
            }
        }
    }

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
        // Header Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = CinemaAmber,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CinemaAmber
                    )
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Плейлисты", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onBackground) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("EPG Сервисы", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onBackground) }
            )
        }

        if (selectedTab == 0) {
            PlaylistsContent(
                playlists = playlists,
                isRefreshing = isRefreshing,
                onImport = { filePickerLauncher.launch("*/*") },
                onAdd = {
                    playlistName = ""
                    playlistUrl = ""
                    playlistType = "m3u"
                    isInputError = false
                    showAddDialog = true
                },
                onManageChannels = { playlistToManageChannels = it },
                onEdit = { 
                    playlistToEdit = it
                    editName = it.name
                    editUrl = it.url
                    editType = it.type
                    showEditDialog = true
                },
                onRefresh = { viewModel.refreshPlaylist(it.id) },
                onDelete = { viewModel.deletePlaylist(it.id) },
                viewModel = viewModel
            )
        } else {
            EpgSourcesContent(
                sources = epgSources,
                isRefreshing = isRefreshing,
                onAdd = {
                    epgName = ""
                    epgUrl = ""
                    isEpgInputError = false
                    showAddEpgDialog = true
                },
                onRefresh = { viewModel.refreshEpg() },
                onDelete = { viewModel.deleteEpgSource(it.id) },
                onToggle = { source, active -> viewModel.toggleEpgSource(source.id, active) }
            )
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
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showEditDialog = false }
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Редактировать плейлист",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Отмена")
                        }
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
                    }
                }
            }
        }
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
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
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Всего каналов: ${playlistChannels.size}",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        IconButton(onClick = { playlistToManageChannels = null }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onSurface
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
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            items(playlistChannels) { channel ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
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
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
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
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
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
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!channel.category.isNullOrBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            channel.category,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = channel.streamUrl,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Lock/Unlock Button
                                            IconButton(
                                                onClick = { viewModel.toggleChannelLock(channel) }
                                            ) {
                                                Icon(
                                                    imageVector = if (channel.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = "Блокировка",
                                                    tint = if (channel.isLocked) LiveRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

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

    // Modal Add EPG Dialog
    if (showAddEpgDialog) {
        AlertDialog(
            onDismissRequest = { showAddEpgDialog = false },
            title = { Text("Добавить EPG источник") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = epgName,
                        onValueChange = { epgName = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = epgUrl,
                        onValueChange = { epgUrl = it },
                        label = { Text("URL (XMLTV)") },
                        singleLine = true,
                        isError = isEpgInputError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (epgName.isNotBlank() && epgUrl.isNotBlank()) {
                            viewModel.addEpgSource(epgName, epgUrl)
                            showAddEpgDialog = false
                        } else {
                            isEpgInputError = true
                        }
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEpgDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun PlaylistsContent(
    playlists: List<Playlist>,
    isRefreshing: Boolean,
    onImport: () -> Unit,
    onAdd: () -> Unit,
    onManageChannels: (Playlist) -> Unit,
    onEdit: (Playlist) -> Unit,
    onRefresh: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    viewModel: AppViewModel
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Плейлисты IPTV",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Добавление M3U и XML файлов",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var isImportFocused by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier
                        .onFocusChanged { isImportFocused = it.isFocused }
                        .border(
                            if (isImportFocused) 2.dp else 0.dp,
                            if (isImportFocused) Color.White else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .focusable(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CinemaAmber),
                    border = BorderStroke(1.dp, CinemaAmber.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Импорт")
                }

                var isFabFocused by remember { mutableStateOf(false) }
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = CinemaAmber,
                    contentColor = Color.Black,
                    modifier = Modifier
                        .onFocusChanged { isFabFocused = it.isFocused }
                        .border(
                            if (isFabFocused) 2.dp else 0.dp,
                            if (isFabFocused) Color.White else Color.Transparent,
                            CircleShape
                        )
                        .focusable(),
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Добавить")
                }
            }
        }

        if (isRefreshing) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinemaAmber)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (playlists.isEmpty()) {
                    item { PlaylistEmptyState() }
                }

                items(playlists) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onManage = { onManageChannels(playlist) },
                        onEdit = { onEdit(playlist) },
                        onRefresh = { onRefresh(playlist) },
                        onDelete = { onDelete(playlist) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.PlaylistRemove, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Плейлисты не загружены", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Добавьте список (M3U/XML) для начала просмотра", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onManage: () -> Unit,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    viewModel: AppViewModel
) {
    var isCardFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isCardFocused) 1.02f else 1.0f, label = "focusScale")
    val backgroundColor = MaterialTheme.colorScheme.surface
    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isCardFocused = it.isFocused }
            .border(
                if (isCardFocused) 2.dp else 0.dp,
                if (isCardFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .focusable(),
        colors = CardDefaults.cardColors(containerColor = if (isCardFocused) highlightColor else backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (playlist.type == "m3u") Icons.Filled.FeaturedPlayList else Icons.Filled.Code,
                contentDescription = null,
                tint = if (playlist.isBuiltIn) CinemaAmber else SkyBlue,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(playlist.url, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                var fm by remember { mutableStateOf(false) }
                IconButton(onClick = onManage, modifier = Modifier.onFocusChanged { fm = it.isFocused }.background(if (fm) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color.Transparent, CircleShape).focusable()) {
                    Icon(Icons.Filled.List, null, tint = if (fm) CinemaAmber else SkyBlue)
                }
                var fe by remember { mutableStateOf(false) }
                IconButton(onClick = onEdit, modifier = Modifier.onFocusChanged { fe = it.isFocused }.background(if (fe) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color.Transparent, CircleShape).focusable()) {
                    Icon(Icons.Filled.Edit, null, tint = if (fe) MaterialTheme.colorScheme.onSurface else CinemaAmber)
                }
                var fr by remember { mutableStateOf(false) }
                IconButton(onClick = onRefresh, modifier = Modifier.onFocusChanged { fr = it.isFocused }.background(if (fr) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color.Transparent, CircleShape).focusable()) {
                    Icon(Icons.Filled.Refresh, null, tint = if (fr) CinemaAmber else MaterialTheme.colorScheme.onSurface)
                }
                if (!playlist.isBuiltIn) {
                    var fd by remember { mutableStateOf(false) }
                    IconButton(onClick = onDelete, modifier = Modifier.onFocusChanged { fd = it.isFocused }.background(if (fd) LiveRed.copy(alpha = 0.2f) else Color.Transparent, CircleShape).focusable()) {
                        Icon(Icons.Filled.Delete, null, tint = if (fd) MaterialTheme.colorScheme.onSurface else LiveRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgSourcesContent(
    sources: List<EpgSource>,
    isRefreshing: Boolean,
    onAdd: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: (EpgSource) -> Unit,
    onToggle: (EpgSource, Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "EPG Сервисы", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Источники для программы передач (XMLTV)", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var isRfFocused by remember { mutableStateOf(false) }
                IconButton(onClick = onRefresh, modifier = Modifier.onFocusChanged { isRfFocused = it.isFocused }.border(if (isRfFocused) 2.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape).focusable()) {
                    Icon(Icons.Filled.Refresh, null, tint = CinemaAmber)
                }

                var isFabFocused by remember { mutableStateOf(false) }
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = CinemaAmber,
                    contentColor = Color.Black,
                    modifier = Modifier.onFocusChanged { isFabFocused = it.isFocused }.border(if (isFabFocused) 2.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape).focusable(),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, null)
                }
            }
        }

        if (isRefreshing) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinemaAmber)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sources.isEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.RssFeed, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("EPG не настроен", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Добавьте URL XMLTV источника для получения программы", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                items(sources) { source ->
                    EpgSourceCard(source, onDelete = { onDelete(source) }, onToggle = { onToggle(source, it) })
                }
            }
        }
    }
}

@Composable
private fun EpgSourceCard(source: EpgSource, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }.border(if (isFocused) 2.dp else 0.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(16.dp)).focusable(),
        colors = CardDefaults.cardColors(containerColor = if (isFocused) SlateFocus else MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Language, null, tint = SkyBlue, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(source.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(source.url, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Switch(checked = source.isActive, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = CinemaAmber))
            Spacer(modifier = Modifier.width(8.dp))
            var fd by remember { mutableStateOf(false) }
            IconButton(onClick = onDelete, modifier = Modifier.onFocusChanged { fd = it.isFocused }.background(if (fd) LiveRed.copy(alpha = 0.2f) else Color.Transparent, CircleShape).focusable()) {
                Icon(Icons.Filled.Delete, null, tint = if (fd) MaterialTheme.colorScheme.onSurface else LiveRed)
            }
        }
    }
}
