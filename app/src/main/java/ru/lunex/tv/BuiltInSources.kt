package ru.lunex.tv

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Built-in public catalog providers. Rights vary per item; verify licensing before reuse. */
object BuiltInSources {
    private fun json(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 12000
        c.instanceFollowRedirects = false
        c.setRequestProperty("Accept", "application/json")
        try {
            require(c.responseCode == 200) { "HTTP ${c.responseCode}" }
            val text = c.inputStream.bufferedReader().use { it.readText().take(2_000_001) }
            require(text.length <= 2_000_000) { "Слишком большой ответ" }
            return JSONObject(text)
        } finally { c.disconnect() }
    }

    fun nasa(): List<CatalogItem> {
        val root = json("https://images-api.nasa.gov/search?q=space&media_type=video&page_size=30")
        val items = root.getJSONObject("collection").getJSONArray("items")
        return (0 until items.length()).mapNotNull { i ->
            runCatching {
                val item = items.getJSONObject(i)
                val data = item.getJSONArray("data").getJSONObject(0)
                val id = data.getString("nasa_id")
                val poster = item.optJSONArray("links")?.optJSONObject(0)?.optString("href").orEmpty()
                val asset = json("https://images-api.nasa.gov/asset/" + URLEncoder.encode(id, "UTF-8"))
                val files = asset.getJSONObject("collection").getJSONArray("items")
                val mp4 = (0 until files.length()).mapNotNull { j ->
                    files.getJSONObject(j).optString("href").takeIf { it.startsWith("https://") && it.substringBefore('?').endsWith(".mp4", true) }
                }.firstOrNull().orEmpty()
                CatalogItem("nasa:$id", data.optString("title", "NASA video"), "Видео",
                    0, data.optString("description").take(400), poster.takeIf { it.startsWith("https://") }.orEmpty(), mp4, emptyList())
            }.getOrNull()
        }.filter { it.url.isNotEmpty() }
    }

    fun archive(): List<CatalogItem> {
        val q = URLEncoder.encode("mediatype:movies AND licenseurl:*", "UTF-8")
        val root = json("https://archive.org/advancedsearch.php?q=$q&fl[]=identifier&fl[]=title&fl[]=year&rows=18&output=json")
        val docs = root.getJSONObject("response").getJSONArray("docs")
        return (0 until docs.length()).mapNotNull { i ->
            runCatching {
                val doc = docs.getJSONObject(i)
                val id = doc.getString("identifier")
                require(id.matches(Regex("[A-Za-z0-9_-]{1,100}")))
                val meta = json("https://archive.org/metadata/$id")
                val license = meta.optJSONObject("metadata")?.optString("licenseurl").orEmpty()
                require(license.isNotBlank())
                val files = meta.getJSONArray("files")
                val filename = (0 until files.length()).mapNotNull { j ->
                    val name = files.getJSONObject(j).optString("name")
                    name.takeIf { it.endsWith(".mp4", true) }
                }.firstOrNull().orEmpty()
                require(filename.isNotBlank())
                val stream = "https://archive.org/download/$id/" + filename.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                CatalogItem("archive:$id", doc.optString("title", id), "Фильм",
                    doc.optInt("year"), "Лицензия: $license. Проверьте условия использования.", "https://archive.org/services/img/$id", stream, emptyList())
            }.getOrNull()
        }
    }
}
