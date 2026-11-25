# Vidzo — Play Store Deployment Guide

Primary repo: `github.com/chiragkitcodeinfotech-ship-it/vidzo` (switched from `kananichirag/vidzo-android`
on 2026-11-25 — that old repo still exists but is no longer used).

Tracking doc for publishing Vidzo (`com.vidzo.browser`) to Google Play. Check items off as we
finish them. Keep this file updated — it's our shared source of truth for where we are.

Account: ITCODE INFOTECH (Play Console personal account, ID 4816617903312651088).

---

## 0. Blockers — must fix before the first upload

| # | Item | Status | Why it matters |
|---|------|--------|-----------------|
| 1 | Real upload keystore (not debug key) | ✅ done | `keystore/vidzo-upload.jks` generated, wired into `app/build.gradle.kts` via `local.properties` (git-ignored). `./gradlew bundleRelease` now produces a real-signed AAB at `app/build/outputs/bundle/release/app-release.aab`. |
| 2 | Real AdMob IDs in `gradle.properties` | ❌ empty | Release build falls back to **Google's test ad IDs** when these are blank. Serving test ads to real users violates AdMob policy and can get the account suspended. Must be filled in before going live. |
| 3 | Privacy policy URL | ✅ live | `https://chiragkitcodeinfotech-ship-it.github.io/vidzo/privacy-policy.html` — paste into Play Console's Privacy Policy field. |
| 4 | Store listing graphics (icon, feature graphic, screenshots) | ✅ done | `store-assets/icon-512.png`, `store-assets/feature-graphic.png`, `store-assets/screenshots/*.png` (6 real app captures) — all uploaded. App name/short/full description filled in. Category: Tools. |
| 5 | Content rating questionnaire | ✅ done | Category: All Other App Types. All content questions answered No. |
| 6 | Data safety form | ✅ done | |
| 7 | Target audience & ads declaration | ✅ done | Target age: 13-15, 16-17, 18+ (excluded under-13 — browsers with unrestricted internet access can't target children). Ads: Yes. Advertising ID: Yes (purpose: advertising/marketing). Health: no health features. |

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

## Phase 5 — Create a release ✅ Internal testing live

Release 1 (1.0.0) is "Available to internal testers" (not reviewed by Google — internal testing skips review).

Installed on a real device via internal testing and confirmed working (2026-11-25). First install attempt failed with "Can't download" — resolved itself after a short propagation wait, as expected for a brand-new app's first internal release.

Remaining before Production:
1. ✅ Category was indeed wrongly set to Social — confirmed and fixed to **Tools** via Store presence → Store settings → App category (2026-11-25).
2. ✅ Real AdMob account created, app `Vidzo` added, 3 ad units created (App Open, Native, Rewarded). Real IDs in `gradle.properties` (2026-11-25), confirmed baked into rebuilt `app-release.aab`. AdMob app review still pending — ad serving will be limited until Google reviews/approves (can take a couple days), and full ad serving needs the app in Production.
3. ✅ Done — bumped versionCode to 2 (Play requires each upload to have a higher version code), rebuilt, uploaded as internal testing release 2 (1.0.0), live 2026-11-25.

---

## ✅ Phase 6 — Submitted to Production (2026-11-25)

Release 2 (1.0.0) is live in the Production track, targeted at India, awaiting Google's review
("unreviewed" badge still showing — normal until review completes). 0 installs so far, as expected
for a brand-new unreviewed submission.

Non-blocking suggestions Play Console surfaced (optional polish, not required to publish):
- Edge-to-edge display may not show correctly for all users (uses deprecated APIs)
- R8 memory/performance optimization available

These can be addressed in a future update — they did not block this submission.

## Phase 6 (original) — Submit & wait

- First-time app review typically takes **a few hours to a few days**.
- Watch for policy rejections in the Play Console "Policy" tab — common first-timer issues: missing privacy policy, incomplete data safety form, ads policy mismatch.

---

## Notes / things not to forget

- Debug builds (`com.vidzo.browser.debug`) always use Google test ad IDs — fine, no action needed there.
- Don't commit the keystore file or its passwords — `.gitignore` already blocks `*.jks`/`*.keystore`.
- `CLAUDE.md` says release signing currently uses the debug key as a placeholder — Phase 2 above replaces that.
- Per project rules: I won't touch env vars, auth, or package versions without you explicitly asking — signing config counts as sensitive, so I'll show the diff before applying it.
