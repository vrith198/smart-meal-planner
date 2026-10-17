package com.smartmealplanner.ai.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.ui.dashboard.DashboardFragment;
import com.smartmealplanner.ai.ui.grocery.GroceryFragment;
import com.smartmealplanner.ai.ui.history.MealHistoryActivity;
import com.smartmealplanner.ai.ui.planner.MealPlannerFragment;
import com.smartmealplanner.ai.ui.profile.ProfileFragment;
import com.smartmealplanner.ai.ui.recommendation.RecommendationsFragment;
import com.smartmealplanner.ai.ui.scan.ScanMealFragment;
import com.smartmealplanner.ai.ui.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ImageView ivSyncStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        ivSyncStatus = findViewById(R.id.ivSyncStatus);
        ImageView btnHistory = findViewById(R.id.btnHistoryMenu);
        ImageView btnSettings = findViewById(R.id.btnSettingsMenu);

        // Header toolbar actions
        btnHistory.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MealHistoryActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        // Network sync indicator
        updateSyncStatus();

        // Bottom Navigation listener
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.navigation_recommendations) {
                selectedFragment = new RecommendationsFragment();
            } else if (itemId == R.id.navigation_scan) {
                selectedFragment = new ScanMealFragment();
            } else if (itemId == R.id.navigation_planner) {
                selectedFragment = new MealPlannerFragment();
            } else if (itemId == R.id.navigation_grocery) {
                selectedFragment = new GroceryFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSyncStatus();
    }

    private void updateSyncStatus() {
        boolean isConnected = FirebaseManager.getInstance(this).isNetworkConnected(this);
        if (isConnected) {
            ivSyncStatus.setColorFilter(getColor(R.color.sync_done));
        } else {
            ivSyncStatus.setColorFilter(getColor(R.color.sync_pending));
        }
    }

    /**
     * Programmatic navigation between tabs (e.g. from Dashboard quick action cards).
     */
    public void navigateToTab(int navItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(navItemId);
        }
    }
}
