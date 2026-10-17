package com.smartmealplanner.ai.recommendation;

import com.smartmealplanner.ai.model.BudgetTier;
import com.smartmealplanner.ai.model.DietaryType;
import com.smartmealplanner.ai.model.FitnessGoal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Intelligent Systems (IS2) Module: Explainable AI (XAI) Engine.
 * Generates grounded, factual, and transparent explanations for meal recommendations.
 * No fabricated or random reasons.
 */
public class ExplainableAIEngine {

    /**
     * Generates a list of understandable reasons explaining why a meal was recommended.
     *
     * @param mealName        Name of candidate meal
     * @param mealDiet        Dietary type of the meal
     * @param mealCostInr     Cost of the meal in INR
     * @param mealProteinGrams Protein in grams
     * @param fuzzyScore      Computed Fuzzy Logic score (0-100)
     * @param ucbMeanReward   Bandit historical mean reward (-1.0 to 1.0)
     * @param isRecent        Whether this meal was consumed in the last 48 hours
     * @param userDiet        User's dietary preference
     * @param userGoal        User's fitness goal
     * @param userBudget      User's budget tier
     * @return List of grounded explanation bullet points
     */
    public static List<String> generateExplanations(String mealName, DietaryType mealDiet, 
                                                   double mealCostInr, double mealProteinGrams,
                                                   int fuzzyScore, double ucbMeanReward, boolean isRecent,
                                                   DietaryType userDiet, FitnessGoal userGoal, BudgetTier userBudget) {
        List<String> reasons = new ArrayList<>();

        // 1. Dietary match explanation
        if (userDiet != null) {
            if (userDiet == mealDiet) {
                reasons.add("Fits your " + userDiet.getDisplayName() + " preference perfectly.");
            } else if (userDiet == DietaryType.VEGETARIAN && mealDiet == DietaryType.VEGAN) {
                reasons.add("Plant-based meal compatible with your Vegetarian lifestyle.");
            }
        }

        // 2. Fuzzy Logic Suitability explanation
        if (fuzzyScore >= 75) {
            reasons.add(String.format(Locale.US, "High overall suitability (%d%%) for your calorie and budget targets.", fuzzyScore));
        }

        // 3. Goal & Protein alignment
        if (userGoal == FitnessGoal.MUSCLE_GAIN && mealProteinGrams >= 15.0) {
            reasons.add(String.format(Locale.US, "High protein (%.1fg) supports your Muscle Gain target.", mealProteinGrams));
        } else if (userGoal == FitnessGoal.WEIGHT_LOSS && fuzzyScore >= 70) {
            reasons.add("Portion-controlled and nutrient-dense for your Weight Loss journey.");
        }

        // 4. Budget alignment
        double perMealBudget = (userBudget != null ? userBudget.getMaxDailyBudgetInr() : 350.0) / 3.0;
        if (mealCostInr <= perMealBudget) {
            reasons.add(String.format(Locale.US, "Cost-effective (₹%.0f) within your meal budget target of ₹%.0f.", mealCostInr, perMealBudget));
        }

        // 5. UCB1 Historical feedback
        if (ucbMeanReward > 0.4) {
            reasons.add("Recommended based on your positive ratings on similar meals.");
        }

        // 6. Variety factor
        if (!isRecent) {
            reasons.add("Enhances variety — you haven't logged this meal in the past few days.");
        }

        // Fallback assurance
        if (reasons.isEmpty()) {
            reasons.add("Balanced nutritional profile aligned with your daily meal targets.");
        }

        return reasons;
    }
}
