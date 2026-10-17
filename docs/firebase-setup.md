# Smart Meal Planner AI - Firebase Cloud Integration Guide

## 1. Cloud Architecture Overview

Smart Meal Planner AI implements an **Offline-First Synchronized Architecture**. The application is 100% operational in offline/guest mode without Firebase. When cloud synchronization is enabled, user data is securely synchronized with Firebase Cloud Firestore under isolated user document paths.

---

## 2. Step-by-Step Setup Instructions

### Step 1: Create a Firebase Project
1. Navigate to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add Project** and name it `Smart-Meal-Planner-AI`.
3. Disable Google Analytics (optional) and click **Create Project**.

### Step 2: Register the Android Application
1. In project overview, click the Android icon to add an Android app.
2. Enter the exact application package name:
   ```
   com.smartmealplanner.ai
   ```
3. Enter App Nickname: `Smart Meal Planner AI`.
4. (Optional) Provide Debug SHA-1 fingerprint for Google Sign-In or App Check:
   To generate your local debug SHA-1:
   ```powershell
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Click **Register App**.

### Step 3: Download & Install `google-services.json`
1. Download the generated `google-services.json` configuration file.
2. Copy it into the app directory, replacing the existing template:
   ```
   app/google-services.json
   ```
   > [!NOTE]
   > The application currently includes a valid placeholder `app/google-services.json` so the app builds and runs without compilation errors even before you connect your own Firebase project.

### Step 4: Enable Firebase Authentication
1. In the Firebase Console left navigation, select **Build > Authentication**.
2. Click **Get Started**.
3. Under the **Sign-in method** tab:
   - Enable **Email/Password**.
   - Enable **Anonymous** (allows users to explore as Guest without creating an account immediately).

### Step 5: Enable Cloud Firestore
1. Under **Build > Firestore Database**, click **Create Database**.
2. Select database location (e.g., `asia-south1` for India / Mumbai or closest region).
3. Start in **Production mode**.

### Step 6: Deploy Security Rules
Deploy the verified production security rules located in `firebase/firestore.rules`:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Restrict access so each user can only read and write their own documents
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

To deploy via Firebase CLI:
```bash
npm install -g firebase-tools
firebase login
firebase init firestore
firebase deploy --only firestore:rules
```

---

## 3. Data Schema in Firestore

All user data is partitioned under `users/{userId}`:

```
users/
  └── {userId}/
        ├── user_profile (document)
        │     ├── dailyCalorieGoal: 2200
        │     ├── dietaryType: "VEGETARIAN"
        │     ├── fitnessGoal: "MUSCLE_GAIN"
        │     └── budgetTier: "MEDIUM"
        │
        ├── logged_meals (subcollection)
        │     └── {mealId}
        │           ├── foodName: "Yellow Moong Dal Tadka"
        │           ├── calories: 236.0
        │           ├── protein: 14.6
        │           ├── carbs: 35.8
        │           ├── fat: 5.4
        │           ├── servingName: "Medium Bowl"
        │           ├── mealSlot: "LUNCH"
        │           └── timestamp: 1729209600000
        │
        ├── meal_plans (subcollection)
        │     └── {planId}
        │           ├── dayOfWeek: "MONDAY"
        │           ├── mealCategory: "BREAKFAST"
        │           ├── foodName: "Idli"
        │           └── targetCalories: 156.0
        │
        └── ucb1_state (subcollection)
              └── {foodId}
                    ├── pulls: 12
                    ├── totalReward: 10.4
                    └── lastUpdated: 1729209600000
```

---

## 4. WorkManager Deferred Offline Synchronization
When the user logs meals while offline:
1. Records are committed immediately to the local SQLite database via Room.
2. A mutation record is enqueued in the Room `sync_queue` table with status `PENDING`.
3. `SyncWorker` is registered with AndroidX `WorkManager`:
   - Constraints: `NetworkType.CONNECTED`
   - Backoff: Exponential (initial 30 seconds)
4. When connectivity returns, `SyncWorker` dequeues mutations in batches, synchronizes them with Firestore, and marks local records as `SYNCED`.
