package com.walyo.browser.ui.browser

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.walyo.browser.browser.BrowserCallbacks
import com.walyo.browser.browser.BrowserTab
import com.walyo.browser.browser.WebViewFactory

private const val FULL_PROGRESS = 100

@Composable
fun BrowserScreen(
    tab: BrowserTab,
    callbacks: BrowserCallbacks,
    topBar: @Composable () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onShare: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        topBar()
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (tab.progress < FULL_PROGRESS) {
                LinearProgressIndicator(
                    progress = { tab.progress / FULL_PROGRESS.toFloat() },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background),
        ) {
            key(tab.id, tab.generation) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        val webView = tab.webView ?: WebViewFactory.create(context, tab, callbacks)
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        webView
                    },
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", enabled = true, onClick = onBack)
            NavButton(Icons.AutoMirrored.Rounded.ArrowForward, "Forward", enabled = tab.canGoForward) {
                tab.webView?.goForward()
            }
            NavButton(Icons.Rounded.Home, "Home", enabled = true, onClick = onHome)
            NavButton(Icons.Rounded.Refresh, "Reload", enabled = true) { tab.webView?.reload() }
            NavButton(Icons.Rounded.Share, "Share", enabled = true, onClick = onShare)
        }
    }
}

@Composable
private fun NavButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = label)
    }
}
