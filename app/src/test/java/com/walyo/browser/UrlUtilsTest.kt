package com.walyo.browser

import com.walyo.browser.data.SearchEngine
import com.walyo.browser.util.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlUtilsTest {

    private val google = SearchEngine.GOOGLE

    @Test
    fun `full web url is kept as is`() {
        assertEquals("https://m.youtube.com/", UrlUtils.resolve("https://m.youtube.com/", google))
        assertEquals("http://example.com", UrlUtils.resolve("  http://example.com ", google))
    }

    @Test
    fun `bare domain becomes https url`() {
        assertEquals("https://youtube.com", UrlUtils.resolve("youtube.com", google))
        assertEquals("https://example.co.in/path?q=1", UrlUtils.resolve("example.co.in/path?q=1", google))
        assertEquals("https://192.168.1.1:8080", UrlUtils.resolve("192.168.1.1:8080", google))
        assertEquals("https://localhost:3000", UrlUtils.resolve("localhost:3000", google))
    }

    @Test
    fun `text with spaces or no dot becomes a search`() {
        assertEquals("https://www.google.com/search?q=best+phones", UrlUtils.resolve("best phones", google))
        assertEquals("https://www.google.com/search?q=cricket", UrlUtils.resolve("cricket", google))
        assertEquals("https://www.google.com/search?q=e.g.", UrlUtils.resolve("e.g.", google))
    }

    @Test
    fun `search uses the chosen engine`() {
        assertEquals("https://duckduckgo.com/?q=walyo", UrlUtils.resolve("walyo", SearchEngine.DUCKDUCKGO))
        assertEquals("https://www.bing.com", UrlUtils.resolve("   ", SearchEngine.BING))
    }

    @Test
    fun `display host strips www and mobile prefixes`() {
        assertEquals("youtube.com", UrlUtils.displayHost("https://m.youtube.com/watch?v=1"))
        assertEquals("google.com", UrlUtils.displayHost("https://www.google.com/"))
        assertEquals("", UrlUtils.displayHost(null))
    }

    @Test
    fun `display url hides scheme and trailing slash`() {
        assertEquals("m.youtube.com", UrlUtils.displayUrl("https://m.youtube.com/"))
        assertEquals("google.com/search?q=a", UrlUtils.displayUrl("https://www.google.com/search?q=a"))
    }

    @Test
    fun `normalize address rejects search text`() {
        assertEquals("https://flipkart.com", UrlUtils.normalizeAddress("flipkart.com"))
        assertNull(UrlUtils.normalizeAddress("not a site"))
    }
}
