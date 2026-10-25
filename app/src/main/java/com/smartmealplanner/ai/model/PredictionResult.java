package com.smartmealplanner.ai.model;

/**
 * Encapsulates a prediction from on-device food recognition,
 * enriched with immediate nutritional estimation (Calories, Protein, Carbs, Fat).
 */
public class PredictionResult {
    private final String label;
    private final String displayName;
    private final float confidence; // [0.0, 1.0]

    // Nutritional estimation fields
    private int calories = 0;
    private float protein = 0.0f;
    private float carbs = 0.0f;
    private float fat = 0.0f;
    private String servingUnit = "1 Serving";
    private String foodId = "";

    // Multimodal AI Agent fields
    private java.util.List<String> ingredients = new java.util.ArrayList<>();
    private String healthNotes = "";
    private String dietaryType = "";
    private String aiSource = "ON_DEVICE"; // "ON_DEVICE" or "GEMINI_AGENT"

    public PredictionResult(String label, String displayName, float confidence) {
        this.label = label;
        this.displayName = displayName;
        this.confidence = confidence;
    }

    public PredictionResult(String label, String displayName, float confidence,
                            int calories, float protein, float carbs, float fat,
                            String servingUnit, String foodId) {
        this.label = label;
        this.displayName = displayName;
        this.confidence = confidence;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.servingUnit = servingUnit != null ? servingUnit : "1 Serving";
        this.foodId = foodId != null ? foodId : "";
    }

    public String getLabel() {
        return label;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getConfidence() {
        return confidence;
    }

    public int getConfidencePercentage() {
        return Math.round(confidence * 100.0f);
    }

    public boolean isHighConfidence() {
        return confidence >= 0.65f;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public float getProtein() {
        return protein;
    }

    public void setProtein(float protein) {
        this.protein = protein;
    }

    public float getCarbs() {
        return carbs;
    }

    public void setCarbs(float carbs) {
        this.carbs = carbs;
    }

    public float getFat() {
        return fat;
    }

    public void setFat(float fat) {
        this.fat = fat;
    }

    public String getServingUnit() {
        return servingUnit;
    }

    public void setServingUnit(String servingUnit) {
        this.servingUnit = servingUnit;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public java.util.List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(java.util.List<String> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new java.util.ArrayList<>();
    }

    public String getHealthNotes() {
        return healthNotes;
    }

    public void setHealthNotes(String healthNotes) {
        this.healthNotes = healthNotes != null ? healthNotes : "";
    }

    public String getDietaryType() {
        return dietaryType;
    }

    public void setDietaryType(String dietaryType) {
        this.dietaryType = dietaryType != null ? dietaryType : "";
    }

    public String getAiSource() {
        return aiSource;
    }

    public void setAiSource(String aiSource) {
        this.aiSource = aiSource != null ? aiSource : "ON_DEVICE";
    }

    public boolean isGeminiResult() {
        return "GEMINI_AGENT".equalsIgnoreCase(aiSource);
    }

    @Override
    public String toString() {
        return displayName + " (" + getConfidencePercentage() + "% - " + calories + " kcal)";
    }
}
