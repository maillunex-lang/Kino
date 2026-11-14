package ru.lunex.tv

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ink = Color(0xFF0B1019)
private val panel = Color(0xFF202B3B)
private val soft = Color(0xFFB9C4D5)
private val accent = Color(0xFF67D6CB)

/** Cinema 2.0 home uses only the user's own library, never invented catalog entries. */
@Composable
fun CinemaHomeScreen(
    entries: List<LibraryEntry>,
    library: LocalLibrary,
    onOpen: (LibraryEntry) -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val history = remember(context) { WatchHistory(context) }
    val settings = remember(context) { CinemaSettings(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val progress = remember(entries, refresh) { history.recent().toMap() }
    val continueItems = entries.filter { item ->
        val p = progress[item.id] ?: return@filter false
        p.positionMs > 0 && (settings.load().showCompletedInContinue ||
            p.durationMs <= 0 || p.positionMs < p.durationMs - 10000)
    }.sortedBy { entry -> history.recent().indexOfFirst { it.first == entry.id }.let { if (it < 0) Int.MAX_VALUE else it } }
    val movies = entries.filter { it.kind == "Фильм" }
    val series = entries.filter { it.kind == "Сериал" }
    val featured = continueItems.firstOrNull() ?: entries.lastOrNull()
    val play: (LibraryEntry) -> Unit = { item ->
        context.startActivity(Intent(context, PlayerActivity::class.java)
            .putExtra("stream_url", item.url)
            .putExtra("media_id", item.id)
            .putExtra("media_title", item.title))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF234B55), Color(0xFF172537), ink)),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("LUNEX  /  CINEMA 2.0", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (featured != null) {
                    Text(featured.title, color = Color.White, fontSize = 30.sp,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(featured.kind + "  ·  В вашей библиотеке", color = soft)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { play(featured) }) { Text("▶ Смотреть") }
                        Button(onClick = { onOpen(featured) }) { Text("Подробнее") }
                    }
                } else {
                    Text("Ваш личный кинотеатр", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Добавьте фильм или сериал, чтобы увидеть его здесь.", color = soft)
                    Button(onClick = { onNavigate("Моя библиотека") }) { Text("Добавить видео") }
                }
            }
        }
        if (continueItems.isNotEmpty()) {
            item { Shelf("Продолжить просмотр", continueItems, progress, onOpen, play) }
        }
        if (entries.isNotEmpty()) {
            item { Shelf("Моя коллекция", entries.asReversed(), progress, onOpen, play) }
        }
        if (movies.isNotEmpty()) {
            item { Shelf("Фильмы", movies, progress, onOpen, play) }
        }
        if (series.isNotEmpty()) {
            item { Shelf("Сериалы", series, progress, onOpen, play) }
        }
        val favorites = entries.filter { library.isFavorite(it.id) }
        if (favorites.isNotEmpty()) {
            item { Shelf("Избранное", favorites, progress, onOpen, play) }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Открыть раздел", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Поиск", "Киноисточники", "Онлайн-каталог", "Моя библиотека", "Настройки")) { section ->
                        Button(onClick = { onNavigate(section) }) { Text(section) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Shelf(
    title: String,
    entries: List<LibraryEntry>,
    progress: Map<String, WatchProgress>,
    onOpen: (LibraryEntry) -> Unit,
    onPlay: (LibraryEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(entries, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier.width(200.dp).height(168.dp)
                        .clickable { onOpen(entry) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize().background(panel).padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(entry.kind.uppercase(), color = accent, fontSize = 11.sp)
                        Text(entry.title, color = Color.White, fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val p = progress[entry.id]
                        if (p != null && p.durationMs > 0) {
                            LinearProgressIndicator(
                                progress = { (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Button(onClick = { onPlay(entry) }) { Text("▶ Смотреть") }
                    }
                }
            }
        }
    }
}
