plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    id("kotlin-kapt") // Keep kapt only for Hilt
}

android {
    namespace = "com.commerin.telemetri"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.commerin.telemetri"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

    buildFeatures {
        compose = true
    }


    hilt {
        enableAggregatingTask = false
    }
}

dependencies {
    implementation(project(":telemetri-sdk"))

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose BOM and dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation and ViewModel for Compose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room database dependencies
    implementation(libs.androidx.room.runtime.app)
    implementation(libs.androidx.room.ktx.app)
    ksp(libs.androidx.room.compiler.app) // Changed from kapt to ksp

    // Hilt for Dependency Injection
    implementation(libs.hilt.android.app)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work) // Match SDK version
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.room.ktx)

    kapt(libs.hilt.compiler.app)
    kapt(libs.androidx.hilt.compiler) // Match SDK version

    // Explicit kotlinx-metadata-jvm dependency to fix version compatibility
//    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.8.0")

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Permission handling
    implementation(libs.accompanist.permissions)

    // Additional UI components
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)

    // Coroutines for async operations
    implementation(libs.kotlinx.coroutines.android)

    // Location services
    implementation(libs.play.services.location.app)

    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)

    // Testing dependencies
    testImplementation(libs.junit.test)
    androidTestImplementation(libs.androidx.junit.test)
    androidTestImplementation(libs.androidx.espresso.core.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}