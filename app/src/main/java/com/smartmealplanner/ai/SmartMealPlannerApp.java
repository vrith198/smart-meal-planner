package com.smartmealplanner.ai;

import android.app.Application;
import android.util.Log;

import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.worker.SyncWorker;

/**
 * Main Application class for Smart Meal Planner AI.
 * Initializes Room Database, Firebase Services, and WorkManager deferred sync.
 */
public class SmartMealPlannerApp extends Application {

    private static final String TAG = "SmartMealPlannerApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Initializing Smart Meal Planner AI application...");

        // 1. Initialize local SQLite/Room database & pre-populate verified foods in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.prePopulateDatabase(getApplicationContext());
        });

        // 2. Initialize Firebase Architecture
        FirebaseManager.getInstance(this);

        // 3. Schedule WorkManager Periodic Synchronization
        SyncWorker.schedulePeriodicSync(this);

        Log.i(TAG, "Initialization complete. Offline-first architecture active.");
    }
}
