package com.example.ui.components

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import java.io.File
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
import android.view.KeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ui.AppViewModel
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SkyBlue

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    streamUrl: String?,
    title: String,
    logoUrl: String? = null,
    mode: AppViewModel.PlayMediaMode,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    resizeMode: Int = 0,
    onToggleFullscreen: () -> Unit = {},
    onToggleResizeMode: () -> Unit = {},
    onPreviousChannel: (() -> Unit)? = null,
    onNextChannel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // Custom playback controls visible state
    var showControls by remember { mutableStateOf(true) }

    // Track playback progress for recording and archive
    var currentPos by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }

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

    // Instantiate ExoPlayer safely
    val exoPlayer = remember(context) {
        try {
            val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context.applicationContext)
                .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            
            ExoPlayer.Builder(context.applicationContext, renderersFactory)
                .build()
                .apply {
                    playWhenReady = true
                    repeatMode = Player.REPEAT_MODE_OFF
                }
        } catch (e: Throwable) {
            Log.e("VideoPlayer", "Failed to build ExoPlayer instance", e)
            null
        }
    } as ExoPlayer?

    // Set up Player listeners cleanly
    DisposableEffect(exoPlayer) {
        val player = exoPlayer
        if (player == null) {
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isPlaying = player.isPlaying
                    isLoading = state == Player.STATE_BUFFERING
                    hasError = false
                    currentPos = player.currentPosition
                    totalDuration = player.duration
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
            try {
                player.addListener(listener)
            } catch (e: Throwable) {
                Log.e("VideoPlayer", "Failed to add listener", e)
            }
            onDispose {
                try {
                    player.removeListener(listener)
                    player.stop()
                    player.release()
                } catch (e: Throwable) {
                    Log.e("VideoPlayer", "Error releasing player", e)
                }
            }
        }
    }

    // Periodically update progress if playing
    LaunchedEffect(exoPlayer, isPlaying) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (isPlaying) {
            while (true) {
                currentPos = player.currentPosition
                totalDuration = player.duration
                kotlinx.coroutines.delay(500)
            }
        }
    }

    // Auto-hide controls panel
    LaunchedEffect(showControls, isPlaying, isFullscreen) {
        if (showControls && isPlaying) {
            kotlinx.coroutines.delay(4000)
            showControls = false
        }
        if (!showControls && isFullscreen) {
            focusRequester.requestFocus()
        }
    }

    // Prepare and play the media stream URL safely
    LaunchedEffect(streamUrl, mode, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (streamUrl.isNullOrEmpty()) {
            try {
                player.stop()
            } catch (e: Throwable) {
                Log.e("VideoPlayer", "Error stopping player", e)
            }
            return@LaunchedEffect
        }

        hasError = false
        isLoading = true

        try {
            val mediaUri = when (mode) {
                else -> Uri.parse(streamUrl)
            }

            val lowUrl = streamUrl.lowercase()
            val mediaItem = when {
                lowUrl.contains(".m3u8") || lowUrl.contains("m3u8") -> {
                    MediaItem.Builder()
                        .setUri(mediaUri)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build()
                }
                lowUrl.contains(".rtsp") || streamUrl.startsWith("rtsp://") -> {
                    MediaItem.Builder()
                        .setUri(mediaUri)
                        .setMimeType(MimeTypes.APPLICATION_RTSP)
                        .build()
                }
                else -> {
                    MediaItem.fromUri(mediaUri)
                }
            }

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Throwable) {
            Log.e("VideoPlayer", "Error preparing or playing media: $streamUrl", e)
            hasError = true
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .testTag("video_player_box")
            .background(Color.Black)
            .then(
                if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.aspectRatio(16f / 9f)
                }
            )
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    if (!showControls) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                onPreviousChannel?.invoke()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                onNextChannel?.invoke()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                showControls = true
                                true
                            }
                            else -> false
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                        showControls = false
                        true
                    } else false
                } else false
            }
            .focusable()
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
            if (exoPlayer == null) {
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
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Ошибка инициализации",
                        tint = LiveRed,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ошибка инициализации плеера",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Проблема с загрузкой аппаратного декодера",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Android View hosting the ExoPlayer PlayerView
                AndroidView(
                    factory = { ctx ->
                        try {
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false // Force disabled default controller
                                setBackgroundColor(android.graphics.Color.BLACK)
                                this.resizeMode = resizeMode
                                keepScreenOn = true
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        } catch (e: Throwable) {
                            Log.e("VideoPlayer", "Error constructing PlayerView", e)
                            PlayerView(ctx)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        try {
                            view.player = exoPlayer
                            view.resizeMode = resizeMode
                        } catch (e: Throwable) {
                            Log.e("VideoPlayer", "Error updating PlayerView player", e)
                        }
                    },
                    onRelease = { view ->
                        try {
                            view.player = null
                        } catch (e: Throwable) {
                            Log.e("VideoPlayer", "Error releasing PlayerView player", e)
                        }
                    }
                )

                // Transparent background click capturing layer to catch touch actions safely on AndroidView
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showControls = !showControls }
                )

                // Dynamic custom Playback Overlay HUD with fade transitions
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        // 1. TOP HUD ROW: Title, Logo & Mode indicators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                if (isFullscreen) {
                                    // Easily exit fullscreen
                                    var isBackFocused by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = onToggleFullscreen,
                                        modifier = Modifier
                                            .onFocusChanged { isBackFocused = it.isFocused }
                                            .background(
                                                if (isBackFocused) CinemaAmber else Color.Black.copy(alpha = 0.5f), 
                                                CircleShape
                                            )
                                            .size(36.dp)
                                            .focusable()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowBack,
                                            contentDescription = "Выйти из полноэкранного режима",
                                            tint = if (isBackFocused) Color.Black else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (!logoUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = logoUrl,
                                            contentDescription = title,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.15f))
                                                .padding(2.dp)
                                        )
                                    }
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (mode is AppViewModel.PlayMediaMode.ArchivePlay) {
                                    Text(
                                        text = "Запись: ${mode.episode.title}",
                                        color = SkyBlue,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (badgeText, badgeColor) = when (mode) {
                                    is AppViewModel.PlayMediaMode.ArchivePlay -> "АРХИВ" to SkyBlue
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
                            }
                        }

                        // 2. CENTER PANEL: Previous, Play/Pause, Next channel controls
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Channel Button
                                var isPrevFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { onPreviousChannel?.invoke() },
                                    modifier = Modifier
                                        .onFocusChanged { isPrevFocused = it.isFocused }
                                        .background(if (isPrevFocused) CinemaAmber else Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(50.dp)
                                        .focusable()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Предыдущий канал",
                                        tint = if (isPrevFocused) Color.Black else Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                // Play/Pause button
                                var isPlayFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        exoPlayer.let { player ->
                                            if (player.isPlaying) player.pause() else player.play()
                                        }
                                    },
                                    modifier = Modifier
                                        .onFocusChanged { isPlayFocused = it.isFocused }
                                        .background(if (isPlayFocused) Color.White else CinemaAmber, CircleShape)
                                        .size(58.dp)
                                        .focusable()
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                                        tint = Color.Black,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                // Next Channel Button
                                var isNextFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { onNextChannel?.invoke() },
                                    modifier = Modifier
                                        .onFocusChanged { isNextFocused = it.isFocused }
                                        .background(if (isNextFocused) CinemaAmber else Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(50.dp)
                                        .focusable()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Следующий канал",
                                        tint = if (isNextFocused) Color.Black else Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                        // 3. BOTTOM PANEL: SEEKBAR & FULLSCREEN CONTROLLER
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isMediaSeekable = mode is AppViewModel.PlayMediaMode.ArchivePlay
                            if (isMediaSeekable && totalDuration > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = formatTime(currentPos),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )

                                    Slider(
                                        value = currentPos.toFloat().coerceIn(0f, totalDuration.toFloat()),
                                        onValueChange = { newValue ->
                                            exoPlayer.seekTo(newValue.toLong())
                                        },
                                        valueRange = 0f..totalDuration.toFloat(),
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = CinemaAmber,
                                            activeTrackColor = CinemaAmber,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                            activeTickColor = Color.Transparent,
                                            inactiveTickColor = Color.Transparent
                                        )
                                    )

                                    Text(
                                        text = formatTime(totalDuration),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                var isFullFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = onToggleFullscreen,
                                    modifier = Modifier
                                        .onFocusChanged { isFullFocused = it.isFocused }
                                        .background(if (isFullFocused) CinemaAmber else Color.Black.copy(alpha = 0.7f), CircleShape)
                                        .size(38.dp)
                                        .focusable()
                                        .testTag("toggle_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                        contentDescription = if (isFullscreen) "Выйти из полноэкранного режима" else "Войти в полноэкранный режим",
                                        tint = if (isFullFocused) Color.Black else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Aspect Ratio Toggle
                                var isResizeFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = onToggleResizeMode,
                                    modifier = Modifier
                                        .onFocusChanged { isResizeFocused = it.isFocused }
                                        .background(if (isResizeFocused) SkyBlue else Color.Black.copy(alpha = 0.7f), CircleShape)
                                        .size(38.dp)
                                        .focusable()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AspectRatio,
                                        contentDescription = "Соотношение сторон",
                                        tint = if (isResizeFocused) Color.Black else CinemaAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
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

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
