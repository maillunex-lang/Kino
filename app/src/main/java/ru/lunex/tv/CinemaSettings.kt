package ru.lunex.tv

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class CinemaPreferences(
    val autoplayTrailers: Boolean = false,
    val autoplayNextEpisode: Boolean = true,
    val rememberPosition: Boolean = true,
    val reduceMotion: Boolean = false,
    val showCompletedInContinue: Boolean = false,
    val personalizedRecommendations: Boolean = false
)

class CinemaSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lunex_settings_v1", Context.MODE_PRIVATE)
    fun load() = CinemaPreferences(
        prefs.getBoolean("trailers", false),
        prefs.getBoolean("next_episode", true),
        prefs.getBoolean("remember_position", true),
        prefs.getBoolean("reduce_motion", false),
        prefs.getBoolean("completed", false),
        prefs.getBoolean("recommendations", false)
    )
    fun save(s: CinemaPreferences) {
        prefs.edit()
            .putBoolean("trailers", s.autoplayTrailers)
            .putBoolean("next_episode", s.autoplayNextEpisode)
            .putBoolean("remember_position", s.rememberPosition)
            .putBoolean("reduce_motion", s.reduceMotion)
            .putBoolean("completed", s.showCompletedInContinue)
            .putBoolean("recommendations", s.personalizedRecommendations)
            .apply()
    }
}

@Composable
fun CinemaSettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { CinemaSettings(context) }
    var settings by remember { mutableStateOf(store.load()) }
    fun update(value: CinemaPreferences) { settings = value; store.save(value) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item { Text("Настройки просмотра", color = Color.White) }
        item { SettingSwitch("Автовоспроизведение трейлеров", settings.autoplayTrailers) { update(settings.copy(autoplayTrailers = it)) } }
        item { SettingSwitch("Автопереход к следующей серии", settings.autoplayNextEpisode) { update(settings.copy(autoplayNextEpisode = it)) } }
        item { SettingSwitch("Запоминать позицию просмотра", settings.rememberPosition) { update(settings.copy(rememberPosition = it)) } }
        item { SettingSwitch("Уменьшенные анимации", settings.reduceMotion) { update(settings.copy(reduceMotion = it)) } }
        item { SettingSwitch("Показывать завершённые фильмы в «Продолжить»", settings.showCompletedInContinue) { update(settings.copy(showCompletedInContinue = it)) } }
        item { SettingSwitch("Персональные рекомендации", settings.personalizedRecommendations) { update(settings.copy(personalizedRecommendations = it)) } }
        item { Text("Все настройки сохраняются только на этом устройстве. Автотрейлеры и персональные рекомендации будут учитываться при подключении соответствующих функций каталога.", color = Color.LightGray) }
    }
}

@Composable
private fun SettingSwitch(title: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, modifier = Modifier.weight(1f).padding(end = 16.dp), color = Color.White)
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}
