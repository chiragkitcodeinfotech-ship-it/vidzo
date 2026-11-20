package com.walyo.browser.ui

import android.view.View
import android.webkit.WebChromeClient
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** UI state owned by the activity: app lock and fullscreen video. */
@Stable
class SessionState {
    var isLocked by mutableStateOf(false)
    var fullscreenView by mutableStateOf<View?>(null)
    var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
}

/** Things the UI asks the activity to do (they need an Activity to launch intents or ads). */
class RootActions(
    val share: (String) -> Unit,
    val openDownloads: () -> Unit,
    val requestDefaultBrowser: () -> Unit,
    val watchRewardedAd: () -> Unit,
    val exitFullscreen: () -> Unit,
    val toast: (String) -> Unit,
)
