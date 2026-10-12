plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.safecomm.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.safecomm.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("vartaSigning") {
            storeFile = file("varta-radio.jks")
            storePassword = "vartaradio123"
            keyAlias = "vartaradio"
            keyPassword = "vartaradio123"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("vartaSigning")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("vartaSigning")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    // Android Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Networking & Real-time PubSub
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.paho.mqtt)

    // Google Services & Auth
    implementation(libs.play.services.auth)

    // Image Loading
    implementation(libs.coil.compose)

    // Map
    implementation(libs.osmdroid)

    testImplementation(libs.junit)
}
