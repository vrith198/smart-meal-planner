package com.smartmealplanner.ai;

import com.smartmealplanner.ai.model.MealCategory;
import com.smartmealplanner.ai.recommendation.NaiveBayesMealClassifier;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests verifying Naive Bayes probabilistic meal categorization and Laplace smoothing.
 */
public class NaiveBayesMealClassifierTest {

    private static final double EPSILON = 0.001;
    private NaiveBayesMealClassifier classifier;

    @Before
    public void setUp() {
        classifier = new NaiveBayesMealClassifier();
    }

    @Test
    public void testProbabilitiesSumToOne() {
        Map<String, Double> probs = classifier.predictProbabilities(
                "Vegetarian", "North Indian", "Low", "Weight Loss", "Moderate", "Morning", "Medium"
        );

        assertNotNull(probs);
        assertEquals(4, probs.size());

        double sum = 0.0;
        for (double p : probs.values()) {
            assertTrue("Probability must be non-negative", p >= 0.0);
            sum += p;
        }

        assertEquals("Sum of posterior probabilities must equal 1.0", 1.0, sum, EPSILON);
    }

    @Test
    public void testPredictMealCategory_ValidCategory() {
        MealCategory cat = classifier.predict(
                "Vegetarian", "South Indian", "Low", "Maintenance", "Moderate", "Morning", "Medium"
        );

        assertNotNull(cat);
        assertTrue(cat == MealCategory.BREAKFAST || cat == MealCategory.LUNCH ||
                   cat == MealCategory.SNACK || cat == MealCategory.DINNER);
    }

    @Test
    public void testLaplaceSmoothing_HandlesUnseenOrNullFeaturesWithoutCrashing() {
        // All null features
        Map<String, Double> probs = classifier.predictProbabilities(
                null, null, null, null, null, null, null
        );

        assertNotNull(probs);
        assertEquals(4, probs.size());
        for (double p : probs.values()) {
            assertEquals("Uniform prior expected with no features", 0.25, p, EPSILON);
        }
    }
}
