package com.smartmealplanner.ai.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.ui.auth.AuthActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseManager firebaseManager = FirebaseManager.getInstance(this);
            // If already logged in, go straight to MainActivity; otherwise AuthActivity
            if (firebaseManager.isUserLoggedIn()) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            }
            finish();
        }, 1500);
    }
}
