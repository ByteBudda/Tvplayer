package com.example.ui.screens

import androidx.compose.foundation.background
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

                            // Actions: Refresh or Delete
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
}
