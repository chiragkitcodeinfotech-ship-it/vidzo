package com.walyo.browser.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.walyo.browser.util.UrlUtils
import java.net.URISyntaxException

/** Opens non-web links (intent://, tel:, mailto:, whatsapp:, market: …) in other apps, safely. */
object ExternalLinks {
    private const val TAG = "ExternalLinks"
    private const val EXTRA_FALLBACK_URL = "browser_fallback_url"
    private const val URI_GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

    sealed interface Result {
        data object Launched : Result
        data class Fallback(val url: String) : Result
        data object NoApp : Result
    }

    fun open(activity: Activity, url: String): Result {
        val intent = parse(url) ?: return Result.NoApp
        return try {
            activity.startActivity(intent)
            Result.Launched
        } catch (e: ActivityNotFoundException) {
            Log.i(TAG, "No app for $url")
            intent.getStringExtra(EXTRA_FALLBACK_URL)
                ?.takeIf(UrlUtils::isWebUrl)
                ?.let { Result.Fallback(it) }
                ?: Result.NoApp
        }
    }

    private fun parse(url: String): Intent? = try {
        val intent = if (url.startsWith("intent:", ignoreCase = true)) {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        // Web pages must only reach browsable activities, never a specific internal component.
        intent.apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            component = null
            selector = null
            // Never let a web page grant other apps access to our files.
            flags = flags and URI_GRANT_FLAGS.inv()
        }
    } catch (e: URISyntaxException) {
        Log.w(TAG, "Malformed intent URL", e)
        null
    }
}
