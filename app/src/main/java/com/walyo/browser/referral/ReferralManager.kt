package com.walyo.browser.referral

import android.util.Log
import com.walyo.browser.data.SettingsRepository

/**
 * Runs once per install: if the user came from a marketing link with a known campaign code,
 * the campaign page becomes a home shortcut (in place of Cricbuzz). Organic installs get nothing.
 * APKs downloaded from your own server carry their campaign in [linkCampaign] instead.
 */
class ReferralManager(
    private val reader: InstallReferrerReader,
    private val settings: SettingsRepository,
    /** Campaign baked into an APK shared from your own server; empty for Play Store builds. */
    private val linkCampaign: String,
) {

    /** [debugReferrer] lets debug builds simulate a marketing install (see README). */
    suspend fun applyOnce(debugReferrer: String? = null) {
        if (debugReferrer == null && settings.isReferralChecked()) return

        val referrer = debugReferrer ?: when (val result = reader.read()) {
            is InstallReferrerReader.Result.Found -> result.referrer
            InstallReferrerReader.Result.NotSupported -> null
            InstallReferrerReader.Result.RetryLater -> return // keep unchecked, try next launch
        }
        settings.setReferralChecked()

        val code = ReferrerParser.campaignCode(referrer, linkCampaign)
        val campaign = Campaigns.find(code)
        if (campaign == null) {
            Log.i(TAG, "No campaign for this install (code=$code)")
            return
        }
        Log.i(TAG, "Install from campaign '$code' → ${campaign.url}")
        settings.updateShortcuts { Campaigns.withCampaignShortcut(it, campaign) }
    }

    private companion object {
        const val TAG = "ReferralManager"
    }
}
