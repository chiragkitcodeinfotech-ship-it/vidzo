package com.walyo.browser

import android.Manifest
import android.app.DownloadManager
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import android.util.Log
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.walyo.browser.ads.AdPolicy
import com.walyo.browser.browser.BrowserCallbacks
import com.walyo.browser.browser.TabManager
import com.walyo.browser.data.SearchEngine
import com.walyo.browser.platform.DownloadHelper
import com.walyo.browser.platform.DownloadRequest
import com.walyo.browser.platform.ExternalLinks
import com.walyo.browser.ui.RootActions
import com.walyo.browser.ui.SessionState
import com.walyo.browser.ui.WalyoRoot
import com.walyo.browser.ui.splash.LaunchSplash
import com.walyo.browser.util.UrlUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity(), BrowserCallbacks {

    private val app: WalyoApp get() = application as WalyoApp
    private val tabManager = TabManager()
    private val session = SessionState()

    private var isReady = false
    private var showLaunchSplash by mutableStateOf(true)
    private val splashOnScreen = CompletableDeferred<Unit>()
    private var pendingLaunchInput: String? = null
    private var debugReferrer: String? = null
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingDownload: DownloadRequest? = null
    private var leftForOtherApp = false
    private var backgroundedAt = 0L
    private var hasStartedOnce = false

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            pendingFileCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data),
            )
            pendingFileCallback = null
        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val request = pendingDownload ?: return@registerForActivityResult
            pendingDownload = null
            if (granted) startDownload(request) else toast("Storage permission is needed to download files")
        }

    private val defaultBrowserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = onAppForeground()

        override fun onStop(owner: LifecycleOwner) {
            backgroundedAt = SystemClock.elapsedRealtime()
        }
    }

    private val rootActions by lazy {
        RootActions(
            share = ::shareUrl,
            openDownloads = ::openDownloads,
            requestDefaultBrowser = ::requestDefaultBrowser,
            watchRewardedAd = ::watchRewardedAd,
            exitFullscreen = { exitFullscreen(notifyPage = true) },
            toast = ::toast,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash is a plain dark screen that leaves on the first frame; LaunchSplash takes over.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingLaunchInput = launchInputFrom(intent)
        // Debug builds only: simulate a marketing-link install, e.g. --es walyo_test_referrer "c=offer1"
        debugReferrer = if (BuildConfig.DEBUG) intent.getStringExtra(EXTRA_TEST_REFERRER) else null
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)

        setContent {
            Box(Modifier.fillMaxSize()) {
                WalyoRoot(
                    tabManager = tabManager,
                    session = session,
                    settingsRepository = app.settingsRepository,
                    historyRepository = app.historyRepository,
                    callbacks = this@MainActivity,
                    actions = rootActions,
                )
                AnimatedVisibility(
                    visible = showLaunchSplash,
                    enter = EnterTransition.None,
                    exit = fadeOut(tween(SPLASH_EXIT_MS)) + scaleOut(tween(SPLASH_EXIT_MS), targetScale = 1.08f),
                ) {
                    LaunchSplash(
                        durationMs = AdPolicy.SPLASH_MIN_MS.toInt(),
                        onFirstFrame = { splashOnScreen.complete(Unit) },
                        onBack = ::finish,
                    )
                }
            }
        }
        lifecycleScope.launch { prepareFirstScreen() }
    }

    /**
     * Plays the launch animation while tabs restore and the launch ad loads in parallel.
     * Timing counts from the splash's first frame (slow phones take a while to draw it):
     * after [AdPolicy.SPLASH_MIN_MS] the ad is shown as soon as it is ready, latest at
     * [AdPolicy.SPLASH_AD_TIMEOUT_MS]; the splash fades into Home once the ad is closed.
     */
    private suspend fun prepareFirstScreen() = coroutineScope {
        // Runs alongside the launch ad; the campaign shortcut appears as soon as it is saved.
        app.appScope.launch { app.referralManager.applyOnce(debugReferrer) }
        val settings = app.settingsRepository.settings.first()
        val adsOn = AdPolicy.adsEnabled(settings.adFreeUntil, System.currentTimeMillis())
        // Start loading right away; only the waiting is limited below.
        val adLoad = async { adsOn && app.appOpenAdManager.awaitLoaded() }
        val saved = app.settingsRepository.loadTabs()
        tabManager.restore(saved.tabs, saved.currentIndex)
        session.isLocked = settings.isPinSet

        splashOnScreen.await()
        delay(AdPolicy.SPLASH_MIN_MS)
        val adLoaded = withTimeoutOrNull(AdPolicy.SPLASH_AD_TIMEOUT_MS - AdPolicy.SPLASH_MIN_MS) { adLoad.await() }
        adLoad.cancel() // stops waiting only; a late ad stays cached for the next app-open
        val showAd = adLoaded == true && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        openPendingLaunchInput(settings.searchEngine)
        isReady = true
        if (showAd) {
            app.appOpenAdManager.show(this@MainActivity) { showLaunchSplash = false }
        } else {
            showLaunchSplash = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val input = launchInputFrom(intent) ?: return
        pendingLaunchInput = input
        if (isReady) {
            lifecycleScope.launch {
                openPendingLaunchInput(app.settingsRepository.settings.first().searchEngine)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Any process-level "came back" event has already been handled by now.
        leftForOtherApp = false
    }

    override fun onStop() {
        super.onStop()
        // Saved from the Activity (not the process observer) so pressing Back to exit also saves tabs.
        persistTabs()
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
        tabManager.destroyAll()
        super.onDestroy()
    }

    private fun openPendingLaunchInput(engine: SearchEngine) {
        val input = pendingLaunchInput ?: return
        pendingLaunchInput = null
        tabManager.newTab(UrlUtils.resolve(input, engine))
    }

    private fun launchInputFrom(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.dataString?.takeIf(UrlUtils::isWebUrl)
        Intent.ACTION_WEB_SEARCH -> intent.getStringExtra(SearchManager.QUERY)?.takeIf { it.isNotBlank() }
        else -> null
    }

    /** Called when the whole app returns from the background (not on the first launch). */
    private fun onAppForeground() {
        if (!hasStartedOnce) {
            hasStartedOnce = true
            return
        }
        val awayMs = SystemClock.elapsedRealtime() - backgroundedAt
        val shortTripToOtherApp = leftForOtherApp && awayMs < OTHER_APP_GRACE_MS
        leftForOtherApp = false
        if (shortTripToOtherApp) return

        lifecycleScope.launch {
            val settings = app.settingsRepository.settings.first()
            if (settings.isPinSet && awayMs >= LOCK_AFTER_MS) session.isLocked = true

            val adsOn = AdPolicy.adsEnabled(settings.adFreeUntil, System.currentTimeMillis())
            if (adsOn && session.fullscreenView == null && awayMs >= AdPolicy.RESUME_AD_MIN_AWAY_MS) {
                app.appOpenAdManager.showIfReady(this@MainActivity, AdPolicy.RESUME_AD_INTERVAL_MS)
            }
        }
    }

    private fun persistTabs() {
        val snapshots = tabManager.snapshots()
        val index = tabManager.currentIndex
        app.appScope.launch { app.settingsRepository.saveTabs(snapshots, index) }
    }

    // ---------------------------------------------------------------- BrowserCallbacks

    override fun onPageVisited(url: String, title: String) {
        app.appScope.launch { app.historyRepository.record(url, title) }
    }

    override fun onDownloadRequested(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        if (!UrlUtils.isWebUrl(url)) {
            toast("This type of download isn't supported yet")
            return
        }
        val request = DownloadRequest(url, userAgent, contentDisposition, mimeType)
        if (DownloadHelper.needsStoragePermission(this)) {
            pendingDownload = request
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        startDownload(request)
    }

    override fun onExternalLink(url: String) {
        when (val result = ExternalLinks.open(this, url)) {
            ExternalLinks.Result.Launched -> leftForOtherApp = true
            is ExternalLinks.Result.Fallback -> tabManager.current.navigate(result.url)
            ExternalLinks.Result.NoApp -> toast("No app found to open this link")
        }
    }

    override fun onShowFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (session.fullscreenView != null) {
            callback.onCustomViewHidden()
            return
        }
        session.fullscreenView = view
        session.fullscreenCallback = callback
        setSystemBarsHidden(true)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onHideFullscreen() = exitFullscreen(notifyPage = false)

    override fun onFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams,
    ): Boolean {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = callback
        return try {
            leftForOtherApp = true
            fileChooserLauncher.launch(params.createIntent())
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No file picker available", e)
            pendingFileCallback = null
            leftForOtherApp = false
            toast("No app found to pick files")
            false
        }
    }

    // ---------------------------------------------------------------- Actions

    private fun exitFullscreen(notifyPage: Boolean) {
        val callback = session.fullscreenCallback
        session.fullscreenView = null
        session.fullscreenCallback = null
        setSystemBarsHidden(false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (notifyPage) callback?.onCustomViewHidden()
    }

    private fun setSystemBarsHidden(hidden: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (hidden) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun startDownload(request: DownloadRequest) {
        try {
            val fileName = DownloadHelper.enqueue(this, request)
            toast("Downloading $fileName")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Download rejected for ${request.url}", e)
            toast("Couldn't start this download")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Download folder unavailable", e)
            toast("Couldn't start this download")
        }
    }

    private fun shareUrl(url: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, url)
        leftForOtherApp = true
        startActivity(Intent.createChooser(send, null))
    }

    private fun openDownloads() {
        try {
            leftForOtherApp = true
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No downloads app", e)
            leftForOtherApp = false
            toast("Downloads app not available")
        }
    }

    private fun requestDefaultBrowser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    toast("Vidzo is already your default browser")
                } else {
                    defaultBrowserLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
                }
                return
            }
        }
        try {
            leftForOtherApp = true
            startActivity(Intent(AndroidSettings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Default apps settings not available", e)
            leftForOtherApp = false
            toast("Open Settings → Apps → Default apps to choose Vidzo")
        }
    }

    /** Opt-in rewarded video: the user chooses to watch it to get 1 hour without ads. */
    private fun watchRewardedAd() {
        toast("Loading video…")
        lifecycleScope.launch {
            val ad = app.rewardedAdManager.load()
            if (ad == null) {
                toast("No video available right now. Please try again later.")
                return@launch
            }
            app.rewardedAdManager.show(this@MainActivity, ad) {
                app.appScope.launch {
                    app.settingsRepository.setAdFreeUntil(System.currentTimeMillis() + AdPolicy.AD_FREE_DURATION_MS)
                }
                toast("Ads are off for the next hour. Enjoy!")
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "MainActivity"
        const val EXTRA_TEST_REFERRER = "walyo_test_referrer"
        const val SPLASH_EXIT_MS = 350
        val LOCK_AFTER_MS: Long = TimeUnit.SECONDS.toMillis(30)
        val OTHER_APP_GRACE_MS: Long = TimeUnit.MINUTES.toMillis(10)
    }
}
