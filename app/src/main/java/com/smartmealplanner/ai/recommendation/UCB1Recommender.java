package com.smartmealplanner.ai.recommendation;

import java.util.HashMap;
import java.util.Map;

/**
 * Intelligent Systems (IS2) Module: UCB1 (Upper Confidence Bound 1) Multi-Armed Bandit
 * for Adaptive Contextual Meal Recommendations.
 *
 * Balance Exploration vs. Exploitation:
 * UCB(i) = mean_reward(i) + c * sqrt(ln(total_selections) / selections(i))
 *
 * Exploration constant: c = sqrt(2) approx 1.4142
 * Cold start: Unselected arms (selections == 0) are prioritized for initial exploration.
 */
public class UCB1Recommender {

    public static final double DEFAULT_EXPLORATION_CONSTANT = Math.sqrt(2.0); // 1.41421356

    public static class BanditArm {
        private final String armId; // Food item id or category id
        private int selections;     // n_i
        private double totalReward; // W_i
        private double meanReward;  // r_bar_i = W_i / n_i

        public BanditArm(String armId, int selections, double totalReward) {
            this.armId = armId;
            this.selections = Math.max(0, selections);
            this.totalReward = totalReward;
            this.meanReward = (this.selections > 0) ? (this.totalReward / this.selections) : 0.0;
        }

        public String getArmId() {
            return armId;
        }

        public int getSelections() {
            return selections;
        }

        public double getTotalReward() {
            return totalReward;
        }

        public double getMeanReward() {
            return meanReward;
        }

        /**
         * Updates arm state with a new reward: +1.0 for like, -1.0 for explicit dislike.
         */
        public void update(double reward) {
            this.selections++;
            this.totalReward += reward;
            this.meanReward = this.totalReward / this.selections;
        }
    }

    private final double explorationConstant;

    public UCB1Recommender() {
        this(DEFAULT_EXPLORATION_CONSTANT);
    }

    public UCB1Recommender(double explorationConstant) {
        this.explorationConstant = explorationConstant;
    }

    /**
     * Calculates the UCB1 score for a single candidate arm given current system state.
     *
     * @param arm The candidate bandit arm
     * @param totalSystemSelections Total selections across all arms in the pool (N)
     * @return UCB score (higher = stronger candidate to explore or exploit)
     */
    public double calculateUcbScore(BanditArm arm, int totalSystemSelections) {
        if (arm == null) return 0.0;

        // Cold-Start handling: Unexplored arms receive high exploration bonus
        if (arm.getSelections() == 0) {
            return 2.5; // High priority baseline for unselected arms
        }

        if (totalSystemSelections <= 0) {
            totalSystemSelections = arm.getSelections();
        }

        // Penalty guard: If explicitly and consistently disliked, suppress exploration
        if (arm.getMeanReward() <= -0.5 && arm.getSelections() >= 2) {
            return -1.0;
        }

        double exploitation = arm.getMeanReward();
        double exploration = explorationConstant * Math.sqrt(Math.log(Math.max(1, totalSystemSelections)) / (double) arm.getSelections());

        return exploitation + exploration;
    }

    /**
     * Maps user feedback into discrete numerical rewards:
     * - Like = +1.0
     * - Dislike = -1.0
     * - Saved without rating = +0.25 (mild positive signal)
     */
    public static double mapFeedbackToReward(boolean isLiked, boolean isDisliked, boolean isSaved) {
        if (isLiked) return 1.0;
        if (isDisliked) return -1.0;
        if (isSaved) return 0.25;
        return 0.0; // No feedback = no update
    }
}
