package com.walyo.browser.browser

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.walyo.browser.data.TabSnapshot
import com.walyo.browser.util.UrlUtils

/** Holds the open tabs. There is always at least one tab. */
@Stable
class TabManager {

    private var nextId = 1L

    val tabs = mutableStateListOf(createTab(null, BrowserTab.NEW_TAB_TITLE))

    var currentIndex by mutableIntStateOf(0)
        private set

    val current: BrowserTab get() = tabs[currentIndex.coerceIn(0, tabs.lastIndex)]

    fun newTab(url: String? = null): BrowserTab {
        val title = url?.let(UrlUtils::displayHost) ?: BrowserTab.NEW_TAB_TITLE
        val tab = createTab(url, title)
        tabs.add(tab)
        currentIndex = tabs.lastIndex
        return tab
    }

    fun select(tab: BrowserTab) {
        val index = tabs.indexOf(tab)
        if (index >= 0) currentIndex = index
    }

    fun close(tab: BrowserTab) {
        val index = tabs.indexOf(tab)
        if (index < 0) return
        tab.releaseWebView()
        tabs.removeAt(index)
        if (tabs.isEmpty()) {
            tabs.add(createTab(null, BrowserTab.NEW_TAB_TITLE))
            currentIndex = 0
            return
        }
        if (index < currentIndex || currentIndex > tabs.lastIndex) {
            currentIndex -= 1
        }
        currentIndex = currentIndex.coerceIn(0, tabs.lastIndex)
    }

    fun closeAll() {
        tabs.forEach { it.releaseWebView() }
        tabs.clear()
        tabs.add(createTab(null, BrowserTab.NEW_TAB_TITLE))
        currentIndex = 0
    }

    fun restore(snapshots: List<TabSnapshot>, index: Int) {
        if (snapshots.isEmpty()) return
        tabs.forEach { it.releaseWebView() }
        tabs.clear()
        snapshots.forEach { tabs.add(createTab(it.url, it.title)) }
        currentIndex = index.coerceIn(0, tabs.lastIndex)
    }

    fun snapshots(): List<TabSnapshot> = tabs.map { it.snapshot() }

    fun destroyAll() {
        tabs.forEach { it.releaseWebView() }
    }

    private fun createTab(url: String?, title: String) = BrowserTab(nextId++, url, title)
}
