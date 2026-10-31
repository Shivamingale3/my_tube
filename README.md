# MyTube

An ad-free YouTube client for Android TV, with two switchable skins (Dot and Tonal). Personal project.

## Install on a TV

1. Download the latest `mytube-debug-*` artifact from the **Actions** tab and unzip it.
2. With wireless debugging enabled on the TV:

```bash
adb connect <TV_IP>:<PORT>
adb install -r app-debug.apk
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
