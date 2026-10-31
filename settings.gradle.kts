pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // The SmartTube modules declare their own repositories in places, so project repos stay allowed.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "MyTube"

include(":app")
include(":core:model")
include(":core:youtube-bridge")
include(":core:player")

// YouTube data layer from SmartTube (MIT, git submodules, never edited here).
// Only :core:youtube-bridge may depend on these.
val external = file("external")
mapOf(
    ":sharedutils" to "SharedModules/sharedutils",
    ":sharedtests" to "SharedModules/sharedtests",
    ":commons-io-2.8.0" to "SharedModules/commons-io-2.8.0",
    ":j2v8" to "SharedModules/j2v8",
    ":mediaserviceinterfaces" to "MediaServiceCore/mediaserviceinterfaces",
    ":youtubeapi" to "MediaServiceCore/youtubeapi",
).forEach { (path, dir) ->
    include(path)
    project(path).projectDir = File(external, dir)
}
