plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}
android {
    namespace  = "com.health.heartrate.wear"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.health.heartrate.wear"
        minSdk        = 30
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}
dependencies {
    implementation(project(":heartrate_shared"))

    // Room
    val roomVer = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVer")
    implementation("androidx.room:room-ktx:$roomVer")
    ksp("androidx.room:room-compiler:$roomVer")

    // Coroutines + Guava (required for Tile ListenableFuture)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")

    // Wearable
    implementation("com.google.android.gms:play-services-wearable:18.2.0")

    // Wear OS UI
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")

    // Wear Tiles
    implementation("androidx.wear.tiles:tiles:1.3.0")
    implementation("androidx.wear.tiles:tiles-tooling-preview:1.3.0")
    implementation("androidx.wear.protolayout:protolayout:1.2.0")
    implementation("androidx.wear.protolayout:protolayout-expression:1.2.0")

    // Core
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
