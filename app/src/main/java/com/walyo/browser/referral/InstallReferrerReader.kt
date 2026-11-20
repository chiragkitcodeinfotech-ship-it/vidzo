package com.walyo.browser.referral

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Asks the Play Store how this install happened (Google Play Install Referrer API). */
class InstallReferrerReader(context: Context) {

    sealed interface Result {
        /** Play answered. [referrer] is e.g. "c=offer1" or "utm_source=google-play&utm_medium=organic". */
        data class Found(val referrer: String?) : Result

        /** This device/install can never give a referrer (no Play Store, sideloaded…). */
        data object NotSupported : Result

        /** Play Store busy or unreachable — try again on next launch. */
        data object RetryLater : Result
    }

    private val appContext = context.applicationContext

    suspend fun read(): Result = withTimeoutOrNull(TIMEOUT_MS) { connectAndRead() } ?: Result.RetryLater

    private suspend fun connectAndRead(): Result = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(appContext).build()

        fun finish(result: Result) {
            if (continuation.isActive) continuation.resume(result)
            runCatching { client.endConnection() }
        }

        continuation.invokeOnCancellation { runCatching { client.endConnection() } }
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> try {
                        finish(Result.Found(client.installReferrer.installReferrer))
                    } catch (e: RemoteException) {
                        Log.w(TAG, "Could not read install referrer", e)
                        finish(Result.RetryLater)
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> finish(Result.NotSupported)
                    else -> finish(Result.RetryLater)
                }
            }

            override fun onInstallReferrerServiceDisconnected() = finish(Result.RetryLater)
        })
    }

    private companion object {
        const val TAG = "InstallReferrerReader"
        const val TIMEOUT_MS = 5_000L
    }
}
