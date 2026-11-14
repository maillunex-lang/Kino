package ru.lunex.tv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Episode(val season: Int, val episode: Int, val title: String, val url: String)
data class CatalogItem(
    val id: String, val title: String, val kind: String, val year: Int,
    val description: String, val poster: String, val url: String, val episodes: List<Episode>
)

interface CatalogSource {
    val name: String
    fun load(): List<CatalogItem>
}

class HttpsJsonCatalogSource(override val name: String, private val endpoint: String) : CatalogSource {
    override fun load(): List<CatalogItem> {
        require(endpoint.startsWith("https://", true)) { "Только HTTPS" }
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        try {
            require(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            require(json.length <= 2_000_000) { "Каталог слишком большой" }
            val root = JSONObject(json)
            val items = root.getJSONArray("items")
            return (0 until minOf(items.length(), 500)).map { i ->
                val item = items.getJSONObject(i)
                val eps = item.optJSONArray("episodes") ?: JSONArray()
                CatalogItem(
                    id = item.getString("id"), title = item.getString("title"),
                    kind = item.optString("kind", "Фильм"), year = item.optInt("year", 0),
                    description = item.optString("description"), poster = safeHttps(item.optString("poster")),
                    url = safeHttps(item.optString("url")),
                    episodes = (0 until minOf(eps.length(), 500)).map { j ->
                        val ep = eps.getJSONObject(j)
                        Episode(ep.optInt("season", 1), ep.optInt("episode", j + 1),
                            ep.optString("title", "Серия ${j + 1}"), safeHttps(ep.optString("url")))
                    }
                )
            }
        } finally {
            connection.disconnect()
        }
    }
    private fun safeHttps(value: String) = value.takeIf { it.startsWith("https://", true) }.orEmpty()
}

class SourceRegistry(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lunex_sources", Context.MODE_PRIVATE)
    fun endpoints(): List<String> = prefs.getString("endpoints", "").orEmpty()
        .lines().map { it.trim() }.filter { it.startsWith("https://", true) }.distinct().take(5)
    fun save(endpoints: List<String>) {
        prefs.edit().putString("endpoints", endpoints.distinct().take(5).joinToString("\n")).apply()
    }
}

/** JSON contract: { "items": [{ "id", "title", "kind", "year", "description",
 * "poster", "url", "episodes": [{"season", "episode", "title", "url"}] }] }.
 * The endpoint must be controlled by the user or provided with authorization.
 */
