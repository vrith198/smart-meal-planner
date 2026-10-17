package com.smartmealplanner.ai;

import com.smartmealplanner.ai.model.BudgetTier;
import com.smartmealplanner.ai.model.FitnessGoal;
import com.smartmealplanner.ai.recommendation.FuzzyLogicEngine;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests verifying Mamdani Fuzzy Inference System membership functions,
 * rule aggregation, and defuzzified suitability output (0 - 100).
 */
public class FuzzyLogicEngineTest {

    private static final double EPSILON = 0.001;

    @Test
    public void testTriangularMembershipFunction() {
        // a=10, b=20, c=30
        assertEquals(0.0, FuzzyLogicEngine.triangle(5.0, 10.0, 20.0, 30.0), EPSILON);
        assertEquals(0.5, FuzzyLogicEngine.triangle(15.0, 10.0, 20.0, 30.0), EPSILON);
        assertEquals(1.0, FuzzyLogicEngine.triangle(20.0, 10.0, 20.0, 30.0), EPSILON);
        assertEquals(0.5, FuzzyLogicEngine.triangle(25.0, 10.0, 20.0, 30.0), EPSILON);
        assertEquals(0.0, FuzzyLogicEngine.triangle(35.0, 10.0, 20.0, 30.0), EPSILON);
    }

    @Test
    public void testTrapezoidalMembershipFunction() {
        // a=10, b=20, c=30, d=40
        assertEquals(0.0, FuzzyLogicEngine.trapezoid(5.0, 10.0, 20.0, 30.0, 40.0), EPSILON);
        assertEquals(0.5, FuzzyLogicEngine.trapezoid(15.0, 10.0, 20.0, 30.0, 40.0), EPSILON);
        assertEquals(1.0, FuzzyLogicEngine.trapezoid(25.0, 10.0, 20.0, 30.0, 40.0), EPSILON); // Plateau
        assertEquals(0.5, FuzzyLogicEngine.trapezoid(35.0, 10.0, 20.0, 30.0, 40.0), EPSILON);
        assertEquals(0.0, FuzzyLogicEngine.trapezoid(45.0, 10.0, 20.0, 30.0, 40.0), EPSILON);
    }

    @Test
    public void testSuitabilityScore_HighSuitabilityMeal() {
        // Affordable (₹40), perfectly matched calories (500 kcal), good protein (20g)
        int score = FuzzyLogicEngine.evaluateSuitability(
                40.0, 500.0, 20.0, 500.0, BudgetTier.MEDIUM, FitnessGoal.MAINTENANCE
        );

        assertTrue("High suitability expected (>= 75)", score >= 75);
        assertTrue("Score must not exceed 100", score <= 100);
    }

    @Test
    public void testSuitabilityScore_MuscleGainProteinBoost() {
        // Meal A: High protein (28g)
        int scoreHighProtein = FuzzyLogicEngine.evaluateSuitability(
                60.0, 520.0, 28.0, 500.0, BudgetTier.MEDIUM, FitnessGoal.MUSCLE_GAIN
        );

        // Meal B: Low protein (4g)
        int scoreLowProtein = FuzzyLogicEngine.evaluateSuitability(
                60.0, 520.0, 4.0, 500.0, BudgetTier.MEDIUM, FitnessGoal.MUSCLE_GAIN
        );

        assertTrue("High protein meal must score significantly higher for muscle gain",
                scoreHighProtein > scoreLowProtein);
    }

    @Test
    public void testSuitabilityScore_LowBudgetPenaltyForExpensiveMeal() {
        // Expensive meal (₹250) evaluated for Low Budget user (< ₹50/meal)
        int scoreExpensive = FuzzyLogicEngine.evaluateSuitability(
                250.0, 500.0, 15.0, 500.0, BudgetTier.LOW, FitnessGoal.MAINTENANCE
        );

        // Affordable meal (₹30) evaluated for Low Budget user
        int scoreAffordable = FuzzyLogicEngine.evaluateSuitability(
                30.0, 500.0, 15.0, 500.0, BudgetTier.LOW, FitnessGoal.MAINTENANCE
        );

        assertTrue("Affordable meal must score much higher than expensive meal for Low budget tier",
                scoreAffordable > scoreExpensive);
        assertTrue("Expensive meal should have low suitability (< 50)", scoreExpensive < 50);
    }

    @Test
    public void testSuitabilityScore_StrictBoundsGuarantee() {
        // Extreme values test
        int scoreNegative = FuzzyLogicEngine.evaluateSuitability(
                -10.0, -500.0, -10.0, 500.0, BudgetTier.LOW, FitnessGoal.WEIGHT_LOSS
        );
        int scoreMassive = FuzzyLogicEngine.evaluateSuitability(
                10000.0, 10000.0, 1000.0, 500.0, BudgetTier.HIGH, FitnessGoal.MUSCLE_GAIN
        );

        assertTrue(scoreNegative >= 0 && scoreNegative <= 100);
        assertTrue(scoreMassive >= 0 && scoreMassive <= 100);
    }
}
