package ru.lunex.tv

import android.content.Context
import org.json.JSONObject

data class WatchProgress(val positionMs: Long, val durationMs: Long, val updatedAt: Long)

class WatchHistory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lunex_watch_history", Context.MODE_PRIVATE)
    fun get(id: String): WatchProgress? = runCatching {
        val json = JSONObject(prefs.getString(id, null) ?: return null)
        WatchProgress(json.optLong("position"), json.optLong("duration"), json.optLong("updated"))
    }.getOrNull()
    fun save(id: String, positionMs: Long, durationMs: Long) {
        if (id.isBlank()) return
        val json = JSONObject().apply {
            put("position", positionMs.coerceAtLeast(0))
            put("duration", durationMs.coerceAtLeast(0))
            put("updated", System.currentTimeMillis())
        }
        prefs.edit().putString(id, json.toString()).apply()
        val keys = prefs.all.keys
        if (keys.size > 300) {
            keys.mapNotNull { key -> get(key)?.let { key to it.updatedAt } }
                .sortedBy { it.second }.take(keys.size - 300).forEach { prefs.edit().remove(it.first).apply() }
        }
    }
    fun recent(): List<Pair<String, WatchProgress>> = prefs.all.keys.mapNotNull { key ->
        get(key)?.let { key to it }
    }.sortedByDescending { it.second.updatedAt }
}
