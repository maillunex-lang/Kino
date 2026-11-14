package ru.lunex.tv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * KinoPub v1 API adapter. Uses an access token obtained by the user from their own
 * authorized client. Never stores OAuth client secrets or bypasses account activation.
 */
class KinoPubSource(private val accessToken: String) {
    private val base = "https://api.service-kp.com"
    private fun get(path: String): JSONObject {
        require(accessToken.isNotBlank()) { "Требуется авторизация KinoPub" }
        val connection = URL(base + path).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 12000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Accept", "application/json")
        try {
            require(connection.responseCode == 200) { "KinoPub HTTP ${connection.responseCode}" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            require(body.length < 4_000_000) { "Слишком большой ответ API" }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    fun catalog(): List<CatalogItem> {
        val array = get("/v1/items?page=1&perpage=40").optJSONArray("items") ?: JSONArray()
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            mapItem(item)
        }
    }

    fun details(id: String): CatalogItem {
        require(id.matches(Regex("[0-9]{1,12}")))
        return mapItem(get("/v1/items/$id?nolinks=1").getJSONObject("item"), includeEpisodes = true)
    }

    private fun mapItem(item: JSONObject, includeEpisodes: Boolean = false): CatalogItem {
        val type = item.optString("type")
        val seasons = if (includeEpisodes) item.optJSONArray("seasons") ?: JSONArray() else JSONArray()
        val episodes = mutableListOf<Episode>()
        for (i in 0 until seasons.length()) {
            val season = seasons.getJSONObject(i)
            val number = season.optInt("number", i + 1)
            val entries = season.optJSONArray("episodes") ?: JSONArray()
            for (j in 0 until entries.length()) {
                val ep = entries.getJSONObject(j)
                episodes += Episode(number, ep.optInt("number", j + 1),
                    ep.optString("title", "Серия ${j + 1}"), "")
            }
        }
        val poster = item.optJSONObject("posters")
        return CatalogItem(
            id = item.get("id").toString(),
            title = item.optString("title", "Без названия"),
            kind = if (type == "serial") "Сериал" else "Фильм",
            year = item.optInt("year"),
            description = item.optString("plot"),
            poster = poster?.optString("medium").orEmpty().takeIf { it.startsWith("https://") }.orEmpty(),
            url = "",
            episodes = episodes
        )
    }
}

