package com.walyo.browser.ui.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walyo.browser.browser.BrowserTab
import com.walyo.browser.ui.components.Favicon
import com.walyo.browser.ui.components.ScreenHeader
import com.walyo.browser.ui.components.WalyoLogo
import com.walyo.browser.ui.theme.WalyoColors
import com.walyo.browser.util.UrlUtils

@Composable
fun TabsScreen(
    tabs: List<BrowserTab>,
    currentTab: BrowserTab,
    onSelect: (BrowserTab) -> Unit,
    onClose: (BrowserTab) -> Unit,
    onNewTab: () -> Unit,
    onCloseAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = if (tabs.size == 1) "1 tab" else "${tabs.size} tabs",
                navigation = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCloseAll) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Close all tabs")
                    }
                },
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tabs, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        isCurrent = tab.id == currentTab.id,
                        onClick = { onSelect(tab) },
                        onClose = { onClose(tab) },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNewTab,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("New tab") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun TabCard(tab: BrowserTab, isCurrent: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val border = if (isCurrent) {
        Modifier.border(2.dp, WalyoColors.Brand, shape)
    } else {
        Modifier.border(1.dp, colors.outline, shape)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(border)
            .background(colors.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val url = tab.url
            if (url == null) WalyoLogo(size = 18.dp) else Favicon(url, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                tab.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close tab", modifier = Modifier.size(18.dp))
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val url = tab.url
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (url == null) WalyoLogo(size = 48.dp) else Favicon(url, size = 48.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (url == null) "Home" else UrlUtils.displayHost(url),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

