import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ai.dadacast.app"
    compileSdk = 35
    // CarUxRestrictionsManager lives in the platform's optional android.car library.
    useLibrary("android.car")

    defaultConfig {
        applicationId = "ai.dadacast.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    // Release signing: read from a keystore.properties kept OUTSIDE the repo
    // (default ../../_keystore/keystore.properties, override with -PdadacastKeystoreProps=<path>).
    // If it is missing, release falls back to the debug signing config so the public repo still builds.
    val keystorePropsFile = rootProject.file(
        (project.findProperty("dadacastKeystoreProps") as String?) ?: "../_keystore/keystore.properties"
    )
    val hasReleaseKeystore = keystorePropsFile.exists()

    signingConfigs {
        if (hasReleaseKeystore) {
            val props = Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
            create("release") {
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Keep R8 off: shrinking/obfuscation can break the Pleos SDKs.
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navi.helper)
    implementation(libs.pleos.tts)
    implementation(libs.pleos.stt)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
