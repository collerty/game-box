pluginManagement {
    repositories {
        google(); mavenCentral(); gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.10.1" apply false
        id("com.android.library")     version "8.10.1" apply false
        id("org.jetbrains.kotlin.android") version "2.1.0" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false // NEW
        id("com.google.gms.google-services") version "4.4.0"       apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GameHub"
include(":app")
