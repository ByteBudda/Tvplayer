package com.example.ui.components

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.AppViewModel
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SkyBlue
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    streamUrl: String?,
    title: String,
    mode: AppViewModel.PlayMediaMode,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onToggleRecording: () -> Unit = {}
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // Pulse animation for Recording or Live Indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Instantiate ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Set up Player listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isPlaying = exoPlayer.isPlaying
                isLoading = state == Player.STATE_BUFFERING
                hasError = false
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("VideoPlayer", "Player error occur", error)
                hasError = true
                isLoading = false
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Prepare and play the URI based on URL / File path
    LaunchedEffect(streamUrl, mode) {
        if (streamUrl.isNullOrEmpty()) {
            exoPlayer.stop()
            return@LaunchedEffect
        }

        hasError = false
        isLoading = true

        val mediaUri = when (mode) {
            is AppViewModel.PlayMediaMode.RecordingPlay -> {
                // Play local file
                val file = File(mode.recording.filePath)
                if (file.exists()) Uri.fromFile(file) else Uri.parse(streamUrl)
            }
            else -> Uri.parse(streamUrl)
        }

        val mediaItem = MediaItem.fromUri(mediaUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    Box(
        modifier = modifier
            .testTag("video_player_box")
            .background(Color.Black)
            .aspectRatio(16f / 9f)
    ) {
        if (streamUrl.isNullOrEmpty()) {
            // Visual Empty Placeholder
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF14171E), Color(0xFF090A0D))
                        )
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Tv,
                    contentDescription = "Телевизор",
                    tint = CinemaAmber.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Выберите канал для просмотра",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Прямой эфир и архив передач",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Android View hosting the ExoPlayer PlayerView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        // Custom styling to fit Cinema Dark theme
                        setBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.player = exoPlayer
                }
            )

            // Dynamic Playback Overlay HUD
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // TOP HUD: Channel Title & Action Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        
                        // Archive notice
                        if (mode is AppViewModel.PlayMediaMode.ArchivePlay) {
                            Text(
                                text = "Запись: ${mode.episode.title}",
                                color = SkyBlue,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Badge Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Mode Badge
                        val (badgeText, badgeColor) = when (mode) {
                            is AppViewModel.PlayMediaMode.ArchivePlay -> "АРХИВ" to SkyBlue
                            is AppViewModel.PlayMediaMode.RecordingPlay -> "ЗАПИСЬ" to CinemaAmber
                            is AppViewModel.PlayMediaMode.DirectLive -> "ЭФИР" to LiveRed
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor.copy(alpha = if (mode is AppViewModel.PlayMediaMode.DirectLive) pulseAlpha else 1.0f))
                                )
                                Text(
                                    text = badgeText,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Physical Live Recording button inside the player
                        if (mode is AppViewModel.PlayMediaMode.DirectLive) {
                            IconButton(
                                onClick = onToggleRecording,
                                modifier = Modifier
                                    .testTag("record_channel_button")
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FiberManualRecord,
                                    contentDescription = "Записать передачу",
                                    tint = if (isRecording) LiveRed.copy(alpha = pulseAlpha) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Loading Spinner overlay
                if (isLoading) {
                    CircularProgressIndicator(
                        color = CinemaAmber,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }

                // Error Notice overlay
                if (hasError) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Ошибка воспроизведения",
                            tint = LiveRed,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Канал недоступен",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Попробуйте другие каналы или добавьте свой плейлист",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
