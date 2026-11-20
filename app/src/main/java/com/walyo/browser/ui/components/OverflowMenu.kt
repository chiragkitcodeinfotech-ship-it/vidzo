package com.walyo.browser.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

enum class MenuAction {
    NEW_TAB, ADD_SHORTCUT, SHARE, DESKTOP_SITE,
    HISTORY, DOWNLOADS, THEME, SEARCH_ENGINE, PIN_LOCK, SUGGESTIONS, DEFAULT_BROWSER, AD_FREE,
}

data class MenuState(
    val isBrowser: Boolean,
    val isDesktopMode: Boolean,
    val suggestionsEnabled: Boolean,
    val isPinSet: Boolean,
    val adsEnabled: Boolean,
)

@Composable
fun OverflowMenu(state: MenuState, onAction: (MenuAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    @Composable
    fun Item(label: String, icon: ImageVector, action: MenuAction, checked: Boolean? = null) {
        DropdownMenuItem(
            text = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            trailingIcon = checked?.let { { Checkbox(checked = it, onCheckedChange = null) } },
            onClick = {
                expanded = false
                onAction(action)
            },
        )
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Menu")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Item("New tab", Icons.Rounded.Add, MenuAction.NEW_TAB)
            if (state.isBrowser) {
                Item("Add to home", Icons.Rounded.BookmarkAdd, MenuAction.ADD_SHORTCUT)
                Item("Share", Icons.Rounded.Share, MenuAction.SHARE)
                Item("Desktop site", Icons.Rounded.DesktopWindows, MenuAction.DESKTOP_SITE, state.isDesktopMode)
            }
            HorizontalDivider()
            Item("History", Icons.Rounded.History, MenuAction.HISTORY)
            Item("Downloads", Icons.Rounded.Download, MenuAction.DOWNLOADS)
            Item("Theme", Icons.Rounded.Palette, MenuAction.THEME)
            Item("Search engine", Icons.Rounded.TravelExplore, MenuAction.SEARCH_ENGINE)
            if (state.isPinSet) {
                Item("Remove PIN lock", Icons.Rounded.LockOpen, MenuAction.PIN_LOCK)
            } else {
                Item("Lock with a PIN", Icons.Rounded.Lock, MenuAction.PIN_LOCK)
            }
            Item("Search suggestions", Icons.Rounded.TravelExplore, MenuAction.SUGGESTIONS, state.suggestionsEnabled)
            Item("Set as default browser", Icons.Rounded.Language, MenuAction.DEFAULT_BROWSER)
            if (state.adsEnabled) {
                Item("1 hour ad-free", Icons.Rounded.PlayCircle, MenuAction.AD_FREE)
            }
        }
    }
}
