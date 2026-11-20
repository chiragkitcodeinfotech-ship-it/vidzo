package com.walyo.browser.ui

import android.graphics.Color as AndroidColor
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walyo.browser.ads.AdPolicy
import com.walyo.browser.browser.BrowserCallbacks
import com.walyo.browser.browser.TabManager
import com.walyo.browser.browser.WebViewFactory
import com.walyo.browser.data.HistoryRepository
import com.walyo.browser.data.SearchEngine
import com.walyo.browser.data.Settings
import com.walyo.browser.data.SettingsRepository
import com.walyo.browser.data.Shortcut
import com.walyo.browser.data.ThemeMode
import com.walyo.browser.ui.browser.BrowserScreen
import com.walyo.browser.ui.browser.FullscreenVideo
import com.walyo.browser.ui.components.BrowserTopBar
import com.walyo.browser.ui.components.MenuAction
import com.walyo.browser.ui.components.MenuState
import com.walyo.browser.ui.components.OverflowMenu
import com.walyo.browser.ui.dialogs.AddShortcutDialog
import com.walyo.browser.ui.dialogs.ChoiceDialog
import com.walyo.browser.ui.history.HistoryScreen
import com.walyo.browser.ui.home.HomeScreen
import com.walyo.browser.ui.home.PromoCard
import com.walyo.browser.ui.lock.PinMode
import com.walyo.browser.ui.lock.PinScreen
import com.walyo.browser.ui.search.SearchScreen
import com.walyo.browser.ui.tabs.TabsScreen
import com.walyo.browser.ui.theme.WalyoTheme
import com.walyo.browser.ui.theme.isDarkTheme
import com.walyo.browser.util.UrlUtils
import kotlinx.coroutines.launch
import java.util.Date

private sealed interface Overlay {
    data object None : Overlay
    data class Search(val initialText: String) : Overlay
    data object Tabs : Overlay
    data object History : Overlay
}

private enum class RootDialog { THEME, SEARCH_ENGINE, ADD_SHORTCUT, SET_PIN, REMOVE_PIN }

@Composable
fun WalyoRoot(
    tabManager: TabManager,
    session: SessionState,
    settingsRepository: SettingsRepository,
    historyRepository: HistoryRepository,
    callbacks: BrowserCallbacks,
    actions: RootActions,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(Settings.DEFAULT)
    val history by historyRepository.history.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var dialog by remember { mutableStateOf<RootDialog?>(null) }
    val tab = tabManager.current
    val adsEnabled = AdPolicy.adsEnabled(settings.adFreeUntil, System.currentTimeMillis())
    val dark = isDarkTheme(settings.theme)

    fun openInput(input: String) {
        tabManager.current.navigate(UrlUtils.resolve(input, settings.searchEngine))
        overlay = Overlay.None
    }

    fun saveShortcuts(updated: List<Shortcut>) = scope.launch { settingsRepository.setShortcuts(updated) }

    fun onMenuAction(action: MenuAction) {
        when (action) {
            MenuAction.NEW_TAB -> {
                tabManager.newTab()
                overlay = Overlay.None
            }
            MenuAction.ADD_SHORTCUT -> tab.url?.let { url ->
                val title = tab.title.take(24).ifBlank { UrlUtils.displayHost(url) }
                saveShortcuts(settings.shortcuts.filterNot { it.url == url } + Shortcut(title, url))
                actions.toast("Added to home")
            }
            MenuAction.SHARE -> tab.url?.let(actions.share)
            MenuAction.DESKTOP_SITE -> {
                tab.isDesktopMode = !tab.isDesktopMode
                tab.webView?.let {
                    WebViewFactory.applyDesktopMode(it, tab.isDesktopMode)
                    it.reload()
                }
            }
            MenuAction.HISTORY -> overlay = Overlay.History
            MenuAction.DOWNLOADS -> actions.openDownloads()
            MenuAction.THEME -> dialog = RootDialog.THEME
            MenuAction.SEARCH_ENGINE -> dialog = RootDialog.SEARCH_ENGINE
            MenuAction.PIN_LOCK -> dialog = if (settings.isPinSet) RootDialog.REMOVE_PIN else RootDialog.SET_PIN
            MenuAction.SUGGESTIONS -> scope.launch {
                settingsRepository.setSuggestionsEnabled(!settings.suggestionsEnabled)
            }
            MenuAction.DEFAULT_BROWSER -> actions.requestDefaultBrowser()
            MenuAction.AD_FREE -> actions.watchRewardedAd()
        }
    }

    val menuState = MenuState(
        isBrowser = !tab.isHome,
        isDesktopMode = tab.isDesktopMode,
        suggestionsEnabled = settings.suggestionsEnabled,
        isPinSet = settings.isPinSet,
        adsEnabled = adsEnabled,
    )
    val topBar: @Composable () -> Unit = {
        BrowserTopBar(
            address = tab.url?.let(UrlUtils::displayUrl),
            isSecure = UrlUtils.isSecure(tab.url),
            tabCount = tabManager.tabs.size,
            onAddressClick = { overlay = Overlay.Search(tab.url.orEmpty()) },
            onTabsClick = { overlay = Overlay.Tabs },
            menu = { OverflowMenu(menuState, ::onMenuAction) },
        )
    }

    WalyoTheme(darkTheme = dark) {
        SystemBarsEffect(dark)
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                if (tab.isHome) {
                    HomeScreen(
                        topBar = topBar,
                        shortcuts = settings.shortcuts,
                        recent = history,
                        promoCards = promoCards(settings, adsEnabled, actions) { dialog = it },
                        showAds = adsEnabled,
                        onOpenUrl = ::openInput,
                        onAddShortcut = { dialog = RootDialog.ADD_SHORTCUT },
                        onRemoveShortcut = { removed -> saveShortcuts(settings.shortcuts - removed) },
                    )
                } else {
                    BrowserScreen(
                        tab = tab,
                        callbacks = callbacks,
                        topBar = topBar,
                        onBack = { goBack(tab) },
                        onHome = { tab.goHome() },
                        onShare = { tab.url?.let(actions.share) },
                    )
                    BackHandler(enabled = overlay == Overlay.None) { goBack(tab) }
                }

                when (val current = overlay) {
                    Overlay.None -> Unit
                    is Overlay.Search -> SearchScreen(
                        initialText = current.initialText,
                        suggestionsEnabled = settings.suggestionsEnabled,
                        history = history,
                        onSubmit = ::openInput,
                        onDismiss = { overlay = Overlay.None },
                    )
                    Overlay.Tabs -> TabsScreen(
                        tabs = tabManager.tabs,
                        currentTab = tab,
                        onSelect = {
                            tabManager.select(it)
                            overlay = Overlay.None
                        },
                        onClose = tabManager::close,
                        onNewTab = {
                            tabManager.newTab()
                            overlay = Overlay.None
                        },
                        onCloseAll = {
                            tabManager.closeAll()
                            overlay = Overlay.None
                        },
                        onDismiss = { overlay = Overlay.None },
                    )
                    Overlay.History -> HistoryScreen(
                        entries = history,
                        onOpen = ::openInput,
                        onDelete = { entry -> scope.launch { historyRepository.remove(entry.url) } },
                        onClearAll = { scope.launch { historyRepository.clear() } },
                        onDismiss = { overlay = Overlay.None },
                    )
                }
            }

            RootDialogHost(
                dialog = dialog,
                settings = settings,
                settingsRepository = settingsRepository,
                session = session,
                onShortcutAdded = { saveShortcuts(settings.shortcuts.filterNot { s -> s.url == it.url } + it) },
                onDismiss = { dialog = null },
            )

            if (session.isLocked) {
                PinScreen(
                    mode = PinMode.UNLOCK,
                    storedHash = settings.pinHash,
                    onSuccess = { session.isLocked = false },
                    onCancel = null,
                )
            }

            session.fullscreenView?.let { FullscreenVideo(it, onExit = actions.exitFullscreen) }
        }
    }
}

private fun goBack(tab: com.walyo.browser.browser.BrowserTab) {
    val webView = tab.webView
    if (webView != null && webView.canGoBack()) webView.goBack() else tab.goHome()
}

@Composable
private fun RootDialogHost(
    dialog: RootDialog?,
    settings: Settings,
    settingsRepository: SettingsRepository,
    session: SessionState,
    onShortcutAdded: (Shortcut) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    when (dialog) {
        null -> Unit
        RootDialog.THEME -> ChoiceDialog(
            title = "Theme",
            options = ThemeMode.entries,
            selected = settings.theme,
            label = { it.label },
            onSelect = { scope.launch { settingsRepository.setTheme(it) } },
            onDismiss = onDismiss,
        )
        RootDialog.SEARCH_ENGINE -> ChoiceDialog(
            title = "Search engine",
            options = SearchEngine.entries,
            selected = settings.searchEngine,
            label = { it.label },
            onSelect = { scope.launch { settingsRepository.setSearchEngine(it) } },
            onDismiss = onDismiss,
        )
        RootDialog.ADD_SHORTCUT -> AddShortcutDialog(
            onAdd = {
                onShortcutAdded(it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        RootDialog.SET_PIN -> PinScreen(
            mode = PinMode.CREATE,
            storedHash = null,
            onSuccess = { hash ->
                scope.launch { settingsRepository.setPinHash(hash) }
                onDismiss()
            },
            onCancel = onDismiss,
        )
        RootDialog.REMOVE_PIN -> PinScreen(
            mode = PinMode.VERIFY,
            storedHash = settings.pinHash,
            onSuccess = {
                scope.launch { settingsRepository.setPinHash(null) }
                session.isLocked = false
                onDismiss()
            },
            onCancel = onDismiss,
        )
    }
}

@Composable
private fun promoCards(
    settings: Settings,
    adsEnabled: Boolean,
    actions: RootActions,
    openDialog: (RootDialog) -> Unit,
): List<PromoCard> {
    val context = LocalContext.current
    val adFreeCard = if (adsEnabled) {
        PromoCard(Icons.Rounded.Bolt, "Go ad-free", "Watch one short video and browse without ads for 1 hour.", "Watch video", actions.watchRewardedAd)
    } else {
        val until = DateFormat.getTimeFormat(context).format(Date(settings.adFreeUntil))
        PromoCard(Icons.Rounded.Bolt, "Ad-free is on", "Enjoy Vidzo without ads until $until.", "Extend", actions.watchRewardedAd)
    }
    return listOfNotNull(
        adFreeCard,
        PromoCard(
            Icons.Rounded.TravelExplore,
            "Pick a search engine",
            "Using ${settings.searchEngine.label}. Switch to Bing, DuckDuckGo and more.",
            "Set up",
        ) { openDialog(RootDialog.SEARCH_ENGINE) },
        PromoCard(
            Icons.Rounded.Language,
            "Make Vidzo your default",
            "Open every link in Vidzo — fast, light and private.",
            "Set default",
            actions.requestDefaultBrowser,
        ),
        if (settings.isPinSet) null else PromoCard(
            Icons.Rounded.Lock,
            "Lock with a PIN",
            "Keep your tabs and history private.",
            "Set PIN",
        ) { openDialog(RootDialog.SET_PIN) },
    )
}

/** Status/navigation bar icons follow the in-app theme, not only the system theme. */
@Composable
private fun SystemBarsEffect(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    LaunchedEffect(dark) {
        val style = if (dark) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
