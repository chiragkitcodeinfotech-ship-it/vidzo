package com.walyo.browser.browser

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.walyo.browser.data.TabSnapshot
import com.walyo.browser.util.UrlUtils

/** One browser tab. A null [url] means the tab shows the Walyo home page. */
@Stable
class BrowserTab(val id: Long, url: String?, title: String) {

    var url by mutableStateOf(url)
        private set
    var title by mutableStateOf(title)
    var progress by mutableIntStateOf(100)
    var canGoBack by mutableStateOf(false)
        private set
    var canGoForward by mutableStateOf(false)
        private set
    var isDesktopMode by mutableStateOf(false)

    /** Bumped when the WebView must be re-created (e.g. after a renderer crash). */
    var generation by mutableIntStateOf(0)
        private set

    var webView: WebView? = null
        private set

    val isHome: Boolean get() = url == null

    fun attach(view: WebView) {
        webView = view
    }

    fun navigate(target: String) {
        url = target
        title = UrlUtils.displayHost(target)
        webView?.loadUrl(target)
    }

    fun onUrlChanged(newUrl: String) {
        url = newUrl
    }

    fun updateNavigation(view: WebView) {
        canGoBack = view.canGoBack()
        canGoForward = view.canGoForward()
    }

    fun goHome() {
        releaseWebView()
        url = null
        title = NEW_TAB_TITLE
        progress = 100
        canGoBack = false
        canGoForward = false
    }

    fun onRendererCrashed() {
        releaseWebView()
        generation++
    }

    fun releaseWebView() {
        webView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.destroy()
        }
        webView = null
    }

    fun snapshot() = TabSnapshot(url, title)

    companion object {
        const val NEW_TAB_TITLE = "New tab"
    }
}
