package com.smartmealplanner.ai;

import com.smartmealplanner.ai.database.entity.FoodItemEntity;
import com.smartmealplanner.ai.database.entity.LoggedMealEntity;
import com.smartmealplanner.ai.database.entity.UCB1StateEntity;
import com.smartmealplanner.ai.database.entity.UserProfileEntity;
import com.smartmealplanner.ai.model.DietaryType;
import com.smartmealplanner.ai.recommendation.MealRanker;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests verifying Multi-Objective Meal Ranking and Hard Dietary Restriction enforcement.
 */
public class MealRankerTest {

    private MealRanker ranker;
    private List<FoodItemEntity> samplePool;

    @Before
    public void setUp() {
        ranker = new MealRanker();
        samplePool = new ArrayList<>();

        // 1. Veg meal with gluten
        samplePool.add(new FoodItemEntity(
                "food_roti", "Roti", "roti", "VEGETARIAN", "LUNCH",
                300.0, 8.75, 57.5, 3.0, 40, 80, 120, "Pieces", 20.0,
                "[\"gluten\"]", "[]", "IFCT-2017"
        ));

        // 2. Vegan meal gluten-free
        samplePool.add(new FoodItemEntity(
                "food_rice", "Basmati Rice", "rice", "VEGAN", "LUNCH",
                130.0, 2.7, 28.0, 0.3, 100, 150, 250, "Bowl", 25.0,
                "[]", "[]", "USDA"
        ));

        // 3. Non-veg meal
        samplePool.add(new FoodItemEntity(
                "food_chicken", "Chicken Curry", "chicken_curry", "NON_VEGETARIAN", "DINNER",
                155.0, 12.25, 3.0, 10.0, 130, 200, 300, "Bowl", 130.0,
                "[]", "[]", "IFCT-2017"
        ));

        // 4. Eggetarian meal
        samplePool.add(new FoodItemEntity(
                "food_omelette", "Egg Omelette", "omelette", "EGGETARIAN", "BREAKFAST",
                170.0, 11.5, 1.8, 13.0, 80, 120, 180, "Omelette", 40.0,
                "[\"egg\"]", "[]", "USDA"
        ));
    }

    @Test
    public void testHardDietaryRestriction_VegetarianUserNeverGetsNonVegOrEggetarian() {
        UserProfileEntity vegProfile = new UserProfileEntity(
                "u1", "Veg User", "veg@test.com", 25, 170.0, 65.0, true, "Moderate",
                "VEGETARIAN", "MAINTENANCE", "MEDIUM", "Indian",
                2000.0, 100.0, 250.0, 65.0, "[]", 0, System.currentTimeMillis()
        );

        List<MealRanker.RankedMeal> results = ranker.rankMeals(
                samplePool, vegProfile, Collections.emptyMap(), 10, Collections.emptyList(), 500.0
        );

        assertFalse("Results must not be empty", results.isEmpty());
        for (MealRanker.RankedMeal m : results) {
            String diet = m.getFood().dietaryType;
            assertNotEquals("Vegetarian user must NEVER receive Non-Vegetarian food", "NON_VEGETARIAN", diet);
            assertNotEquals("Vegetarian user must NEVER receive Eggetarian food", "EGGETARIAN", diet);
            assertTrue(diet.equals("VEGETARIAN") || diet.equals("VEGAN"));
        }
    }

    @Test
    public void testHardDietaryRestriction_VeganUserOnlyGetsVegan() {
        UserProfileEntity veganProfile = new UserProfileEntity(
                "u2", "Vegan User", "vegan@test.com", 28, 165.0, 58.0, false, "Moderate",
                "VEGAN", "WEIGHT_LOSS", "LOW", "Continental",
                1600.0, 80.0, 200.0, 50.0, "[]", 0, System.currentTimeMillis()
        );

        List<MealRanker.RankedMeal> results = ranker.rankMeals(
                samplePool, veganProfile, Collections.emptyMap(), 10, Collections.emptyList(), 400.0
        );

        assertFalse(results.isEmpty());
        for (MealRanker.RankedMeal m : results) {
            assertEquals("Vegan user must ONLY receive VEGAN food", "VEGAN", m.getFood().dietaryType);
        }
    }

    @Test
    public void testAllergenFiltering_GlutenAllergyExcludesRoti() {
        UserProfileEntity glutenAllergicProfile = new UserProfileEntity(
                "u3", "Gluten Sensitive", "gluten@test.com", 30, 175.0, 72.0, true, "Active",
                "NON_VEGETARIAN", "MUSCLE_GAIN", "HIGH", "Continental",
                2400.0, 140.0, 280.0, 75.0, "[\"gluten\"]", 0, System.currentTimeMillis()
        );

        List<MealRanker.RankedMeal> results = ranker.rankMeals(
                samplePool, glutenAllergicProfile, Collections.emptyMap(), 10, Collections.emptyList(), 600.0
        );

        for (MealRanker.RankedMeal m : results) {
            assertNotEquals("Roti containing gluten must be excluded", "food_roti", m.getFood().id);
        }
    }

    @Test
    public void testVarietyPenaltyAppliedToRecentlyLoggedMeals() {
        UserProfileEntity profile = new UserProfileEntity(
                "u4", "General User", "gen@test.com", 25, 170.0, 65.0, true, "Moderate",
                "NON_VEGETARIAN", "MAINTENANCE", "MEDIUM", "Indian",
                2000.0, 100.0, 250.0, 65.0, "[]", 0, System.currentTimeMillis()
        );

        // Meal consumed yesterday: Chicken Curry
        LoggedMealEntity recentLog = new LoggedMealEntity(
                "m1", "u4", "food_chicken", "Chicken Curry", "Dinner",
                "MEDIUM", 200, 310.0, 24.5, 6.0, 20.0,
                System.currentTimeMillis() - 86400000L, "2026-10-16", 0, System.currentTimeMillis()
        );

        List<MealRanker.RankedMeal> results = ranker.rankMeals(
                samplePool, profile, Collections.emptyMap(), 10, Collections.singletonList(recentLog), 500.0
        );

        // Find chicken curry in results
        MealRanker.RankedMeal chickenMeal = null;
        for (MealRanker.RankedMeal m : results) {
            if (m.getFood().id.equals("food_chicken")) {
                chickenMeal = m;
                break;
            }
        }

        assertNotNull(chickenMeal);
        assertTrue("Recent meal should be marked as recent for variety penalty", chickenMeal.isRecent());
    }

    @Test
    public void testExplainableAIProducesGroundedReasons() {
        UserProfileEntity profile = new UserProfileEntity(
                "u5", "Fitness User", "fit@test.com", 24, 180.0, 75.0, true, "Active",
                "VEGETARIAN", "MUSCLE_GAIN", "MEDIUM", "Indian",
                2500.0, 150.0, 300.0, 75.0, "[]", 0, System.currentTimeMillis()
        );

        List<MealRanker.RankedMeal> results = ranker.rankMeals(
                samplePool, profile, Collections.emptyMap(), 10, Collections.emptyList(), 500.0
        );

        for (MealRanker.RankedMeal m : results) {
            assertNotNull(m.getExplanations());
            assertFalse("Every recommended meal must have transparent explanations", m.getExplanations().isEmpty());
        }
    }
}
