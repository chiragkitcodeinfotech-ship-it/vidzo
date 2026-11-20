package com.walyo.browser

import com.walyo.browser.data.Settings
import com.walyo.browser.data.Shortcut
import com.walyo.browser.referral.Campaigns
import org.junit.Assert.assertEquals
import org.junit.Test

class CampaignShortcutTest {

    private val campaign = Shortcut("My page", "https://example.com")
    private val defaults = Settings.DEFAULT_SHORTCUTS

    @Test
    fun `campaign replaces Cricbuzz in the same position`() {
        val result = Campaigns.withCampaignShortcut(defaults, campaign)

        val cricbuzzIndex = defaults.indexOfFirst { it.title == "Cricbuzz" }
        assertEquals(campaign, result[cricbuzzIndex])
        assertEquals(defaults.size, result.size)
        assertEquals(false, result.any { it.title == "Cricbuzz" })
    }

    @Test
    fun `campaign goes to 7th tile when Cricbuzz was removed`() {
        val withoutCricbuzz = defaults.filterNot { it.title == "Cricbuzz" }

        val result = Campaigns.withCampaignShortcut(withoutCricbuzz, campaign)

        assertEquals(campaign, result[6])
        assertEquals(withoutCricbuzz.size + 1, result.size)
    }

    @Test
    fun `campaign is appended when user has fewer than 7 shortcuts`() {
        val few = defaults.take(2)

        assertEquals(few + campaign, Campaigns.withCampaignShortcut(few, campaign))
    }

    @Test
    fun `applying twice does not duplicate the campaign`() {
        val once = Campaigns.withCampaignShortcut(defaults, campaign)

        assertEquals(once, Campaigns.withCampaignShortcut(once, campaign))
    }

    @Test
    fun `never exceeds the shortcut limit`() {
        val full = (1..Settings.MAX_SHORTCUTS).map { Shortcut("S$it", "https://s$it.com") }

        val result = Campaigns.withCampaignShortcut(full, campaign)

        assertEquals(Settings.MAX_SHORTCUTS, result.size)
        assertEquals(campaign, result[6])
    }
}
