package com.smartmealplanner.ai.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.LoggedMealEntity;
import com.smartmealplanner.ai.database.entity.UserProfileEntity;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.ui.MainActivity;
import com.smartmealplanner.ai.ui.history.LoggedMealAdapter;
import com.smartmealplanner.ai.ui.history.MealHistoryActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView tvGreeting, tvCurrentDate, tvCaloriesConsumed, tvCaloriesTarget;
    private TextView tvProteinProgress, tvCarbsProgress, tvFatProgress, tvNoMeals;
    private ProgressBar pbCalories, pbProtein, pbCarbs, pbFat;
    private RecyclerView rvTodayMeals;
    private LoggedMealAdapter mealAdapter;

    private double targetCalories = 2000.0;
    private double targetProtein = 100.0;
    private double targetCarbs = 250.0;
    private double targetFat = 65.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvGreeting = v.findViewById(R.id.tvDashboardGreeting);
        tvCurrentDate = v.findViewById(R.id.tvCurrentDate);
        tvCaloriesConsumed = v.findViewById(R.id.tvCaloriesConsumed);
        tvCaloriesTarget = v.findViewById(R.id.tvCaloriesTarget);
        tvProteinProgress = v.findViewById(R.id.tvProteinProgress);
        tvCarbsProgress = v.findViewById(R.id.tvCarbsProgress);
        tvFatProgress = v.findViewById(R.id.tvFatProgress);
        tvNoMeals = v.findViewById(R.id.tvNoMealsLogged);

        pbCalories = v.findViewById(R.id.pbCalories);
        pbProtein = v.findViewById(R.id.pbProtein);
        pbCarbs = v.findViewById(R.id.pbCarbs);
        pbFat = v.findViewById(R.id.pbFat);

        rvTodayMeals = v.findViewById(R.id.rvTodayMeals);
        rvTodayMeals.setLayoutManager(new LinearLayoutManager(getContext()));
        mealAdapter = new LoggedMealAdapter();
        rvTodayMeals.setAdapter(mealAdapter);

        // Date format
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault());
        tvCurrentDate.setText(sdf.format(new Date()));

        // Greeting click opens Profile
        tvGreeting.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openProfile();
            }
        });

        // Quick Actions
        MaterialCardView cardScan = v.findViewById(R.id.cardQuickScan);
        MaterialCardView cardPlanner = v.findViewById(R.id.cardQuickPlanner);
        MaterialCardView cardHistory = v.findViewById(R.id.cardQuickHistory);
        MaterialCardView cardGrocery = v.findViewById(R.id.cardQuickGrocery);

        cardScan.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.navigation_scan);
            }
        });

        cardPlanner.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.navigation_planner);
            }
        });

        cardHistory.setOnClickListener(view -> {
            startActivity(new Intent(getActivity(), MealHistoryActivity.class));
        });

        cardGrocery.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.navigation_grocery);
            }
        });

        observeData();
        return v;
    }

    private void observeData() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();
        String todayString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        // Observe User Profile for targets
        db.userDao().getUserProfileLive(userId).observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                if (profile.fullName != null && !profile.fullName.isEmpty()) {
                    tvGreeting.setText(String.format("Hello, %s 👋", profile.fullName));
                }
                if (profile.targetCalories > 0) {
                    targetCalories = profile.targetCalories;
                    targetProtein = profile.targetProteinGrams;
                    targetCarbs = profile.targetCarbsGrams;
                    targetFat = profile.targetFatGrams;

                    tvCaloriesTarget.setText(String.format(Locale.US, "/ %.0f kcal target", targetCalories));
                    pbCalories.setMax((int) targetCalories);
                    pbProtein.setMax((int) targetProtein);
                    pbCarbs.setMax((int) targetCarbs);
                    pbFat.setMax((int) targetFat);
                }
            }
        });

        // Observe Today's Logged Meals
        db.loggedMealDao().getMealsForDateLive(userId, todayString).observe(getViewLifecycleOwner(), meals -> {
            updateNutritionProgress(meals);
        });
    }

    private void updateNutritionProgress(List<LoggedMealEntity> meals) {
        if (meals == null || meals.isEmpty()) {
            tvNoMeals.setVisibility(View.VISIBLE);
            rvTodayMeals.setVisibility(View.GONE);
            tvCaloriesConsumed.setText("0");
            pbCalories.setProgress(0);
            tvProteinProgress.setText(String.format(Locale.US, "0g / %.0fg", targetProtein));
            tvCarbsProgress.setText(String.format(Locale.US, "0g / %.0fg", targetCarbs));
            tvFatProgress.setText(String.format(Locale.US, "0g / %.0fg", targetFat));
            pbProtein.setProgress(0);
            pbCarbs.setProgress(0);
            pbFat.setProgress(0);
            mealAdapter.setMeals(null);
            return;
        }

        tvNoMeals.setVisibility(View.GONE);
        rvTodayMeals.setVisibility(View.VISIBLE);
        mealAdapter.setMeals(meals);

        double totalCal = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;

        for (LoggedMealEntity m : meals) {
            totalCal += m.calories;
            totalProtein += m.proteinGrams;
            totalCarbs += m.carbsGrams;
            totalFat += m.fatGrams;
        }

        tvCaloriesConsumed.setText(String.format(Locale.US, "%.0f", totalCal));
        pbCalories.setProgress((int) totalCal);

        tvProteinProgress.setText(String.format(Locale.US, "%.0fg / %.0fg", totalProtein, targetProtein));
        pbProtein.setProgress((int) totalProtein);

        tvCarbsProgress.setText(String.format(Locale.US, "%.0fg / %.0fg", totalCarbs, targetCarbs));
        pbCarbs.setProgress((int) totalCarbs);

        tvFatProgress.setText(String.format(Locale.US, "%.0fg / %.0fg", totalFat, targetFat));
        pbFat.setProgress((int) totalFat);
    }
}
