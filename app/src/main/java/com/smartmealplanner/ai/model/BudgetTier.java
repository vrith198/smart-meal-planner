package com.smartmealplanner.ai.model;

/**
 * Daily budget tiers for meals.
 */
public enum BudgetTier {
    LOW("Low Budget", 150.0),      // < ₹150/day (approx ₹40-50/meal)
    MEDIUM("Moderate", 350.0),    // ₹150 - ₹350/day
    HIGH("Premium", 700.0);       // > ₹350/day

    private final String displayName;
    private final double maxDailyBudgetInr;

    BudgetTier(String displayName, double maxDailyBudgetInr) {
        this.displayName = displayName;
        this.maxDailyBudgetInr = maxDailyBudgetInr;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMaxDailyBudgetInr() {
        return maxDailyBudgetInr;
    }

    public static BudgetTier fromString(String text) {
        if (text == null) return MEDIUM;
        for (BudgetTier b : BudgetTier.values()) {
            if (b.name().equalsIgnoreCase(text) || b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return MEDIUM;
    }
}
