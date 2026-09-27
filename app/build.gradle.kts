import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.gms.google.services)
}

// Base URL of the Node/Express + Claude backend (see backend/README.md).
// Resolution order: -PBACKEND_BASE_URL / gradle.properties, then local.properties,
// then the BACKEND_BASE_URL environment variable, then the Android-emulator default
// (10.0.2.2 is the emulator's alias for the host machine's localhost).
val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
val backendBaseUrl: String =
    (project.findProperty("BACKEND_BASE_URL") as String?)
        ?: localProperties.getProperty("BACKEND_BASE_URL")
        ?: System.getenv("BACKEND_BASE_URL")
        ?: "http://10.0.2.2:3000/"
require(backendBaseUrl.startsWith("http://") || backendBaseUrl.startsWith("https://")) {
    "BACKEND_BASE_URL must be an http(s) URL, got '$backendBaseUrl'"
}

android {
    namespace = "com.example.cafeshopassignment"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.cafeshopassignment"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    testOptions {
        // Android framework calls (e.g. android.util.Log) return defaults in JVM unit tests.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    implementation(libs.glide)
    kapt(libs.glide.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
