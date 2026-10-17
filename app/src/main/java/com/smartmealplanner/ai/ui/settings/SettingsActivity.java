package com.smartmealplanner.ai.ui.settings;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.ui.auth.AuthActivity;
import com.smartmealplanner.ai.worker.SyncWorker;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvSyncDetail;
    private MaterialButton btnForceSync, btnSignOut, btnDeleteAccount;
    private SwitchMaterial switchDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvSyncDetail = findViewById(R.id.tvSyncStatusDetail);
        btnForceSync = findViewById(R.id.btnForceSync);
        btnSignOut = findViewById(R.id.btnSignOut);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccountData);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        // Check current dark mode
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switchDarkMode.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppCompatDelegate.setDefaultNightMode(
                isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        btnForceSync.setOnClickListener(v -> handleForceSync());
        btnSignOut.setOnClickListener(v -> handleSignOut());
        btnDeleteAccount.setOnClickListener(v -> handleDeleteAccountData());

        updateSyncStatus();
    }

    private void updateSyncStatus() {
        FirebaseManager fm = FirebaseManager.getInstance(this);
        boolean online = fm.isNetworkConnected(this);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int pendingMeals = AppDatabase.getInstance(this).loggedMealDao().getPendingSyncMeals().size();
            int pendingGrocery = AppDatabase.getInstance(this).groceryDao().getPendingSyncItems().size();
            int totalPending = pendingMeals + pendingGrocery;

            runOnUiThread(() -> {
                String networkState = online ? "Connected Online" : "Offline Mode (Local Room DB active)";
                tvSyncDetail.setText(networkState + "\n" + totalPending + " changes pending synchronization.");
            });
        });
    }

    private void handleForceSync() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(this).enqueue(work);
        Toast.makeText(this, "Triggered Cloud Sync WorkManager task", Toast.LENGTH_SHORT).show();
        updateSyncStatus();
    }

    private void handleSignOut() {
        FirebaseManager.getInstance(this).signOut();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleDeleteAccountData() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Account Data?")
                .setMessage("This will erase all locally stored meal logs, plans, and preferences. This action cannot be undone.")
                .setPositiveButton("Erase Data", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        String userId = FirebaseManager.getInstance(this).getCurrentUserId();
                        AppDatabase.getInstance(this).clearAllTables();
                        AppDatabase.prePopulateDatabase(getApplicationContext());
                    });
                    Toast.makeText(this, "Account data erased.", Toast.LENGTH_SHORT).show();
                    handleSignOut();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
