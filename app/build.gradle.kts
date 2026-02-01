import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.filenest.photo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.filenest.photo"
        minSdk = 33
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        jvmToolchain(11)
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
    implementation(composeBom)

    // Material Design 3
    implementation("androidx.compose.material3:material3")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Constraintlayout
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")

    // Material Design 图标库
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // window size utils 适配不同屏幕尺寸的工具类
    implementation("androidx.compose.material3.adaptive:adaptive")

    // 在 Compose 中使用 ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")

    // Navigation
    val navigationVersion = "2.9.3"
    implementation("androidx.navigation:navigation-compose:$navigationVersion")
    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVersion")

    // DataStore
    val datastoreVersion = "1.1.7"
    implementation("androidx.datastore:datastore:$datastoreVersion")
    implementation("androidx.datastore:datastore-preferences:$datastoreVersion")
    implementation("androidx.datastore:datastore-preferences-core:$datastoreVersion")

    // Room 数据库
    val roomVersion = "2.7.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // JSON serialization library, works with the Kotlin serialization plugin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:5.1.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57")
    ksp("com.google.dagger:hilt-android-compiler:2.57")
    ksp("com.google.dagger:hilt-compiler:2.57")

    // Hilt For Compose
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coil 2.x 版本（适用于现代 Compose 项目）
    implementation("io.coil-kt:coil-compose:2.6.0")

}
