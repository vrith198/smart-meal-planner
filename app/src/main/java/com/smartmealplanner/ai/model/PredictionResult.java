package com.smartmealplanner.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a prediction from on-device or multimodal food recognition,
 * enriched with immediate nutritional estimation (Calories, Protein, Carbs, Fat)
 * and structured ingredient separation (Visible, Inferred, Uncertain).
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

    // Base per-100g values for honest dynamic portion scaling
    private double baseCaloriesPer100g = 0.0;
    private double baseProteinPer100g = 0.0;
    private double baseCarbsPer100g = 0.0;
    private double baseFatPer100g = 0.0;
    private int servingWeightGrams = 150;
    private ServingOption servingOption = ServingOption.MEDIUM;

    // Structured ingredient breakdown (visually detected vs recipe-inferred vs uncertain)
    private List<String> visibleIngredients = new ArrayList<>();
    private List<String> inferredIngredients = new ArrayList<>();
    private List<String> uncertainIngredients = new ArrayList<>();

    // Legacy / unified ingredients list
    private List<String> ingredients = new ArrayList<>();
    private String healthNotes = "";
    private String dietaryType = "VEGETARIAN";
    private String aiSource = "ON_DEVICE"; // "ON_DEVICE" or "GEMINI_AGENT"
    private String explanation = "";

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

    public void setBaseNutrition(double calPer100g, double protPer100g, double carbPer100g, double fatPer100g, int initialGrams) {
        this.baseCaloriesPer100g = calPer100g;
        this.baseProteinPer100g = protPer100g;
        this.baseCarbsPer100g = carbPer100g;
        this.baseFatPer100g = fatPer100g;
        this.servingWeightGrams = initialGrams > 0 ? initialGrams : 150;
        recalculateNutrition(this.servingWeightGrams);
    }

    /**
     * Dynamically recalculates nutritional values based on chosen serving weight in grams.
     */
    public void recalculateNutrition(int grams) {
        if (grams <= 0) grams = 100;
        this.servingWeightGrams = grams;
        if (baseCaloriesPer100g > 0) {
            double factor = (double) grams / 100.0;
            this.calories = (int) Math.round(baseCaloriesPer100g * factor);
            this.protein = (float) (Math.round(baseProteinPer100g * factor * 10.0) / 10.0);
            this.carbs = (float) (Math.round(baseCarbsPer100g * factor * 10.0) / 10.0);
            this.fat = (float) (Math.round(baseFatPer100g * factor * 10.0) / 10.0);
        }
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

    public int getServingWeightGrams() {
        return servingWeightGrams;
    }

    public void setServingWeightGrams(int servingWeightGrams) {
        this.servingWeightGrams = servingWeightGrams;
    }

    public ServingOption getServingOption() {
        return servingOption;
    }

    public void setServingOption(ServingOption servingOption) {
        this.servingOption = servingOption;
    }

    public List<String> getVisibleIngredients() {
        return visibleIngredients;
    }

    public void setVisibleIngredients(List<String> visibleIngredients) {
        this.visibleIngredients = visibleIngredients != null ? visibleIngredients : new ArrayList<>();
    }

    public List<String> getInferredIngredients() {
        return inferredIngredients;
    }

    public void setInferredIngredients(List<String> inferredIngredients) {
        this.inferredIngredients = inferredIngredients != null ? inferredIngredients : new ArrayList<>();
    }

    public List<String> getUncertainIngredients() {
        return uncertainIngredients;
    }

    public void setUncertainIngredients(List<String> uncertainIngredients) {
        this.uncertainIngredients = uncertainIngredients != null ? uncertainIngredients : new ArrayList<>();
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
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

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation != null ? explanation : "";
    }

    public double getBaseCaloriesPer100g() {
        return baseCaloriesPer100g;
    }

    public double getBaseProteinPer100g() {
        return baseProteinPer100g;
    }

    public double getBaseCarbsPer100g() {
        return baseCarbsPer100g;
    }

    public double getBaseFatPer100g() {
        return baseFatPer100g;
    }

    public boolean isGeminiResult() {
        return "GEMINI_AGENT".equalsIgnoreCase(aiSource) || "GEMINI_BUILTIN_AI".equalsIgnoreCase(aiSource);
    }

    @Override
    public String toString() {
        return displayName + " (" + getConfidencePercentage() + "% - " + calories + " kcal)";
    }
}
