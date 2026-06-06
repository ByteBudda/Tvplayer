package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Recording
import com.example.ui.AppViewModel
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SlateCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()

    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatSize(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.getDefault(), "%.1f МБ", mb)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Записанные Архивы Эфира",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Локальная библиотека записанных телетрансляций",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (recordings.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileDownloadOff,
                            contentDescription = "Пусто",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Библиотека записей пуста",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Нажмите круглую кнопку записи в плеере во время просмотра",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            items(recordings) { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recording_card_${rec.id}"),
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
                        // Icon status indicator
                        val icon = if (rec.status == "Recording") {
                            Icons.Filled.FiberManualRecord
                        } else {
                            Icons.Filled.VideoLibrary
                        }
                        val color = if (rec.status == "Recording") {
                            LiveRed
                        } else {
                            CinemaAmber
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = "Статус",
                            tint = color,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = rec.channelName,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sdf.format(Date(rec.startTime)),
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Box(
                                    modifier = Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.Gray)
                                )
                                Text(
                                    text = if (rec.status == "Recording") "Пишется..." else formatDuration(rec.durationMs),
                                    color = if (rec.status == "Recording") LiveRed else CinemaAmber,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (rec.fileSize > 0) {
                                    Box(
                                        modifier = Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.Gray)
                                    )
                                    Text(
                                        text = formatSize(rec.fileSize),
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Playback / Delete buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (rec.status != "Recording") {
                                IconButton(
                                    onClick = { viewModel.selectRecordingForPlay(rec) },
                                    modifier = Modifier.testTag("play_recording_${rec.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Воспроизвести запись",
                                        tint = CinemaAmber,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteRecording(rec.id) },
                                modifier = Modifier.testTag("delete_recording_${rec.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Удалить запись",
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
