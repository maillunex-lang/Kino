package ru.lunex.tv

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

private val background = Color(0xFF0B1019)
private val sections = listOf("Главная", "Фильмы", "Сериалы", "Избранное", "Поиск", "Онлайн-каталог", "Киноисточники", "История", "Моя библиотека", "Плеер", "Настройки")
private val muted = Color(0xFFB9C4D5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTv = manager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        setContent { LunexHome(isTv) }
    }
}

@Composable
private fun LunexHome(isTv: Boolean) {
    val context = LocalContext.current
    val library = remember(context) { LocalLibrary(context) }
    var entries by remember { mutableStateOf(library.load()) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(isTv) { if (isTv) firstFocus.requestFocus() }
    var favoritesVersion by remember { mutableIntStateOf(0) }
    var section by remember { mutableStateOf("Главная") }
    var opened by remember { mutableStateOf<LibraryEntry?>(null) }
    var search by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("Фильм") }
    var formError by remember { mutableStateOf("") }
    val launchPlayer = {
        context.startActivity(Intent(context, PlayerActivity::class.java))
    }
    val navigate: (String) -> Unit = { item ->
        if (item == "Плеер") launchPlayer() else { section = item; opened = null }
    }
    BackHandler(opened != null) { opened = null }
    MaterialTheme {
        if (isTv) {
            Row(Modifier.fillMaxSize().background(background).padding(24.dp)) {
                LazyColumn(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    item { Text("LUNEX TV", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                    items(sections) { item -> Button(onClick = { navigate(item) }, modifier = if (item == "Главная") Modifier.focusRequester(firstFocus) else Modifier) { Text(item) } }
                }
                Column(Modifier.weight(1f).padding(start = 22.dp)) {
                    LibraryContent(section, opened, entries, search, { search = it }, library,
                        favoritesVersion, { favoritesVersion++ }, { opened = it },
                        { launchPlayer() }, title, { title = it }, url, { url = it },
                        kind, { kind = it }, formError, { formError = it },
                        { entry -> entries = entries + entry; library.save(entries) },
                        { entry -> entries = entries.filterNot { it.id == entry.id }; library.save(entries); opened = null })
                }
            }
        } else {
            Column(Modifier.fillMaxSize().background(background).safeDrawingPadding()) {
                Text("LUNEX TV", Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(sections) { item ->
                        Button(onClick = { navigate(item) }) { Text(item, fontSize = 12.sp) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.weight(1f).padding(horizontal = 18.dp)) {
                    LibraryContent(section, opened, entries, search, { search = it }, library,
                        favoritesVersion, { favoritesVersion++ }, { opened = it },
                        { launchPlayer() }, title, { title = it }, url, { url = it },
                        kind, { kind = it }, formError, { formError = it },
                        { entry -> entries = entries + entry; library.save(entries) },
                        { entry -> entries = entries.filterNot { it.id == entry.id }; library.save(entries); opened = null })
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(
    section: String, opened: LibraryEntry?, entries: List<LibraryEntry>,
    search: String, setSearch: (String) -> Unit, library: LocalLibrary,
    favoritesVersion: Int, onFavoriteChange: () -> Unit,
    open: (LibraryEntry?) -> Unit, launchPlayer: () -> Unit,
    title: String, setTitle: (String) -> Unit,
    url: String, setUrl: (String) -> Unit,
    kind: String, setKind: (String) -> Unit,
    formError: String, setError: (String) -> Unit,
    add: (LibraryEntry) -> Unit, remove: (LibraryEntry) -> Unit
) {
    // Keep the favorite state reactive when it changes.
    val favoriteIds = remember(entries, favoritesVersion) {
        entries.filter { library.isFavorite(it.id) }.map { it.id }.toSet()
    }
    if (opened != null) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Button(onClick = { open(null) }) { Text("Назад") }
                Spacer(Modifier.height(14.dp))
                Text(opened.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(opened.kind, color = muted)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { library.toggleFavorite(opened.id); onFavoriteChange() }) {
                    Text(if (opened.id in favoriteIds) "Убрать из избранного" else "В избранное")
                }
                Spacer(Modifier.height(8.dp))
                val context = LocalContext.current
                Button(onClick = {
                    context.startActivity(Intent(context, PlayerActivity::class.java).putExtra("stream_url", opened.url).putExtra("media_id", opened.id).putExtra("media_title", opened.title))
                }) { Text("Смотреть") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { remove(opened) }) { Text("Удалить из библиотеки") }
                Text("Для воспроизведения используется сохранённая HTTPS-ссылка.", color = muted)
                Text(opened.url, color = muted, fontSize = 12.sp)
            }
        }
        return
    }
    Text(section, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    if (section == "Киноисточники") {
        ProviderScreen()
        return
    }
    if (section == "Онлайн-каталог") {
        OnlineCatalogScreen()
        return
    }
    if (section == "Главная") {
        ContinueWatchingScreen(entries)
        Spacer(Modifier.height(14.dp))
    }
    if (section == "История") {
        val context = LocalContext.current
        val history = remember(context) { WatchHistory(context) }
        val showCompleted = CinemaSettings(context).load().showCompletedInContinue
        ContinueWatchingScreen(entries, showCompleted, showTitle = false)
        return
    }
    if (section == "Настройки") {
        CinemaSettingsScreen()
        return
    }
    if (section == "Моя библиотека") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Добавить собственную разрешённую HTTPS-ссылку на видео", color = muted)
                OutlinedTextField(title, setTitle, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, setUrl, label = { Text("HTTPS-ссылка MP4/HLS") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { setKind("Фильм") }) { Text(if (kind == "Фильм") "✓ Фильм" else "Фильм") }
                    Button(onClick = { setKind("Сериал") }) { Text(if (kind == "Сериал") "✓ Сериал" else "Сериал") }
                }
                Button(onClick = {
                    if (title.isBlank() || !url.trim().startsWith("https://", ignoreCase = true)) {
                        setError("Введите название и HTTPS-ссылку")
                    } else {
                        add(LibraryEntry(UUID.randomUUID().toString(), title.trim(), url.trim(), kind))
                        setTitle(""); setUrl(""); setError("")
                    }
                }) { Text("Добавить") }
                if (formError.isNotEmpty()) Text(formError, color = Color(0xFFFFB4AB))
                Spacer(Modifier.height(12.dp))
                Text("Сохранённые материалы", color = Color.White, fontSize = 20.sp)
            }
            items(entries, key = { it.id }) { entry ->
                Button(onClick = { open(entry) }) { Text("${entry.title} · ${entry.kind}") }
            }
        }
        return
    }
    if (section == "Поиск") {
        OutlinedTextField(search, setSearch, label = { Text("Поиск в моей библиотеке") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
    }
    val filtered = entries.filter { entry ->
        (section == "Главная" || section == "Поиск" ||
            section == "Фильмы" && entry.kind == "Фильм" ||
            section == "Сериалы" && entry.kind == "Сериал" ||
            section == "Избранное" && entry.id in favoriteIds) &&
            (section != "Поиск" || entry.title.contains(search, ignoreCase = true))
    }
    if (filtered.isEmpty()) {
        Text("Пока пусто. Добавьте видео в разделе «Моя библиотека».", color = muted)
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered, key = { it.id }) { entry ->
                Surface(onClick = { open(entry) }, modifier = Modifier.width(190.dp).height(190.dp),
                    shape = RoundedCornerShape(14.dp), color = Color(0xFF243246)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                        Text(entry.kind.uppercase(), color = muted, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(entry.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        if (entry.id in favoriteIds) Text("★ Избранное", color = muted)
                    }
                }
            }
        }
    }
}
