package ru.lunex.tv

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProviderScreen() {
    val context = LocalContext.current
    var provider by remember { mutableStateOf("KinoPub") }
    var token by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("https://kinogo.parts") }
    var results by remember { mutableStateOf(emptyList<CatalogItem>()) }
    var selected by remember { mutableStateOf<CatalogItem?>(null) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var command by remember { mutableIntStateOf(0) }
    var detailsId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(command) {
        if (command == 0) return@LaunchedEffect
        busy = true
        error = ""
        try {
            val data = withContext(Dispatchers.IO) {
                if (provider == "KinoPub") {
                    if (detailsId != null) listOf(KinoPubSource(token).details(detailsId!!))
                    else KinoPubSource(token).catalog()
                }
                else listOf(KinogoPageSource().read(page))
            }
            if (detailsId != null) selected = data.firstOrNull()
            else { results = data; selected = null }
            detailsId = null
        } catch (e: Exception) {
            error = "Источник недоступен: ${e.message.orEmpty().take(100)}"
        } finally { busy = false }
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Киноисточники", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { provider = "KinoPub"; results = emptyList(); selected = null }) { Text("KinoPub") }
                Button(onClick = { provider = "Kinogo"; results = emptyList(); selected = null }) { Text("Kinogo") }
            }
            if (provider == "KinoPub") {
                Text("API KinoPub. Требуется токен собственного авторизованного клиента. Токен не сохраняется.", color = Color.LightGray)
                OutlinedTextField(token, { token = it }, label = { Text("Access token KinoPub") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            } else {
                Text("kinogo.parts: метаданные указанной страницы. Для просмотра можно открыть её в браузере; защищённые потоки не извлекаются.", color = Color.LightGray)
                Button(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://kinogo.parts")))
                }) { Text("Открыть Kinogo") }
                OutlinedTextField(page, { page = it }, label = { Text("HTTPS-страница Kinogo") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            Button(onClick = { command++ }, enabled = !busy) { Text(if (busy) "Загрузка…" else "Подключить источник") }
            if (error.isNotEmpty()) Text(error, color = Color(0xFFFFB4AB))
        }
        if (selected != null) {
            val item = selected!!
            item {
                Button(onClick = { selected = null }) { Text("Назад") }
                AsyncImage(model = item.poster, contentDescription = item.title,
                    modifier = Modifier.width(110.dp).height(160.dp))
                Text(item.title, color = Color.White)
                Text(item.description, color = Color.LightGray)
                if (provider == "KinoPub") {
                    Button(onClick = {
                        detailsId = item.id
                        command++
                    }) { Text("Сезоны и серии") }
                }
                Text("Воспроизведение возможно только через разрешённые ссылки источника.", color = Color.LightGray)
                if (provider == "Kinogo") {
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(item.id)))
                    }) { Text("Открыть страницу фильма") }
                }
            }
            items(item.episodes) { ep -> Text("Сезон ${ep.season}, серия ${ep.episode}: ${ep.title}", color = Color.White) }
        } else {
            items(results, key = { it.id }) { entry ->
                Button(onClick = { selected = entry }, modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = entry.poster, contentDescription = entry.title,
                            modifier = Modifier.width(60.dp).height(90.dp))
                        Text(entry.title)
                    }
                }
            }
        }
    }
}
