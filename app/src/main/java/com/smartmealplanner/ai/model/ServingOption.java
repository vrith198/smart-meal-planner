package com.smartmealplanner.ai.model;

/**
 * Predefined and custom serving options for meals.
 */
public enum ServingOption {
    SMALL("Small"),
    MEDIUM("Medium (Standard)"),
    LARGE("Large"),
    CUSTOM("Custom Grams");

    private final String displayName;

    ServingOption(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
