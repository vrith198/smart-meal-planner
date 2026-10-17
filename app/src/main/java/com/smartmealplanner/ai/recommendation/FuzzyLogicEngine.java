package com.smartmealplanner.ai.recommendation;

import com.smartmealplanner.ai.model.BudgetTier;
import com.smartmealplanner.ai.model.FitnessGoal;

/**
 * Intelligent Systems (IS2) Module: Mamdani Fuzzy Inference System for Meal Suitability.
 * 
 * Computes a normalized suitability score (0 - 100) combining:
 * 1. Budget Suitability
 * 2. Calorie-Target Compatibility
 * 3. Protein Suitability
 * 4. Goal Compatibility
 *
 * Hard dietary restrictions must ALWAYS be enforced beforehand.
 */
public class FuzzyLogicEngine {

    // Output Linguistic Singletons / Centroids for Defuzzification
    private static final double CENTROID_LOW = 20.0;
    private static final double CENTROID_MEDIUM = 60.0;
    private static final double CENTROID_HIGH = 92.0;

    /**
     * Evaluates total meal suitability (0 to 100) using Fuzzy Inference.
     *
     * @param mealCostInr        Estimated cost of candidate meal
     * @param mealCalories       Estimated calories of candidate meal
     * @param mealProteinGrams   Estimated protein in candidate meal
     * @param targetMealCalories Desired calories for this meal slot (e.g. 500 kcal)
     * @param budgetTier         User's budget preference
     * @param fitnessGoal        User's fitness goal
     * @return Normalized score between 0 and 100
     */
    public static int evaluateSuitability(double mealCostInr, double mealCalories, double mealProteinGrams,
                                         double targetMealCalories, BudgetTier budgetTier, FitnessGoal fitnessGoal) {
        if (targetMealCalories <= 0) targetMealCalories = 500.0;
        if (budgetTier == null) budgetTier = BudgetTier.MEDIUM;
        if (fitnessGoal == null) fitnessGoal = FitnessGoal.MAINTENANCE;

        // --- Step 1: Fuzzification of Inputs ---

        // 1. Budget Ratio = mealCost / targetMealCost
        double targetMealCost = budgetTier.getMaxDailyBudgetInr() / 3.0; // Approx per-meal target
        double budgetRatio = mealCostInr / Math.max(10.0, targetMealCost);
        double budgetHighSuitability = leftShoulder(budgetRatio, 0.8, 1.2);
        double budgetMediumSuitability = triangle(budgetRatio, 0.8, 1.1, 1.5);
        double budgetLowSuitability = rightShoulder(budgetRatio, 1.2, 1.6); // Very expensive for tier

        // 2. Calorie Ratio = mealCalories / targetMealCalories
        double calorieRatio = mealCalories / targetMealCalories;
        double calorieHigh = triangle(calorieRatio, 0.85, 1.0, 1.15); // Close match
        double calorieMedium = Math.max(triangle(calorieRatio, 0.65, 0.85, 1.0), triangle(calorieRatio, 1.0, 1.15, 1.35));
        double calorieLow = (calorieRatio < 0.80) ? leftShoulder(calorieRatio, 0.50, 0.80) : rightShoulder(calorieRatio, 1.25, 1.60);

        // 3. Protein Level (in grams)
        double proteinLow = leftShoulder(mealProteinGrams, 6.0, 12.0);
        double proteinMedium = triangle(mealProteinGrams, 8.0, 16.0, 24.0);
        double proteinHigh = rightShoulder(mealProteinGrams, 18.0, 26.0);

        // --- Step 2: Rule Base Evaluation (Mamdani Min operator) ---
        // Rule 1: IF budget is HIGH AND calorie match is HIGH -> suitability is HIGH
        double rHigh = Math.min(budgetHighSuitability, calorieHigh);

        // Rule 2: Goal rules for MUSCLE_GAIN
        double rMuscleGain = 0.0;
        double rLow = 0.0;
        if (fitnessGoal == FitnessGoal.MUSCLE_GAIN) {
            rMuscleGain = Math.min(proteinHigh, Math.max(calorieHigh, calorieMedium));
            // Low protein in muscle gain significantly penalizes suitability
            rLow = Math.max(rLow, Math.min(proteinLow, 0.8));
        }

        // Rule 3: Goal rules for WEIGHT_LOSS
        double rWeightLoss = 0.0;
        if (fitnessGoal == FitnessGoal.WEIGHT_LOSS) {
            rWeightLoss = Math.min(calorieHigh, Math.max(proteinMedium, proteinHigh));
            if (calorieRatio > 1.2) {
                rLow = Math.max(rLow, calorieLow);
            }
        }

        // Rule 4: Moderate budget + moderate calories = Medium suitability
        double rMedium = Math.min(budgetMediumSuitability, calorieMedium);

        // Rule 5: Budget Low Suitability (meal too expensive for budget tier) -> suitability is LOW
        if (budgetTier == BudgetTier.LOW) {
            rLow = Math.max(rLow, budgetLowSuitability);
        } else {
            rLow = Math.max(rLow, Math.min(budgetLowSuitability, 0.7));
        }

        // Rule 6: Calorie Low match -> suitability is LOW
        rLow = Math.max(rLow, calorieLow * 0.8);

        // Base baseline for acceptable meals
        double rBaseline = Math.min(calorieMedium, budgetHighSuitability) * 0.7;
        rMedium = Math.max(rMedium, rBaseline);

        // --- Step 3: Aggregation (Max operator) ---
        double aggHigh = Math.max(rHigh, Math.max(rMuscleGain, rWeightLoss));
        double aggMedium = rMedium;
        double aggLow = rLow;

        // --- Step 4: Defuzzification (Weighted Centroid Method) ---
        double totalWeight = aggLow + aggMedium + aggHigh;
        if (totalWeight < 1e-6) {
            return 50; // Neutral default
        }

        double defuzzified = ((aggLow * CENTROID_LOW) + (aggMedium * CENTROID_MEDIUM) + (aggHigh * CENTROID_HIGH)) / totalWeight;
        return (int) Math.round(Math.max(0.0, Math.min(100.0, defuzzified)));
    }

    /**
     * Triangular membership function.
     */
    public static double triangle(double x, double a, double b, double c) {
        if (x <= a || x >= c) return 0.0;
        if (x == b) return 1.0;
        if (x < b) return (x - a) / (b - a);
        return (c - x) / (c - b);
    }

    /**
     * Trapezoidal membership function.
     */
    public static double trapezoid(double x, double a, double b, double c, double d) {
        if (x <= a || x >= d) return 0.0;
        if (x >= b && x <= c) return 1.0;
        if (x < b) return (x - a) / (b - a);
        return (d - x) / (d - c);
    }

    /**
     * Left-shoulder membership function (1.0 below a, descending to 0.0 at b).
     */
    public static double leftShoulder(double x, double a, double b) {
        if (x <= a) return 1.0;
        if (x >= b) return 0.0;
        return (b - x) / (b - a);
    }

    /**
     * Right-shoulder membership function (0.0 below a, ascending to 1.0 at b).
     */
    public static double rightShoulder(double x, double a, double b) {
        if (x <= a) return 0.0;
        if (x >= b) return 1.0;
        return (x - a) / (b - a);
    }
}
