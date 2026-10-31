import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Version: raise it for every release (see README, "What the version numbers mean").
// The release tag must match it: version 1.0.0 is released with the tag v1.0.0.
val appVersion = "1.2.1"

// Signing key. Its SHA-1 is registered in the Spotify dashboard, and Android only installs
// updates signed with the same key, so every build must use this one key.
// - On GitHub: read from the repository secrets (see .github/workflows/release.yml).
// - On this PC: read from signing/keystore.properties (never uploaded).
val signingProps = Properties().apply {
    val file = rootProject.file("signing/keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: signingProps.getProperty(prop)

val keystorePath = signingValue("PIXELSPOT_KEYSTORE_PATH", "storeFile")
val hasKey = keystorePath != null && rootProject.file(keystorePath).exists()

android {
    namespace = "dev.vibe.pixelspot"
    compileSdk = 37

    defaultConfig {
        // This package name (and the SHA-1 of signing/pixelspot-debug.jks) must be
        // registered in the Spotify developer dashboard. See README.md.
        applicationId = "dev.vibe.pixelspot"
        minSdk = 29
        targetSdk = 36
        versionName = appVersion
        // Android only installs an update with a higher versionCode: 1.2.3 -> 10203.
        versionCode = appVersion.split(".").map { it.toInt() }.let { (a, b, c) -> a * 10000 + b * 100 + c }
    }

    signingConfigs {
        // Without the key (a fresh clone), Gradle's own debug key is used instead.
        if (hasKey) {
            getByName("debug") {
                storeFile = rootProject.file(keystorePath!!)
                storePassword = signingValue("PIXELSPOT_STORE_PASSWORD", "storePassword")
                keyAlias = signingValue("PIXELSPOT_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("PIXELSPOT_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Spotify App Remote SDK (official AAR from github.com/spotify/android-sdk releases).
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))
    implementation("com.google.code.gson:gson:2.15.0")

    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    // Newer than the BOM: Material 3 Expressive components (wavy progress, shape morphs).
    implementation("androidx.compose.material3:material3:1.5.0-beta02")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.browser:browser:1.10.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Used by the PixelPlayer UI pieces (album-art colour engine, smooth corners).
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.github.racra:smooth-corner-rect-android-compose:v1.0.0")
}
