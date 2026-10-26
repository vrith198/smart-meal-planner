package com.smartmealplanner.ai.ui.planner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.*;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.recommendation.MealRanker;

import java.text.SimpleDateFormat;
import java.util.*;

public class MealPlannerFragment extends Fragment {

    private TextView tvPlannerDate, tvPlanTotalCal, tvPlanTotalProtein, tvPlanTotalCarbs, tvPlanTotalCost;
    private TextView tvBreakfastName, tvBreakfastDetails;
    private TextView tvLunchName, tvLunchDetails;
    private TextView tvSnackName, tvSnackDetails;
    private TextView tvDinnerName, tvDinnerDetails;

    private MaterialButton btnRegeneratePlan;
    private MaterialButton btnReplaceBreakfast, btnReplaceLunch, btnReplaceSnack, btnReplaceDinner;

    private MealPlanEntity currentPlan;
    private List<FoodItemEntity> allFoods = new ArrayList<>();
    private final Map<String, FoodItemEntity> foodMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_meal_planner, container, false);

        tvPlannerDate = v.findViewById(R.id.tvPlannerDate);
        tvPlanTotalCal = v.findViewById(R.id.tvPlanTotalCal);
        tvPlanTotalProtein = v.findViewById(R.id.tvPlanTotalProtein);
        tvPlanTotalCarbs = v.findViewById(R.id.tvPlanTotalCarbs);
        tvPlanTotalCost = v.findViewById(R.id.tvPlanTotalCost);

        tvBreakfastName = v.findViewById(R.id.tvBreakfastName);
        tvBreakfastDetails = v.findViewById(R.id.tvBreakfastDetails);
        tvLunchName = v.findViewById(R.id.tvLunchName);
        tvLunchDetails = v.findViewById(R.id.tvLunchDetails);
        tvSnackName = v.findViewById(R.id.tvSnackName);
        tvSnackDetails = v.findViewById(R.id.tvSnackDetails);
        tvDinnerName = v.findViewById(R.id.tvDinnerName);
        tvDinnerDetails = v.findViewById(R.id.tvDinnerDetails);

        btnRegeneratePlan = v.findViewById(R.id.btnRegeneratePlan);
        btnReplaceBreakfast = v.findViewById(R.id.btnReplaceBreakfast);
        btnReplaceLunch = v.findViewById(R.id.btnReplaceLunch);
        btnReplaceSnack = v.findViewById(R.id.btnReplaceSnack);
        btnReplaceDinner = v.findViewById(R.id.btnReplaceDinner);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault());
        tvPlannerDate.setText(sdf.format(new Date()));

        btnRegeneratePlan.setOnClickListener(view -> regenerateCompletePlan());
        btnReplaceBreakfast.setOnClickListener(view -> showReplaceMealDialog("Breakfast"));
        btnReplaceLunch.setOnClickListener(view -> showReplaceMealDialog("Lunch"));
        btnReplaceSnack.setOnClickListener(view -> showReplaceMealDialog("Snack"));
        btnReplaceDinner.setOnClickListener(view -> showReplaceMealDialog("Dinner"));

        loadPlanData();
        return v;
    }

    private void loadPlanData() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();
        String todayString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            allFoods = db.foodItemDao().getAllFoodsSync();
            foodMap.clear();
            for (FoodItemEntity f : allFoods) {
                foodMap.put(f.id, f);
            }

            currentPlan = db.mealPlanDao().getPlanForDateSync(userId, todayString);
            if (currentPlan == null) {
                // Auto-generate initial plan
                generateIntelligentPlan(db, userId, todayString);
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::updatePlanUI);
                }
            }
        });
    }

    private void generateIntelligentPlan(AppDatabase db, String userId, String dateString) {
        UserProfileEntity profile = db.userDao().getUserProfileSync(userId);
        List<UCB1StateEntity> armEntities = db.ucb1Dao().getAllArmsSync(userId);
        int totalSelections = db.ucb1Dao().getTotalSelections(userId);
        List<LoggedMealEntity> recentMeals = db.loggedMealDao().getAllMealsSync(userId);

        Map<String, UCB1StateEntity> armMap = new HashMap<>();
        for (UCB1StateEntity e : armEntities) {
            armMap.put(e.armId, e);
        }

        MealRanker ranker = new MealRanker();
        List<MealRanker.RankedMeal> ranked = ranker.rankMeals(
                allFoods, profile, armMap, totalSelections, recentMeals, 500.0
        );

        String bId = null, lId = null, sId = null, dId = null;
        for (MealRanker.RankedMeal m : ranked) {
            FoodItemEntity f = m.getFood();
            if (bId == null && ("BREAKFAST".equalsIgnoreCase(f.mealCategory) || f.caloriesPer100g < 200)) {
                bId = f.id;
            } else if (lId == null && !f.id.equals(bId)) {
                lId = f.id;
            } else if (sId == null && !f.id.equals(bId) && !f.id.equals(lId)) {
                sId = f.id;
            } else if (dId == null && !f.id.equals(bId) && !f.id.equals(lId) && !f.id.equals(sId)) {
                dId = f.id;
                break;
            }
        }

        long now = System.currentTimeMillis();
        currentPlan = new MealPlanEntity(
                UUID.randomUUID().toString(), userId, dateString,
                bId != null ? bId : "food_idli",
                lId != null ? lId : "food_dal_tadka",
                sId != null ? sId : "food_curd_dahi",
                dId != null ? dId : "food_paneer_tikka",
                0, 0, 0, 0, 1, now
        );

        db.mealPlanDao().insertOrUpdate(currentPlan);

        FirebaseManager.getInstance(getContext()).syncMealPlanToCloud(currentPlan, new FirebaseManager.SyncCallback() {
            @Override
            public void onSuccess() {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        db.mealPlanDao().markAsSynced(currentPlan.planId);
                    } catch (Exception ignored) {}
                });
            }
            @Override
            public void onFailure(Exception e) {}
        });

        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updatePlanUI);
        }
    }

    private void regenerateCompletePlan() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();
        String todayString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            generateIntelligentPlan(db, userId, todayString);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Generated fresh personalized meal plan!", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showReplaceMealDialog(String slot) {
        if (getContext() == null || allFoods.isEmpty()) return;

        List<String> foodNames = new ArrayList<>();
        List<FoodItemEntity> choices = new ArrayList<>(allFoods);
        for (FoodItemEntity f : choices) {
            foodNames.add(f.name + " (" + f.dietaryType + " • ₹" + (int)f.estimatedCostInr + ")");
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Replace " + slot)
                .setItems(foodNames.toArray(new String[0]), (dialog, which) -> {
                    FoodItemEntity selected = choices.get(which);
                    replaceSlot(slot, selected.id);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void replaceSlot(String slot, String newFoodId) {
        if (currentPlan == null || getContext() == null) return;

        if ("Breakfast".equalsIgnoreCase(slot)) currentPlan.breakfastFoodId = newFoodId;
        else if ("Lunch".equalsIgnoreCase(slot)) currentPlan.lunchFoodId = newFoodId;
        else if ("Snack".equalsIgnoreCase(slot)) currentPlan.snackFoodId = newFoodId;
        else if ("Dinner".equalsIgnoreCase(slot)) currentPlan.dinnerFoodId = newFoodId;

        currentPlan.updatedAtMs = System.currentTimeMillis();
        currentPlan.syncState = 2; // pending update

        AppDatabase db = AppDatabase.getInstance(getContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.mealPlanDao().insertOrUpdate(currentPlan);

        FirebaseManager.getInstance(getContext()).syncMealPlanToCloud(currentPlan, new FirebaseManager.SyncCallback() {
            @Override
            public void onSuccess() {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        db.mealPlanDao().markAsSynced(currentPlan.planId);
                    } catch (Exception ignored) {}
                });
            }
            @Override
            public void onFailure(Exception e) {}
        });
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updatePlanUI();
                    Toast.makeText(getContext(), slot + " replaced successfully!", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updatePlanUI() {
        if (currentPlan == null) return;

        double totalCal = 0, totalProt = 0, totalCarb = 0, totalCost = 0;

        // Breakfast
        FoodItemEntity bFood = foodMap.get(currentPlan.breakfastFoodId);
        if (bFood != null) {
            tvBreakfastName.setText(bFood.name);
            int g = bFood.mediumServingGrams > 0 ? bFood.mediumServingGrams : 100;
            double f = g / 100.0;
            double cal = bFood.caloriesPer100g * f;
            double prot = bFood.proteinPer100g * f;
            double carb = bFood.carbsPer100g * f;
            double fat = bFood.fatPer100g * f;
            tvBreakfastDetails.setText(String.format(Locale.US, "%.0f kcal • P: %.1fg • C: %.1fg • F: %.1fg • ₹%.0f",
                    cal, prot, carb, fat, bFood.estimatedCostInr));
            totalCal += cal; totalProt += prot; totalCarb += carb; totalCost += bFood.estimatedCostInr;
        }

        // Lunch
        FoodItemEntity lFood = foodMap.get(currentPlan.lunchFoodId);
        if (lFood != null) {
            tvLunchName.setText(lFood.name);
            int g = lFood.mediumServingGrams > 0 ? lFood.mediumServingGrams : 100;
            double f = g / 100.0;
            double cal = lFood.caloriesPer100g * f;
            double prot = lFood.proteinPer100g * f;
            double carb = lFood.carbsPer100g * f;
            double fat = lFood.fatPer100g * f;
            tvLunchDetails.setText(String.format(Locale.US, "%.0f kcal • P: %.1fg • C: %.1fg • F: %.1fg • ₹%.0f",
                    cal, prot, carb, fat, lFood.estimatedCostInr));
            totalCal += cal; totalProt += prot; totalCarb += carb; totalCost += lFood.estimatedCostInr;
        }

        // Snack
        FoodItemEntity sFood = foodMap.get(currentPlan.snackFoodId);
        if (sFood != null) {
            tvSnackName.setText(sFood.name);
            int g = sFood.mediumServingGrams > 0 ? sFood.mediumServingGrams : 100;
            double f = g / 100.0;
            double cal = sFood.caloriesPer100g * f;
            double prot = sFood.proteinPer100g * f;
            double carb = sFood.carbsPer100g * f;
            double fat = sFood.fatPer100g * f;
            tvSnackDetails.setText(String.format(Locale.US, "%.0f kcal • P: %.1fg • C: %.1fg • F: %.1fg • ₹%.0f",
                    cal, prot, carb, fat, sFood.estimatedCostInr));
            totalCal += cal; totalProt += prot; totalCarb += carb; totalCost += sFood.estimatedCostInr;
        }

        // Dinner
        FoodItemEntity dFood = foodMap.get(currentPlan.dinnerFoodId);
        if (dFood != null) {
            tvDinnerName.setText(dFood.name);
            int g = dFood.mediumServingGrams > 0 ? dFood.mediumServingGrams : 100;
            double f = g / 100.0;
            double cal = dFood.caloriesPer100g * f;
            double prot = dFood.proteinPer100g * f;
            double carb = dFood.carbsPer100g * f;
            double fat = dFood.fatPer100g * f;
            tvDinnerDetails.setText(String.format(Locale.US, "%.0f kcal • P: %.1fg • C: %.1fg • F: %.1fg • ₹%.0f",
                    cal, prot, carb, fat, dFood.estimatedCostInr));
            totalCal += cal; totalProt += prot; totalCarb += carb; totalCost += dFood.estimatedCostInr;
        }

        tvPlanTotalCal.setText(String.format(Locale.US, "%.0f kcal", totalCal));
        tvPlanTotalProtein.setText(String.format(Locale.US, "P: %.0fg", totalProt));
        tvPlanTotalCarbs.setText(String.format(Locale.US, "C: %.0fg", totalCarb));
        tvPlanTotalCost.setText(String.format(Locale.US, "₹%.0f", totalCost));
    }
}
