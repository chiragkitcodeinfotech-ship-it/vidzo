package com.walyo.browser

import com.walyo.browser.util.PinHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `correct pin verifies`() {
        val stored = PinHasher.hash("1234")
        assertTrue(PinHasher.verify("1234", stored))
    }

    @Test
    fun `wrong pin fails`() {
        val stored = PinHasher.hash("1234")
        assertFalse(PinHasher.verify("4321", stored))
    }

    @Test
    fun `same pin produces different hashes because of salt`() {
        assertNotEquals(PinHasher.hash("0000"), PinHasher.hash("0000"))
    }

    @Test
    fun `missing or corrupted hash never verifies`() {
        assertFalse(PinHasher.verify("1234", null))
        assertFalse(PinHasher.verify("1234", "garbage"))
        assertFalse(PinHasher.verify("1234", "zz:yy"))
    }
}
