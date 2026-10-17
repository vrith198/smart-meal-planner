# Smart Meal Planner AI

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Pure%20Java-orange.svg)](https://www.java.com/)
[![UI](https://img.shields.io/badge/UI-Material%20Components%203-blue.svg)](https://m3.material.io/)
[![On-Device ML](https://img.shields.io/badge/ML-LiteRT%20%2F%20TensorFlow%20Lite-ff69b4.svg)](https://www.tensorflow.org/lite)
[![Database](https://img.shields.io/badge/Local%20DB-Room%20%2F%20SQLite-brightgreen.svg)](https://developer.android.com/training/data-storage/room)
[![Cloud](https://img.shields.io/badge/Cloud-Firebase%20Firestore-yellow.svg)](https://firebase.google.com/)
[![Unit Tests](https://img.shields.io/badge/Unit%20Tests-27%20Passed%20(100%25)-success.svg)](docs/testing.md)

**Smart Meal Planner AI** is a complete, production-ready native Android application built in pure Java. It integrates on-device computer vision, clinical nutrition science (IFCT 2017/USDA), and advanced Intelligent Systems (IS2)—including Naïve Bayes classification, Mamdani Fuzzy Logic, and UCB1 Multi-Armed Bandit adaptation—to provide personalized, explainable, and offline-capable meal planning and nutritional tracking.

---

## 1. Key Features

### 📸 On-Device Food Recognition (LiteRT / TensorFlow Lite)
- Identifies supported meals directly on the device using a quantized **MobileNetV2** deep neural network (Float16 quantized, 4.99 MB).
- Runs 100% locally with zero latency (~42ms inference time) without requiring an internet connection or external server.
- Supports CameraX live image acquisition and system photo gallery picker.
- Surfaces Top-3 confidence candidates with user confirmation.

### 🥗 Clinically Verified Nutrition Engine
- Pre-populated with 18 curated Indian staple meals based on the **Indian Food Composition Tables (IFCT 2017)** and **USDA FoodData Central**.
- Dynamic volumetric portion scaling: Small, Medium, Large, or custom gram weights.
- Calculates Mifflin-St Jeor Basal Metabolic Rate (BMR) and Total Daily Energy Expenditure (TDEE).
- Visual progress rings for daily calories and macro targets (Protein, Carbs, Fat).

### 🧠 Intelligent Systems (IS2) Recommendation Layer
1. **Contextual Meal Slot Classifier (Naïve Bayes)**: Determines meal slot (Breakfast, Lunch, Dinner, Snack) based on time of day, fitness goal, and user profile with Laplace smoothing.
2. **Multi-Objective Suitability (Mamdani Fuzzy Logic)**: Evaluates caloric delta, protein suitability, and budget constraints via continuous triangular and trapezoidal membership functions.
3. **Adaptive Personalization (UCB1 Multi-Armed Bandit)**: Balances exploitation of favorite dishes with exploration of untried culinary options, preventing recommendation fatigue.
4. **Hard Dietary & Allergen Filtering**: Strict boolean constraint enforcement ensures Vegetarian, Vegan, and Allergen (e.g., Gluten) boundaries are 100% respected.
5. **Explainable AI (XAI)**: Generates clear, human-readable justification pills for every recommendation.

### 📅 Meal Planning & Smart Grocery Consolidation
- Generates 7-day nutritional meal plans matching caloric and macronutrient targets.
- Automatically consolidates ingredients across planned meals into an actionable, categorized grocery checklist.
- Calculates total estimated weekly grocery expenditure in Indian Rupees (₹).

### 🔄 Offline-First Architecture & Deferred Cloud Sync
- Operates fully offline using a local **Room / SQLite** database.
- Deferred background synchronization via **AndroidX WorkManager** (`SyncWorker`) with exponential backoff.
- Synchronizes with **Firebase Cloud Firestore** under user-isolated collections (`users/{uid}/...`).

---

## 2. Architecture & Technology Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Operating System** | Android | Min SDK 24 (Android 7.0), Target SDK 34 (Android 14) |
| **Language** | Java 17 | Pure Java application logic (strictly no Kotlin or hybrid webviews) |
| **UI Framework** | Material 3 XML | Material Design 3 with custom Deep Green Nutrition identity |
| **Architecture Pattern** | Decoupled MVC / MVVM | Modular separation of UI, Domain IS2, and Persistence |
| **Local Database** | Room ORM | SQLite relational persistence with pre-population callbacks |
| **On-Device Inference** | LiteRT (TFLite 2.16.1) | MobileNetV2 Float16 model running on CPU/NNAPI |
| **Cloud Services** | Firebase | Firebase Authentication & Cloud Firestore (optional/offline-first) |
| **Background Work** | WorkManager | Guaranteed network-constrained sync |

---

## 3. Project Directory Structure

```
smart-meal-planner/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/              # TFLite model, labels.txt, nutrition & NB JSON
│   │   │   ├── java/com/smartmealplanner/ai/
│   │   │   │   ├── data/            # Room Database, DAOs, Entities, Pre-population
│   │   │   │   ├── is2/             # FuzzyLogicEngine, UCB1, NaiveBayes, MealRanker, XAI
│   │   │   │   ├── ml/              # LiteRT FoodClassifier runtime
│   │   │   │   ├── model/           # POJOs, Enums, NutritionSummary, PredictionResult
│   │   │   │   ├── nutrition/       # NutritionCalculator (BMR, TDEE, exact 100g scaling)
│   │   │   │   ├── sync/            # WorkManager SyncWorker, FirebaseManager
│   │   │   │   └── ui/              # Activities, Fragments, Adapters, ViewHolders
│   │   │   └── res/                 # Material 3 XML layouts, drawables, styles, colors
│   │   └── test/                    # 27 automated unit tests (JUnit 4)
│   ├── build.gradle.kts             # AGP 9.2.1 build configuration
│   └── google-services.json         # Firebase client configuration
├── docs/                            # Comprehensive documentation suite
│   ├── architecture.md              # System design and component diagrams
│   ├── datasets.md                  # Dataset provenance (Food-101, IFCT, USDA, NB)
│   ├── ml-evaluation.md             # Quantitative ML & IS2 metrics and benchmarks
│   ├── firebase-setup.md            # Step-by-step Firebase & Firestore setup
│   ├── deployment.md                # APK installation and release signing guide
│   ├── testing.md                   # Unit test catalog and execution report
│   └── viva-preparation.md         # Comprehensive viva voce and technical defense Q&A
├── ml/                              # Python ML training pipeline
│   ├── data/                        # Curated Indian nutrition & meal preferences CSVs
│   ├── models/                      # Exported TFLite models, metadata, labels
│   ├── notebooks/                   # Jupyter training notebooks
│   ├── reports/                     # Model evaluation reports
│   └── src/                         # Python scripts for training, export, quantization
└── build.gradle.kts                 # Root project Gradle configuration
```

---

## 4. Quick Start & Installation

### Option A: Install the Pre-Built Debug APK
The pre-compiled debug APK is available directly in the build directory:
```powershell
# Sideload onto connected Android device or emulator via ADB:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Option B: Build and Run from Android Studio
1. Open Android Studio.
2. Select **Open** and choose the `smart-meal-planner` directory.
3. Allow Gradle to synchronize dependencies.
4. Select a connected device or AVD emulator.
5. Click **Run** (`Shift + F10`).

### Option C: Build from Terminal
```powershell
# Set Java 17 path
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.1"

# Run the 27 unit tests
.\gradlew.bat testDebugUnitTest

# Assemble debug APK
.\gradlew.bat assembleDebug
```

---

## 5. GitHub Repository Setup

To link this project to your personal GitHub repository:
```bash
git remote add origin https://github.com/<your-username>/smart-meal-planner-ai.git
git branch -M main
git push -u origin main
```

---

## 6. Automated Unit Test Verification

The application features 27 automated unit tests covering all mathematical, algorithmic, and safety requirements:
- **Nutrition Calculations**: Mifflin-St Jeor BMR, TDEE, macro caloric distribution, 100g base scaling.
- **Mamdani Fuzzy Logic**: Triangular/trapezoidal membership functions, shoulder bounds, suitability scoring $[0, 100]$.
- **UCB1 Multi-Armed Bandit**: Exploration vs. exploitation tradeoff, cold-start arm priority, feedback rewards.
- **Safety & Dietary Constraints**: 100% adherence to Vegetarian, Vegan, and Allergen restrictions.
- **Naïve Bayes Classifier**: Laplace-smoothed posterior log-probabilities, zero-frequency resilience.

Result: **27 passed, 0 failed (100% pass rate)**. See [docs/testing.md](docs/testing.md) for full report.

---

## 7. Educational & Wellness Disclaimer

> [!IMPORTANT]
> **Smart Meal Planner AI is an educational wellness and nutrition tracking tool, not a certified medical device or clinical diagnostic system.**
> Nutritional values and recommendations are calculated using standardized reference tables (IFCT 2017 and USDA FoodData Central). Actual nutritional content varies based on specific ingredients, preparation methods, and portion sizes. Individuals with specific medical conditions (such as diabetes, hypertension, celiac disease, or food allergies) should consult a certified physician or registered dietitian before making significant dietary changes.

---

## 8. License

This project is developed for educational, academic, and research purposes.
Licensed under the Apache License, Version 2.0.
