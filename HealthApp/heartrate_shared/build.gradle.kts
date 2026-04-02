plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace  = "com.health.heartrate.shared"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
