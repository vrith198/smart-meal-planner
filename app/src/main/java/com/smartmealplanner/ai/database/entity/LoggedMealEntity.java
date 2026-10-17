package com.smartmealplanner.ai.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a logged meal consumed by the user.
 */
@Entity(
    tableName = "logged_meals",
    indices = {
        @Index(value = {"userId", "dateString"}),
        @Index(value = {"syncState"})
    }
)
public class LoggedMealEntity {

    public static final int SYNCED = 0;
    public static final int PENDING_CREATE = 1;
    public static final int PENDING_UPDATE = 2;
    public static final int PENDING_DELETE = 3;

    @PrimaryKey
    @NonNull
    public String mealId; // UUID

    @NonNull
    public String userId;

    public String foodId;
    public String foodName;
    public String mealSlot;          // BREAKFAST, LUNCH, SNACK, DINNER
    public String servingOption;     // SMALL, MEDIUM, LARGE, CUSTOM
    public int servingWeightGrams;

    public double calories;
    public double proteinGrams;
    public double carbsGrams;
    public double fatGrams;

    public long timestampMs;
    public String dateString;        // yyyy-MM-dd

    public int syncState;            // 0=synced, 1=pending_create, 2=pending_update, 3=pending_delete
    public long updatedAtMs;

    public LoggedMealEntity(@NonNull String mealId, @NonNull String userId, String foodId,
                            String foodName, String mealSlot, String servingOption,
                            int servingWeightGrams, double calories, double proteinGrams,
                            double carbsGrams, double fatGrams, long timestampMs,
                            String dateString, int syncState, long updatedAtMs) {
        this.mealId = mealId;
        this.userId = userId;
        this.foodId = foodId;
        this.foodName = foodName;
        this.mealSlot = mealSlot;
        this.servingOption = servingOption;
        this.servingWeightGrams = servingWeightGrams;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.timestampMs = timestampMs;
        this.dateString = dateString;
        this.syncState = syncState;
        this.updatedAtMs = updatedAtMs;
    }
}
