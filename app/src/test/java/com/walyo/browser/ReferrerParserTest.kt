package com.walyo.browser

import com.walyo.browser.referral.Campaigns
import com.walyo.browser.referral.ReferrerParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReferrerParserTest {

    @Test
    fun `reads campaign code from marketing referrer`() {
        assertEquals("offer1", ReferrerParser.campaignCode("c=offer1&click_id=ABC123"))
    }

    @Test
    fun `falls back to utm_campaign`() {
        assertEquals("diwali", ReferrerParser.campaignCode("utm_source=fb&utm_campaign=diwali"))
    }

    @Test
    fun `decodes url-encoded values`() {
        assertEquals("offer 1", ReferrerParser.campaignCode("c=offer%201"))
    }

    @Test
    fun `organic play install has no campaign`() {
        assertNull(ReferrerParser.campaignCode("utm_source=google-play&utm_medium=organic"))
        assertNull(ReferrerParser.campaignCode(null))
        assertNull(ReferrerParser.campaignCode(""))
        assertNull(ReferrerParser.campaignCode("c="))
    }

    @Test
    fun `server-shared apk uses its baked-in campaign when play has none`() {
        assertEquals("offer1", ReferrerParser.campaignCode(null, "offer1"))
        assertEquals("offer1", ReferrerParser.campaignCode("utm_source=google-play&utm_medium=organic", "offer1"))
    }

    @Test
    fun `play referrer wins over baked-in campaign`() {
        assertEquals("offer2", ReferrerParser.campaignCode("c=offer2", "offer1"))
    }

    @Test
    fun `normal build without link campaign gets nothing`() {
        assertNull(ReferrerParser.campaignCode(null, ""))
    }

    @Test
    fun `only listed campaign codes are accepted`() {
        assertNotNull(Campaigns.find("OFFER1"))
        assertNull(Campaigns.find("https://evil.example"))
        assertNull(Campaigns.find(null))
    }
}
