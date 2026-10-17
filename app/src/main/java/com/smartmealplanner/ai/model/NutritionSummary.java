package com.smartmealplanner.ai.model;

import java.io.Serializable;
import java.util.Locale;

/**
 * Value object holding nutritional totals.
 */
public class NutritionSummary implements Serializable {
    private final double calories;
    private final double proteinGrams;
    private final double carbsGrams;
    private final double fatGrams;
    private final int servingWeightGrams;

    public NutritionSummary(double calories, double proteinGrams, double carbsGrams, double fatGrams, int servingWeightGrams) {
        this.calories = Math.max(0, calories);
        this.proteinGrams = Math.max(0, proteinGrams);
        this.carbsGrams = Math.max(0, carbsGrams);
        this.fatGrams = Math.max(0, fatGrams);
        this.servingWeightGrams = Math.max(1, servingWeightGrams);
    }

    public double getCalories() {
        return calories;
    }

    public double getProteinGrams() {
        return proteinGrams;
    }

    public double getCarbsGrams() {
        return carbsGrams;
    }

    public double getFatGrams() {
        return fatGrams;
    }

    public int getServingWeightGrams() {
        return servingWeightGrams;
    }

    public String getFormattedCalories() {
        return String.format(Locale.US, "%.0f kcal", calories);
    }

    public String getFormattedProtein() {
        return String.format(Locale.US, "%.1fg", proteinGrams);
    }

    public String getFormattedCarbs() {
        return String.format(Locale.US, "%.1fg", carbsGrams);
    }

    public String getFormattedFat() {
        return String.format(Locale.US, "%.1fg", fatGrams);
    }
}
