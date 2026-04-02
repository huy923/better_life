pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        maven { url = uri("https://jitpack.io") }  // MPAndroidChart
    }
}
rootProject.name = "HealthApp"
include(":app", ":heartrate_shared", ":heartrate_phone", ":heartrate_wear")
