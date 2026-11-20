# Walyo Browser — project context for AI assistants

Android browser app (Play app ID `com.vidzo.browser` since 2026-11-19; Kotlin package/namespace stays `com.walyo.browser` — the app ID is the one in Play URLs and marketing links and must never change after publishing) built with Kotlin + Jetpack Compose on top of Android System WebView, monetised with Google AdMob. Inspired by the user's reference app flow: **launch ad → home page → in-app browser**. Branding: user-facing name **"Vidzo"** (renamed from Walyo on 2026-11-19; Kotlin package, class names like `WalyoRoot`/`WalyoTheme` and DataStore file names still say walyo — keep them, renaming the DataStore files would wipe user data). Logo: white rounded play ▷ + small "live" dot on violet→cyan gradient. Launch screen: the system splash is blank (transparent `ic_splash.xml`) and leaves on the first frame; `ui/splash/LaunchSplash.kt` (Compose overlay in `MainActivity`) plays a ~2.5 s animation while the app-open ad loads. Timing counts from the splash's first frame: ad shows after `AdPolicy.SPLASH_MIN_MS` (2.5 s) once loaded, latest `SPLASH_AD_TIMEOUT_MS` (3.5 s), else Home without ad; splash fades out after the ad closes. Measured on Realme RMX3193 release build: splash at ~0.75 s, ad at ~3.8 s after tap (debug build is ~3 s slower to first frame — judge timing on release).

Status (2026-11-18): feature-complete v1.0.0, builds (debug + minified release, ~3.6 MB), 25 unit tests pass, verified on Pixel 8 API 36 emulator. Not yet published.

## Build / run
- **Use JDK 21.** Android Studio's bundled JBR is Java 25 and Gradle 8.14.3 fails on it with a cryptic `25.0.3` error.
  `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`
- `./gradlew assembleDebug` · `./gradlew testDebugUnitTest` · `./gradlew assembleRelease` · `./gradlew bundleRelease`
- Debug installs as `com.vidzo.browser.debug` and always uses Google **test** ad IDs.
- Versions: AGP 8.13.0, Kotlin 2.2.20, Compose BOM 2025.09.00, compileSdk/targetSdk 36, minSdk 26, play-services-ads 24.5.0, Coil 3.2.0, DataStore 1.1.7, installreferrer 2.2. `androidx.fragment` 1.8.6 is pinned only to satisfy lint (AdMob pulls an old Fragment).
- Emulator testing: `emulator -avd Pixel_8_API_36 -no-window`, then `adb exec-out screencap -p` for screenshots. The app-open ad shows a one-time "Viewing full screen — Got it" system hint that blocks BACK; tap Got it (≈925,631) first. `pm clear` resets it.

## Architecture (app/src/main/java/com/walyo/browser/)
| Path | Responsibility |
|---|---|
| `MainActivity.kt` | Splash (`installSplashScreen`, kept until `isReady`), `prepareFirstScreen()` (restore tabs → PIN lock → app-open ad → pending launch URL), process-foreground handling (resume ad, re-lock), implements `BrowserCallbacks` (history, downloads, external links, fullscreen video, file chooser), share/default-browser/rewarded actions |
| `WalyoApp.kt` | App singletons: `appScope`, repositories, ad managers, `ReferralManager`; starts `MobileAdsGate` |
| `ads/` | `MobileAdsGate` (SDK init + await), `AppOpenAdManager`, `RewardedAdManager`, `NativeAdCard` (Compose + XML `res/layout/native_ad.xml`), `AdPolicy` (all timing constants) |
| `browser/` | `BrowserTab` (Compose state + owned WebView), `TabManager` (always ≥1 tab), `WebViewFactory` (settings, WebViewClient/ChromeClient, `BrowserCallbacks` interface) |
| `data/` | `Models.kt`, `SettingsRepository` + `HistoryRepository` (Preferences DataStore storing JSON via org.json), `SuggestionsClient` (Google suggest endpoint) |
| `platform/` | `DownloadHelper` (DownloadManager), `ExternalLinks` (safe intent:// / tel: / app links) |
| `referral/` | Install-referrer marketing links: `Campaigns` (allowlist code→page), `ReferrerParser`, `InstallReferrerReader`, `ReferralManager` |
| `ui/` | `WalyoRoot` (navigation via overlay state, menu actions, dialogs, lock, fullscreen), `SessionState`/`RootActions`, screens: `home/`, `search/`, `browser/`, `tabs/`, `history/`, `lock/`, `dialogs/`, `components/`, `theme/` |
| `util/` | `UrlUtils` (input → URL or search), `PinHasher` (salted SHA-256) |

Navigation: no nav library. Current tab with `url == null` ⇒ Home screen, else Browser screen. Search/Tabs/History are overlays; Theme/Search-engine/Add-shortcut/Set-PIN/Remove-PIN are `RootDialog`s.

## Features
Animated launch screen + App Open ad on launch (and on return after ≥30 s away, max once per 30 min) · Home: address pill, tab counter, ⋮ menu, shortcut grid (add / long-press remove, max 15), promo pager (ad-free, search engine, default browser, PIN), native ad, recently visited · Browser: WebView with progress bar, bottom bar (back/forward/home/reload/share), fullscreen video, downloads, file upload, desktop mode · Tabs (persisted), history (300 max), search suggestions, 5 search engines, theme System/Light/Dark, 4-digit PIN lock (5 tries → 30 s cooldown), set as default browser, opt-in rewarded video → 1 h ad-free · Marketing-link install referrer → campaign page replaces the Cricbuzz shortcut (7th tile; once per install).

## Hard-won decisions — do not undo
1. **Every ad request must `MobileAdsGate.awaitReady()` first.** Requesting an ad on the main thread while `MobileAds.initialize` runs on IO deadlocked the app on the splash forever (SDK needs main thread for WebView init).
2. **`WalyoTheme` provides `LocalContentColor = onBackground`.** Without it icons/text default to black and vanish in dark mode (root is a `Box`, not a `Surface`).
3. **Tabs are persisted in `Activity.onStop()`**, not only the ProcessLifecycleOwner observer — Back-to-exit destroys the activity before the process ON_STOP fires.
4. **Manifest `configChanges` includes orientation/screenSize/uiMode/locale/fontScale/density/…** so WebViews and tabs survive config changes (WebViews hold the Activity context and live in `TabManager`).
5. **AdMob policy:** launch ad is **App Open** (allowed). Never use interstitial on app launch or a forced rewarded ad — the user asked for this; it risks an AdMob ban. Rewarded is opt-in only.
6. **Referrer campaigns use an allowlist of codes** (`Campaigns.kt`), never a raw URL from the link — prevents anyone pushing arbitrary sites into the app. Don't auto-open affiliate tracking links (cookie stuffing = Play/affiliate fraud); the user must tap.
7. **WebView hardening:** file/content access off, no JS bridge, `file:`/`content:` blocked, camera/mic/location requests denied, external intents get `BROWSABLE`, `component=null`, `selector=null`, URI-grant flags stripped.
8. Release AdMob IDs come from `gradle.properties` (`walyo.admob.*`); empty ⇒ falls back to test IDs. Release currently signed with the **debug key** (placeholder).

## Not done yet / next steps
- Real AdMob IDs in `gradle.properties`; own upload keystore (replace debug signing).
- UMP consent form (needed only if shipping to EEA/UK; plan is India-only first).
- Privacy policy page, Play Store assets/listing, `app-ads.txt` + website.
- Optional: prelander web page + server-side postback for affiliate/CPI tracking (`click_id` already parseable from referrer but unused); Firebase Remote Config for campaigns; interstitial at natural breaks (e.g. every 5th page load); longer `SPLASH_AD_TIMEOUT_MS` for slow networks; incognito tabs; bookmarks; strings.xml localisation (UI strings are currently inline English).
- Not yet verified on a real device: fullscreen video, downloads, PIN flow, rewarded flow, real referrer from Play.

## User preferences
- The user writes in informal English with typos. Answer in simple language with steps, and be concise.
- They want working, ready-to-use results, but explain the plan before big changes. Never commit or push without asking (the folder is not a git repo yet).
- Never modify auth, env vars or package versions unless asked.
