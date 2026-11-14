package ru.lunex.tv

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ContinueWatchingScreen(
    entries: List<LibraryEntry>,
    includeCompleted: Boolean? = null,
    showTitle: Boolean = true
) {
    val context = LocalContext.current
    val history = remember(context) { WatchHistory(context) }
    val settings = remember(context) { CinemaSettings(context) }
    val showCompleted = includeCompleted ?: settings.load().showCompletedInContinue
    // Reload on return from the player, not only on the initial composition.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val recent = remember(entries, refresh, showCompleted) {
        val known = entries.associateBy { it.id }
        history.recent().mapNotNull { (id, progress) ->
            val entry = known[id] ?: return@mapNotNull null
            if (progress.positionMs <= 0L) return@mapNotNull null
            val completed = progress.durationMs > 0 &&
                progress.positionMs >= progress.durationMs - 10000
            if (!showCompleted && completed) return@mapNotNull null
            entry to progress
        }.take(20)
    }
    if (showTitle) Text("Продолжить просмотр", color = Color.White)
    if (recent.isEmpty()) {
        Text("Начните смотреть фильм из своей библиотеки. Здесь появится продолжение.", color = Color.LightGray)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
        items(recent, key = { it.first.id }) { (entry, progress) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = {
                    context.startActivity(Intent(context, PlayerActivity::class.java)
                        .putExtra("stream_url", entry.url)
                        .putExtra("media_id", entry.id)
                        .putExtra("media_title", entry.title))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("▶ ${entry.title} · ${progress.positionMs / 60000} мин.")
                }
                if (progress.durationMs > 0L) {
                    LinearProgressIndicator(
                        progress = { (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
