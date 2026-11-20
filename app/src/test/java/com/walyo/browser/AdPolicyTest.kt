package com.walyo.browser

import com.walyo.browser.ads.AdPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {

    @Test
    fun `ads show when no ad-free time was earned`() {
        assertTrue(AdPolicy.adsEnabled(adFreeUntil = 0L, now = 1_000L))
    }

    @Test
    fun `ads hidden during ad-free window and return after it`() {
        val until = 10_000L
        assertFalse(AdPolicy.adsEnabled(until, now = 9_999L))
        assertTrue(AdPolicy.adsEnabled(until, now = 10_000L))
    }
}
