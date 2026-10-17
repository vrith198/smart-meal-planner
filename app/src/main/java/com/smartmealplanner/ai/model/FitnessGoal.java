package com.smartmealplanner.ai.model;

/**
 * User fitness goals driving nutrition calculations and Fuzzy Logic evaluation.
 */
public enum FitnessGoal {
    WEIGHT_LOSS("Weight Loss", -400.0, 0.30, 0.40, 0.30),
    MAINTENANCE("Maintenance", 0.0, 0.20, 0.50, 0.30),
    MUSCLE_GAIN("Muscle Gain", 350.0, 0.35, 0.45, 0.20);

    private final String displayName;
    private final double calorieAdjustment; // Adjustment to TDEE
    private final double proteinRatio;      // Percentage of total calories
    private final double carbRatio;
    private final double fatRatio;

    FitnessGoal(String displayName, double calorieAdjustment, double proteinRatio, double carbRatio, double fatRatio) {
        this.displayName = displayName;
        this.calorieAdjustment = calorieAdjustment;
        this.proteinRatio = proteinRatio;
        this.carbRatio = carbRatio;
        this.fatRatio = fatRatio;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getCalorieAdjustment() {
        return calorieAdjustment;
    }

    public double getProteinRatio() {
        return proteinRatio;
    }

    public double getCarbRatio() {
        return carbRatio;
    }

    public double getFatRatio() {
        return fatRatio;
    }

    public static FitnessGoal fromString(String text) {
        if (text == null) return MAINTENANCE;
        for (FitnessGoal g : FitnessGoal.values()) {
            if (g.name().equalsIgnoreCase(text) || g.displayName.equalsIgnoreCase(text)) {
                return g;
            }
        }
        return MAINTENANCE;
    }
}
