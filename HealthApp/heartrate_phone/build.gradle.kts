plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}
android {
    namespace  = "com.health.heartrate.phone"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.health.heartrate.phone"
        minSdk        = 26
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

    // ViewModel / LiveData / Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Wearable Data Layer
    implementation("com.google.android.gms:play-services-wearable:18.2.0")

    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    // MPAndroidChart — real-time + history charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // UI
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
}
