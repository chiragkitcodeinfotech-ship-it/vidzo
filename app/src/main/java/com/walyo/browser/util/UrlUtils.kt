package com.walyo.browser.util

import com.walyo.browser.data.SearchEngine
import java.net.URI

object UrlUtils {

    private val HOST_PATTERN = Regex(
        """^(localhost|(\d{1,3}\.){3}\d{1,3}|([a-z0-9-]+\.)+[a-z]{2,})(:\d{1,5})?([/?#].*)?$""",
        RegexOption.IGNORE_CASE,
    )

    fun isWebUrl(text: String): Boolean =
        text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)

    fun isSecure(url: String?): Boolean = url?.startsWith("https://", ignoreCase = true) == true

    /** Turns whatever the user typed into a URL: a web address, or a search on [engine]. */
    fun resolve(input: String, engine: SearchEngine): String {
        val text = input.trim()
        return when {
            text.isEmpty() -> engine.homeUrl
            isWebUrl(text) -> text
            text.equals("about:blank", ignoreCase = true) -> "about:blank"
            looksLikeAddress(text) -> "https://$text"
            else -> engine.searchUrl(text)
        }
    }

    /** Normalises a shortcut address typed by the user, or returns null when it is not a web address. */
    fun normalizeAddress(input: String): String? {
        val text = input.trim()
        return when {
            isWebUrl(text) -> text
            looksLikeAddress(text) -> "https://$text"
            else -> null
        }
    }

    /** "https://www.m.youtube.com/watch" -> "youtube.com" */
    fun displayHost(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val host = hostOf(url) ?: return url
        return host.removePrefix("www.").removePrefix("m.")
    }

    /** Address shown in the toolbar: no scheme, no trailing slash. */
    fun displayUrl(url: String?): String =
        url.orEmpty()
            .replaceFirst(Regex("^https?://", RegexOption.IGNORE_CASE), "")
            .removePrefix("www.")
            .removeSuffix("/")

    fun faviconUrl(url: String): String = "https://www.google.com/s2/favicons?sz=128&domain=${displayHost(url)}"

    private fun looksLikeAddress(text: String): Boolean = !text.contains(' ') && HOST_PATTERN.matches(text)

    private fun hostOf(url: String): String? = try {
        URI(url).host
    } catch (e: java.net.URISyntaxException) {
        null
    }
}
