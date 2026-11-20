package com.walyo.browser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.walyo.browser.util.UrlUtils

/** Events a WebView reports back to the app (implemented by MainActivity). */
interface BrowserCallbacks {
    fun onPageVisited(url: String, title: String)
    fun onDownloadRequested(url: String, userAgent: String, contentDisposition: String?, mimeType: String?)
    fun onExternalLink(url: String)
    fun onShowFullscreen(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideFullscreen()
    fun onFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams,
    ): Boolean
}

object WebViewFactory {

    private const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/130.0.0.0 Safari/537.36"

    /** Schemes the WebView handles itself; everything else is handed to other apps. */
    private val IN_APP_SCHEMES = setOf("http", "https", "about", "data", "blob", "javascript")

    /** Schemes we never open from a web page. */
    private val BLOCKED_SCHEMES = setOf("file", "content")

    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context, tab: BrowserTab, callbacks: BrowserCallbacks): WebView {
        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mediaPlaybackRequiresUserGesture = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false)
        }
        applyDesktopMode(webView, tab.isDesktopMode)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.webViewClient = WalyoWebViewClient(tab, callbacks)
        webView.webChromeClient = WalyoChromeClient(tab, callbacks)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            callbacks.onDownloadRequested(url, userAgent, contentDisposition, mimeType)
        }
        tab.attach(webView)
        tab.url?.let(webView::loadUrl)
        return webView
    }

    fun applyDesktopMode(webView: WebView, enabled: Boolean) {
        webView.settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else null
    }

    private class WalyoWebViewClient(
        private val tab: BrowserTab,
        private val callbacks: BrowserCallbacks,
    ) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val uri = request?.url ?: return false
            val scheme = uri.scheme?.lowercase() ?: return false
            return when (scheme) {
                in IN_APP_SCHEMES -> false
                in BLOCKED_SCHEMES -> true
                else -> {
                    callbacks.onExternalLink(uri.toString())
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            url?.let(tab::onUrlChanged)
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            url?.let(tab::onUrlChanged)
            view?.let(tab::updateNavigation)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            view?.let(tab::updateNavigation)
            if (url != null && UrlUtils.isWebUrl(url)) {
                val title = view?.title?.takeIf { it.isNotBlank() } ?: UrlUtils.displayHost(url)
                callbacks.onPageVisited(url, title)
            }
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            Log.e(TAG, "WebView renderer gone (crash=${detail?.didCrash()}), recreating tab ${tab.id}")
            tab.onRendererCrashed()
            return true
        }
    }

    private class WalyoChromeClient(
        private val tab: BrowserTab,
        private val callbacks: BrowserCallbacks,
    ) : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            tab.progress = newProgress
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            if (!title.isNullOrBlank()) tab.title = title
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (view != null && callback != null) callbacks.onShowFullscreen(view, callback)
        }

        override fun onHideCustomView() = callbacks.onHideFullscreen()

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
        ): Boolean {
            if (filePathCallback == null || fileChooserParams == null) return false
            return callbacks.onFileChooser(filePathCallback, fileChooserParams)
        }

        // Camera, microphone and location are not supported yet: deny instead of silently granting.
        override fun onPermissionRequest(request: PermissionRequest?) {
            request?.deny()
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?,
        ) {
            callback?.invoke(origin, false, false)
        }
    }

    private const val TAG = "WebViewFactory"
}
