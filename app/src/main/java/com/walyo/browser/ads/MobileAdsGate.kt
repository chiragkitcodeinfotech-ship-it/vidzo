package com.walyo.browser.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Initialises the Mobile Ads SDK once and lets ad code wait for it.
 *
 * Every ad request MUST call [awaitReady] first. Requesting an ad on the main thread while the SDK
 * is still initialising on a background thread deadlocks: the SDK needs the main thread to set up
 * WebView, and the main thread is blocked waiting for the SDK.
 */
object MobileAdsGate {
    private const val TAG = "MobileAdsGate"
    private val ready = CompletableDeferred<Unit>()
    private var started = false

    fun initialize(context: Context, scope: CoroutineScope) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(appContext) {
                Log.d(TAG, "Mobile Ads SDK ready")
                ready.complete(Unit)
            }
        }
    }

    suspend fun awaitReady() = ready.await()
}
