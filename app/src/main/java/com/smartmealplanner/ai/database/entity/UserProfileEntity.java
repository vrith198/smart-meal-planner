package com.smartmealplanner.ai.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity storing user profile, dietary preferences, and target nutritional values.
 */
@Entity(tableName = "user_profiles")
public class UserProfileEntity {

    @PrimaryKey
    @NonNull
    public String userId;

    public String fullName;
    public String email;
    public int age;
    public double heightCm;
    public double weightKg;
    public boolean isMale;
    public String activityLevel;  // Sedentary, Moderate, Active
    public String dietaryType;    // VEGETARIAN, NON_VEGETARIAN, VEGAN, EGGETARIAN
    public String fitnessGoal;    // WEIGHT_LOSS, MAINTENANCE, MUSCLE_GAIN
    public String budgetTier;     // LOW, MEDIUM, HIGH
    public String cuisinePref;

    public double targetCalories;
    public double targetProteinGrams;
    public double targetCarbsGrams;
    public double targetFatGrams;

    public String allergensJson;
    public int syncState;
    public long updatedAtMs;

    public UserProfileEntity(@NonNull String userId, String fullName, String email, int age,
                             double heightCm, double weightKg, boolean isMale, String activityLevel,
                             String dietaryType, String fitnessGoal, String budgetTier,
                             String cuisinePref, double targetCalories, double targetProteinGrams,
                             double targetCarbsGrams, double targetFatGrams, String allergensJson,
                             int syncState, long updatedAtMs) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.age = age;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.isMale = isMale;
        this.activityLevel = activityLevel;
        this.dietaryType = dietaryType;
        this.fitnessGoal = fitnessGoal;
        this.budgetTier = budgetTier;
        this.cuisinePref = cuisinePref;
        this.targetCalories = targetCalories;
        this.targetProteinGrams = targetProteinGrams;
        this.targetCarbsGrams = targetCarbsGrams;
        this.targetFatGrams = targetFatGrams;
        this.allergensJson = allergensJson;
        this.syncState = syncState;
        this.updatedAtMs = updatedAtMs;
    }
}
