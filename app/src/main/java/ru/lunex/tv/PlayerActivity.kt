package ru.lunex.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PlayerScreen(initialUrl = intent.getStringExtra("stream_url"), mediaId = intent.getStringExtra("media_id"), nextUrl = intent.getStringExtra("next_stream_url"), nextId = intent.getStringExtra("next_media_id"), onClose = { finish() }) } }
    }
}

@Composable
private fun PlayerScreen(initialUrl: String?, mediaId: String?, nextUrl: String?, nextId: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    var address by remember { mutableStateOf(initialUrl.orEmpty()) }
    var showAddressEditor by remember { mutableStateOf(initialUrl.isNullOrBlank()) }
    var currentUrl by remember { mutableStateOf(initialUrl?.takeIf { it.startsWith("https://", ignoreCase = true) }) }
    var error by remember { mutableStateOf("") }
    var playbackStatus by remember { mutableStateOf("Ожидание ссылки") }
    val history = remember(context) { WatchHistory(context) }
    val settings = remember(context) { CinemaSettings(context) }
    var playingId by remember { mutableStateOf(mediaId) }
    var switchedToNext by remember { mutableStateOf(false) }
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    if (settings.load().rememberPosition) playingId?.let { history.save(it, player.currentPosition, player.duration.coerceAtLeast(0)) }
                    player.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(player, mediaId) {
        while (true) {
            delay(5000)
            if (settings.load().rememberPosition) playingId?.let { history.save(it, player.currentPosition, player.duration.coerceAtLeast(0)) }
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                playbackStatus = when (playbackState) {
                    Player.STATE_BUFFERING -> "Загрузка видео…"
                    Player.STATE_READY -> "Видео готово"
                    Player.STATE_ENDED -> {
                        if (settings.load().rememberPosition) playingId?.let { history.save(it, player.duration.coerceAtLeast(0), player.duration.coerceAtLeast(0)) }
                        if (!switchedToNext && settings.load().autoplayNextEpisode && nextUrl?.startsWith("https://", true) == true) {
                            switchedToNext = true
                            playingId = nextId
                            currentUrl = nextUrl
                            "Следующая серия…"
                        } else "Воспроизведение завершено"
                    }
                    else -> "Ожидание видео"
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                playbackStatus = "Ошибка воспроизведения: ${error.errorCodeName}"
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    DisposableEffect(player) {
        onDispose {
            if (settings.load().rememberPosition) playingId?.let { history.save(it, player.currentPosition, player.duration.coerceAtLeast(0)) }
            player.release()
        }
    }
    LaunchedEffect(currentUrl) {
        player.stop()
        player.clearMediaItems()
        currentUrl?.let { url ->
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            val saved = if (settings.load().rememberPosition) playingId?.let { history.get(it) } else null
            if (saved != null && saved.positionMs > 0 && (saved.durationMs == 0L || saved.positionMs < saved.durationMs - 10000)) {
                player.seekTo(saved.positionMs)
            }
            player.playWhenReady = true
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFF0B1019)).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onClose) { Text("Назад") }
            Text("Lunex TV · Плеер", color = Color.White)
        }
        if (showAddressEditor) {
        OutlinedTextField(
            value = address, onValueChange = { address = it; error = "" },
            label = { Text("Прямая HTTPS-ссылка на MP4 или HLS") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                val url = address.trim()
                if (url.startsWith("https://", ignoreCase = true)) {
                    currentUrl = url
                    error = ""
                } else {
                    error = "Укажите ссылку, начинающуюся с https://"
                }
            }) { Text("Воспроизвести") }
            Button(onClick = { currentUrl = null }) { Text("Стоп") }
        }
        }
        if (!showAddressEditor) {
            Button(onClick = { showAddressEditor = true }) { Text("Другая ссылка") }
        }
        if (error.isNotEmpty()) Text(error, color = Color(0xFFFFB4AB))
        Text(playbackStatus, color = Color(0xFFB9C4D5))
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
