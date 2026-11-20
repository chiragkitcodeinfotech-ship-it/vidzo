package com.walyo.browser

import com.walyo.browser.browser.TabManager
import com.walyo.browser.data.TabSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabManagerTest {

    @Test
    fun `starts with one home tab`() {
        val manager = TabManager()
        assertEquals(1, manager.tabs.size)
        assertTrue(manager.current.isHome)
    }

    @Test
    fun `new tab becomes current`() {
        val manager = TabManager()
        val tab = manager.newTab("https://example.com")
        assertEquals(2, manager.tabs.size)
        assertEquals(tab, manager.current)
        assertEquals("example.com", tab.title)
    }

    @Test
    fun `closing a tab before the current keeps the same current tab`() {
        val manager = TabManager()
        val first = manager.current
        manager.newTab("https://a.com")
        val third = manager.newTab("https://b.com")
        manager.close(first)
        assertEquals(third, manager.current)
    }

    @Test
    fun `closing the last remaining tab leaves a fresh home tab`() {
        val manager = TabManager()
        manager.close(manager.current)
        assertEquals(1, manager.tabs.size)
        assertTrue(manager.current.isHome)
    }

    @Test
    fun `closing the current last tab selects the previous one`() {
        val manager = TabManager()
        val second = manager.newTab("https://a.com")
        val third = manager.newTab("https://b.com")
        manager.close(third)
        assertEquals(second, manager.current)
    }

    @Test
    fun `restore rebuilds tabs and clamps the index`() {
        val manager = TabManager()
        manager.restore(
            listOf(TabSnapshot("https://a.com", "A"), TabSnapshot(null, "New tab")),
            index = 9,
        )
        assertEquals(2, manager.tabs.size)
        assertNull(manager.current.url)
        assertEquals(listOf("https://a.com", null), manager.snapshots().map { it.url })
    }

    @Test
    fun `restore with nothing saved keeps the default tab`() {
        val manager = TabManager()
        manager.restore(emptyList(), 0)
        assertEquals(1, manager.tabs.size)
    }
}
