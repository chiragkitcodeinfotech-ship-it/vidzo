package com.walyo.browser.ads

import java.util.concurrent.TimeUnit

/** When and how often Walyo shows ads. Kept in one place so the rules are easy to tune. */
object AdPolicy {
    /** How long ads stay off after the user watches an opt-in rewarded video. */
    val AD_FREE_DURATION_MS: Long = TimeUnit.HOURS.toMillis(1)

    /** Length of the launch animation; the launch ad is never shown before this. */
    const val SPLASH_MIN_MS = 2_500L

    /** Latest moment (from launch) the launch ad may appear; if not loaded by then, Home opens without it. */
    const val SPLASH_AD_TIMEOUT_MS = 3_500L

    /** Minimum gap between two app-open ads when returning from the background. */
    val RESUME_AD_INTERVAL_MS: Long = TimeUnit.MINUTES.toMillis(30)

    /** The user must be away at least this long before a "welcome back" ad. */
    val RESUME_AD_MIN_AWAY_MS: Long = TimeUnit.SECONDS.toMillis(30)

    /** A loaded app-open ad expires after 4 hours (AdMob guidance). */
    val APP_OPEN_AD_TTL_MS: Long = TimeUnit.HOURS.toMillis(4)

    fun adsEnabled(adFreeUntil: Long, now: Long): Boolean = now >= adFreeUntil
}
