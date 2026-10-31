plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.shivamingale3.mytube.skin.dot"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Screenshot previews are written only when asked: ./gradlew :skins:skin-dot:testDebugUnitTest -Ppreview
            it.systemProperty("roborazzi.test.record", project.hasProperty("preview").toString())
            it.systemProperty("mytube.preview", project.hasProperty("preview").toString())
            it.systemProperty("mytube.previewImages", project.findProperty("previewImages")?.toString() ?: "")
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
    api(project(":skins:skin-api"))
    implementation(libs.tv.material)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.coil.test)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.tv.material)
    debugImplementation(libs.compose.ui.test.manifest)
}
