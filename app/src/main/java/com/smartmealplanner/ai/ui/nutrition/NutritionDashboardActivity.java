package com.smartmealplanner.ai.ui.nutrition;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.LoggedMealEntity;
import com.smartmealplanner.ai.firebase.FirebaseManager;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NutritionDashboardActivity extends AppCompatActivity {

    private TextView tvAvgCalories, tvAvgProtein, tvAvgCarbs, tvAvgFat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition_dashboard);

        MaterialToolbar toolbar = findViewById(R.id.toolbarNutrition);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvAvgCalories = findViewById(R.id.tvAvgCalories);
        tvAvgProtein = findViewById(R.id.tvAvgProtein);
        tvAvgCarbs = findViewById(R.id.tvAvgCarbs);
        tvAvgFat = findViewById(R.id.tvAvgFat);

        calculateAverages();
    }

    private void calculateAverages() {
        String userId = FirebaseManager.getInstance(this).getCurrentUserId();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LoggedMealEntity> meals = AppDatabase.getInstance(this).loggedMealDao().getAllMealsSync(userId);
            if (meals == null || meals.isEmpty()) {
                runOnUiThread(() -> {
                    tvAvgCalories.setText("0");
                    tvAvgProtein.setText("0g");
                    tvAvgCarbs.setText("0g");
                    tvAvgFat.setText("0g");
                });
                return;
            }

            Set<String> distinctDays = new HashSet<>();
            double totalCal = 0, totalProt = 0, totalCarb = 0, totalFat = 0;

            for (LoggedMealEntity m : meals) {
                if (m.dateString != null) distinctDays.add(m.dateString);
                totalCal += m.calories;
                totalProt += m.proteinGrams;
                totalCarb += m.carbsGrams;
                totalFat += m.fatGrams;
            }

            int dayCount = Math.max(1, distinctDays.size());
            double avgCal = totalCal / dayCount;
            double avgProt = totalProt / dayCount;
            double avgCarb = totalCarb / dayCount;
            double avgFat = totalFat / dayCount;

            runOnUiThread(() -> {
                tvAvgCalories.setText(String.format(Locale.US, "%.0f", avgCal));
                tvAvgProtein.setText(String.format(Locale.US, "%.0fg", avgProt));
                tvAvgCarbs.setText(String.format(Locale.US, "%.0fg", avgCarb));
                tvAvgFat.setText(String.format(Locale.US, "%.0fg", avgFat));
            });
        });
    }
}
