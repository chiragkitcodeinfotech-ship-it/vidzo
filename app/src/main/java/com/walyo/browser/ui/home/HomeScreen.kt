package com.walyo.browser.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walyo.browser.BuildConfig
import com.walyo.browser.ads.NativeAdCard
import com.walyo.browser.data.HistoryEntry
import com.walyo.browser.data.Shortcut
import com.walyo.browser.ui.components.Favicon
import com.walyo.browser.ui.components.WalyoLogo
import com.walyo.browser.ui.components.WalyoWordmark
import com.walyo.browser.util.UrlUtils

private const val RECENT_LIMIT = 6

@Composable
fun HomeScreen(
    topBar: @Composable () -> Unit,
    shortcuts: List<Shortcut>,
    recent: List<HistoryEntry>,
    promoCards: List<PromoCard>,
    showAds: Boolean,
    onOpenUrl: (String) -> Unit,
    onAddShortcut: () -> Unit,
    onRemoveShortcut: (Shortcut) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        topBar()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            BrandHeader()
            ShortcutGrid(
                shortcuts = shortcuts,
                onOpen = { onOpenUrl(it.url) },
                onAdd = onAddShortcut,
                onRemove = onRemoveShortcut,
            )
            Spacer(Modifier.height(12.dp))
            PromoCarousel(promoCards)
            if (showAds) {
                NativeAdCard(
                    adUnitId = BuildConfig.AD_NATIVE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }
            if (recent.isNotEmpty()) {
                RecentSection(recent.take(RECENT_LIMIT), onOpenUrl)
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        WalyoLogo(size = 44.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            WalyoWordmark()
            Text(
                "Watch. Browse. Enjoy.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentSection(entries: List<HistoryEntry>, onOpenUrl: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "Recently visited",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        entries.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenUrl(entry.url) }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Favicon(entry.url, size = 24.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        UrlUtils.displayHost(entry.url),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
