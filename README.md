# Vidzo Browser (formerly Walyo)

Video-first Android browser (Kotlin + Jetpack Compose + Android System WebView) with AdMob ads.

## Features
- Splash screen → full-screen **App Open ad** → home page
- Home: search bar, tab counter, menu, shortcut tiles (+ add / long-press to remove), promo cards, native ad, recently visited
- In-app browser (WebView): back / forward / home / reload / share, progress bar, fullscreen video, downloads, file upload, `intent://` / `tel:` / `whatsapp:` links
- Tabs (restored after restart), history, search suggestions, 5 search engines, light/dark/system theme, desktop site, PIN lock, set as default browser
- Opt-in **rewarded video** → 1 hour ad-free

## Build & run
Requires JDK 21 (Gradle 8.14 does not run on JDK 25).

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or open the folder in Android Studio and press Run.

Debug builds install as `com.vidzo.browser.debug` and always use Google's **test** ads.

## Before publishing to Play Store
1. **AdMob ids** — create an app + ad units (App open, Native advanced, Rewarded) in AdMob and put the ids in `gradle.properties` (`walyo.admob.*`). Empty values fall back to test ids.
2. **Signing** — create an upload keystore (Android Studio → Build → Generate Signed Bundle) and replace `signingConfig = signingConfigs.getByName("debug")` in `app/build.gradle.kts`.
3. **Consent** — add Google's UMP consent form if you target EEA/UK users.
4. **Privacy policy** URL and Data safety form in Play Console (the app shows ads and stores history locally).
5. In AdMob → Blocking controls, block *Gambling* and other sensitive categories.
6. Build the bundle: `./gradlew bundleRelease`.

## Project layout
```
app/src/main/java/com/walyo/browser/
  MainActivity.kt        splash, launch ad, lifecycle, downloads, intents, fullscreen
  WalyoApp.kt            app singletons
  ads/                   AdMob: init gate, app-open, rewarded, native card, ad policy
  browser/               BrowserTab, TabManager, WebViewFactory (WebView setup + clients)
  data/                  DataStore settings/history, suggestions client, models
  platform/              DownloadManager + external app links
  ui/                    Compose screens: home, search, browser, tabs, history, lock, dialogs, theme
  util/                  UrlUtils, PinHasher
```

## Marketing links (install referrer)
Users who install through a marketing link see the campaign page as a **home shortcut in place of Cricbuzz** (7th tile); normal Play Store installs don't.

1. Add a campaign in `app/src/main/java/com/walyo/browser/referral/Campaigns.kt`:
   `"offer1" to Shortcut("Today's top deals", "https://www.amazon.in/deals")`
2. Share this link (`c%3D` = `c=`):
   `https://play.google.com/store/apps/details?id=com.vidzo.browser&referrer=c%3Doffer1`
3. It runs once per install, on first open. Unknown codes are ignored.

Works only for installs from the Play Store link. To simulate it on a debug build:
```bash
adb shell pm clear com.vidzo.browser.debug
adb shell am start -n com.vidzo.browser.debug/com.walyo.browser.MainActivity --es walyo_test_referrer "c=offer1"
```

## Jenkins (test APKs from your own server)
`Jenkinsfile` runs the unit tests, builds `vidzo.apk` (normal) and `vidzo-<code>.apk` (marketing link,
`-Pvidzo.linkCampaign=<code>`) and copies both to `APK_DIR` (default `/var/www/html/apk`), which the web
server serves at `https://<your-domain>/apk/…`. The server needs JDK 21 and the Android SDK
(`platforms;android-36`, `build-tools;36.0.0`); paths are set at the top of the `Jenkinsfile`.
These are debug builds with test ads — never upload a `-<code>` build to the Play Store.
