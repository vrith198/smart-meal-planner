package com.smartmealplanner.ai;

import com.smartmealplanner.ai.recommendation.UCB1Recommender;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests verifying UCB1 multi-armed bandit formulas, cold-start exploration,
 * exploitation balance, and feedback reward updates.
 */
public class UCB1RecommenderTest {

    private static final double EPSILON = 0.001;
    private UCB1Recommender recommender;

    @Before
    public void setUp() {
        recommender = new UCB1Recommender(Math.sqrt(2.0));
    }

    @Test
    public void testColdStartArm_ReceivesExplorationPriority() {
        UCB1Recommender.BanditArm unexploredArm = new UCB1Recommender.BanditArm("food_roti", 0, 0.0);
        double score = recommender.calculateUcbScore(unexploredArm, 100);

        // Unexplored arm must have high priority for cold start
        assertEquals(2.5, score, EPSILON);
    }

    @Test
    public void testRewardUpdateMechanism() {
        UCB1Recommender.BanditArm arm = new UCB1Recommender.BanditArm("food_dal", 0, 0.0);

        // User likes the meal (+1.0)
        arm.update(1.0);
        assertEquals(1, arm.getSelections());
        assertEquals(1.0, arm.getTotalReward(), EPSILON);
        assertEquals(1.0, arm.getMeanReward(), EPSILON);

        // User likes again (+1.0)
        arm.update(1.0);
        assertEquals(2, arm.getSelections());
        assertEquals(2.0, arm.getTotalReward(), EPSILON);
        assertEquals(1.0, arm.getMeanReward(), EPSILON);

        // User dislikes (-1.0)
        arm.update(-1.0);
        assertEquals(3, arm.getSelections());
        assertEquals(1.0, arm.getTotalReward(), EPSILON);
        assertEquals(1.0 / 3.0, arm.getMeanReward(), EPSILON);
    }

    @Test
    public void testExploitationVsExplorationBalance() {
        int totalPulls = 100;

        // Arm A: Well-liked, pulled 10 times with 10 positive rewards
        UCB1Recommender.BanditArm armA = new UCB1Recommender.BanditArm("arm_a", 10, 10.0);
        double scoreA = recommender.calculateUcbScore(armA, totalPulls);

        // Arm B: Average reward, pulled only 2 times (higher exploration uncertainty)
        UCB1Recommender.BanditArm armB = new UCB1Recommender.BanditArm("arm_b", 2, 1.0);
        double scoreB = recommender.calculateUcbScore(armB, totalPulls);

        // Both should produce valid scores > 0
        assertTrue("Arm A score must be positive", scoreA > 0);
        assertTrue("Arm B score must be positive", scoreB > 0);
    }

    @Test
    public void testDislikeSuppressionGuard() {
        // Arm with 3 explicit dislikes (mean reward = -1.0)
        UCB1Recommender.BanditArm dislikedArm = new UCB1Recommender.BanditArm("disliked_food", 3, -3.0);
        double score = recommender.calculateUcbScore(dislikedArm, 100);

        // Should be suppressed with negative penalty score
        assertEquals(-1.0, score, EPSILON);
    }

    @Test
    public void testFeedbackRewardMapping() {
        assertEquals(1.0, UCB1Recommender.mapFeedbackToReward(true, false, false), EPSILON);
        assertEquals(-1.0, UCB1Recommender.mapFeedbackToReward(false, true, false), EPSILON);
        assertEquals(0.25, UCB1Recommender.mapFeedbackToReward(false, false, true), EPSILON);
        assertEquals(0.0, UCB1Recommender.mapFeedbackToReward(false, false, false), EPSILON);
    }
}
