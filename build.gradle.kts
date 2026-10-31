import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Compatibility shims for the SmartTube modules under external/. They target an older
// toolchain (AGP 7, Kotlin 1.8); everything needed to build them with ours lives here
// so the submodules themselves stay untouched and can be bumped freely.
val externalNamespaces = mapOf(
    "sharedutils" to "com.liskovsoft.sharedutils",
    "sharedtests" to "com.liskovsoft.sharedtests",
    "commons-io-2.8.0" to "org.apache.commons.commonsio",
    "j2v8" to "com.eclipsesource.v8",
    "mediaserviceinterfaces" to "com.liskovsoft.mediaserviceinterfaces",
    "youtubeapi" to "com.liskovsoft.youtubeapi",
)

subprojects {
    val externalNamespace = externalNamespaces[name]
    if (externalNamespace != null) {
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension>("android") {
                namespace = externalNamespace
                buildFeatures.buildConfig = true
            }
        }
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    configurations.configureEach {
        resolutionStrategy {
            // youtubeapi asks for kotlinx-coroutines-android at the Kotlin version, which does not exist.
            force(libs.kotlinx.coroutines.android.get().toString())
            // sharedutils is written against OkHttp 3; SmartTube pins this same version.
            force("com.squareup.okhttp3:okhttp:3.12.13")
        }
    }
}
