# Handoff: MyTube (Android TV, ad-free YouTube)

You're picking up an ongoing personal project. Read this, then read **`AGENTS.md`** in the repo before doing anything. `CLAUDE.md` imports it, so Claude Code sessions load it automatically.

## Repo
- https://github.com/Shivamingale3/my_tube (public), default branch `main`.
- Clone with submodules:

```bash
git clone --recurse-submodules https://github.com/Shivamingale3/my_tube.git
```

- **State:** everything up to **PR #18 is merged**, and there are no open PRs. `main` is at **1.3.1** (versionCode 15).
- `AGENTS.md` covers:
  - the working agreement and hard rules
  - the module map
  - the playback pipeline: the SABR client search, remembering the working client, captions, resume, Up next
  - the TV's quirks
  - remote and focus behaviour
  - debugging recipes and the backlog

## What it is
Kotlin + Jetpack Compose for TV. It reuses only SmartTube's data layer (MediaServiceCore + SharedModules, MIT, unmodified submodules under `external/`).

Screens:
- **Home**
- **Search**: typed and voice; single-column large results
- **Settings**: account sign-in, skin, accent, resume, SponsorBlock, stats button, in-app updates
- **Player**:
  - up to 4K60; quality, speed and captions
  - SponsorBlock skip
  - YouTube-style hold-to-seek
  - audio-only mode: video → thumbnail → black
  - stats panel
  - end screen with "Up next" and a 10 s autoplay, plus the ↓ "Up next" shelf

Two skins:
- **Dot**: dot-matrix
- **Tonal**: Lexend, wavy progress bar. This is the one the owner uses daily.

## The owner's TV
- Redmi / Mi TV 4X 50", Android 9, 4K panel, 32-bit userspace (armeabi-v7a), Amlogic chip, connected over wireless adb.
- To test a change, the owner merges the PR, waits for CI's release, then installs it from Settings → About or with:

```bash
curl -L -o MyTube.apk https://github.com/Shivamingale3/my_tube/releases/latest/download/MyTube.apk
```

```bash
adb install -r MyTube.apk
```

```bash
adb shell dumpsys package io.github.shivamingale3.mytube | grep versionName
```

## How the owner wants you to work (important)
- **Ask before building.**
  - Write numbered questions with lettered options in plain text (no multiple-choice widgets).
  - The owner answers like `1. a 2. c`.
  - Don't assume to fill gaps.
  - Skip questions only when a request is completely clear.
- **One branch and one PR per change.** Branch from the latest `main`; never commit to `main`. Every merge to `main` publishes release `build-<run number>`.
- If a change depends on an unmerged PR, branch from that PR and write "merge #N first".
- **Commit and push before ending a turn.** A stop hook checks for uncommitted changes.
- **Reports:**
  - Give full adb/curl commands, one per code block.
  - Explain what was wrong in plain words.
  - End with numbered things to check on the TV.
- Bump `versionCode` / `versionName` in `app/build.gradle.kts` for visible changes.
- Use the GitHub MCP tools for PRs; `git push` over HTTPS works.
- Run only our modules' tests:

```bash
./gradlew :app:assembleDebug :core:youtube-bridge:testDebugUnitTest :core:screens:testDebugUnitTest :skins:skin-dot:testDebugUnitTest :skins:skin-tonal:testDebugUnitTest
```

- **Previews** (PNG renders for design checks) go to `skins/skin-tonal/build/previews/`. Robolectric doesn't draw focus rings.

```bash
./gradlew :skins:skin-tonal:testDebugUnitTest -Ppreview
```

- **Live YouTube probes** are opt-in; `probeSearch` dumps raw search items:

```bash
./gradlew :core:youtube-bridge:testDebugUnitTest -PliveProbe -i
```

## Recent history
- **#14:** end screen with Up next, a countdown and related videos; Up next shelf.
- **#15:**
  - single-column large search results
  - focus returns to the played video
  - resume only when 1 min+ and under 90%
  - seek state kept per video
- **#16:** `AGENTS.md` + `CLAUDE.md`.
- **#17:** play/pause is always the default focus, and focus returns to it after sheets or the shelf close; audio-only mode; untitled search results dropped.
- **#18:**
  - **Watched search results lost their text.** The watched bar's `fillMaxWidth` stretched the thumbnail box, which squeezed the text column to zero width.
  - **Channel name showed twice.** It's now split from "views • date".

## Not yet confirmed on the TV (ask the owner)
- #18: do watched search results show their title, and does the channel appear only once?
- #17: is play/pause highlighted when the controls appear and after closing Captions; does the audio button cycle art → dark → video?
- #14/#15: end-screen autoplay, the ↓ shelf, focus returning to the played video, no seek carry-over between videos.

## Lessons from recent bugs
- In Compose, a `fillMaxWidth` child inside a `Box` with no width of its own makes the `Box` take the full row. Give thumbnail boxes a fixed size.
- When focus "disappears" after a sheet closes, explicitly request focus on a known control.
- If a playback or data question comes up, ask for a photo of the STATS panel first, or run a live probe. YouTube data is usually fine and the bug is in our UI.

## Backlog (ask before starting)
- SABR playback, needed if every client goes SABR-only (big).
- Live streams without HLS.
- Search paging (`continueGroup`).
- Chapters on the progress bar.
- HDR handling.

## Don't
- Edit `external/`.
- Move OkHttp off 3.12.13 or Coil past 3.5.x.
- Add 64-bit ABIs.
- Replace `app/debug.keystore`.
- Bring back the Subscriptions, Library or Channel screens unless asked.
- Put model names or the owner's email in commits or PRs.
