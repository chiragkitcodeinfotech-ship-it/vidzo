package com.walyo.browser.referral

import com.walyo.browser.data.Settings
import com.walyo.browser.data.Shortcut

/**
 * Your marketing campaigns. The code goes in the Play Store link:
 *
 *   https://play.google.com/store/apps/details?id=com.vidzo.browser&referrer=c%3Doffer1
 *
 * Users who install through that link see the matching page as a home shortcut (instead of Cricbuzz).
 * Only codes listed here are used — unknown codes are ignored, so nobody can push
 * their own website into Walyo with a home-made link.
 */
object Campaigns {

    private val ALL: Map<String, Shortcut> = mapOf(
        // "code" to Shortcut("Title shown in Recently visited", "https://mymukhwas.com"),
        "offer1" to Shortcut("Today's top deals", "https://mymukhwas.com"),
    )

    /** The default tile the campaign takes over (Cricbuzz, 7th tile). */
    private const val REPLACED_URL = "https://m.cricbuzz.com"
    private const val CAMPAIGN_SLOT = 6

    fun find(code: String?): Shortcut? = code?.let { ALL[it.trim().lowercase()] }

    /**
     * Returns [shortcuts] with [campaign] in place of Cricbuzz. If Cricbuzz was removed by the
     * user, the campaign is inserted as the 7th tile instead. Already present ⇒ unchanged.
     */
    fun withCampaignShortcut(shortcuts: List<Shortcut>, campaign: Shortcut): List<Shortcut> {
        if (shortcuts.any { it.url == campaign.url }) return shortcuts
        val replaceIndex = shortcuts.indexOfFirst { it.url == REPLACED_URL }
        if (replaceIndex >= 0) {
            return shortcuts.mapIndexed { index, shortcut -> if (index == replaceIndex) campaign else shortcut }
        }
        val slot = minOf(CAMPAIGN_SLOT, shortcuts.size)
        return (shortcuts.take(slot) + campaign + shortcuts.drop(slot)).take(Settings.MAX_SHORTCUTS)
    }
}
