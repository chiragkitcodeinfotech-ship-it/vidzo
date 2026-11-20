package com.walyo.browser.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Opt-in rewarded video: the user chooses to watch it in exchange for ad-free time. */
class RewardedAdManager(context: Context, private val adUnitId: String) {

    private val appContext = context.applicationContext

    suspend fun load(): RewardedAd? {
        MobileAdsGate.awaitReady()
        return requestAd()
    }

    private suspend fun requestAd(): RewardedAd? = suspendCancellableCoroutine { continuation ->
        RewardedAd.load(
            appContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (continuation.isActive) continuation.resume(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${error.code} ${error.message}")
                    if (continuation.isActive) continuation.resume(null)
                }
            },
        )
    }

    fun show(activity: Activity, ad: RewardedAd, onRewarded: () -> Unit) {
        ad.show(activity) { onRewarded() }
    }

    private companion object {
        const val TAG = "RewardedAdManager"
    }
}
