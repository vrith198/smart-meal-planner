# Smart Meal Planner AI - Deployment & Installation Guide

## 1. Prerequisites & Environment

| Requirement | Supported Version |
| :--- | :--- |
| **Operating System** | Windows 10/11, macOS, or Linux |
| **Java Development Kit** | JDK 17 (verified with OpenJDK 17.0.1) |
| **Android Studio** | Android Studio Hedgehog / Iguana / Jellyfish / Ladybug or newer |
| **Android SDK Tools** | Android SDK Build-Tools 34.0.0+ |
| **Target Android API** | Minimum API 24 (Android 7.0 Nougat), Target API 34 (Android 14) |
| **Hardware Architecture** | ARM64-v8a, ARMeabi-v7a, x86_64 (emulator & physical devices) |

---

## 2. Quick Start: Installing the Pre-Built Debug APK

The debug APK has already been assembled and verified:
- **File location**: `app/build/outputs/apk/debug/app-debug.apk`
- **Binary size**: ~30.7 MB

### 2.1 Installing via Android Debug Bridge (ADB)
Connect your Android phone via USB with **USB Debugging** enabled, or launch an Android Virtual Device (AVD):

```powershell
# Check connected devices
adb devices

# Install APK onto device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2.2 Installing Directly on Phone (Sideloading)
1. Transfer `app-debug.apk` to your phone via USB cable, Google Drive, or local file sharing.
2. Open the file on your device and tap **Install**.
3. If prompted, enable *Allow from this source* for your file manager.

---

## 3. Building from Source

### 3.1 Command Line (Gradle)
```powershell
# Set JDK 17 environment
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.1"

# Run all unit tests
.\gradlew.bat testDebugUnitTest

# Assemble debug APK
.\gradlew.bat assembleDebug

# Assemble release APK (unsigned)
.\gradlew.bat assembleRelease
```

### 3.2 Opening in Android Studio
1. Open Android Studio.
2. Select **Open** and browse to the project root:
   `C:\Users\INDIA\.gemini\antigravity\scratch\smart-meal-planner`
3. Wait for Gradle Sync to complete.
4. Select the `app` run configuration and click the green **Run** (Play) button or press `Shift + F10`.

---

## 4. Production Release & Signing (AAB / APK)

To build a production signed release for Google Play Store:

### 4.1 Generate a Production Keystore
```powershell
keytool -genkeypair -v -keystore release-key.jks -alias mealplanner -keyalg RSA -keysize 2048 -validity 10000
```

### 4.2 Configure Signing in `app/build.gradle.kts`
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "your_password"
            keyAlias = "mealplanner"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "your_password"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 4.3 Build Release Bundle
```powershell
.\gradlew.bat bundleRelease
```
The output `.aab` file will be generated in `app/build/outputs/bundle/release/`.

---

## 5. Required Runtime Permissions
The application requests the following permissions at runtime when relevant:
- `android.permission.CAMERA`: Used by CameraX to capture meal photos on-demand.
- `android.permission.READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE`: Used by Android Photo Picker to select existing meal images from the gallery.
- `android.permission.INTERNET`: Used by Firebase Auth and Cloud Firestore to synchronize data when connectivity is present.
- `android.permission.ACCESS_NETWORK_STATE`: Used by WorkManager to defer synchronization until an unmetered or connected network is available.
