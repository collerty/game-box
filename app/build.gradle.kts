import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)   // NEW
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.gamehub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gamehub"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Read local.properties
        val localPropertiesFile = rootProject.file("local.properties")
        val localProperties = Properties()
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        // Define the manifest placeholder
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "YOUR_DEFAULT_KEY_IF_NOT_FOUND")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose-android:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.8.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.runtime.android)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation ("com.google.firebase:firebase-auth-ktx:21.1.0")
    implementation ("com.google.firebase:firebase-firestore-ktx:24.1.0")
    implementation ("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-svg:2.4.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.maps.android:maps-compose:2.11.4")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:2.11.4") // Check for the latest version
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Core Maps SDK
    implementation("io.agora.rtc:full-sdk:4.2.6")
    implementation("com.airbnb.android:lottie-compose:6.0.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")
    implementation ("androidx.core:core-ktx:1.13.1")
    implementation ("androidx.compose.material3:material3:1.3.2")

    implementation("androidx.compose.material:material-icons-extended") // For Icons.Default.Mic and MicOff
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.coil-kt:coil-gif:2.4.0")
}