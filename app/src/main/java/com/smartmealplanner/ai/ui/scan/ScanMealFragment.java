package com.smartmealplanner.ai.ui.scan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.FoodItemEntity;
import com.smartmealplanner.ai.ml.FoodClassifier;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartmealplanner.ai.agent.GeminiVisionAgent;
import com.smartmealplanner.ai.model.PredictionResult;
import com.smartmealplanner.ai.ui.analysis.MealAnalysisActivity;

import android.widget.EditText;
import android.widget.FrameLayout;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ScanMealFragment extends Fragment {

    private ImageView ivPreview;
    private View layoutEmptyPreview;
    private ProgressBar pbInference;
    private LinearLayout layoutPredictions;

    // AI Calorie Estimation Card Views
    private View cardAiCalorieEstimate;
    private TextView tvAiSourceBadge;
    private TextView tvAiDetectedFoodName;
    private TextView tvAiConfidenceBadge;
    private TextView tvAiEstimatedCalories;
    private TextView tvAiServingDesc;
    private TextView tvAiMacroProtein, tvAiMacroCarbs, tvAiMacroFat;
    private TextView tvAiIngredients, tvAiHealthNotes;
    private MaterialButton btnQuickLogMeal;

    // Gemini Multimodal Vision Agent Views
    private View layoutGeminiAgentRow;
    private MaterialButton btnAnalyzeWithGemini;
    private MaterialButton btnGeminiKeySettings;
    private View layoutGeminiLoading;
    private TextView tvGeminiLoadingStatus;

    private TextView tvPred1Name, tvPred1Conf;
    private ProgressBar pbPred1;
    private TextView tvPred2Name, tvPred2Conf;
    private ProgressBar pbPred2;
    private TextView tvPred3Name, tvPred3Conf;
    private ProgressBar pbPred3;

    private TextView tvLowConfidenceWarning;
    private Spinner spinnerCorrection;
    private MaterialButton btnConfirmFood;

    private FoodClassifier foodClassifier;
    private GeminiVisionAgent geminiVisionAgent;
    private Bitmap currentBitmap;
    private List<FoodItemEntity> allCatalogFoods = new ArrayList<>();
    private List<PredictionResult> currentPredictions = new ArrayList<>();

    // Activity Result Launchers
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                            "Camera permission is required to capture meal photos. You can also pick from gallery or tap sample foods below.",
                            Toast.LENGTH_LONG).show();
                    }
                }
            }
        );

        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    processCapturedImage(bitmap);
                }
            }
        );

        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && getContext() != null) {
                    try {
                        Bitmap bitmap = decodeUriToBitmap(uri);
                        if (bitmap != null) {
                            processCapturedImage(bitmap);
                        } else {
                            Toast.makeText(getContext(), "Could not decode selected image file. Please try another image.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Failed to load selected photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    private void checkAndLaunchCamera() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            cameraLauncher.launch(null);
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Could not open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadSampleFood(String assetFileName) {
        if (getContext() == null) return;
        try (InputStream is = getContext().getAssets().open("sample_foods/" + assetFileName)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                processCapturedImage(bitmap);
            } else {
                Toast.makeText(getContext(), "Could not load sample meal image.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to load sample: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeUriToBitmap(Uri uri) {
        if (getContext() == null || uri == null) return null;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.Source source = 
                    android.graphics.ImageDecoder.createSource(getContext().getContentResolver(), uri);
                return android.graphics.ImageDecoder.decodeBitmap(source, (decoder, info, s) -> {
                    decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                    int maxDim = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                    if (maxDim > 1024) {
                        decoder.setTargetSampleSize(Math.max(1, maxDim / 1024));
                    }
                });
            } else {
                java.io.InputStream input = getContext().getContentResolver().openInputStream(uri);
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bmp = android.graphics.BitmapFactory.decodeStream(input, null, options);
                if (input != null) input.close();
                return bmp;
            }
        } catch (Exception e) {
            try {
                return MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_scan_meal, container, false);

        ivPreview = v.findViewById(R.id.ivScanPreview);
        layoutEmptyPreview = v.findViewById(R.id.layoutEmptyPreview);
        pbInference = v.findViewById(R.id.pbInference);
        layoutPredictions = v.findViewById(R.id.layoutPredictions);

        tvPred1Name = v.findViewById(R.id.tvPred1Name);
        tvPred1Conf = v.findViewById(R.id.tvPred1Conf);
        pbPred1 = v.findViewById(R.id.pbPred1);

        tvPred2Name = v.findViewById(R.id.tvPred2Name);
        tvPred2Conf = v.findViewById(R.id.tvPred2Conf);
        pbPred2 = v.findViewById(R.id.pbPred2);

        tvPred3Name = v.findViewById(R.id.tvPred3Name);
        tvPred3Conf = v.findViewById(R.id.tvPred3Conf);
        pbPred3 = v.findViewById(R.id.pbPred3);

        cardAiCalorieEstimate = v.findViewById(R.id.cardAiCalorieEstimate);
        tvAiSourceBadge = v.findViewById(R.id.tvAiSourceBadge);
        tvAiDetectedFoodName = v.findViewById(R.id.tvAiDetectedFoodName);
        tvAiConfidenceBadge = v.findViewById(R.id.tvAiConfidenceBadge);
        tvAiEstimatedCalories = v.findViewById(R.id.tvAiEstimatedCalories);
        tvAiServingDesc = v.findViewById(R.id.tvAiServingDesc);
        tvAiMacroProtein = v.findViewById(R.id.tvAiMacroProtein);
        tvAiMacroCarbs = v.findViewById(R.id.tvAiMacroCarbs);
        tvAiMacroFat = v.findViewById(R.id.tvAiMacroFat);
        tvAiIngredients = v.findViewById(R.id.tvAiIngredients);
        tvAiHealthNotes = v.findViewById(R.id.tvAiHealthNotes);
        btnQuickLogMeal = v.findViewById(R.id.btnQuickLogMeal);

        layoutGeminiAgentRow = v.findViewById(R.id.layoutGeminiAgentRow);
        btnAnalyzeWithGemini = v.findViewById(R.id.btnAnalyzeWithGemini);
        btnGeminiKeySettings = v.findViewById(R.id.btnGeminiKeySettings);
        layoutGeminiLoading = v.findViewById(R.id.layoutGeminiLoading);
        tvGeminiLoadingStatus = v.findViewById(R.id.tvGeminiLoadingStatus);

        btnAnalyzeWithGemini.setOnClickListener(view -> onAnalyzeWithGeminiClicked());
        btnGeminiKeySettings.setOnClickListener(view -> showApiKeyInputDialog(null));

        tvLowConfidenceWarning = v.findViewById(R.id.tvLowConfidenceWarning);
        spinnerCorrection = v.findViewById(R.id.spinnerFoodCorrection);
        btnConfirmFood = v.findViewById(R.id.btnConfirmFood);

        MaterialButton btnCapture = v.findViewById(R.id.btnCaptureCamera);
        MaterialButton btnGallery = v.findViewById(R.id.btnChooseGallery);

        btnCapture.setOnClickListener(view -> checkAndLaunchCamera());
        btnGallery.setOnClickListener(view -> galleryLauncher.launch("image/*"));

        Chip chipPizza = v.findViewById(R.id.chipSamplePizza);
        Chip chipSalad = v.findViewById(R.id.chipSampleSalad);
        Chip chipFries = v.findViewById(R.id.chipSampleFries);

        if (chipPizza != null) chipPizza.setOnClickListener(view -> loadSampleFood("pizza.jpg"));
        if (chipSalad != null) chipSalad.setOnClickListener(view -> loadSampleFood("caesar_salad.jpg"));
        if (chipFries != null) chipFries.setOnClickListener(view -> loadSampleFood("french_fries.jpg"));

        btnConfirmFood.setOnClickListener(view -> handleConfirmFood());

        // Initialize food classifier and Gemini Vision Agent
        foodClassifier = new FoodClassifier(getContext());
        geminiVisionAgent = new GeminiVisionAgent();

        // Load catalog foods in background for correction spinner
        loadCatalogFoods();

        return v;
    }

    private void loadCatalogFoods() {
        if (getContext() == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            allCatalogFoods = AppDatabase.getInstance(getContext()).foodItemDao().getAllFoodsSync();
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::setupCorrectionSpinner);
            }
        });
    }

    private void setupCorrectionSpinner() {
        if (getContext() == null || allCatalogFoods.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (FoodItemEntity f : allCatalogFoods) {
            names.add(f.name + " (" + FoodClassifier.formatLabel(f.dietaryType) + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, names);
        spinnerCorrection.setAdapter(adapter);
    }

    private void processCapturedImage(Bitmap bitmap) {
        this.currentBitmap = bitmap;
        ivPreview.setImageBitmap(bitmap);
        layoutEmptyPreview.setVisibility(View.GONE);
        pbInference.setVisibility(View.VISIBLE);
        layoutPredictions.setVisibility(View.GONE);
        layoutGeminiLoading.setVisibility(View.GONE);

        // Run local inference in background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            currentPredictions = foodClassifier.recognizeFood(bitmap, 3);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> displayPredictions(currentPredictions));
            }
        });
    }

    private void displayPredictions(List<PredictionResult> predictions) {
        pbInference.setVisibility(View.GONE);
        layoutPredictions.setVisibility(View.VISIBLE);

        if (predictions == null || predictions.isEmpty()) {
            Toast.makeText(getContext(), "No food detected with confidence. Choose from catalog below.", Toast.LENGTH_LONG).show();
            tvLowConfidenceWarning.setVisibility(View.VISIBLE);
            return;
        }

        // Top 1 Primary Detection
        PredictionResult p1 = predictions.get(0);

        // Bind AI Calorie Estimation Card
        if (cardAiCalorieEstimate != null) {
            cardAiCalorieEstimate.setVisibility(View.VISIBLE);
            if (tvAiSourceBadge != null) tvAiSourceBadge.setText("📱 On-Device AI");
            tvAiDetectedFoodName.setText(p1.getDisplayName());
            tvAiConfidenceBadge.setText(p1.getConfidencePercentage() + "% Match");
            tvAiEstimatedCalories.setText("🔥 " + p1.getCalories() + " kcal");
            tvAiServingDesc.setText("(" + p1.getServingUnit() + ")");
            tvAiMacroProtein.setText("Protein: " + p1.getProtein() + "g");
            tvAiMacroCarbs.setText("Carbs: " + p1.getCarbs() + "g");
            tvAiMacroFat.setText("Fat: " + p1.getFat() + "g");

            if (p1.getIngredients() != null && !p1.getIngredients().isEmpty()) {
                if (tvAiIngredients != null) {
                    tvAiIngredients.setText("🥗 Ingredients: " + joinStrings(p1.getIngredients(), ", "));
                    tvAiIngredients.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvAiIngredients != null) tvAiIngredients.setVisibility(View.GONE);
            }

            if (p1.getHealthNotes() != null && !p1.getHealthNotes().isEmpty()) {
                if (tvAiHealthNotes != null) {
                    tvAiHealthNotes.setText("💡 " + p1.getHealthNotes());
                    tvAiHealthNotes.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvAiHealthNotes != null) tvAiHealthNotes.setVisibility(View.GONE);
            }

            if (btnAnalyzeWithGemini != null) btnAnalyzeWithGemini.setEnabled(true);
            if (layoutGeminiLoading != null) layoutGeminiLoading.setVisibility(View.GONE);

            btnQuickLogMeal.setText("⚡ Log " + p1.getDisplayName() + " (" + p1.getCalories() + " kcal)");
            btnQuickLogMeal.setOnClickListener(view -> handleQuickLog(p1));
        }

        tvPred1Name.setText("1. " + p1.getDisplayName() + " (" + p1.getCalories() + " kcal)");
        tvPred1Conf.setText(p1.getConfidencePercentage() + "%");
        pbPred1.setProgress(p1.getConfidencePercentage());

        // Top 2
        if (predictions.size() > 1) {
            PredictionResult p2 = predictions.get(1);
            tvPred2Name.setText("2. " + p2.getDisplayName() + " (" + p2.getCalories() + " kcal)");
            tvPred2Conf.setText(p2.getConfidencePercentage() + "%");
            pbPred2.setProgress(p2.getConfidencePercentage());
        }

        // Top 3
        if (predictions.size() > 2) {
            PredictionResult p3 = predictions.get(2);
            tvPred3Name.setText("3. " + p3.getDisplayName() + " (" + p3.getCalories() + " kcal)");
            tvPred3Conf.setText(p3.getConfidencePercentage() + "%");
            pbPred3.setProgress(p3.getConfidencePercentage());
        }

        // Warning if top confidence < 65%
        if (!p1.isHighConfidence()) {
            tvLowConfidenceWarning.setVisibility(View.VISIBLE);
        } else {
            tvLowConfidenceWarning.setVisibility(View.GONE);
        }

        // Auto-select best match in correction spinner if available
        boolean matched = false;
        for (int i = 0; i < allCatalogFoods.size(); i++) {
            FoodItemEntity food = allCatalogFoods.get(i);
            if (food.classifierLabel != null &&
                food.classifierLabel.equalsIgnoreCase(p1.getLabel())) {
                spinnerCorrection.setSelection(i);
                matched = true;
                break;
            }
        }
        if (!matched) {
            String cleanLabel = p1.getLabel().toLowerCase().replace("_", " ");
            for (int i = 0; i < allCatalogFoods.size(); i++) {
                FoodItemEntity food = allCatalogFoods.get(i);
                if (food.name != null && (food.name.toLowerCase().contains(cleanLabel) || cleanLabel.contains(food.name.toLowerCase()))) {
                    spinnerCorrection.setSelection(i);
                    break;
                }
            }
        }
    }

    private void handleConfirmFood() {
        int selectedIndex = spinnerCorrection.getSelectedItemPosition();
        if (selectedIndex >= 0 && selectedIndex < allCatalogFoods.size()) {
            FoodItemEntity chosenFood = allCatalogFoods.get(selectedIndex);
            Intent intent = new Intent(getActivity(), MealAnalysisActivity.class);
            intent.putExtra("EXTRA_FOOD_ID", chosenFood.id);
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Please select a food item to continue", Toast.LENGTH_SHORT).show();
        }
    }

    private void onAnalyzeWithGeminiClicked() {
        if (currentBitmap == null) {
            Toast.makeText(getContext(), "Please capture or choose a meal photo first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!GeminiVisionAgent.hasApiKey(getContext())) {
            showApiKeyInputDialog(this::triggerGeminiAnalysis);
        } else {
            triggerGeminiAnalysis();
        }
    }

    private void triggerGeminiAnalysis() {
        if (currentBitmap == null || getContext() == null) return;

        if (layoutGeminiLoading != null) {
            layoutGeminiLoading.setVisibility(View.VISIBLE);
        }
        if (btnAnalyzeWithGemini != null) {
            btnAnalyzeWithGemini.setEnabled(false);
        }

        geminiVisionAgent.analyzeMealPhoto(getContext(), currentBitmap, new GeminiVisionAgent.AgentCallback() {
            @Override
            public void onSuccess(PredictionResult result) {
                if (getActivity() == null) return;
                if (layoutGeminiLoading != null) layoutGeminiLoading.setVisibility(View.GONE);
                if (btnAnalyzeWithGemini != null) btnAnalyzeWithGemini.setEnabled(true);
                displayGeminiPrediction(result);
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null) return;
                if (layoutGeminiLoading != null) layoutGeminiLoading.setVisibility(View.GONE);
                if (btnAnalyzeWithGemini != null) btnAnalyzeWithGemini.setEnabled(true);

                if ("API_KEY_REQUIRED".equals(errorMessage)) {
                    showApiKeyInputDialog(ScanMealFragment.this::triggerGeminiAnalysis);
                } else {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void displayGeminiPrediction(PredictionResult p) {
        if (cardAiCalorieEstimate != null) {
            cardAiCalorieEstimate.setVisibility(View.VISIBLE);
            if (tvAiSourceBadge != null) tvAiSourceBadge.setText("✨ Gemini Vision Agent");
            tvAiDetectedFoodName.setText(p.getDisplayName());
            tvAiConfidenceBadge.setText(p.getConfidencePercentage() + "% Match");
            tvAiEstimatedCalories.setText("🔥 " + p.getCalories() + " kcal");
            tvAiServingDesc.setText("(" + p.getServingUnit() + ")");
            tvAiMacroProtein.setText("Protein: " + Math.round(p.getProtein()) + "g");
            tvAiMacroCarbs.setText("Carbs: " + Math.round(p.getCarbs()) + "g");
            tvAiMacroFat.setText("Fat: " + Math.round(p.getFat()) + "g");

            if (p.getIngredients() != null && !p.getIngredients().isEmpty()) {
                if (tvAiIngredients != null) {
                    tvAiIngredients.setText("🥗 Ingredients: " + joinStrings(p.getIngredients(), ", "));
                    tvAiIngredients.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvAiIngredients != null) tvAiIngredients.setVisibility(View.GONE);
            }

            if (p.getHealthNotes() != null && !p.getHealthNotes().isEmpty()) {
                if (tvAiHealthNotes != null) {
                    tvAiHealthNotes.setText("💡 " + p.getHealthNotes());
                    tvAiHealthNotes.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvAiHealthNotes != null) tvAiHealthNotes.setVisibility(View.GONE);
            }

            btnQuickLogMeal.setText("⚡ Log " + p.getDisplayName() + " (" + p.getCalories() + " kcal)");
            btnQuickLogMeal.setOnClickListener(view -> handleQuickLog(p));
        }

        Toast.makeText(getContext(), "✨ Gemini Vision Agent analyzed " + p.getDisplayName() + " (" + p.getCalories() + " kcal)!", Toast.LENGTH_LONG).show();
    }

    private void showApiKeyInputDialog(@Nullable Runnable onKeySaved) {
        if (getContext() == null) return;

        final EditText input = new EditText(getContext());
        input.setSingleLine(true);
        input.setHint("AIzaSy...");
        String existingKey = GeminiVisionAgent.getApiKey(getContext());
        if (existingKey != null && !existingKey.isEmpty()) {
            input.setText(existingKey);
            input.setSelection(existingKey.length());
        }

        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginPx = (int) (20 * getResources().getDisplayMetrics().density);
        params.leftMargin = marginPx;
        params.rightMargin = marginPx;
        params.topMargin = marginPx / 2;
        params.bottomMargin = marginPx / 2;
        input.setLayoutParams(params);
        container.addView(input);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Google Gemini Vision API Key")
                .setMessage("Paste your Gemini API key (starts with AIzaSy...) to analyze custom or restaurant meals via cloud AI.\n\nDon't have a key? Tap 'Get Free Key' to open Google AI Studio in your browser.")
                .setView(container)
                .setPositiveButton("Save & Use", (dialog, which) -> {
                    String entered = input.getText().toString().trim();
                    if (!entered.isEmpty()) {
                        GeminiVisionAgent.setApiKey(getContext(), entered);
                        Toast.makeText(getContext(), "Gemini API Key saved!", Toast.LENGTH_SHORT).show();
                        if (onKeySaved != null) {
                            onKeySaved.run();
                        }
                    } else {
                        Toast.makeText(getContext(), "API Key cannot be empty. Paste your AIzaSy... key or use On-Device AI.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null);

        if (existingKey != null && !existingKey.isEmpty()) {
            builder.setNeutralButton("Clear Key", (dialog, which) -> {
                GeminiVisionAgent.clearApiKey(getContext());
                Toast.makeText(getContext(), "Gemini API Key removed.", Toast.LENGTH_SHORT).show();
            });
        } else {
            builder.setNeutralButton("Get Free Key", (dialog, which) -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/"));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Please open aistudio.google.com in your browser.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        builder.show();
    }

    private String joinStrings(List<String> list, String delimiter) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(delimiter);
        }
        return sb.toString();
    }

    private void handleQuickLog(PredictionResult p) {
        if (getContext() == null || p == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                String userId = com.smartmealplanner.ai.firebase.FirebaseManager.getInstance(requireContext()).getCurrentUserId();
                if (userId == null || userId.isEmpty()) {
                    com.smartmealplanner.ai.database.entity.UserProfileEntity profile = db.userDao().getAnyUserProfileSync();
                    userId = profile != null ? profile.userId : "local_user";
                }

                String mealSlot = determineMealSlot();
                String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());

                com.smartmealplanner.ai.database.entity.LoggedMealEntity meal = new com.smartmealplanner.ai.database.entity.LoggedMealEntity(
                    java.util.UUID.randomUUID().toString(),
                    userId,
                    p.getFoodId(),
                    p.getDisplayName(),
                    mealSlot,
                    "MEDIUM",
                    200,
                    p.getCalories(),
                    p.getProtein(),
                    p.getCarbs(),
                    p.getFat(),
                    System.currentTimeMillis(),
                    dateStr,
                    com.smartmealplanner.ai.database.entity.LoggedMealEntity.PENDING_CREATE,
                    System.currentTimeMillis()
                );

                db.loggedMealDao().insert(meal);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Logged " + p.getDisplayName() + " (" + p.getCalories() + " kcal) to Dashboard!", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error logging meal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private String determineMealSlot() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 11) return "BREAKFAST";
        if (hour >= 11 && hour < 16) return "LUNCH";
        if (hour >= 16 && hour < 19) return "SNACK";
        return "DINNER";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (foodClassifier != null) {
            foodClassifier.close();
        }
    }
}
