# AGENTS.md

Guide for anyone (human or AI agent) changing MyTube: how the project is put together, the rules
learned the hard way, how to debug on the TV, and what is still open. Read it before touching code.

## What this is

MyTube is a personal, ad-free YouTube client for **Android TV**, written from scratch in Kotlin +
Jetpack Compose for TV. Only the data layer (YouTube API, sign-in, stream decoding, SponsorBlock)
comes from SmartTube's **MediaServiceCore** and **SharedModules** (MIT, yuliskov), included
unmodified as git submodules under `external/`.

Scope is deliberately small: **Home, Search, Settings and the player**. Subscriptions, Library,
channel pages and playlists were dropped on purpose; don't add them back unless asked.

**Target device:** Redmi / Mi TV 4X 50", Android 9, 4K panel, **32-bit userspace (armeabi-v7a)**,
Amlogic chip. The owner tests over wireless adb.

## How to work on this repo

- **Ask before building.** The owner wants questions first, then the work. List doubts as
  numbered questions with lettered options in plain text (no multiple-choice widgets), wait for
  answers like `1. a 2. c`, then build. Don't fill gaps with assumptions.
- **One step at a time.** One focused change per branch and pull request; never push to `main`.
  A merged PR's branch is finished: start follow-up work from the latest `main`. If a new change
  depends on an unmerged PR, branch from that PR and say "merge #N first" in the description.
- **Every merge to `main` is a release.** CI builds the APK and publishes release `build-<run
  number>`. The newest APK is always at
  `https://github.com/Shivamingale3/my_tube/releases/latest/download/MyTube.apk`. The app's
  Settings → About can install it on the TV.
- **Give TV commands in full**, one command per code block, for example:

  ```bash
  curl -L -o MyTube.apk https://github.com/Shivamingale3/my_tube/releases/latest/download/MyTube.apk
  ```

  ```bash
  adb install -r MyTube.apk
  ```

  ```bash
  adb shell am start -n io.github.shivamingale3.mytube/.MainActivity
  ```

- **Explain in plain words** what was wrong and what changed. End reports with numbered things to
  check on the TV.
- Bump `versionCode` / `versionName` in `app/build.gradle.kts` for user-visible changes, unless
  another open PR bumps it too (then leave it and say so). Updates are matched by CI build number,
  not by version name.

## Build and test

```bash
git clone --recurse-submodules https://github.com/Shivamingale3/my_tube.git
```

```bash
./gradlew :app:assembleDebug
```

Run **only our modules' tests**; SmartTube's own test suites fail and aren't ours to fix:

```bash
./gradlew :app:assembleDebug :core:youtube-bridge:testDebugUnitTest :core:screens:testDebugUnitTest :skins:skin-dot:testDebugUnitTest :skins:skin-tonal:testDebugUnitTest
```

**Previews** (Robolectric + Roborazzi) render screens to PNG for design checks; they're skipped
unless `-Ppreview` is given. Pass `-PpreviewImages=<dir>` with `<name>.jpg` files (`t_<videoId>`,
`a_<channel>`) for real thumbnails.

```bash
./gradlew :skins:skin-tonal:testDebugUnitTest -Ppreview
```

The PNGs land in `skins/skin-tonal/build/previews/` (both skins) and `skins/skin-dot/build/previews/`.
Robolectric doesn't draw focus rings, so focus has to be checked on the TV.

**Live probes** hit YouTube for real (`HomeFeedProbe`); opt-in:

```bash
./gradlew :core:youtube-bridge:testDebugUnitTest -PliveProbe -i
```

Stream decoding (J2V8, native) can't run on the desktop JVM, so playback can only be tested on
the TV.

**Sandboxed build machines:** Maven Central may answer HTTP 429. A local, uncommitted Gradle init
script (`~/.gradle/init.d/`) that swaps `repo.maven.apache.org` for
`https://maven-central.storage-download.googleapis.com/maven2/` and sets
`robolectric.dependency.repo.url` to the same mirror fixes it. Don't commit it.

Toolchain: AGP 8.13, Kotlin 2.3, Gradle 8.14, JDK 17, compileSdk 36, minSdk 26, targetSdk 35.

## Hard rules

1. **Never edit `external/`.** Submodules stay pinned and unmodified. Work around them from our code
   (reflection is used once for this, see "Client memory" below).
2. **OkHttp stays at 3.12.13** (forced in the root `build.gradle.kts`); SmartTube's code needs it.
   That's why images load through **Coil 3 over Ktor**, not OkHttp.
3. **Coil stays ≤ 3.5.x** while compileSdk is 36 (Coil 3.6+ needs compileSdk 37).
4. **ABI is armeabi-v7a only.** The TV's userspace is 32-bit; don't add 64-bit ABIs.
5. Keep the debug keystore `app/debug.keystore`: every build must install over the previous one.
6. Skins only draw. Behaviour lives in view models in `core/screens`, so both skins behave the same.
7. Don't put model identifiers or the owner's email in commits, PRs or code.

## Architecture

| Module | Role |
|---|---|
| `app/` | Entry (`MyTubeApp`, `MainActivity`), navigation (`MyTubeRoot`), `PlayerRoute` (video surface + captions), `SettingsViewModel`, `AppUpdater`, `Skins` registry, splash |
| `core/model/` | Our own data types: `Video`, `VideoRow`, `HomeFeed`, `Related`, `PlaybackInfo`, `StreamSource`, `SponsorSegment`, accounts |
| `core/youtube-bridge/` | **The only code that talks to MediaServiceCore**: `YouTubeSource` (home, history, search, suggestions, related), `YouTubePlayback` (streams, prefetch, history position, SponsorBlock), `YouTubeAccounts` (device-code sign-in) |
| `core/player/` | `PlayerController` (the one ExoPlayer, Media3 1.10), `TvRenderersFactory` (TV decoder tuning), stats |
| `core/screens/` | View models + UI state: `HomeViewModel`, `SearchViewModel`, `PlayerViewModel`, `AppSettings` / `Preferences`, `SettingsUiState` |
| `skins/skin-api/` | The `Skin` contract and shared helpers: `DesignScale`, `SkinAccent`, `Scrubber`, `UpNextShelf`, `FocusRestore` |
| `skins/skin-dot/` | Dot skin (Nothing-style dot matrix; Doto, Geist, Geist Mono fonts) |
| `skins/skin-tonal/` | Tonal skin (black, white pills, Lexend, wavy progress) – the owner's daily skin |

- **Skin contract:** `Skin` has `Theme`, `Shell`, `Home`, `Search`, `Settings`, `Player`. A new
  skin implements all six and is added to `Skins.all`.
- **`DesignScale`** sets density so **1 dp = 1 px of a 1280×720 design**. Use the design
  numbers directly.
- **Accent** is `SkinAccent.color` (Compose state). Skins read it through `DotColors.Accent` /
  `TonalColors.Accent`, so a change in Settings shows at once.
- **DI:** Koin, module in `MyTubeApp.kt`. View models are activity-scoped; `PlayerViewModel` is
  shared across every video opened.
- **Navigation** (`MyTubeRoot`): splash (0.7 s) → Shell with tabs Home/Search; the profile icon
  opens Settings; a played video opens `PlayerRoute`. Tab content sits in a
  `SaveableStateHolder` so scroll survives the player; `FocusRestore` gives focus back to the
  video that was played.

## Playback pipeline (read before touching the player)

1. `YouTubePlayback.resolve(videoId)` asks MediaServiceCore for format info.
2. **SABR:** some YouTube clients now return only SABR streams (needs SmartTube's modified
   ExoPlayer, which we don't have) plus a 360p file. `resolve` switches clients
   (`switchNextClientNow`, up to 6 tries) until one offers DASH formats, an HLS URL or a DASH URL.
   If none does, it plays the first playable answer and skips the search for 30 minutes.
   Resolves are serialized with a `Mutex`, because the client choice is global state in
   MediaServiceCore.
3. **Client memory:** MediaServiceCore saves the working client but always starts a launch from TV.
   We store the client that gave DASH/HLS in our prefs and restore it into
   `VideoInfoService.mNextClient` by reflection before the first request. If that fails, the
   first video just searches again.
4. `pickSource()` order: live + HLS → DASH formats (manifest built locally by `YouTubeMPDBuilder`)
   → DASH URL → HLS → single file → SABR (error).
5. **Captions:** the local DASH manifest already lists caption tracks, so they aren't sideloaded for
   DASH. Other sources sideload captions with `setTreatLoadErrorsAsEndOfStream(true)`, so a broken
   caption never stops the video. Text renderers have legacy decoding on for raw WebVTT/TTML.
   `PlayerRoute` draws cues with a Media3 `SubtitleView`, re-centred, because YouTube cues are
   pinned left (`align:start position:0%`).
6. **Prefetch:** Home prefetches the featured and continue-watching videos; focusing a card for
   800 ms prefetches it; the end-screen countdown prefetches Up next. The cache lasts 10 minutes.
7. **Resume:** only when the video was left ≥ 1 min in and < 90% (`resumePosition`). Positions are
   reported to YouTube history every 30 s and on close.
8. **End of video:** `related()` gives Up next + related videos; when the video ends a 10 s
   countdown plays Up next. `open()` stops the old video first, so an ended state can't restart
   the countdown.

### TV hardware quirks (Amlogic, Android 9)

- The VP9 decoder declares 3840×2160 but only enough blocks/s for 4K30, so Media3 would skip
  4K60. `TvRenderersFactory` lets formats within the declared **size** past the declared
  **speed** (HDR is not lifted). Stats shows them as `OK · PAST LIMIT`.
- Android reports a single 1920×1080 @60 display mode on the 4K panel, so the viewport cap on
  automatic quality is cleared. **Frame-rate matching is impossible** here (one mode only).
- No AV1 decoder; HEVC and VP9 are hardware.
- `PlayerRoute` sets `keepScreenOn` while playing, buffering or resolving; otherwise the
  screensaver starts over a playing video.

### Remote and focus behaviour

- `Scrubber` (skin-api): ←/→ with controls hidden, or with the progress bar focused, seeks. A
  tap is 10 s; holding speeds up (10 s, 30 s, 1 min, 3 min, 5 min, 10 min). Only the target moves
  while held; the seek happens on release. It's keyed per video.
- The progress bar is the default focus when controls appear (the play button on live streams).
- ↓ from the control buttons opens the Up next shelf (`UpNextShelf`); Back closes it.
- Back order in the player: close sheet → close shelf → hide controls → leave player.
- Use `Modifier.restoreFocus(video.id)` on anything that plays a video from Home or Search.

## Debugging on the TV

- **STATS** button in the player (toggle in Settings → Playback). It shows the source and client
  (e.g. `DASH · VISIONOS`), how many clients were tried, resolve and first-frame times, screen
  mode, decoders, bandwidth, buffer, dropped frames, and every offered stream with codec, bitrate
  and whether this TV can play it. Ask for a photo of it first; it answers most playback
  questions.
- **Crash or odd behaviour:** Android's log trimming hides most app lines. Turn it off for the app
  (UID shown in logcat, e.g. 10081), clear the log, record while reproducing, then filter:

  ```bash
  adb logcat -P "10081"
  ```

  ```bash
  adb logcat -c
  ```

  ```bash
  adb logcat > mytube-full.txt
  ```

  (Press Ctrl+C after reproducing.)

  ```bash
  grep -iE "mytube|ExoPlayer|AndroidRuntime|FATAL|Fatal signal|libc|DEBUG|MediaCodec|died|ActivityManager" mytube-full.txt | tail -300 > mytube-crash.txt
  ```

  A real crash shows `FATAL EXCEPTION` or "Process … has died". `VsyncCallback` /
  `VSyncSamplerV33` lines on Android 9 are harmless Media3 notices.
- **Installed version:**

  ```bash
  adb shell dumpsys package io.github.shivamingale3.mytube | grep versionName
  ```

- **Decoder limits:**

  ```bash
  adb shell "cat /vendor/etc/media_codecs.xml" | grep -A10 'vp9.decoder.awesome"'
  ```

- **Display modes:**

  ```bash
  adb shell dumpsys display | grep -iE "supportedModes|DisplayModeRecord"
  ```

- Two agent sessions editing the same checkout at once collide; if files change unexpectedly,
  check that nothing else is working in the repo.

## YouTube / MediaServiceCore facts

- Signed out, YouTube's TV home is empty (only a "Make YouTube your own" nudge). `home()` then
  builds rows from Live, Gaming, Sports, News and Shorts. Trending and Music are empty signed out.
- `homeObserve` pages through continuations (capped at 40 rows); `getHome()` returns only
  the first page.
- MediaServiceCore's Rx results are delivered on the **main thread**; in Robolectric tests pump
  the main looper (`shadowOf(mainLooper).idle()`).
- `GlobalPreferences.instance(context)` must be initialised before `YouTubeServiceManager` is
  used (`MediaServiceCore.services()` does it).
- YouTube Music items in history (`TYPE_MUSIC`, "Artist - Topic" channels) fail with HTTP 403 and
  are filtered out.
- `getSearch`, `getSearchTags` and `getMetadata` are synchronous network calls; call them on
  `Dispatchers.IO`.
- If streams break after a YouTube change, first update the MediaServiceCore/SharedModules
  submodule pins to SmartTube's latest (they track YouTube changes); that's allowed, editing them
  isn't.

## Known gaps and ideas

Open, roughly in order of value. Ask the owner before starting any of them.

- **SABR playback:** if every client ends up SABR-only, HD needs porting SmartTube's SABR support
  into Media3. This is big.
- **Live streams without HLS** fall back to a locally built dynamic DASH manifest that Media3 can't
  refresh, so they may stall.
- **Search paging:** only the first page of results loads (`continueGroup` isn't used yet).
- **Home after account switch** reloads from scratch.
- **Chapters** (available in metadata) aren't shown on the progress bar.
- **HDR** formats aren't specially handled or tested on this panel.
- **Shorts** are only filtered out, never shown in a dedicated way.
- The **in-app updater** can only install builds newer than the installed one. Android asks once
  to allow installing apps.
- Thumbnails use a plain placeholder box (cheapest); no shimmer by design.

## Credits and licences

- MediaServiceCore and SharedModules by yuliskov, MIT. Unmodified submodules.
- Fonts under the SIL Open Font License, with their licence files in each skin's `licenses/`:
  - Doto, Geist and Geist Mono (Dot skin)
  - Lexend and Roboto Flex (Tonal skin)
- Logo art is in `assets/logo/` (cleaned masters for the icon, banner and splash).
