package ru.lunex.tv

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OnlineCatalogScreen() {
    val context = LocalContext.current
    val registry = remember(context) { SourceRegistry(context) }
    var endpoint by remember { mutableStateOf(registry.endpoints().firstOrNull().orEmpty()) }
    var items by remember { mutableStateOf(emptyList<CatalogItem>()) }
    var selected by remember { mutableStateOf<CatalogItem?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedSource by remember { mutableStateOf("Kinogo") }
    LaunchedEffect(refresh) {
        if (refresh == 0) return@LaunchedEffect
        loading = true
        error = ""
        try {
            items = withContext(Dispatchers.IO) {
                when (selectedSource) {
                    "Kinogo" -> listOf(KinogoPageSource().read("https://kinogo.parts"))
                    else -> HttpsJsonCatalogSource("Мой источник", endpoint.trim()).load()
                }
            }
            if (selectedSource == "Мой JSON") registry.save(listOf(endpoint.trim()))
        } catch (e: Exception) {
            error = "Не удалось загрузить каталог: ${e.message.orEmpty().take(120)}"
        } finally {
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh++ }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Text("Онлайн-каталог", color = Color.White)
            Text("Источник по умолчанию: kinogo.parts. Пока доступны метаданные страницы, не видеопотоки.", color = Color.LightGray)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Kinogo", "Мой JSON").forEach { name ->
                    Button(onClick = { selectedSource = name; refresh++ }) { Text(if (selectedSource == name) "✓ $name" else name) }
                }
            }
            if (selectedSource == "Мой JSON") {
                OutlinedTextField(endpoint, { endpoint = it }, label = { Text("HTTPS URL каталога") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            Button(onClick = { refresh++ }) { Text("Обновить каталог") }
            if (loading) CircularProgressIndicator()
            if (error.isNotEmpty()) Text(error, color = Color(0xFFFFB4AB))
        }
        val current = selected
        if (current != null) {
            item {
                Button(onClick = { selected = null }) { Text("Назад к списку") }
                Poster(current.poster)
                Text(current.title, color = Color.White)
                Text("${current.year} · ${current.kind}", color = Color.LightGray)
                Text(current.description, color = Color.White)
                if (current.url.isNotEmpty()) {
                    Button(onClick = {
                        context.startActivity(Intent(context, PlayerActivity::class.java).putExtra("stream_url", current.url)
                            .putExtra("media_id", current.id).putExtra("media_title", current.title))
                    }) { Text("Смотреть фильм") }
                }
            }
            val seasons = current.episodes.groupBy { it.season }.toSortedMap()
            seasons.forEach { (season, episodes) ->
                item { Text("Сезон $season", color = Color.White) }
                items(episodes.sortedBy { it.episode }) { episode ->
                    Button(onClick = {
                        if (episode.url.isNotEmpty()) {
                            val ordered = current.episodes.sortedWith(compareBy<Episode> { it.season }.thenBy { it.episode })
                            val next = ordered.getOrNull(ordered.indexOf(episode) + 1)
                            val intent = Intent(context, PlayerActivity::class.java)
                                .putExtra("stream_url", episode.url)
                                .putExtra("media_id", "${current.id}:s${episode.season}e${episode.episode}")
                                .putExtra("media_title", "${current.title} · ${episode.title}")
                            if (next != null && next.url.startsWith("https://", true)) {
                                intent.putExtra("next_stream_url", next.url)
                                    .putExtra("next_media_id", "${current.id}:s${next.season}e${next.episode}")
                            }
                            context.startActivity(intent)
                        }
                    }, enabled = episode.url.isNotEmpty()) {
                        Text("${episode.episode}. ${episode.title}")
                    }
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                Button(onClick = { selected = item }, modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Poster(item.poster)
                        Column {
                            Text(item.title)
                            Text("${item.year} · ${item.kind}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Poster(url: String) {
    if (url.isNotBlank()) {
        Image(painter = rememberAsyncImagePainter(url), contentDescription = "Постер",
            modifier = Modifier.width(90.dp).height(130.dp), contentScale = ContentScale.Crop)
    } else {
        Text("Нет постера", color = Color.LightGray)
    }
}
