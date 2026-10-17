package com.smartmealplanner.ai.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a personalized daily meal plan.
 */
@Entity(
    tableName = "meal_plans",
    indices = {
        @Index(value = {"userId", "dateString"}, unique = true),
        @Index(value = {"syncState"})
    }
)
public class MealPlanEntity {

    @PrimaryKey
    @NonNull
    public String planId; // UUID

    @NonNull
    public String userId;

    @NonNull
    public String dateString; // yyyy-MM-dd

    public String breakfastFoodId;
    public String lunchFoodId;
    public String snackFoodId;
    public String dinnerFoodId;

    public double totalCalories;
    public double totalProteinGrams;
    public double totalCarbsGrams;
    public double totalFatGrams;

    public int syncState;
    public long updatedAtMs;

    public MealPlanEntity(@NonNull String planId, @NonNull String userId, @NonNull String dateString,
                          String breakfastFoodId, String lunchFoodId, String snackFoodId,
                          String dinnerFoodId, double totalCalories, double totalProteinGrams,
                          double totalCarbsGrams, double totalFatGrams, int syncState, long updatedAtMs) {
        this.planId = planId;
        this.userId = userId;
        this.dateString = dateString;
        this.breakfastFoodId = breakfastFoodId;
        this.lunchFoodId = lunchFoodId;
        this.snackFoodId = snackFoodId;
        this.dinnerFoodId = dinnerFoodId;
        this.totalCalories = totalCalories;
        this.totalProteinGrams = totalProteinGrams;
        this.totalCarbsGrams = totalCarbsGrams;
        this.totalFatGrams = totalFatGrams;
        this.syncState = syncState;
        this.updatedAtMs = updatedAtMs;
    }
}
