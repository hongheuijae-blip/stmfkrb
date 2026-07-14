plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.hjhong.steampunkgame"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hjhong.steampunkgame"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
    }

    // 🔥 최신 Compose Compiler Extension (2026년 기준 안정)
    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    // 🔥 최신 Compose BOM (2024.10.00 안정 버전)
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Material3 최신
    implementation("androidx.compose.material3:material3")

    // Compose Foundation (gesture 포함)
    implementation("androidx.compose.foundation:foundation")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.9.0")

    // Lifecycle Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Coil 이미지 로더
    implementation("io.coil-kt:coil-compose:2.6.0")
}
