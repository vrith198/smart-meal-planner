import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val geminiApiKey: String = localProperties.getProperty("gemini.api.key")
    ?: localProperties.getProperty("GEMINI_API_KEY")
    ?: ""

android {
    namespace = "com.smartmealplanner.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartmealplanner.ai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose safe, local untracked Gemini API key via BuildConfig if present in local.properties
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
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

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    // AndroidX & Material UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.activity)

    // Architecture Components (Room, Lifecycle, WorkManager)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.work.runtime)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // On-device Machine Learning (TensorFlow Lite / LiteRT)
    implementation(libs.tensorflow.lite)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Unit & Instrumentation Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
