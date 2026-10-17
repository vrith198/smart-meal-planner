package com.smartmealplanner.ai.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a verified food item in the local catalog.
 */
@Entity(tableName = "food_items")
public class FoodItemEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String name;

    public String classifierLabel;
    public String dietaryType;      // VEGETARIAN, NON_VEGETARIAN, VEGAN, EGGETARIAN
    public String mealCategory;     // BREAKFAST, LUNCH, SNACK, DINNER

    public double caloriesPer100g;
    public double proteinPer100g;
    public double carbsPer100g;
    public double fatPer100g;

    public int smallServingGrams;
    public int mediumServingGrams;
    public int largeServingGrams;
    public String servingUnitName;

    public double estimatedCostInr;
    public String allergensJson;
    public String ingredientsJson;
    public String source;

    public FoodItemEntity(@NonNull String id, @NonNull String name, String classifierLabel,
                          String dietaryType, String mealCategory, double caloriesPer100g,
                          double proteinPer100g, double carbsPer100g, double fatPer100g,
                          int smallServingGrams, int mediumServingGrams, int largeServingGrams,
                          String servingUnitName, double estimatedCostInr, String allergensJson,
                          String ingredientsJson, String source) {
        this.id = id;
        this.name = name;
        this.classifierLabel = classifierLabel;
        this.dietaryType = dietaryType;
        this.mealCategory = mealCategory;
        this.caloriesPer100g = caloriesPer100g;
        this.proteinPer100g = proteinPer100g;
        this.carbsPer100g = carbsPer100g;
        this.fatPer100g = fatPer100g;
        this.smallServingGrams = smallServingGrams;
        this.mediumServingGrams = mediumServingGrams;
        this.largeServingGrams = largeServingGrams;
        this.servingUnitName = servingUnitName;
        this.estimatedCostInr = estimatedCostInr;
        this.allergensJson = allergensJson;
        this.ingredientsJson = ingredientsJson;
        this.source = source;
    }
}
