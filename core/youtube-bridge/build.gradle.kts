plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.shivamingale3.mytube.youtube"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        // youtubeapi ships beta/stable/fdroid flavours; we always use stable.
        missingDimensionStrategy("default", "ststable")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Live probes hit YouTube; run them only with -PliveProbe.
            it.systemProperty("mytube.liveProbe", project.hasProperty("liveProbe").toString())
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":youtubeapi"))
    implementation(project(":mediaserviceinterfaces"))
    implementation(project(":sharedutils"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.rx2)
    implementation(libs.rxjava2)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
