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

## Build locally

```bash
git clone --recurse-submodules https://github.com/Shivamingale3/my_tube.git
cd my_tube
./gradlew :app:assembleDebug
```

Needs JDK 17+ and the Android SDK (platform 36).

## Layout

| Path | What it is |
|---|---|
| `app/` | App entry, navigation, skin switching |
| `core/model/` | Our own data types |
| `core/youtube-bridge/` | The only code that talks to MediaServiceCore |
| `external/` | Git submodules, never edited (see below) |

## Credits

The YouTube data layer comes from [MediaServiceCore](https://github.com/yuliskov/MediaServiceCore) and
[SharedModules](https://github.com/yuliskov/SharedModules) by yuliskov (MIT License), the engine behind SmartTube.
They are included unmodified as git submodules under `external/`; build compatibility settings live in the root
`build.gradle.kts`.
