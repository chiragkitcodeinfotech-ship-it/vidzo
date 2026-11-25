import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Google's official AdMob TEST ids — safe to click during development.
// https://developers.google.com/admob/android/test-ads
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAppOpenId = "ca-app-pub-3940256099942544/9257395921"
val testNativeId = "ca-app-pub-3940256099942544/1044960115"
val testRewardedId = "ca-app-pub-3940256099942544/5224354917"

/** Reads a release AdMob id from gradle.properties, falling back to the test id when empty. */
fun admobId(key: String, fallback: String): String =
    providers.gradleProperty(key).orNull?.takeIf { it.isNotBlank() } ?: fallback

/**
 * Release upload keystore, read from local.properties (git-ignored — never commit real passwords
 * to gradle.properties, which is tracked). Falls back to debug signing when absent, so CI/other
 * machines without the real keystore still build (Play Store release builds require the real one).
 */
val releaseSigningProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val releaseStoreFile = releaseSigningProps.getProperty("vidzo.release.storeFile")
val hasReleaseSigning = !releaseStoreFile.isNullOrBlank() && rootProject.file(releaseStoreFile).exists()

/**
 * Campaign code baked into an APK you share from your own server (sideloaded installs have no
 * Play install referrer). Build: ./gradlew assembleDebug -Pvidzo.linkCampaign=offer1
 * Leave it unset for Play Store builds, otherwise every Play user gets the campaign shortcut.
 */
fun linkCampaign(): String {
    val code = providers.gradleProperty("vidzo.linkCampaign").orNull?.trim()?.lowercase().orEmpty()
    require(code.isEmpty() || code.matches(Regex("[a-z0-9_-]{1,32}"))) {
        "vidzo.linkCampaign must be a short code like offer1 (letters, digits, - or _)"
    }
    return code
}

android {
    namespace = "com.walyo.browser"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vidzo.browser"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
        buildConfigField("String", "LINK_CAMPAIGN", "\"${linkCampaign()}\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseSigningProps.getProperty("vidzo.release.storePassword")
                keyAlias = releaseSigningProps.getProperty("vidzo.release.keyAlias")
                keyPassword = releaseSigningProps.getProperty("vidzo.release.keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("String", "AD_APP_OPEN", "\"$testAppOpenId\"")
            buildConfigField("String", "AD_NATIVE", "\"$testNativeId\"")
            buildConfigField("String", "AD_REWARDED", "\"$testRewardedId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["admobAppId"] = admobId("walyo.admob.appId", testAdmobAppId)
            buildConfigField("String", "AD_APP_OPEN", "\"${admobId("walyo.admob.appOpen", testAppOpenId)}\"")
            buildConfigField("String", "AD_NATIVE", "\"${admobId("walyo.admob.native", testNativeId)}\"")
            buildConfigField("String", "AD_REWARDED", "\"${admobId("walyo.admob.rewarded", testRewardedId)}\"")
            signingConfig = signingConfigs.getByName(if (hasReleaseSigning) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.play.services.ads)
    // AdMob depends on an old Fragment version; this pins one compatible with registerForActivityResult.
    implementation(libs.androidx.fragment)
    implementation(libs.install.referrer)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
