package com.smartmealplanner.ai.recommendation;

import com.smartmealplanner.ai.database.entity.FoodItemEntity;
import com.smartmealplanner.ai.database.entity.LoggedMealEntity;
import com.smartmealplanner.ai.database.entity.UCB1StateEntity;
import com.smartmealplanner.ai.database.entity.UserProfileEntity;
import com.smartmealplanner.ai.model.BudgetTier;
import com.smartmealplanner.ai.model.DietaryType;
import com.smartmealplanner.ai.model.FitnessGoal;

import java.util.*;

/**
 * Intelligent Systems (IS2) Module: Multi-Objective Meal Ranker.
 * 
 * 1. Hard Dietary Restriction Filtering (Vegetarian, Vegan, Allergens) - MANDATORY.
 * 2. Fuzzy Logic Suitability Scoring (0-100).
 * 3. UCB1 Multi-Armed Bandit Scoring (Exploration vs. Exploitation).
 * 4. Goal Macro Alignment & Variety Check.
 * 5. Explainable AI Reason Generation.
 */
public class MealRanker {

    public static class RankedMeal {
        private final FoodItemEntity food;
        private final int fuzzyScore;
        private final double ucbScore;
        private final double compositeScore;
        private final List<String> explanations;
        private final boolean isRecent;

        public RankedMeal(FoodItemEntity food, int fuzzyScore, double ucbScore, 
                          double compositeScore, List<String> explanations, boolean isRecent) {
            this.food = food;
            this.fuzzyScore = fuzzyScore;
            this.ucbScore = ucbScore;
            this.compositeScore = compositeScore;
            this.explanations = explanations;
            this.isRecent = isRecent;
        }

        public FoodItemEntity getFood() {
            return food;
        }

        public int getFuzzyScore() {
            return fuzzyScore;
        }

        public double getUcbScore() {
            return ucbScore;
        }

        public double getCompositeScore() {
            return compositeScore;
        }

        public List<String> getExplanations() {
            return explanations;
        }

        public boolean isRecent() {
            return isRecent;
        }
    }

    private final UCB1Recommender ucb1Recommender;

    public MealRanker() {
        this(new UCB1Recommender());
    }

    public MealRanker(UCB1Recommender ucb1Recommender) {
        this.ucb1Recommender = ucb1Recommender;
    }

    /**
     * Ranks meal candidates against user profile and historical state.
     */
    public List<RankedMeal> rankMeals(List<FoodItemEntity> candidatePool,
                                     UserProfileEntity profile,
                                     Map<String, UCB1StateEntity> armStates,
                                     int totalSystemSelections,
                                     List<LoggedMealEntity> recentMeals,
                                     double targetSlotCalories) {
        List<RankedMeal> rankedList = new ArrayList<>();
        if (candidatePool == null || candidatePool.isEmpty()) {
            return rankedList;
        }

        DietaryType userDiet = (profile != null && profile.dietaryType != null)
                ? DietaryType.fromString(profile.dietaryType) : DietaryType.VEGETARIAN;
        FitnessGoal userGoal = (profile != null && profile.fitnessGoal != null)
                ? FitnessGoal.fromString(profile.fitnessGoal) : FitnessGoal.MAINTENANCE;
        BudgetTier userBudget = (profile != null && profile.budgetTier != null)
                ? BudgetTier.fromString(profile.budgetTier) : BudgetTier.MEDIUM;

        // Build set of recent food IDs (consumed in last 48 hours)
        Set<String> recentFoodIds = new HashSet<>();
        if (recentMeals != null) {
            for (LoggedMealEntity lm : recentMeals) {
                if (lm.foodId != null) {
                    recentFoodIds.add(lm.foodId);
                }
            }
        }

        // --- Stage 1: Enforce Hard Dietary Restrictions & Allergies ---
        List<FoodItemEntity> eligibleCandidates = new ArrayList<>();
        for (FoodItemEntity candidate : candidatePool) {
            DietaryType mealDiet = DietaryType.fromString(candidate.dietaryType);
            if (!mealDiet.isCompatibleWith(userDiet)) {
                continue; // Hard constraint violated -> Reject candidate immediately
            }

            // Allergen check
            if (hasAllergenConflict(candidate.allergensJson, profile != null ? profile.allergensJson : null)) {
                continue; // Hard allergy constraint violated -> Reject
            }

            eligibleCandidates.add(candidate);
        }

        if (eligibleCandidates.isEmpty()) {
            return rankedList;
        }

        // --- Stage 2: Multi-Objective Scoring ---
        for (FoodItemEntity food : eligibleCandidates) {
            // Medium standard serving reference
            int servingGrams = food.mediumServingGrams > 0 ? food.mediumServingGrams : 100;
            double factor = servingGrams / 100.0;
            double mealCalories = food.caloriesPer100g * factor;
            double mealProtein = food.proteinPer100g * factor;
            double mealCost = food.estimatedCostInr;

            // 1. Fuzzy Logic Suitability (0 - 100)
            int fuzzyScore = FuzzyLogicEngine.evaluateSuitability(
                    mealCost, mealCalories, mealProtein, targetSlotCalories, userBudget, userGoal
            );

            // 2. UCB1 Bandit Score
            UCB1StateEntity stateEntity = (armStates != null) ? armStates.get(food.id) : null;
            UCB1Recommender.BanditArm arm;
            if (stateEntity != null) {
                arm = new UCB1Recommender.BanditArm(food.id, stateEntity.selections, stateEntity.totalReward);
            } else {
                arm = new UCB1Recommender.BanditArm(food.id, 0, 0.0); // Cold-start
            }
            double rawUcb = ucb1Recommender.calculateUcbScore(arm, totalSystemSelections);

            // Normalize UCB score to [0, 100]: mapping [-1.0, 3.0] -> [0, 100]
            double normUcb = Math.max(0.0, Math.min(100.0, ((rawUcb + 1.0) / 4.0) * 100.0));

            // 3. Goal Alignment Score (0 - 100)
            double goalScore = 60.0; // Baseline
            if (userGoal == FitnessGoal.MUSCLE_GAIN) {
                goalScore = Math.min(100.0, 50.0 + (mealProtein * 2.0));
            } else if (userGoal == FitnessGoal.WEIGHT_LOSS) {
                goalScore = Math.min(100.0, 100.0 - Math.abs(mealCalories - targetSlotCalories) * 0.1);
            }

            // 4. Variety Score (0 - 100)
            boolean isRecent = recentFoodIds.contains(food.id);
            double varietyScore = isRecent ? 30.0 : 90.0;

            // Composite Weighted Rank
            // W_fuzzy = 0.40, W_ucb = 0.30, W_goal = 0.15, W_variety = 0.15
            double compositeScore = (0.40 * fuzzyScore) + (0.30 * normUcb) + (0.15 * goalScore) + (0.15 * varietyScore);

            // Grounded XAI Explanations
            DietaryType mealDiet = DietaryType.fromString(food.dietaryType);
            List<String> explanations = ExplainableAIEngine.generateExplanations(
                    food.name, mealDiet, mealCost, mealProtein, fuzzyScore,
                    arm.getMeanReward(), isRecent, userDiet, userGoal, userBudget
            );

            rankedList.add(new RankedMeal(food, fuzzyScore, rawUcb, compositeScore, explanations, isRecent));
        }

        // Sort descending by composite score
        Collections.sort(rankedList, (a, b) -> Double.compare(b.getCompositeScore(), a.getCompositeScore()));

        return rankedList;
    }

    private boolean hasAllergenConflict(String foodAllergensJson, String userAllergensJson) {
        if (foodAllergensJson == null || userAllergensJson == null) return false;
        String lowerFood = foodAllergensJson.toLowerCase();
        String lowerUser = userAllergensJson.toLowerCase();

        String[] commonAllergens = {"gluten", "dairy", "peanuts", "nuts", "egg", "fish", "soy"};
        for (String allergen : commonAllergens) {
            if (lowerUser.contains(allergen) && lowerFood.contains(allergen)) {
                return true;
            }
        }
        return false;
    }
}
