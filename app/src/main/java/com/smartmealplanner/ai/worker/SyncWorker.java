package com.smartmealplanner.ai.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.GroceryItemEntity;
import com.smartmealplanner.ai.database.entity.LoggedMealEntity;
import com.smartmealplanner.ai.database.entity.UCB1StateEntity;
import com.smartmealplanner.ai.database.entity.UserProfileEntity;
import com.smartmealplanner.ai.firebase.FirebaseManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Background WorkManager Worker for Deferred Offline-First Cloud Synchronization.
 * Triggers when network connectivity is available, syncing pending Room changes to Cloud Firestore.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    public static final String WORK_NAME = "SmartMealPlanner_SyncWork";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        FirebaseManager firebaseManager = FirebaseManager.getInstance(context);

        if (!firebaseManager.isNetworkConnected(context) || !firebaseManager.isFirebaseAvailable()) {
            Log.d(TAG, "Network or Firebase unavailable. Deferred sync will retry later.");
            return Result.retry();
        }

        AppDatabase db = AppDatabase.getInstance(context);
        Log.d(TAG, "Starting deferred offline-first cloud synchronization...");

        try {
            // 1. Sync Pending Logged Meals
            List<LoggedMealEntity> pendingMeals = db.loggedMealDao().getPendingSyncMeals();
            for (LoggedMealEntity meal : pendingMeals) {
                CountDownLatch latch = new CountDownLatch(1);
                firebaseManager.syncMealToCloud(meal, new FirebaseManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                db.loggedMealDao().markAsSynced(meal.mealId);
                            } catch (Exception ignored) {}
                            latch.countDown();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.w(TAG, "Failed syncing meal " + meal.mealId + ": " + e.getMessage());
                        latch.countDown();
                    }
                });
                latch.await(3, TimeUnit.SECONDS);
            }

            // 2. Sync Pending Profile Updates
            List<UserProfileEntity> pendingProfiles = db.userDao().getPendingSyncProfiles();
            for (UserProfileEntity profile : pendingProfiles) {
                CountDownLatch latch = new CountDownLatch(1);
                firebaseManager.syncProfileToCloud(profile, new FirebaseManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                db.userDao().markAsSynced(profile.userId);
                            } catch (Exception ignored) {}
                            latch.countDown();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        latch.countDown();
                    }
                });
                latch.await(3, TimeUnit.SECONDS);
            }

            // 3. Sync Pending Grocery Items
            List<GroceryItemEntity> pendingGrocery = db.groceryDao().getPendingSyncItems();
            for (GroceryItemEntity item : pendingGrocery) {
                CountDownLatch latch = new CountDownLatch(1);
                firebaseManager.syncGroceryToCloud(item, new FirebaseManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                db.groceryDao().markAsSynced(item.itemId);
                            } catch (Exception ignored) {}
                            latch.countDown();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        latch.countDown();
                    }
                });
                latch.await(3, TimeUnit.SECONDS);
            }

            // 4. Sync Pending UCB1 Bandit States
            List<UCB1StateEntity> pendingStates = db.ucb1Dao().getPendingSyncStates();
            for (UCB1StateEntity state : pendingStates) {
                CountDownLatch latch = new CountDownLatch(1);
                firebaseManager.syncUcbStateToCloud(state, new FirebaseManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                db.ucb1Dao().markAsSynced(state.armId, state.userId);
                            } catch (Exception ignored) {}
                            latch.countDown();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        latch.countDown();
                    }
                });
                latch.await(3, TimeUnit.SECONDS);
            }

            // 5. Sync Pending Meal Plans
            List<com.smartmealplanner.ai.database.entity.MealPlanEntity> pendingPlans = db.mealPlanDao().getPendingSyncPlans();
            for (com.smartmealplanner.ai.database.entity.MealPlanEntity plan : pendingPlans) {
                CountDownLatch latch = new CountDownLatch(1);
                firebaseManager.syncMealPlanToCloud(plan, new FirebaseManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                db.mealPlanDao().markAsSynced(plan.planId);
                            } catch (Exception ignored) {}
                            latch.countDown();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        latch.countDown();
                    }
                });
                latch.await(3, TimeUnit.SECONDS);
            }

            Log.d(TAG, "Offline sync pass completed successfully.");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error during offline synchronization: " + e.getMessage(), e);
            return Result.retry();
        }
    }

    /**
     * Schedules periodic background synchronization when device is connected to network.
     */
    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
        Log.d(TAG, "Periodic WorkManager sync scheduled.");
    }
}
