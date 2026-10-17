package com.smartmealplanner.ai;

import com.smartmealplanner.ai.model.FitnessGoal;
import com.smartmealplanner.ai.model.NutritionSummary;
import com.smartmealplanner.ai.model.ServingOption;
import com.smartmealplanner.ai.nutrition.NutritionCalculator;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests verifying exact mathematical nutrient calculations,
 * serving size conversions, Mifflin-St Jeor BMR, and macro distributions.
 */
public class NutritionCalculatorTest {

    private static final double EPSILON = 0.001;

    @Test
    public void testNutrientPer100gCalculation_ExactScaling() {
        // Given 100g values: 300 kcal, 8.75g protein, 57.5g carbs, 3.0g fat
        double cal100 = 300.0;
        double prot100 = 8.75;
        double carb100 = 57.5;
        double fat100 = 3.0;

        // When serving weight is 80g (e.g. 2 rotis)
        NutritionSummary summary = NutritionCalculator.calculateNutrients(cal100, prot100, carb100, fat100, 80);

        // Then: nutrient = per100g * 80 / 100
        assertEquals(240.0, summary.getCalories(), EPSILON);
        assertEquals(7.0, summary.getProteinGrams(), EPSILON);
        assertEquals(46.0, summary.getCarbsGrams(), EPSILON);
        assertEquals(2.4, summary.getFatGrams(), EPSILON);
        assertEquals(80, summary.getServingWeightGrams());
    }

    @Test
    public void testNutrientPer100gCalculation_ZeroAndNegativeGuard() {
        NutritionSummary summary = NutritionCalculator.calculateNutrients(100.0, 5.0, 10.0, 2.0, -50);
        // Should fallback gracefully to 100g default
        assertEquals(100.0, summary.getCalories(), EPSILON);
        assertEquals(100, summary.getServingWeightGrams());
    }

    @Test
    public void testResolveServingWeight_AllOptions() {
        int small = 40;
        int medium = 80;
        int large = 120;
        int custom = 150;

        assertEquals(40, NutritionCalculator.resolveServingWeight(ServingOption.SMALL, small, medium, large, custom));
        assertEquals(80, NutritionCalculator.resolveServingWeight(ServingOption.MEDIUM, small, medium, large, custom));
        assertEquals(120, NutritionCalculator.resolveServingWeight(ServingOption.LARGE, small, medium, large, custom));
        assertEquals(150, NutritionCalculator.resolveServingWeight(ServingOption.CUSTOM, small, medium, large, custom));
    }

    @Test
    public void testMifflinStJeorBmr_Male() {
        // Male: 10 * weight_kg + 6.25 * height_cm - 5 * age + 5
        // Weight: 70kg, Height: 175cm, Age: 25
        // BMR = 10*70 + 6.25*175 - 5*25 + 5 = 700 + 1093.75 - 125 + 5 = 1673.75
        double bmr = NutritionCalculator.calculateBmr(70.0, 175.0, 25, true);
        assertEquals(1673.75, bmr, EPSILON);
    }

    @Test
    public void testMifflinStJeorBmr_Female() {
        // Female: 10 * weight_kg + 6.25 * height_cm - 5 * age - 161
        // Weight: 60kg, Height: 165cm, Age: 30
        // BMR = 10*60 + 6.25*165 - 5*30 - 161 = 600 + 1031.25 - 150 - 161 = 1320.25
        double bmr = NutritionCalculator.calculateBmr(60.0, 165.0, 30, false);
        assertEquals(1320.25, bmr, EPSILON);
    }

    @Test
    public void testTdeeCalculation_ActivityMultipliers() {
        double bmr = 1500.0;
        assertEquals(1800.0, NutritionCalculator.calculateTdee(bmr, "Sedentary"), EPSILON);   // 1.2
        assertEquals(2325.0, NutritionCalculator.calculateTdee(bmr, "Moderate"), EPSILON);    // 1.55
        assertEquals(2587.5, NutritionCalculator.calculateTdee(bmr, "Active"), EPSILON);      // 1.725
    }

    @Test
    public void testGoalCalorieAdjustment() {
        double tdee = 2200.0;
        // Weight Loss: -400 kcal
        assertEquals(1800.0, NutritionCalculator.calculateTargetCalories(tdee, FitnessGoal.WEIGHT_LOSS), EPSILON);
        // Maintenance: 0 kcal
        assertEquals(2200.0, NutritionCalculator.calculateTargetCalories(tdee, FitnessGoal.MAINTENANCE), EPSILON);
        // Muscle Gain: +350 kcal
        assertEquals(2550.0, NutritionCalculator.calculateTargetCalories(tdee, FitnessGoal.MUSCLE_GAIN), EPSILON);
    }

    @Test
    public void testMacroDistributionCalculation() {
        double targetCalories = 2000.0;
        // Muscle Gain: Protein ratio 0.35 (4 kcal/g), Carb ratio 0.45 (4 kcal/g), Fat ratio 0.20 (9 kcal/g)
        NutritionSummary macros = NutritionCalculator.calculateTargetMacros(targetCalories, FitnessGoal.MUSCLE_GAIN);

        // Expected Protein = (2000 * 0.35) / 4 = 175.0g
        assertEquals(175.0, macros.getProteinGrams(), EPSILON);
        // Expected Carbs = (2000 * 0.45) / 4 = 225.0g
        assertEquals(225.0, macros.getCarbsGrams(), EPSILON);
        // Expected Fat = (2000 * 0.20) / 9 = 44.44g
        assertEquals(44.444, macros.getFatGrams(), 0.01);
    }
}
