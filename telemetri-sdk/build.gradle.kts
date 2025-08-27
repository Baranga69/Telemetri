plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.commerin.telemetri.sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    hilt {
        enableAggregatingTask = false
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Lifecycle and Architecture Components
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.common.java8)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Room Database
    implementation(libs.androidx.room.runtime.app)
    implementation(libs.androidx.room.ktx.app)
    implementation(libs.androidx.navigation.common.ktx)
    kapt(libs.androidx.room.compiler.app)

    // Location Services
    implementation(libs.play.services.location)

    // JavaPoet dependency to fix Hilt compilation issues
    implementation(libs.javapoet)

    // Dependency Injection (Hilt) - updated to version 2.51 for compatibility
    implementation(libs.hilt.android.sdk)
    kapt(libs.hilt.android.compiler.sdk)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    // Explicit kotlinx-metadata-jvm dependency to fix version compatibility
//    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.8.0")

    // JSON Processing
    implementation(libs.gson)

    // Network (for future API integration)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
}