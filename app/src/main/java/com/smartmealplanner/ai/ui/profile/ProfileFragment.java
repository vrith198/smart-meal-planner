package com.smartmealplanner.ai.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.UserProfileEntity;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.model.BudgetTier;
import com.smartmealplanner.ai.model.DietaryType;
import com.smartmealplanner.ai.model.FitnessGoal;
import com.smartmealplanner.ai.model.NutritionSummary;
import com.smartmealplanner.ai.nutrition.NutritionCalculator;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView tvTargetCalories, tvTargetMacros, tvBmrTdee;
    private TextInputEditText etAge, etHeight, etWeight;
    private Spinner spinnerDiet, spinnerGoal, spinnerBudget, spinnerActivity;
    private MaterialButton btnSaveProfile;

    private UserProfileEntity currentProfile;
    private double calculatedTargetCal = 2000.0;
    private NutritionSummary calculatedTargetMacros;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        tvTargetCalories = v.findViewById(R.id.tvTargetCaloriesDisplay);
        tvTargetMacros = v.findViewById(R.id.tvTargetMacrosDisplay);
        tvBmrTdee = v.findViewById(R.id.tvBmrTdeeDisplay);

        etAge = v.findViewById(R.id.etProfileAge);
        etHeight = v.findViewById(R.id.etProfileHeight);
        etWeight = v.findViewById(R.id.etProfileWeight);

        spinnerDiet = v.findViewById(R.id.spinnerDietaryPreference);
        spinnerGoal = v.findViewById(R.id.spinnerFitnessGoal);
        spinnerBudget = v.findViewById(R.id.spinnerBudgetTier);
        spinnerActivity = v.findViewById(R.id.spinnerActivityLevel);
        btnSaveProfile = v.findViewById(R.id.btnSaveProfile);

        setupSpinners();
        setupListeners();
        loadExistingProfile();

        btnSaveProfile.setOnClickListener(view -> handleSaveProfile());
        return v;
    }

    private void setupSpinners() {
        if (getContext() == null) return;

        // Dietary
        String[] diets = {"Vegetarian", "Non-Vegetarian", "Vegan", "Eggetarian"};
        spinnerDiet.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, diets));

        // Goals
        String[] goals = {"Weight Loss", "Maintenance", "Muscle Gain"};
        spinnerGoal.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, goals));

        // Budget
        String[] budgets = {"Low Budget (< ₹150/day)", "Moderate (₹150 - ₹350/day)", "Premium (> ₹350/day)"};
        spinnerBudget.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, budgets));

        // Activity
        String[] activities = {"Sedentary", "Moderate", "Active"};
        spinnerActivity.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, activities));
    }

    private void setupListeners() {
        TextWatcher recalculateWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateTargets();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etAge.addTextChangedListener(recalculateWatcher);
        etHeight.addTextChangedListener(recalculateWatcher);
        etWeight.addTextChangedListener(recalculateWatcher);

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                recalculateTargets();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerGoal.setOnItemSelectedListener(spinnerListener);
        spinnerActivity.setOnItemSelectedListener(spinnerListener);
    }

    private void loadExistingProfile() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            currentProfile = db.userDao().getUserProfileSync(userId);
            if (currentProfile == null) {
                currentProfile = db.userDao().getUserProfileSync("guest_user");
            }
            if (getActivity() != null && currentProfile != null) {
                getActivity().runOnUiThread(this::populateUI);
            }
        });
    }

    private void populateUI() {
        if (currentProfile == null) return;

        etAge.setText(String.valueOf(currentProfile.age > 0 ? currentProfile.age : 25));
        etHeight.setText(String.format(Locale.US, "%.0f", currentProfile.heightCm > 0 ? currentProfile.heightCm : 172.0));
        etWeight.setText(String.format(Locale.US, "%.1f", currentProfile.weightKg > 0 ? currentProfile.weightKg : 68.0));

        // Select Diet
        if (currentProfile.dietaryType != null) {
            if (currentProfile.dietaryType.contains("NON")) spinnerDiet.setSelection(1);
            else if (currentProfile.dietaryType.contains("VEGAN")) spinnerDiet.setSelection(2);
            else if (currentProfile.dietaryType.contains("EGG")) spinnerDiet.setSelection(3);
            else spinnerDiet.setSelection(0);
        }

        // Select Goal
        if (currentProfile.fitnessGoal != null) {
            if (currentProfile.fitnessGoal.contains("LOSS")) spinnerGoal.setSelection(0);
            else if (currentProfile.fitnessGoal.contains("MUSCLE") || currentProfile.fitnessGoal.contains("GAIN")) spinnerGoal.setSelection(2);
            else spinnerGoal.setSelection(1);
        }

        // Select Budget
        if (currentProfile.budgetTier != null) {
            if (currentProfile.budgetTier.contains("LOW")) spinnerBudget.setSelection(0);
            else if (currentProfile.budgetTier.contains("HIGH") || currentProfile.budgetTier.contains("PREMIUM")) spinnerBudget.setSelection(2);
            else spinnerBudget.setSelection(1);
        }

        // Select Activity
        if (currentProfile.activityLevel != null) {
            if (currentProfile.activityLevel.contains("Sedentary")) spinnerActivity.setSelection(0);
            else if (currentProfile.activityLevel.contains("Active")) spinnerActivity.setSelection(2);
            else spinnerActivity.setSelection(1);
        }

        recalculateTargets();
    }

    private void recalculateTargets() {
        int age = 25;
        double height = 172.0;
        double weight = 68.0;

        try {
            if (etAge.getText() != null && etAge.getText().length() > 0) age = Integer.parseInt(etAge.getText().toString());
            if (etHeight.getText() != null && etHeight.getText().length() > 0) height = Double.parseDouble(etHeight.getText().toString());
            if (etWeight.getText() != null && etWeight.getText().length() > 0) weight = Double.parseDouble(etWeight.getText().toString());
        } catch (Exception ignored) {}

        String activity = spinnerActivity.getSelectedItem() != null ? spinnerActivity.getSelectedItem().toString() : "Moderate";
        String goalStr = spinnerGoal.getSelectedItem() != null ? spinnerGoal.getSelectedItem().toString() : "Maintenance";
        FitnessGoal goal = FitnessGoal.fromString(goalStr);

        // 1. Calculate BMR & TDEE
        double bmr = NutritionCalculator.calculateBmr(weight, height, age, true);
        double tdee = NutritionCalculator.calculateTdee(bmr, activity);

        // 2. Calculate Target Calories & Macros
        calculatedTargetCal = NutritionCalculator.calculateTargetCalories(tdee, goal);
        calculatedTargetMacros = NutritionCalculator.calculateTargetMacros(calculatedTargetCal, goal);

        tvTargetCalories.setText(String.format(Locale.US, "%.0f", calculatedTargetCal));
        tvTargetMacros.setText(String.format(Locale.US, "Protein: %.0fg • Carbs: %.0fg • Fat: %.0fg",
                calculatedTargetMacros.getProteinGrams(),
                calculatedTargetMacros.getCarbsGrams(),
                calculatedTargetMacros.getFatGrams()));
        tvBmrTdee.setText(String.format(Locale.US, "BMR: %.0f kcal • TDEE: %.0f kcal", bmr, tdee));
    }

    private void handleSaveProfile() {
        if (getContext() == null) return;
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();

        int age = 25;
        double height = 172.0;
        double weight = 68.0;

        try {
            if (etAge.getText() != null) age = Integer.parseInt(etAge.getText().toString());
            if (etHeight.getText() != null) height = Double.parseDouble(etHeight.getText().toString());
            if (etWeight.getText() != null) weight = Double.parseDouble(etWeight.getText().toString());
        } catch (Exception ignored) {}

        String diet = spinnerDiet.getSelectedItem().toString().toUpperCase().replace("-", "_").replace(" ", "_");
        String goal = spinnerGoal.getSelectedItem().toString().toUpperCase().replace(" ", "_");
        String budget = spinnerBudget.getSelectedItemPosition() == 0 ? "LOW" :
                (spinnerBudget.getSelectedItemPosition() == 2 ? "HIGH" : "MEDIUM");
        String activity = spinnerActivity.getSelectedItem().toString();

        long now = System.currentTimeMillis();
        UserProfileEntity updated = new UserProfileEntity(
                userId,
                currentProfile != null && currentProfile.fullName != null ? currentProfile.fullName : "User",
                currentProfile != null && currentProfile.email != null ? currentProfile.email : "user@smartmealplanner.ai",
                age, height, weight, true, activity, diet, goal, budget, "Indian",
                calculatedTargetCal,
                calculatedTargetMacros.getProteinGrams(),
                calculatedTargetMacros.getCarbsGrams(),
                calculatedTargetMacros.getFatGrams(),
                "[]", 2, now
        );

        AppDatabase db = AppDatabase.getInstance(getContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.userDao().insertOrUpdate(updated);
            FirebaseManager.getInstance(getContext()).syncProfileToCloud(updated, new FirebaseManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            db.userDao().markAsSynced(updated.userId);
                        } catch (Exception ignored) {}
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.w("ProfileFragment", "Profile sync deferred: " + e.getMessage());
                }
            });

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Profile and nutrition targets saved & syncing to cloud!", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
