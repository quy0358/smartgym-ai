import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}
val debugDeepSeekApiKey = localProperties.getProperty("DEEPSEEK_API_KEY", "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val debugDeepSeekBaseUrl = localProperties.getProperty("DEEPSEEK_BASE_URL", "https://api.deepseek.com/chat/completions")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val debugDeepSeekModel = localProperties.getProperty("DEEPSEEK_MODEL", "deepseek-v4-flash")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "ntu.quy65132908.smartgym_ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "ntu.quy65132908.smartgym_ai"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        debug {
            // Development-only key from local.properties. Release builds must not ship a raw AI key.
            buildConfigField("String", "DEEPSEEK_API_KEY", "\"$debugDeepSeekApiKey\"")
            buildConfigField("String", "DEEPSEEK_BASE_URL", "\"$debugDeepSeekBaseUrl\"")
            buildConfigField("String", "DEEPSEEK_MODEL", "\"$debugDeepSeekModel\"")
        }
        release {
            buildConfigField("String", "DEEPSEEK_API_KEY", "\"\"")
            buildConfigField("String", "DEEPSEEK_BASE_URL", "\"https://api.deepseek.com/chat/completions\"")
            buildConfigField("String", "DEEPSEEK_MODEL", "\"deepseek-v4-flash\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.swiperefreshlayout)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.google.id)

    // Image Loading
    implementation(libs.glide)
    ksp(libs.glide.compiler)

    // Chart
    implementation(libs.mpandroidchart)

    // Camera + pose detection
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.pose.detection)
    implementation(libs.work.runtime)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.mockito.core)
    testImplementation(libs.core.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
