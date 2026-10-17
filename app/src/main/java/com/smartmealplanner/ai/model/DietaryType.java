package com.smartmealplanner.ai.model;

/**
 * Supported dietary types for hard dietary restriction filtering.
 */
public enum DietaryType {
    VEGETARIAN("Vegetarian"),
    NON_VEGETARIAN("Non-Vegetarian"),
    VEGAN("Vegan"),
    EGGETARIAN("Eggetarian");

    private final String displayName;

    DietaryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DietaryType fromString(String text) {
        if (text == null) return VEGETARIAN;
        for (DietaryType b : DietaryType.values()) {
            if (b.name().equalsIgnoreCase(text) || b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return VEGETARIAN;
    }

    /**
     * Checks if this dietary type is compatible with user preference.
     * Hard constraint: A vegetarian user must NEVER be offered non-vegetarian foods.
     */
    public boolean isCompatibleWith(DietaryType userPreference) {
        if (userPreference == null) return true;
        if (userPreference == NON_VEGETARIAN) return true; // Non-veg users can eat anything
        if (userPreference == VEGETARIAN) {
            return this == VEGETARIAN || this == VEGAN;
        }
        if (userPreference == EGGETARIAN) {
            return this == EGGETARIAN || this == VEGETARIAN || this == VEGAN;
        }
        if (userPreference == VEGAN) {
            return this == VEGAN;
        }
        return true;
    }
}
