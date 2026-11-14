package ru.lunex.tv

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val background = Color(0xFF0B1019)
private val sections = listOf("Главная", "Фильмы", "Сериалы", "Поиск", "Настройки")
private val samples = listOf("Каталог появится после подключения источника", "Поиск", "Продолжить просмотр")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uiMode = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTv = uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        setContent { LunexHome(isTv) }
    }
}

@Composable
private fun LunexHome(isTv: Boolean) {
    var selected by remember { mutableStateOf("Главная") }
    MaterialTheme {
        if (isTv) {
            Row(Modifier.fillMaxSize().background(background).padding(28.dp)) {
                Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LUNEX TV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    Spacer(Modifier.height(26.dp))
                    sections.forEach { item -> Button(onClick = { selected = item }) { Text(item) } }
                }
                Column(Modifier.weight(1f).padding(start = 24.dp)) {
                    CinemaContent(selected)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().background(background).safeDrawingPadding()) {
                Text("LUNEX TV", modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sections) { item ->
                        Button(onClick = { selected = item }) { Text(item, fontSize = 13.sp) }
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    CinemaContent(selected)
                }
            }
        }
    }
}

@Composable
private fun CinemaContent(selected: String) {
    Text(selected, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    Text("Добро пожаловать в Lunex TV", color = Color(0xFFB9C4D5), fontSize = 18.sp)
    Spacer(Modifier.height(28.dp))
    Text("Скоро на экране", color = Color.White, fontSize = 22.sp)
    Spacer(Modifier.height(12.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(samples) { title ->
            Surface(
                modifier = Modifier.width(190.dp).height(130.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF243246)
            ) {
                Box(Modifier.fillMaxSize().padding(14.dp)) {
                    Text(title, color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
