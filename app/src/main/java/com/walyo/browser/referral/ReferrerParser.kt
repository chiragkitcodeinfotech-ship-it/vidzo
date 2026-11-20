package com.walyo.browser.referral

import java.net.URLDecoder

/** Reads the campaign code from a Play install referrer such as "c=offer1&click_id=abc". */
object ReferrerParser {

    /** Accepted parameter names, in priority order. */
    private val CAMPAIGN_KEYS = listOf("c", "utm_campaign")

    fun campaignCode(referrer: String?): String? {
        if (referrer.isNullOrBlank()) return null
        val params = referrer.split('&').mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) null else decode(part.substring(0, separator)) to decode(part.substring(separator + 1))
        }.toMap()
        return CAMPAIGN_KEYS.firstNotNullOfOrNull { key -> params[key]?.trim()?.takeIf { it.isNotEmpty() } }
    }

    /**
     * The Play referrer's code wins; otherwise the [linkCampaign] baked into a server-shared APK
     * (empty for Play Store builds).
     */
    fun campaignCode(referrer: String?, linkCampaign: String): String? =
        campaignCode(referrer) ?: linkCampaign.trim().ifEmpty { null }

    private fun decode(value: String): String = try {
        URLDecoder.decode(value, "UTF-8")
    } catch (e: IllegalArgumentException) {
        value
    }
}
