package com.walyo.browser.ui.browser

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

/** Hosts the WebView's fullscreen video view (e.g. YouTube fullscreen). */
@Composable
fun FullscreenVideo(view: View, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                (view.parent as? ViewGroup)?.removeView(view)
                view
            },
        )
    }
}
