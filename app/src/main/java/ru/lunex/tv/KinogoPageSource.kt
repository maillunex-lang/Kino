package ru.lunex.tv

import java.net.HttpURLConnection
import java.net.URL
import org.jsoup.Jsoup

/**
 * Kinogo public-page metadata adapter. Does not bypass access restrictions,
 * extract protected player URLs, or reuse another app's credentials.
 */
class KinogoPageSource {
    fun read(publicPageUrl: String): CatalogItem {
        val url = URL(publicPageUrl.trim())
        require(url.protocol == "https") { "Нужен HTTPS-адрес" }
        val host = url.host.lowercase()
        require(host == "kinogo" || host.startsWith("kinogo.") || host.endsWith(".kinogo") ||
            host.matches(Regex("kinogo[0-9]*\\.[a-z.]+"))) { "Адрес должен принадлежать Kinogo" }
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 10000
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("User-Agent", "LunexTV/0.1 (+metadata reader)")
        try {
            require(conn.responseCode == 200) { "Kinogo HTTP ${conn.responseCode}" }
            require(conn.contentType.orEmpty().contains("text/html")) { "Ожидалась HTML-страница" }
            val html = conn.inputStream.bufferedReader().use { it.readText().take(1_000_000) }
            val doc = Jsoup.parse(html, url.toString())
            val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
                ?.takeIf { it.isNotBlank() } ?: doc.title()
            val description = doc.selectFirst("meta[property=og:description]")?.attr("content")
                ?: doc.selectFirst("meta[name=description]")?.attr("content").orEmpty()
            val poster = doc.selectFirst("meta[property=og:image]")?.absUrl("content")
                ?.takeIf { it.startsWith("https://") }.orEmpty()
            require(title.isNotBlank()) { "Не удалось прочитать название" }
            return CatalogItem(url.toString(), title, "Фильм", 0, description, poster, "", emptyList())
        } finally {
            conn.disconnect()
        }
    }
}
