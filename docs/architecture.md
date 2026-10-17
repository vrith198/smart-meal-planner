# Smart Meal Planner AI - System Architecture

## 1. Architectural Overview

**Smart Meal Planner AI** is built on a clean, decoupled, multi-tier Java architecture designed specifically for local-first execution, explainable intelligent recommendations, on-device machine learning, and resilient cloud synchronization.

```
+-----------------------------------------------------------------------------------+
|                                PRESENTATION TIER                                  |
|   Activities: SplashActivity, AuthActivity, MainActivity, MealAnalysisActivity,   |
|               NutritionDashboardActivity, MealHistoryActivity, SettingsActivity   |
|   Fragments:  DashboardFragment, ScanMealFragment, RecommendationsFragment,       |
|               MealPlannerFragment, GroceryFragment, ProfileFragment               |
|   Adapters:   LoggedMealAdapter, RecommendationAdapter, GroceryAdapter            |
+-----------------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------------+
|                        INTELLIGENT SYSTEMS (IS2) LAYER                            |
|  - NaiveBayesMealClassifier: Predicts meal slot (Breakfast, Lunch, Dinner, Snack) |
|  - FuzzyLogicEngine: Evaluates multi-criteria suitability using Mamdani inference |
|  - UCB1Recommender: Multi-Armed Bandit balancing exploitation with exploration    |
|  - MealRanker: Enforces hard dietary constraints and multi-objective ranking     |
|  - ExplainableAIEngine: Generates transparent, human-readable justification pills |
+-----------------------------------------------------------------------------------+
             |                                                  |
             v                                                  v
+-----------------------------+               +-------------------------------------+
|   ON-DEVICE ML INFERENCE    |               |          NUTRITION ENGINE           |
| - FoodClassifier (LiteRT)   |               | - NutritionCalculator               |
| - MobileNetV2 (Float16)     |               |   * Mifflin-St Jeor BMR / TDEE      |
| - 224x224x3 Input Normaliz. |               |   * Exact 100g scaling              |
| - 112 Categorical Labels    |               |   * Macro caloric distribution      |
+-----------------------------+               +-------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------------+
|                         LOCAL PERSISTENCE LAYER (ROOM)                            |
|  - AppDatabase: SQLite relational store with Room ORM                             |
|  - Entities: FoodItemEntity, LoggedMealEntity, MealPlanEntity, GroceryItemEntity, |
|              UCB1StateEntity, UserProfileEntity, SyncQueueEntity                  |
|  - DAOs: FoodItemDao, LoggedMealDao, MealPlanDao, GroceryDao, UCB1Dao, UserDao     |
|  - Pre-population Callback: Bundles verified nutrition database on database create |
+-----------------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------------+
|                        DEFERRED SYNC & CLOUD INTEGRATION                          |
|  - SyncWorker: AndroidX WorkManager task triggered on network constraints         |
|  - FirebaseManager: Singleton managing Firebase Authentication & Cloud Firestore  |
|  - Collections: users/{uid}/logged_meals, meal_plans, ucb1_state, user_profile    |
+-----------------------------------------------------------------------------------+
```

---

## 2. Core Architectural Principles

### 2.1 Pure Java & Native Android
The application is implemented 100% in Java using standard Android Jetpack libraries, Material Components 3, ViewBinding, Room ORM, and AndroidX WorkManager. It strictly avoids hybrid frameworks (Flutter, React Native), WebViews, and Kotlin to ensure raw performance, deterministic memory management, and clean object-oriented architecture.

### 2.2 Offline-First Design
Every user interaction—food identification, nutrition calculation, meal planning, recommendation ranking, and grocery consolidation—operates completely without an active internet connection:
- The Food Classifier model (`food_classifier.tflite`) runs on-device using LiteRT/TensorFlow Lite.
- The 18-item verified nutrition dataset and Naïve Bayes probabilistic parameters are bundled directly in `app/src/main/assets/`.
- Room Database persists all user meals, plans, grocery lists, and recommendation state locally.

### 2.3 Strict Safety & Dietary Boundaries
Recommendation engines often suffer from boundary violation when optimising soft scores. In Smart Meal Planner AI:
- **Hard Constraints**: Vegan, Vegetarian, Eggetarian, and Allergen restrictions are enforced as strict binary filters **before** fuzzy or bandit scoring occurs.
- If a user is Vegetarian, no Non-Vegetarian food item will ever enter the recommendation pipeline, regardless of its suitability score or exploration bonus.

---

## 3. Component Details

### 3.1 On-Device ML Pipeline
1. **Acquisition**: Image acquired via CameraX (`PreviewView` + `ImageCapture`) or Android Photo Picker (`ActivityResultContracts.GetContent`).
2. **Preprocessing**:
   - Bitmap scaled to 224x224 RGB.
   - Normalized into a 4D `ByteBuffer` (1 x 224 x 224 x 3) using standard MobileNet normalization:
     $$\text{pixel}_{\text{norm}} = \frac{\text{pixel} - 127.5}{127.5}$$
3. **Inference**: LiteRT Interpreter computes softmax probabilities across 112 classes.
4. **Resolution**: Top 3 predictions are surfaced. If the top prediction exceeds the confidence threshold (e.g., > 0.40), it automatically maps to a verified nutrition record; otherwise, the user is presented with alternative matches.

### 3.2 Intelligent Systems (IS2) Decision Pipeline
1. **Contextual Meal Slot**: `NaiveBayesMealClassifier` calculates posterior probabilities:
   $$P(\text{Slot} \mid \text{Diet}, \text{Goal}, \text{Hour}) \propto P(\text{Slot}) \prod P(\text{Feature}_i \mid \text{Slot})$$
   with Laplace smoothing to guarantee zero-frequency resilience.
2. **Multi-Objective Suitability**: `FuzzyLogicEngine` applies Mamdani min-max inference across Goal Caloric Delta, Macro Balance, and Budget Constraints using triangular and trapezoidal membership functions with open shoulder bounds.
3. **Contextual Adaptation (UCB1)**:
   $$\text{Score}_{\text{final}} = (1 - w_{\text{bandit}}) \cdot \text{Suitability} + w_{\text{bandit}} \cdot \left( \hat{\mu}_i + \sqrt{\frac{2 \ln N}{n_i}} \right) - \text{Penalty}_{\text{variety}}$$
4. **Transparent XAI**: `ExplainableAIEngine` generates specific reason pills (e.g., "Matches high-protein fitness goal (+18.3g)", "Fits moderate budget (₹80)", "Exploration recommendation").

### 3.3 Deferred Cloud Synchronization Pipeline
- When offline, modifications are written to SQLite via Room and enqueued into `sync_queue`.
- `SyncWorker` is registered with WorkManager requiring `NetworkType.CONNECTED`.
- Upon network restoration, `SyncWorker` reads pending mutations, uploads to Cloud Firestore under `users/{userId}/...`, and flushes successfully synced records.
