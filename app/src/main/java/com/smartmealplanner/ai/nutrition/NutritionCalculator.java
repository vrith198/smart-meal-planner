package com.smartmealplanner.ai.nutrition;

import com.smartmealplanner.ai.model.FitnessGoal;
import com.smartmealplanner.ai.model.NutritionSummary;
import com.smartmealplanner.ai.model.ServingOption;

/**
 * Standardized, tested mathematical engine for nutrition estimation and BMR/TDEE calculation.
 * Formula for 100g nutrient scaling:
 * estimated_nutrient = nutrient_per_100g * serving_weight_g / 100.0
 */
public class NutritionCalculator {

    /**
     * Calculates nutritional values for a given serving weight from base per-100g values.
     *
     * @param calPer100g  Calories per 100g
     * @param protPer100g Protein in grams per 100g
     * @param carbPer100g Carbs in grams per 100g
     * @param fatPer100g  Fat in grams per 100g
     * @param servingWeightGrams Chosen serving weight in grams
     * @return NutritionSummary with estimated values
     */
    public static NutritionSummary calculateNutrients(double calPer100g, double protPer100g, 
                                                      double carbPer100g, double fatPer100g, 
                                                      int servingWeightGrams) {
        if (servingWeightGrams <= 0) {
            servingWeightGrams = 100;
        }
        double factor = (double) servingWeightGrams / 100.0;
        double totalCal = calPer100g * factor;
        double totalProt = protPer100g * factor;
        double totalCarb = carbPer100g * factor;
        double totalFat = fatPer100g * factor;

        return new NutritionSummary(totalCal, totalProt, totalCarb, totalFat, servingWeightGrams);
    }

    /**
     * Resolves the actual serving weight in grams based on chosen option and item catalog reference weights.
     */
    public static int resolveServingWeight(ServingOption option, int smallGrams, int mediumGrams, int largeGrams, int customGrams) {
        if (option == null) return mediumGrams > 0 ? mediumGrams : 100;
        switch (option) {
            case SMALL:
                return smallGrams > 0 ? smallGrams : Math.max(1, (int)(mediumGrams * 0.6));
            case LARGE:
                return largeGrams > 0 ? largeGrams : (int)(mediumGrams * 1.5);
            case CUSTOM:
                return customGrams > 0 ? customGrams : (mediumGrams > 0 ? mediumGrams : 100);
            case MEDIUM:
            default:
                return mediumGrams > 0 ? mediumGrams : 100;
        }
    }

    /**
     * Calculates Basal Metabolic Rate (BMR) using Mifflin-St Jeor equation.
     */
    public static double calculateBmr(double weightKg, double heightCm, int age, boolean isMale) {
        if (weightKg <= 0 || heightCm <= 0 || age <= 0) {
            return 1600.0; // Sensible adult baseline fallback
        }
        double base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age);
        return isMale ? (base + 5.0) : (base - 161.0);
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE).
     * @param activityLevel "Sedentary", "Moderate", or "Active"
     */
    public static double calculateTdee(double bmr, String activityLevel) {
        double multiplier = 1.375; // Moderate default
        if (activityLevel != null) {
            String lower = activityLevel.toLowerCase();
            if (lower.contains("sedentary") || lower.contains("low")) {
                multiplier = 1.2;
            } else if (lower.contains("moderate") || lower.contains("medium")) {
                multiplier = 1.55;
            } else if (lower.contains("active") || lower.contains("high")) {
                multiplier = 1.725;
            }
        }
        return bmr * multiplier;
    }

    /**
     * Calculates daily calorie target adjusted for user's fitness goal.
     */
    public static double calculateTargetCalories(double tdee, FitnessGoal goal) {
        if (goal == null) goal = FitnessGoal.MAINTENANCE;
        double target = tdee + goal.getCalorieAdjustment();
        return Math.max(1200.0, target); // Safe floor for health and wellness
    }

    /**
     * Calculates target macronutrients in grams for a given calorie target and fitness goal.
     * Protein: 4 kcal/g, Carbs: 4 kcal/g, Fat: 9 kcal/g.
     */
    public static NutritionSummary calculateTargetMacros(double targetCalories, FitnessGoal goal) {
        if (goal == null) goal = FitnessGoal.MAINTENANCE;
        double proteinGrams = (targetCalories * goal.getProteinRatio()) / 4.0;
        double carbGrams = (targetCalories * goal.getCarbRatio()) / 4.0;
        double fatGrams = (targetCalories * goal.getFatRatio()) / 9.0;
        return new NutritionSummary(targetCalories, proteinGrams, carbGrams, fatGrams, 0);
    }
}
