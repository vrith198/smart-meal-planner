package com.smartmealplanner.ai.ui.recommendation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.*;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.recommendation.MealRanker;
import com.smartmealplanner.ai.recommendation.UCB1Recommender;

import java.text.SimpleDateFormat;
import java.util.*;

public class RecommendationsFragment extends Fragment implements RecommendationAdapter.OnRecommendationActionListener {

    private RecyclerView rvRecommendations;
    private RecommendationAdapter adapter;
    private ProgressBar pbLoading;
    private TextView tvEmpty;
    private ChipGroup chipGroupFilters;

    private MealRanker mealRanker;
    private List<MealRanker.RankedMeal> allRankedMeals = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recommendations, container, false);

        rvRecommendations = v.findViewById(R.id.rvRecommendations);
        pbLoading = v.findViewById(R.id.pbLoadingRecs);
        tvEmpty = v.findViewById(R.id.tvEmptyRecs);
        chipGroupFilters = v.findViewById(R.id.chipGroupFilters);

        rvRecommendations.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecommendationAdapter();
        adapter.setListener(this);
        rvRecommendations.setAdapter(adapter);

        mealRanker = new MealRanker();

        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipVeg) {
                currentFilter = "VEG";
            } else if (checkedId == R.id.chipHighProtein) {
                currentFilter = "HIGH_PROTEIN";
            } else if (checkedId == R.id.chipLowBudget) {
                currentFilter = "LOW_BUDGET";
            } else {
                currentFilter = "ALL";
            }
            applyFilters();
        });

        loadRecommendations();
        return v;
    }

    private void loadRecommendations() {
        if (getContext() == null) return;
        pbLoading.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<FoodItemEntity> candidatePool = db.foodItemDao().getAllFoodsSync();
            UserProfileEntity profile = db.userDao().getUserProfileSync(userId);
            List<UCB1StateEntity> armEntities = db.ucb1Dao().getAllArmsSync(userId);
            int totalSelections = db.ucb1Dao().getTotalSelections(userId);
            List<LoggedMealEntity> recentMeals = db.loggedMealDao().getAllMealsSync(userId);

            Map<String, UCB1StateEntity> armMap = new HashMap<>();
            for (UCB1StateEntity e : armEntities) {
                armMap.put(e.armId, e);
            }

            // Target per-slot calories (approx total / 4)
            double targetSlotCalories = (profile != null && profile.targetCalories > 0)
                    ? (profile.targetCalories / 4.0) : 500.0;

            allRankedMeals = mealRanker.rankMeals(
                    candidatePool, profile, armMap, totalSelections, recentMeals, targetSlotCalories
            );

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    applyFilters();
                });
            }
        });
    }

    private void applyFilters() {
        if (allRankedMeals == null || allRankedMeals.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            adapter.setMeals(null);
            return;
        }

        List<MealRanker.RankedMeal> filtered = new ArrayList<>();
        for (MealRanker.RankedMeal m : allRankedMeals) {
            FoodItemEntity food = m.getFood();
            if ("VEG".equals(currentFilter)) {
                if ("VEGETARIAN".equalsIgnoreCase(food.dietaryType) || "VEGAN".equalsIgnoreCase(food.dietaryType)) {
                    filtered.add(m);
                }
            } else if ("HIGH_PROTEIN".equals(currentFilter)) {
                if (food.proteinPer100g >= 10.0) {
                    filtered.add(m);
                }
            } else if ("LOW_BUDGET".equals(currentFilter)) {
                if (food.estimatedCostInr <= 50.0) {
                    filtered.add(m);
                }
            } else {
                filtered.add(m);
            }
        }

        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.setMeals(filtered);
    }

    @Override
    public void onLike(MealRanker.RankedMeal meal) {
        updateBanditReward(meal.getFood().id, 1.0);
        Toast.makeText(getContext(), "Liked! UCB1 bandit updated (+1.0 reward)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDislike(MealRanker.RankedMeal meal) {
        updateBanditReward(meal.getFood().id, -1.0);
        Toast.makeText(getContext(), "Disliked! UCB1 bandit updated (-1.0 reward)", Toast.LENGTH_SHORT).show();
    }

    private void updateBanditReward(String foodId, double reward) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            UCB1StateEntity state = db.ucb1Dao().getArmState(foodId, userId);
            long now = System.currentTimeMillis();
            if (state == null) {
                state = new UCB1StateEntity(foodId, userId, 1, reward, reward, now, 1);
            } else {
                int newSelections = state.selections + 1;
                double newTotalReward = state.totalReward + reward;
                double newMeanReward = newTotalReward / newSelections;
                state = new UCB1StateEntity(foodId, userId, newSelections, newTotalReward, newMeanReward, now, 2);
            }
            db.ucb1Dao().insertOrUpdate(state);
            FirebaseManager.getInstance(getContext()).syncUcbStateToCloud(state, null);
        });
    }

    @Override
    public void onAddToPlan(MealRanker.RankedMeal meal) {
        if (getContext() == null) return;
        String[] slots = {"Breakfast", "Lunch", "Snack", "Dinner"};
        new AlertDialog.Builder(getContext())
            .setTitle("Add to Meal Plan")
            .setItems(slots, (dialog, which) -> {
                String chosenSlot = slots[which];
                saveToPlan(meal.getFood(), chosenSlot);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveToPlan(FoodItemEntity food, String slot) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();
        String todayString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            MealPlanEntity plan = db.mealPlanDao().getPlanForDateSync(userId, todayString);
            long now = System.currentTimeMillis();
            if (plan == null) {
                plan = new MealPlanEntity(
                    UUID.randomUUID().toString(), userId, todayString,
                    null, null, null, null,
                    0, 0, 0, 0, 1, now
                );
            }

            if ("Breakfast".equalsIgnoreCase(slot)) plan.breakfastFoodId = food.id;
            else if ("Lunch".equalsIgnoreCase(slot)) plan.lunchFoodId = food.id;
            else if ("Snack".equalsIgnoreCase(slot)) plan.snackFoodId = food.id;
            else if ("Dinner".equalsIgnoreCase(slot)) plan.dinnerFoodId = food.id;

            db.mealPlanDao().insertOrUpdate(plan);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), food.name + " added to " + slot + " plan!", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
