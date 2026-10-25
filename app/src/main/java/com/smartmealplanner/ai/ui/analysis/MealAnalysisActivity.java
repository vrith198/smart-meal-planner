package com.smartmealplanner.ai.ui.analysis;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.FoodItemEntity;
import com.smartmealplanner.ai.database.entity.LoggedMealEntity;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.model.NutritionSummary;
import com.smartmealplanner.ai.model.ServingOption;
import com.smartmealplanner.ai.nutrition.NutritionCalculator;
import com.smartmealplanner.ai.ui.substitute.IngredientSubstituteBottomSheet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MealAnalysisActivity extends AppCompatActivity {

    private String foodId;
    private FoodItemEntity currentFood;
    private ServingOption currentOption = ServingOption.MEDIUM;
    private int currentServingGrams = 100;

    private TextView tvFoodName, tvDietSource, tvServingDescription;
    private TextView tvCalories, tvProtein, tvCarbs, tvFat;
    private MaterialButtonToggleGroup toggleServing;
    private TextInputLayout tilCustomGrams;
    private TextInputEditText etCustomGrams;
    private Spinner spinnerMealSlot;
    private MaterialButton btnSaveMeal;
    private MaterialCardView cardIngredients;
    private ChipGroup chipGroupIngredients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_analysis);

        foodId = getIntent().getStringExtra("EXTRA_FOOD_ID");
        if (foodId == null) {
            Toast.makeText(this, "Food item not specified.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvFoodName = findViewById(R.id.tvAnalysisFoodName);
        tvDietSource = findViewById(R.id.tvAnalysisDietSource);
        tvServingDescription = findViewById(R.id.tvServingDescription);
        tvCalories = findViewById(R.id.tvCalculatedCalories);
        tvProtein = findViewById(R.id.tvCalculatedProtein);
        tvCarbs = findViewById(R.id.tvCalculatedCarbs);
        tvFat = findViewById(R.id.tvCalculatedFat);

        toggleServing = findViewById(R.id.toggleServing);
        tilCustomGrams = findViewById(R.id.tilCustomGrams);
        etCustomGrams = findViewById(R.id.etCustomGrams);
        spinnerMealSlot = findViewById(R.id.spinnerMealSlot);
        btnSaveMeal = findViewById(R.id.btnSaveMeal);
        cardIngredients = findViewById(R.id.cardIngredients);
        chipGroupIngredients = findViewById(R.id.chipGroupIngredients);

        setupMealSlotSpinner();
        setupServingToggle();

        btnSaveMeal.setOnClickListener(v -> handleSaveMeal());

        loadFoodDetails();
    }

    private void setupMealSlotSpinner() {
        String[] slots = {
            getString(R.string.meal_type_breakfast),
            getString(R.string.meal_type_lunch),
            getString(R.string.meal_type_snack),
            getString(R.string.meal_type_dinner)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, slots);
        spinnerMealSlot.setAdapter(adapter);

        // Auto-select based on time of day
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 11) {
            spinnerMealSlot.setSelection(0); // Breakfast
        } else if (hour < 16) {
            spinnerMealSlot.setSelection(1); // Lunch
        } else if (hour < 19) {
            spinnerMealSlot.setSelection(2); // Snack
        } else {
            spinnerMealSlot.setSelection(3); // Dinner
        }
    }

    private void setupServingToggle() {
        toggleServing.check(R.id.btnServingMedium);
        toggleServing.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || currentFood == null) return;

            if (checkedId == R.id.btnServingSmall) {
                currentOption = ServingOption.SMALL;
                tilCustomGrams.setVisibility(View.GONE);
                currentServingGrams = currentFood.smallServingGrams > 0 ? currentFood.smallServingGrams : 60;
                tvServingDescription.setText(String.format(Locale.US, "Small portion (%dg)", currentServingGrams));
            } else if (checkedId == R.id.btnServingLarge) {
                currentOption = ServingOption.LARGE;
                tilCustomGrams.setVisibility(View.GONE);
                currentServingGrams = currentFood.largeServingGrams > 0 ? currentFood.largeServingGrams : 200;
                tvServingDescription.setText(String.format(Locale.US, "Large portion (%dg)", currentServingGrams));
            } else if (checkedId == R.id.btnServingCustom) {
                currentOption = ServingOption.CUSTOM;
                tilCustomGrams.setVisibility(View.VISIBLE);
                tvServingDescription.setText("Enter custom weight in grams");
            } else {
                currentOption = ServingOption.MEDIUM;
                tilCustomGrams.setVisibility(View.GONE);
                currentServingGrams = currentFood.mediumServingGrams > 0 ? currentFood.mediumServingGrams : 120;
                tvServingDescription.setText(String.format(Locale.US, "Standard medium portion (%dg - %s)",
                        currentServingGrams, currentFood.servingUnitName != null ? currentFood.servingUnitName : "Serving"));
            }
            recalculateNutrients();
        });

        etCustomGrams.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentOption == ServingOption.CUSTOM && s != null && s.length() > 0) {
                    try {
                        currentServingGrams = Math.max(1, Integer.parseInt(s.toString()));
                        recalculateNutrients();
                    } catch (NumberFormatException ignored) {}
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadFoodDetails() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            currentFood = AppDatabase.getInstance(this).foodItemDao().getFoodById(foodId);
            runOnUiThread(() -> {
                if (currentFood != null) {
                    tvFoodName.setText(currentFood.name);
                    tvDietSource.setText(String.format("%s • %s", currentFood.dietaryType, currentFood.source));
                    currentServingGrams = currentFood.mediumServingGrams > 0 ? currentFood.mediumServingGrams : 120;
                    tvServingDescription.setText(String.format(Locale.US, "Standard medium portion (%dg - %s)",
                            currentServingGrams, currentFood.servingUnitName != null ? currentFood.servingUnitName : "Serving"));
                    setupIngredientsChips(currentFood.ingredientsJson);
                    recalculateNutrients();
                } else {
                    Toast.makeText(this, "Food not found in catalog.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void setupIngredientsChips(String ingredientsJson) {
        if (ingredientsJson == null || ingredientsJson.trim().isEmpty()) {
            if (cardIngredients != null) cardIngredients.setVisibility(View.GONE);
            return;
        }

        try {
            JSONArray array = new JSONArray(ingredientsJson);
            if (array.length() == 0) {
                if (cardIngredients != null) cardIngredients.setVisibility(View.GONE);
                return;
            }

            if (cardIngredients != null) cardIngredients.setVisibility(View.VISIBLE);
            if (chipGroupIngredients != null) {
                chipGroupIngredients.removeAllViews();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject ing = array.getJSONObject(i);
                    String name = ing.getString("name");
                    double amount = ing.optDouble("amount", 0);
                    String unit = ing.optString("unit", "");

                    Chip chip = new Chip(this);
                    if (amount > 0) {
                        chip.setText(String.format(Locale.US, "%s (%.0f %s)", name, amount, unit));
                    } else {
                        chip.setText(name);
                    }
                    chip.setChipIconResource(R.drawable.ic_swap_horiz);
                    chip.setClickable(true);
                    chip.setFocusable(true);
                    chip.setOnClickListener(v -> {
                        IngredientSubstituteBottomSheet sheet = IngredientSubstituteBottomSheet.newInstance(
                                name, amount, unit, false, ""
                        );
                        sheet.show(getSupportFragmentManager(), "substitute_sheet");
                    });
                    chipGroupIngredients.addView(chip);
                }
            }
        } catch (Exception e) {
            if (cardIngredients != null) cardIngredients.setVisibility(View.GONE);
        }
    }

    private void recalculateNutrients() {
        if (currentFood == null) return;
        NutritionSummary summary = NutritionCalculator.calculateNutrients(
            currentFood.caloriesPer100g,
            currentFood.proteinPer100g,
            currentFood.carbsPer100g,
            currentFood.fatPer100g,
            currentServingGrams
        );

        tvCalories.setText(String.format(Locale.US, "%.0f", summary.getCalories()));
        tvProtein.setText(String.format(Locale.US, "%.1fg", summary.getProteinGrams()));
        tvCarbs.setText(String.format(Locale.US, "%.1fg", summary.getCarbsGrams()));
        tvFat.setText(String.format(Locale.US, "%.1fg", summary.getFatGrams()));
    }

    private void handleSaveMeal() {
        if (currentFood == null) return;

        NutritionSummary summary = NutritionCalculator.calculateNutrients(
            currentFood.caloriesPer100g,
            currentFood.proteinPer100g,
            currentFood.carbsPer100g,
            currentFood.fatPer100g,
            currentServingGrams
        );

        String mealSlot = spinnerMealSlot.getSelectedItem().toString();
        String userId = FirebaseManager.getInstance(this).getCurrentUserId();
        String mealId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now));

        LoggedMealEntity meal = new LoggedMealEntity(
            mealId, userId, currentFood.id, currentFood.name, mealSlot,
            currentOption.name(), currentServingGrams, summary.getCalories(),
            summary.getProteinGrams(), summary.getCarbsGrams(), summary.getFatGrams(),
            now, dateString, LoggedMealEntity.PENDING_CREATE, now
        );

        // Persist to Room and trigger cloud sync
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getInstance(this).loggedMealDao().insert(meal);

            // Attempt cloud sync if online
            FirebaseManager.getInstance(this).syncMealToCloud(meal, new FirebaseManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    AppDatabase.getInstance(MealAnalysisActivity.this).loggedMealDao().markAsSynced(mealId);
                }

                @Override
                public void onFailure(Exception e) {
                    // Stays pending_create in Room, WorkManager will sync when online
                }
            });
        });

        Toast.makeText(this, R.string.meal_saved_success, Toast.LENGTH_SHORT).show();
        finish();
    }
}
