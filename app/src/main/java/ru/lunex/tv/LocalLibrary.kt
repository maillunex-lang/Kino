package ru.lunex.tv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LibraryEntry(val id: String, val title: String, val url: String, val kind: String)

class LocalLibrary(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lunex_library_v1", Context.MODE_PRIVATE)

    fun load(): List<LibraryEntry> = runCatching {
        val array = JSONArray(prefs.getString("entries", "[]") ?: "[]")
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            LibraryEntry(
                id = obj.getString("id"),
                title = obj.getString("title"),
                url = obj.getString("url"),
                kind = obj.optString("kind", "Фильм")
            )
        }
    }.getOrDefault(emptyList())

    fun save(entries: List<LibraryEntry>) {
        val array = JSONArray()
        entries.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("kind", item.kind)
            })
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }

    fun isFavorite(id: String): Boolean = prefs.getStringSet("favorites", emptySet())?.contains(id) == true

    fun toggleFavorite(id: String): Boolean {
        val values = prefs.getStringSet("favorites", emptySet()).orEmpty().toMutableSet()
        val added = if (id in values) { values.remove(id); false } else { values.add(id); true }
        prefs.edit().putStringSet("favorites", values).apply()
        return added
    }
}
