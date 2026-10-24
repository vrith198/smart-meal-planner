package com.smartmealplanner.ai.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.UserProfileEntity;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.ui.MainActivity;

public class AuthActivity extends AppCompatActivity {

    private boolean isRegisterMode = false;
    private FirebaseManager firebaseManager;

    private TextView tvAuthTitle, tvAuthSubtitle, tvErrorMessage, btnForgotPassword;
    private TextInputLayout tilName, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnSubmitAuth, btnGuestMode;
    private MaterialButtonToggleGroup toggleAuthMode;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        firebaseManager = FirebaseManager.getInstance(this);

        tvAuthTitle = findViewById(R.id.tvAuthTitle);
        tvAuthSubtitle = findViewById(R.id.tvAuthSubtitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);

        tilName = findViewById(R.id.tilName);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnSubmitAuth = findViewById(R.id.btnSubmitAuth);
        btnGuestMode = findViewById(R.id.btnGuestMode);
        toggleAuthMode = findViewById(R.id.toggleAuthMode);
        progressBar = findViewById(R.id.progressBarAuth);

        toggleAuthMode.check(R.id.btnToggleLogin);
        toggleAuthMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                setRegisterMode(checkedId == R.id.btnToggleRegister);
            }
        });

        btnSubmitAuth.setOnClickListener(v -> handleSubmitAuth());
        btnForgotPassword.setOnClickListener(v -> handleForgotPassword());
        btnGuestMode.setOnClickListener(v -> handleContinueAsGuest());
    }

    private void setRegisterMode(boolean register) {
        isRegisterMode = register;
        tvErrorMessage.setVisibility(View.GONE);
        if (isRegisterMode) {
            tvAuthTitle.setText(R.string.register_title);
            tvAuthSubtitle.setText(R.string.register_subtitle);
            tilName.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
            btnSubmitAuth.setText(R.string.btn_register);
            btnForgotPassword.setVisibility(View.GONE);
        } else {
            tvAuthTitle.setText(R.string.login_title);
            tvAuthSubtitle.setText(R.string.login_subtitle);
            tilName.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
            btnSubmitAuth.setText(R.string.btn_login);
            btnForgotPassword.setVisibility(View.VISIBLE);
        }
    }

    private void handleSubmitAuth() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError(getString(R.string.auth_error_empty));
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.auth_error_invalid_email));
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (isRegisterMode) {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(name)) {
                showError("Please enter your name.");
                return;
            }

            if (!password.equals(confirmPass)) {
                showError(getString(R.string.auth_error_password_match));
                return;
            }

            setLoading(true);
            firebaseManager.register(email, password, name, new FirebaseManager.AuthCallback() {
                @Override
                public void onSuccess(String userId, String userEmail) {
                    setLoading(false);
                    // Create user profile in Room
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        UserProfileEntity profile = new UserProfileEntity(
                            userId, name, userEmail, 25, 170.0, 65.0, true, "Moderate",
                            "VEGETARIAN", "MAINTENANCE", "MEDIUM", "North Indian",
                            2000.0, 100.0, 250.0, 65.0, "[]", 0, System.currentTimeMillis()
                        );
                        AppDatabase.getInstance(AuthActivity.this).userDao().insertOrUpdate(profile);
                    });
                    startActivity(new Intent(AuthActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    if (isPlaceholderApiKeyError(errorMessage)) {
                        Toast.makeText(AuthActivity.this, "Operating in local offline mode", Toast.LENGTH_SHORT).show();
                        loginLocally(email, name);
                        return;
                    }
                    showError(errorMessage);
                }
            });

        } else {
            setLoading(true);
            firebaseManager.signIn(email, password, new FirebaseManager.AuthCallback() {
                @Override
                public void onSuccess(String userId, String userEmail) {
                    setLoading(false);
                    startActivity(new Intent(AuthActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    if (isPlaceholderApiKeyError(errorMessage)) {
                        Toast.makeText(AuthActivity.this, "Operating in local offline mode", Toast.LENGTH_SHORT).show();
                        String fallbackName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
                        loginLocally(email, fallbackName);
                        return;
                    }
                    showError(errorMessage);
                }
            });
        }
    }

    private boolean isPlaceholderApiKeyError(String errorMessage) {
        if (errorMessage == null) return false;
        String lower = errorMessage.toLowerCase();
        return lower.contains("api key") || lower.contains("apikey") || lower.contains("internal error");
    }

    private void loginLocally(String email, String displayName) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserProfileEntity profile = AppDatabase.getInstance(AuthActivity.this).userDao().getUserProfileSync();
            if (profile == null) {
                profile = new UserProfileEntity(
                    "local_" + System.currentTimeMillis(),
                    displayName != null && !displayName.isEmpty() ? displayName : "Guest User",
                    email != null && !email.isEmpty() ? email : "guest@smartmealplanner.local",
                    25, 170.0, 65.0, true, "Moderate",
                    "VEGETARIAN", "MAINTENANCE", "MEDIUM", "North Indian",
                    2000.0, 100.0, 250.0, 65.0, "[]", 0, System.currentTimeMillis()
                );
                AppDatabase.getInstance(AuthActivity.this).userDao().insertOrUpdate(profile);
            }
        });
        startActivity(new Intent(AuthActivity.this, MainActivity.class));
        finish();
    }

    private void handleForgotPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (TextUtils.isEmpty(email)) {
            showError("Enter your email address to reset password.");
            return;
        }
        setLoading(true);
        firebaseManager.sendPasswordReset(email, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String userId, String userEmail) {
                setLoading(false);
                Toast.makeText(AuthActivity.this, R.string.password_reset_sent, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    private void handleContinueAsGuest() {
        Toast.makeText(this, "Operating in local offline guest mode", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(AuthActivity.this, MainActivity.class));
        finish();
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitAuth.setEnabled(!loading);
        btnGuestMode.setEnabled(!loading);
        tvErrorMessage.setVisibility(View.GONE);
    }
}
