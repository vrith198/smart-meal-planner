package com.smartmealplanner.ai.ui.history;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.LoggedMealEntity;
import com.smartmealplanner.ai.firebase.FirebaseManager;

import java.util.Locale;

public class MealHistoryActivity extends AppCompatActivity implements LoggedMealAdapter.OnMealClickListener {

    private RecyclerView rvHistory;
    private LoggedMealAdapter adapter;
    private TextView tvSummary, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rvMealHistory);
        tvSummary = findViewById(R.id.tvHistorySummary);
        tvEmpty = findViewById(R.id.tvEmptyHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LoggedMealAdapter();
        adapter.setListener(this);
        rvHistory.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        String userId = FirebaseManager.getInstance(this).getCurrentUserId();
        AppDatabase.getInstance(this).loggedMealDao().getAllMealsLive(userId).observe(this, meals -> {
            if (meals == null || meals.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);
                tvSummary.setText("No meals logged yet.");
                adapter.setMeals(null);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvHistory.setVisibility(View.VISIBLE);
                adapter.setMeals(meals);

                double totalCalories = 0;
                for (LoggedMealEntity m : meals) {
                    totalCalories += m.calories;
                }
                tvSummary.setText(String.format(Locale.US, "Logged: %d meals • Total: %.0f kcal recorded",
                        meals.size(), totalCalories));
            }
        });
    }

    @Override
    public void onMealClick(LoggedMealEntity meal) {
        Toast.makeText(this, meal.foodName + " (" + meal.servingWeightGrams + "g)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMealLongClick(LoggedMealEntity meal) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Meal Log")
                .setMessage("Are you sure you want to delete " + meal.foodName + " from your meal history?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getInstance(MealHistoryActivity.this).loggedMealDao().delete(meal);
                    });
                    Toast.makeText(this, "Meal removed from history.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
