package com.smartmealplanner.ai.model;

/**
 * Supported meal slots for meal planning and Naive Bayes categorization.
 */
public enum MealCategory {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    SNACK("Snack"),
    DINNER("Dinner");

    private final String displayName;

    MealCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MealCategory fromString(String text) {
        if (text == null) return LUNCH;
        for (MealCategory mc : MealCategory.values()) {
            if (mc.name().equalsIgnoreCase(text) || mc.displayName.equalsIgnoreCase(text)) {
                return mc;
            }
        }
        return LUNCH;
    }
}
