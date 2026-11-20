package com.walyo.browser.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Loads and shows the full-screen "app open" ad used on launch and when returning to the app.
 * Must be used from the main thread.
 */
class AppOpenAdManager(
    context: Context,
    private val adUnitId: String,
    private val scope: CoroutineScope,
) {

    private val appContext = context.applicationContext
    private var ad: AppOpenAd? = null
    private var loadedAt = 0L
    private var lastShownAt = 0L
    private var pendingLoad: CompletableDeferred<Boolean>? = null

    var isShowing = false
        private set

    private val isAdFresh: Boolean
        get() = ad != null && SystemClock.elapsedRealtime() - loadedAt < AdPolicy.APP_OPEN_AD_TTL_MS

    /** Starts loading an ad if none is ready. Completes with true when an ad is ready to show. */
    fun load(): CompletableDeferred<Boolean> {
        if (isAdFresh) return CompletableDeferred(true)
        pendingLoad?.takeIf { !it.isCompleted }?.let { return it }

        val result = CompletableDeferred<Boolean>()
        pendingLoad = result
        scope.launch {
            MobileAdsGate.awaitReady()
            requestAd(result)
        }
        return result
    }

    private fun requestAd(result: CompletableDeferred<Boolean>) {
        AppOpenAd.load(
            appContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(loaded: AppOpenAd) {
                    ad = loaded
                    loadedAt = SystemClock.elapsedRealtime()
                    result.complete(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "App open ad failed to load: ${error.code} ${error.message}")
                    result.complete(false)
                }
            },
        )
    }

    suspend fun awaitLoaded(): Boolean = load().await()

    /** Shows the ad if one is ready and the last one was at least [minIntervalMs] ago. */
    fun showIfReady(activity: Activity, minIntervalMs: Long, onFinished: () -> Unit = {}) {
        val elapsed = SystemClock.elapsedRealtime() - lastShownAt
        if (!isAdFresh) {
            load()
            onFinished()
            return
        }
        if (lastShownAt != 0L && elapsed < minIntervalMs) {
            onFinished()
            return
        }
        show(activity, onFinished)
    }

    fun show(activity: Activity, onFinished: () -> Unit = {}) {
        val current = ad
        if (current == null || isShowing || activity.isFinishing) {
            onFinished()
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "App open ad failed to show: ${error.message}")
                finish()
            }

            private fun finish() {
                ad = null
                isShowing = false
                lastShownAt = SystemClock.elapsedRealtime()
                onFinished()
                load() // preload the next one
            }
        }
        isShowing = true
        current.show(activity)
    }

    private companion object {
        const val TAG = "AppOpenAdManager"
    }
}
