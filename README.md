# MyTube

An ad-free YouTube client for Android TV, with two switchable skins (Dot and Tonal). Personal project.

## Install on a TV

Every push to `main` publishes a release; the newest APK is always at
`https://github.com/Shivamingale3/my_tube/releases/latest/download/MyTube.apk`.

With the TV connected over adb:

```bash
curl -L -o MyTube.apk https://github.com/Shivamingale3/my_tube/releases/latest/download/MyTube.apk
adb install -r MyTube.apk
```

Builds are signed with the fixed debug key in `app/debug.keystore`, so each new build installs over the previous one.
After the first install, **Settings → About → Install build N** downloads and installs newer releases on the TV itself
(Android asks once to allow MyTube to install apps).

## What's in it

- **Home**: your feed (or popular sections when signed out), continue watching, rows of videos.
- **Search**: typed or voice; Dot has its own dot keyboard, Tonal uses the TV keyboard.
- **Settings** (profile icon): accounts (sign in with a code at youtube.com/activate, switch, sign out), skin and
  accent colour, resume, SponsorBlock (offer or auto-skip), stats button, and updates.
- **Player**: up to 4K60 (Amlogic VP9 decoder limits lifted), quality/speed/captions, SponsorBlock skip, resume and
  watch-history sync, and a stats panel.

## Build locally

```bash
git clone --recurse-submodules https://github.com/Shivamingale3/my_tube.git
cd my_tube
./gradlew :app:assembleDebug
```

Needs JDK 17+ and the Android SDK (platform 36).

To check what YouTube currently returns for each feed (hits the network, so it's opt-in):

```bash
./gradlew :core:youtube-bridge:testDebugUnitTest -PliveProbe -i | grep PROBE
```

## Layout

| Path | What it is |
|---|---|
| `app/` | App entry, navigation, skin switching, Settings and updates |
| `core/model/` | Our own data types |
| `core/youtube-bridge/` | The only code that talks to MediaServiceCore |
| `core/player/` | The shared ExoPlayer (Media3) and its TV tuning |
| `core/screens/` | View models and screen state shared by every skin |
| `skins/skin-api/` | The `Skin` contract |
| `skins/skin-dot/`, `skins/skin-tonal/` | The two skins (fonts under OFL, see `licenses/`) |
| `external/` | Git submodules, never edited (see below) |

Previews of both skins: `./gradlew :skins:skin-tonal:testDebugUnitTest -Ppreview` writes PNGs to
`skins/skin-tonal/build/previews/`.

## Credits

The YouTube data layer comes from [MediaServiceCore](https://github.com/yuliskov/MediaServiceCore) and
[SharedModules](https://github.com/yuliskov/SharedModules) by yuliskov (MIT License), the engine behind SmartTube.
They are included unmodified as git submodules under `external/`; build compatibility settings live in the root
`build.gradle.kts`.
