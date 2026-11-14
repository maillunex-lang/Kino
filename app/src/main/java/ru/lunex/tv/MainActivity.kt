package ru.lunex.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

private val background = Color(0xFF0B1019)
private val sample = listOf("Каталог появится после подключения источника", "Поиск", "Продолжить просмотр")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CinemaHome() }
    }
}

@Composable
private fun CinemaHome() {
    var selected by remember { mutableStateOf("Главная") }
    MaterialTheme {
        Row(Modifier.fillMaxSize().background(background).padding(28.dp)) {
            Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("LUNEX TV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                Spacer(Modifier.height(26.dp))
                listOf("Главная", "Фильмы", "Сериалы", "Поиск", "Настройки").forEach { label ->
                    Button(onClick = { selected = label }) { Text(label) }
                }
            }
            Column(Modifier.fillMaxSize().padding(start = 24.dp)) {
                Text(selected, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Text("Добро пожаловать в Lunex TV", color = Color(0xFFB9C4D5), fontSize = 20.sp)
                Spacer(Modifier.height(32.dp))
                Text("Скоро на экране", color = Color.White, fontSize = 23.sp)
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(sample) { title ->
                        Surface(onClick = {}, modifier = Modifier.width(220.dp).height(130.dp), shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp))) {
                            Box(Modifier.fillMaxSize().background(Color(0xFF243246)).padding(16.dp)) {
                                Text(title, color = Color.White, fontSize = 17.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
