package ru.lunex.tv

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val background = Color(0xFF0B1019)
private val sections = listOf("Главная", "Фильмы", "Сериалы", "Поиск", "Плеер", "Настройки")
private data class Film(val id: String, val title: String, val year: Int, val kind: String, val description: String)
private val demo = listOf(
    Film("demo-1", "Орбита", 2026, "Фильм", "Демонстрационная карточка фантастического фильма."),
    Film("demo-2", "Ночной город", 2025, "Сериал", "Демонстрационная карточка городского детектива."),
    Film("demo-3", "За горизонтом", 2024, "Фильм", "Демонстрационная карточка приключенческого фильма."),
    Film("demo-4", "Море света", 2026, "Сериал", "Демонстрационная карточка истории о путешествиях.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTv = mode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        setContent { LunexHome(isTv) }
    }
}

@Composable
private fun LunexHome(isTv: Boolean) {
    val context = LocalContext.current
    var section by remember { mutableStateOf("Главная") }
    var opened by remember { mutableStateOf<Film?>(null) }
    var search by remember { mutableStateOf("") }
    BackHandler(opened != null) { opened = null }
    MaterialTheme {
        if (isTv) {
            Row(Modifier.fillMaxSize().background(background).padding(28.dp)) {
                Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("LUNEX TV", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    sections.forEach { item ->
                        Button(onClick = { if (item == "Плеер") context.startActivity(Intent(context, PlayerActivity::class.java)) else { section = item; opened = null } }) { Text(item) }
                    }
                }
                Column(Modifier.weight(1f).padding(start = 22.dp)) {
                    Content(section, opened, { opened = it }, search, { search = it })
                }
            }
        } else {
            Column(Modifier.fillMaxSize().background(background).safeDrawingPadding()) {
                Text("LUNEX TV", Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(sections) { item ->
                        Button(onClick = { section = item; opened = null }) { Text(item, fontSize = 12.sp) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.weight(1f).padding(horizontal = 18.dp)) {
                    Content(section, opened, { opened = it }, search, { search = it })
                }
            }
        }
    }
}

@Composable
private fun Content(section: String, opened: Film?, open: (Film?) -> Unit, search: String, setSearch: (String) -> Unit) {
    if (opened != null) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Button(onClick = { open(null) }) { Text("Назад к каталогу") }
                Spacer(Modifier.height(16.dp))
                Text(opened.title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("${opened.year} · ${opened.kind}", color = Color(0xFFB9C4D5))
                Spacer(Modifier.height(12.dp))
                Text(opened.description, color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Text("Демо-карточка. Воспроизведение появится после подключения разрешённого источника.",
                    color = Color(0xFFB9C4D5), fontSize = 15.sp)
            }
        }
        return
    }
    Text(section, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(14.dp))
    if (section == "Настройки") {
        Text("Lunex TV · тестовая версия. Источники пока не подключены.", color = Color.White)
        return
    }
    if (section == "Поиск") {
        OutlinedTextField(value = search, onValueChange = setSearch, label = { Text("Название фильма или сериала") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    } else {
        Text("Демонстрационный каталог", color = Color(0xFFB9C4D5), fontSize = 17.sp)
        Spacer(Modifier.height(20.dp))
    }
    val filtered = demo.filter {
        (section == "Главная" || section == "Поиск" || section == "Фильмы" && it.kind == "Фильм" ||
            section == "Сериалы" && it.kind == "Сериал") &&
            (section != "Поиск" || it.title.contains(search, ignoreCase = true))
    }
    if (filtered.isEmpty()) {
        Text("Ничего не найдено", color = Color.White)
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(filtered, key = { it.id }) { film ->
                Surface(
                    onClick = { open(film) },
                    modifier = Modifier.width(190.dp).height(200.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF243246)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                        Text(film.kind.uppercase(), color = Color(0xFFB9C4D5), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(film.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(film.year.toString(), color = Color(0xFFB9C4D5))
                    }
                }
            }
        }
    }
}
