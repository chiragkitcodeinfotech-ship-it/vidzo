# Vidzo — Play Store Deployment Guide

Tracking doc for publishing Vidzo (`com.vidzo.browser`) to Google Play. Check items off as we
finish them. Keep this file updated — it's our shared source of truth for where we are.

Account: ITCODE INFOTECH (Play Console personal account, ID 4816617903312651088).

---

## 0. Blockers — must fix before the first upload

| # | Item | Status | Why it matters |
|---|------|--------|-----------------|
| 1 | Real upload keystore (not debug key) | ✅ done | `keystore/vidzo-upload.jks` generated, wired into `app/build.gradle.kts` via `local.properties` (git-ignored). `./gradlew bundleRelease` now produces a real-signed AAB at `app/build/outputs/bundle/release/app-release.aab`. |
| 2 | Real AdMob IDs in `gradle.properties` | ❌ empty | Release build falls back to **Google's test ad IDs** when these are blank. Serving test ads to real users violates AdMob policy and can get the account suspended. Must be filled in before going live. |
| 3 | Privacy policy URL | ❌ none yet | Mandatory in Play Console's "App content" section for every app. |
| 4 | Store listing graphics (icon, feature graphic, screenshots) | ❌ none yet | Required to publish; can't submit without them. |
| 5 | Content rating questionnaire | ❌ not started | Required. |
| 6 | Data safety form | ❌ not started | Required — declares what data the app collects (AdMob collects advertising ID etc.). |
| 7 | Target audience & ads declaration | ❌ not started | Required — declares the app is not aimed at children (important since it shows ads). |

We'll knock these out in order below.

---

## Phase 1 — Finish "Create app" ✅ DONE (app created in Play Console)

Fill in exactly:

- **App name**: `Vidzo` (this becomes the default store listing title — can be changed later in the main store listing, so don't overthink it now)
- **Package name**: `com.vidzo.browser` — must match `applicationId` in `app/build.gradle.kts:38` **exactly**. Click "Check availability" to confirm it's free.
- **Default language**: English (United States) — en-US
- **App or game**: App
- **Free or paid**: Free
- **Declarations**: check both boxes (Developer Program Policies, US export laws) — these are legal confirmations only you can tick.

⚠️ **Important**: the package name is locked forever once you create the app here. Triple-check it's `com.vidzo.browser` before clicking Create.

Click **Create app**.

---

## Phase 2 — Build a signed release AAB ✅ DONE

Google Play requires an `.aab` (Android App Bundle), not an `.apk`, for new apps.

What was done:

1. Generated an upload keystore: `keystore/vidzo-upload.jks` (alias `vidzo`, valid until 2054).
   Passwords saved in `keystore/vidzo-upload.passwords.txt` — **copy these into a password manager and
   then delete that file.** Both the keystore and this folder are git-ignored (`/keystore` in `.gitignore`).
   ⚠️ If you ever lose this keystore or its password, you can **never** publish an update to this app
   under the same Play listing again — back it up somewhere durable (not just this machine).
2. Wired it up via `local.properties` (git-ignored, already had `sdk.dir`) — new keys:
   `vidzo.release.storeFile`, `vidzo.release.storePassword`, `vidzo.release.keyAlias`, `vidzo.release.keyPassword`.
3. `app/build.gradle.kts` now reads those at build time and uses the real `release` signing config when
   present, falling back to the debug key otherwise (so other machines/CI without the keystore still build).
4. Verified: `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` (7.6 MB),
   confirmed signed with `CN=Vidzo` certificate (not the debug one).

Remaining in this phase: fill in real AdMob IDs in `gradle.properties` (`walyo.admob.appId`,
`walyo.admob.appOpen`, `walyo.admob.native`, `walyo.admob.rewarded`) from your AdMob console — currently
still blank, so this AAB still serves **test ads**. Must be set before uploading to Production (internal
testing with test ads is fine).

---

## Phase 3 — App content section (Play Console → your app → "App content" / "Policy")

Six short questionnaires, all required:

- **Privacy policy**: paste a URL. If you don't have one yet, tell me — I can draft a simple privacy policy page for a browser app (covers WebView, AdMob, DataStore-stored history/settings) and we host it somewhere (e.g. a simple static page).
- **Ads**: declare "Yes, my app contains ads" (AdMob app open + native + rewarded).
- **Content rating**: fill the IARC questionnaire (category: Utility/Browser; no violence/gambling content — should rate "Everyone" easily).
- **Target audience**: select age groups the app targets (standard answer for a general browser: 18+ or "not targeting children" track — we'll pick based on your actual intent).
- **Data safety**: declare what's collected. For Vidzo: advertising ID (via AdMob), no account/login, history/bookmarks stored locally only (DataStore, not uploaded).
- **Government apps / COVID app / News app**: No for all.

---

## Phase 4 — Store listing (Play Console → "Main store listing")

Need from you (or I can draft copy, you supply images):

- Short description (80 chars max)
- Full description (4000 chars max)
- App icon: 512×512 PNG
- Feature graphic: 1024×500 PNG/JPG
- Phone screenshots: minimum 2, recommend 4–8 (take from the Pixel 8 emulator or a real device)

---

## Phase 5 — Create a release

**Recommended path**: Internal testing track first (instant, no review wait), test the real signed build on a device, then promote to Production.

1. Play Console → Testing → Internal testing → Create release
2. Upload the signed `.aab`
3. Add release notes (e.g. "Initial release")
4. Add yourself as a tester (your email), save, start rollout
5. Install via the opt-in link Play gives you, verify it works on a real device
6. Once happy: Production → Create release → same AAB → submit for review

---

## Phase 6 — Submit & wait

- First-time app review typically takes **a few hours to a few days**.
- Watch for policy rejections in the Play Console "Policy" tab — common first-timer issues: missing privacy policy, incomplete data safety form, ads policy mismatch.

---

## Notes / things not to forget

- Debug builds (`com.vidzo.browser.debug`) always use Google test ad IDs — fine, no action needed there.
- Don't commit the keystore file or its passwords — `.gitignore` already blocks `*.jks`/`*.keystore`.
- `CLAUDE.md` says release signing currently uses the debug key as a placeholder — Phase 2 above replaces that.
- Per project rules: I won't touch env vars, auth, or package versions without you explicitly asking — signing config counts as sensitive, so I'll show the diff before applying it.
