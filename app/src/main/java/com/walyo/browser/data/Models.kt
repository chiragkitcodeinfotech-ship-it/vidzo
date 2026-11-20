package com.walyo.browser.data

import java.net.URLEncoder

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class SearchEngine(val label: String, val homeUrl: String, private val queryUrl: String) {
    GOOGLE("Google", "https://www.google.com", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com", "https://www.bing.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com", "https://duckduckgo.com/?q="),
    YAHOO("Yahoo", "https://search.yahoo.com", "https://search.yahoo.com/search?p="),
    YANDEX("Yandex", "https://yandex.com", "https://yandex.com/search/?text=");

    fun searchUrl(query: String): String = queryUrl + URLEncoder.encode(query, "UTF-8")
}

data class Shortcut(val title: String, val url: String)

data class HistoryEntry(val title: String, val url: String, val visitedAt: Long)

data class TabSnapshot(val url: String?, val title: String)

data class SavedTabs(val tabs: List<TabSnapshot>, val currentIndex: Int)

data class Settings(
    val theme: ThemeMode,
    val searchEngine: SearchEngine,
    val suggestionsEnabled: Boolean,
    val pinHash: String?,
    val adFreeUntil: Long,
    val shortcuts: List<Shortcut>,
) {
    val isPinSet: Boolean get() = pinHash != null

    companion object {
        const val MAX_SHORTCUTS = 15

        val DEFAULT_SHORTCUTS = listOf(
            Shortcut("Google", "https://www.google.com"),
            Shortcut("YouTube", "https://m.youtube.com"),
            Shortcut("Instagram", "https://www.instagram.com"),
            Shortcut("WhatsApp", "https://www.whatsapp.com"),
            Shortcut("Facebook", "https://m.facebook.com"),
            Shortcut("Wikipedia", "https://www.wikipedia.org"),
            // Keep Cricbuzz as the 7th tile: install-referrer campaigns replace it (see Campaigns.kt).
            Shortcut("Cricbuzz", "https://m.cricbuzz.com"),
        )

        val DEFAULT = Settings(
            theme = ThemeMode.SYSTEM,
            searchEngine = SearchEngine.GOOGLE,
            suggestionsEnabled = true,
            pinHash = null,
            adFreeUntil = 0L,
            shortcuts = DEFAULT_SHORTCUTS,
        )
    }
}
