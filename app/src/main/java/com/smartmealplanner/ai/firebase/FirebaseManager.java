package com.smartmealplanner.ai.firebase;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates Firebase Authentication and Cloud Firestore synchronization.
 * Operates offline-first: Room is the single source of truth; when online, changes sync seamlessly.
 */
public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    public static final String GUEST_USER_ID = "guest_user";
    private static volatile FirebaseManager INSTANCE;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private boolean isFirebaseAvailable = false;

    public interface AuthCallback {
        void onSuccess(String userId, String email);
        void onError(String errorMessage);
    }

    public interface SyncCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    private FirebaseManager(Context context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
            isFirebaseAvailable = true;
            Log.d(TAG, "Firebase initialized successfully.");
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization warning: " + e.getMessage() + ". Running in offline-first Room mode.");
            isFirebaseAvailable = false;
        }
    }

    public static FirebaseManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FirebaseManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FirebaseManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public boolean isFirebaseAvailable() {
        return isFirebaseAvailable;
    }

    public boolean isUserLoggedIn() {
        if (!isFirebaseAvailable || auth == null) return false;
        return auth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        if (isFirebaseAvailable && auth != null) {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                return user.getUid();
            }
        }
        return GUEST_USER_ID;
    }

    public String getCurrentUserEmail() {
        if (isFirebaseAvailable && auth != null) {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                return user.getEmail();
            }
        }
        return "guest@smartmealplanner.local";
    }

    public void signIn(String email, String password, AuthCallback callback) {
        if (!isFirebaseAvailable || auth == null) {
            callback.onError("Firebase service unavailable. Please continue in offline mode.");
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user != null) {
                    callback.onSuccess(user.getUid(), user.getEmail());
                } else {
                    callback.onError("Authentication failed: User profile null.");
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void register(String email, String password, String fullName, AuthCallback callback) {
        if (!isFirebaseAvailable || auth == null) {
            callback.onError("Firebase service unavailable. Please continue in offline mode.");
            return;
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user != null) {
                    callback.onSuccess(user.getUid(), user.getEmail());
                } else {
                    callback.onError("Registration failed: User profile null.");
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendPasswordReset(String email, AuthCallback callback) {
        if (!isFirebaseAvailable || auth == null) {
            callback.onError("Firebase unavailable.");
            return;
        }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener(aVoid -> callback.onSuccess("", email))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void signOut() {
        if (isFirebaseAvailable && auth != null) {
            auth.signOut();
        }
    }

    public boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                       capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                       capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    // --- Cloud Firestore Push Operations ---

    public void syncMealToCloud(LoggedMealEntity meal, SyncCallback callback) {
        if (!isFirebaseAvailable || firestore == null || meal == null) {
            if (callback != null) callback.onSuccess();
            return;
        }
        String uid = meal.userId;
        Map<String, Object> data = new HashMap<>();
        data.put("mealId", meal.mealId);
        data.put("foodId", meal.foodId);
        data.put("foodName", meal.foodName);
        data.put("mealSlot", meal.mealSlot);
        data.put("servingOption", meal.servingOption);
        data.put("servingWeightGrams", meal.servingWeightGrams);
        data.put("calories", meal.calories);
        data.put("proteinGrams", meal.proteinGrams);
        data.put("carbsGrams", meal.carbsGrams);
        data.put("fatGrams", meal.fatGrams);
        data.put("timestampMs", meal.timestampMs);
        data.put("dateString", meal.dateString);
        data.put("updatedAtMs", meal.updatedAtMs);

        firestore.collection("users").document(uid)
                 .collection("meals").document(meal.mealId)
                 .set(data, SetOptions.merge())
                 .addOnSuccessListener(AppDatabase.databaseWriteExecutor, aVoid -> {
                     if (callback != null) callback.onSuccess();
                 })
                 .addOnFailureListener(AppDatabase.databaseWriteExecutor, e -> {
                     if (callback != null) callback.onFailure(e);
                 });
    }

    public void syncProfileToCloud(UserProfileEntity profile, SyncCallback callback) {
        if (!isFirebaseAvailable || firestore == null || profile == null) {
            if (callback != null) callback.onSuccess();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", profile.userId);
        data.put("fullName", profile.fullName);
        data.put("email", profile.email);
        data.put("age", profile.age);
        data.put("heightCm", profile.heightCm);
        data.put("weightKg", profile.weightKg);
        data.put("isMale", profile.isMale);
        data.put("activityLevel", profile.activityLevel);
        data.put("dietaryType", profile.dietaryType);
        data.put("fitnessGoal", profile.fitnessGoal);
        data.put("budgetTier", profile.budgetTier);
        data.put("cuisinePref", profile.cuisinePref);
        data.put("targetCalories", profile.targetCalories);
        data.put("targetProteinGrams", profile.targetProteinGrams);
        data.put("targetCarbsGrams", profile.targetCarbsGrams);
        data.put("targetFatGrams", profile.targetFatGrams);
        data.put("updatedAtMs", profile.updatedAtMs);

        firestore.collection("users").document(profile.userId)
                 .set(data, SetOptions.merge())
                 .addOnSuccessListener(AppDatabase.databaseWriteExecutor, aVoid -> {
                     if (callback != null) callback.onSuccess();
                 })
                 .addOnFailureListener(AppDatabase.databaseWriteExecutor, e -> {
                     if (callback != null) callback.onFailure(e);
                 });
    }

    public void syncGroceryToCloud(GroceryItemEntity item, SyncCallback callback) {
        if (!isFirebaseAvailable || firestore == null || item == null) {
            if (callback != null) callback.onSuccess();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("itemId", item.itemId);
        data.put("ingredientName", item.ingredientName);
        data.put("quantity", item.quantity);
        data.put("unit", item.unit);
        data.put("category", item.category);
        data.put("isChecked", item.isChecked);
        data.put("updatedAtMs", item.updatedAtMs);

        firestore.collection("users").document(item.userId)
                 .collection("grocery").document(item.itemId)
                 .set(data, SetOptions.merge())
                 .addOnSuccessListener(AppDatabase.databaseWriteExecutor, aVoid -> {
                     if (callback != null) callback.onSuccess();
                 })
                 .addOnFailureListener(AppDatabase.databaseWriteExecutor, e -> {
                     if (callback != null) callback.onFailure(e);
                 });
    }

    public void syncUcbStateToCloud(UCB1StateEntity state, SyncCallback callback) {
        if (!isFirebaseAvailable || firestore == null || state == null) {
            if (callback != null) callback.onSuccess();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("armId", state.armId);
        data.put("selections", state.selections);
        data.put("totalReward", state.totalReward);
        data.put("meanReward", state.meanReward);
        data.put("lastUpdatedMs", state.lastUpdatedMs);

        firestore.collection("users").document(state.userId)
                 .collection("recommendation_state").document(state.armId)
                 .set(data, SetOptions.merge())
                 .addOnSuccessListener(AppDatabase.databaseWriteExecutor, aVoid -> {
                     if (callback != null) callback.onSuccess();
                 })
                 .addOnFailureListener(AppDatabase.databaseWriteExecutor, e -> {
                     if (callback != null) callback.onFailure(e);
                 });
    }
}
