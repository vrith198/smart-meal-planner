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
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.google.android.material.chip.ChipGroup;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.FoodItemEntity;
import com.smartmealplanner.ai.ml.FoodClassifier;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartmealplanner.ai.agent.GeminiVisionAgent;
import com.smartmealplanner.ai.model.PredictionResult;
import com.smartmealplanner.ai.ui.analysis.MealAnalysisActivity;

import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScanMealFragment extends Fragment {

    private static final String TAG = "ScanMealFragment";

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

    // Portion & Structured Ingredient breakdown views
    private ChipGroup chipGroupServingPortion;
    private Chip chipPortionSmall, chipPortionMedium, chipPortionLarge;
    private TextView tvAiVisibleIngredients;
    private TextView tvAiInferredIngredients;
    private TextView tvAiUncertainIngredients;
    private MaterialButton btnEditIngredients;
    private MaterialButton btnRetryScan;
    private TextView tvNutritionDisclaimer;
    private PredictionResult activePrediction;

    // Gemini Multimodal Vision Agent Views
    private View layoutGeminiAgentRow;
    private MaterialButton btnAnalyzeWithGemini;
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

    // Robust Camera & Gallery Capture
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri currentPhotoUri;
    private File currentPhotoFile;

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
                            "Camera permission is required to capture meal photos.",
                            Toast.LENGTH_LONG).show();
                    }
                }
            }
        );

        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (Boolean.TRUE.equals(isSuccess)) {
                    Bitmap bitmap = null;
                    if (currentPhotoFile != null && currentPhotoFile.exists()) {
                        bitmap = decodeAndOrientBitmap(currentPhotoFile.getAbsolutePath());
                    }
                    if (bitmap == null && currentPhotoUri != null) {
                        bitmap = decodeUriWithOrientation(currentPhotoUri);
                    }
                    if (bitmap != null) {
                        processCapturedImage(bitmap);
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Could not process captured photo. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );

        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && getContext() != null) {
                    try {
                        Bitmap bitmap = decodeUriWithOrientation(uri);
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
        if (getContext() == null) return;
        try {
            currentPhotoFile = createImageFile();
            if (currentPhotoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    currentPhotoFile
                );
                cameraLauncher.launch(currentPhotoUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching camera: " + e.getMessage(), e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Could not open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "MEAL_" + timeStamp + "_";
        File storageDir = requireContext().getCacheDir();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private Bitmap decodeAndOrientBitmap(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            int maxDim = Math.max(options.outWidth, options.outHeight);
            int sampleSize = 1;
            while (maxDim / sampleSize > 1280) {
                sampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            if (bitmap == null) return null;

            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            return rotateBitmapIfRequired(bitmap, orientation);
        } catch (Exception e) {
            Log.e(TAG, "decodeAndOrientBitmap error: " + e.getMessage(), e);
            return null;
        }
    }

    private Bitmap decodeUriWithOrientation(Uri uri) {
        if (getContext() == null || uri == null) return null;
        try {
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream is = getContext().getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    ExifInterface exif = new ExifInterface(is);
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                }
            } catch (Exception ignored) {}

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = getContext().getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, options);
            }
            int maxDim = Math.max(options.outWidth, options.outHeight);
            int sampleSize = 1;
            while (maxDim / sampleSize > 1280) {
                sampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            Bitmap bitmap;
            try (InputStream is = getContext().getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is, null, options);
            }

            return rotateBitmapIfRequired(bitmap, orientation);
        } catch (Exception e) {
            Log.e(TAG, "decodeUriWithOrientation error: " + e.getMessage(), e);
            return null;
        }
    }

    private Bitmap rotateBitmapIfRequired(Bitmap bitmap, int orientation) {
        if (bitmap == null) return null;
        int rotationDegrees = 0;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotationDegrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotationDegrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotationDegrees = 270;
                break;
            default:
                return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
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

        chipGroupServingPortion = v.findViewById(R.id.chipGroupServingPortion);
        chipPortionSmall = v.findViewById(R.id.chipPortionSmall);
        chipPortionMedium = v.findViewById(R.id.chipPortionMedium);
        chipPortionLarge = v.findViewById(R.id.chipPortionLarge);
        tvAiVisibleIngredients = v.findViewById(R.id.tvAiVisibleIngredients);
        tvAiInferredIngredients = v.findViewById(R.id.tvAiInferredIngredients);
        tvAiUncertainIngredients = v.findViewById(R.id.tvAiUncertainIngredients);
        btnEditIngredients = v.findViewById(R.id.btnEditIngredients);
        btnRetryScan = v.findViewById(R.id.btnRetryScan);
        tvNutritionDisclaimer = v.findViewById(R.id.tvNutritionDisclaimer);

        if (btnRetryScan != null) {
            btnRetryScan.setOnClickListener(view -> resetScanState());
        }

        if (btnEditIngredients != null) {
            btnEditIngredients.setOnClickListener(view -> showEditIngredientsDialog());
        }

        if (chipGroupServingPortion != null) {
            chipGroupServingPortion.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (activePrediction == null) return;
                int baseGrams = activePrediction.getServingWeightGrams() > 0 ? activePrediction.getServingWeightGrams() : 150;
                if (checkedIds.contains(R.id.chipPortionSmall)) {
                    int smallGrams = Math.max(50, (int) Math.round(baseGrams * 0.6));
                    activePrediction.recalculateNutrition(smallGrams);
                } else if (checkedIds.contains(R.id.chipPortionLarge)) {
                    int largeGrams = (int) Math.round(baseGrams * 1.5);
                    activePrediction.recalculateNutrition(largeGrams);
                } else {
                    activePrediction.recalculateNutrition(baseGrams);
                }
                renderActiveNutritionUI(activePrediction);
            });
        }

        layoutGeminiAgentRow = v.findViewById(R.id.layoutGeminiAgentRow);
        btnAnalyzeWithGemini = v.findViewById(R.id.btnAnalyzeWithGemini);
        layoutGeminiLoading = v.findViewById(R.id.layoutGeminiLoading);
        tvGeminiLoadingStatus = v.findViewById(R.id.tvGeminiLoadingStatus);

        btnAnalyzeWithGemini.setOnClickListener(view -> onAnalyzeWithGeminiClicked());

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
        this.activePrediction = p1;
        renderActiveNutritionUI(p1);

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
        triggerGeminiAnalysis();
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
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayGeminiPrediction(PredictionResult p) {
        this.activePrediction = p;
        renderActiveNutritionUI(p);
        Toast.makeText(getContext(), "✨ Gemini Vision Agent analyzed " + p.getDisplayName() + " (" + p.getCalories() + " kcal)!", Toast.LENGTH_LONG).show();
    }

    private void renderActiveNutritionUI(PredictionResult p) {
        if (p == null || cardAiCalorieEstimate == null) return;
        cardAiCalorieEstimate.setVisibility(View.VISIBLE);

        if (tvAiSourceBadge != null) {
            tvAiSourceBadge.setText(p.isGeminiResult() ? "✨ Gemini Cloud AI" : "📱 On-Device AI");
        }
        if (tvAiDetectedFoodName != null) {
            tvAiDetectedFoodName.setText(p.getDisplayName());
        }
        if (tvAiConfidenceBadge != null) {
            if (p.isHighConfidence()) {
                tvAiConfidenceBadge.setText(p.getConfidencePercentage() + "% Match");
                tvAiConfidenceBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            } else {
                tvAiConfidenceBadge.setText(p.getConfidencePercentage() + "% (Uncertain)");
                tvAiConfidenceBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.tertiary));
            }
        }
        if (tvAiEstimatedCalories != null) {
            tvAiEstimatedCalories.setText("🔥 " + p.getCalories() + " kcal");
        }
        if (tvAiServingDesc != null) {
            tvAiServingDesc.setText("(Portion: ~" + p.getServingWeightGrams() + "g)");
        }
        if (tvAiMacroProtein != null) {
            tvAiMacroProtein.setText("Protein: " + p.getProtein() + "g");
        }
        if (tvAiMacroCarbs != null) {
            tvAiMacroCarbs.setText("Carbs: " + p.getCarbs() + "g");
        }
        if (tvAiMacroFat != null) {
            tvAiMacroFat.setText("Fat: " + p.getFat() + "g");
        }

        // Categorized Ingredients (Visible vs Inferred vs Uncertain)
        if (tvAiVisibleIngredients != null) {
            List<String> vis = p.getVisibleIngredients();
            if (vis != null && !vis.isEmpty()) {
                tvAiVisibleIngredients.setText("👁️ Visually Detected: " + joinStrings(vis, ", "));
                tvAiVisibleIngredients.setVisibility(View.VISIBLE);
            } else {
                tvAiVisibleIngredients.setVisibility(View.GONE);
            }
        }

        if (tvAiInferredIngredients != null) {
            List<String> inf = p.getInferredIngredients();
            if (inf != null && !inf.isEmpty()) {
                tvAiInferredIngredients.setText("📖 Recipe Inferred: " + joinStrings(inf, ", "));
                tvAiInferredIngredients.setVisibility(View.VISIBLE);
            } else {
                tvAiInferredIngredients.setVisibility(View.GONE);
            }
        }

        if (tvAiUncertainIngredients != null) {
            List<String> unc = p.getUncertainIngredients();
            if (unc != null && !unc.isEmpty()) {
                tvAiUncertainIngredients.setText("❓ Uncertain: " + joinStrings(unc, ", "));
                tvAiUncertainIngredients.setVisibility(View.VISIBLE);
            } else {
                tvAiUncertainIngredients.setVisibility(View.GONE);
            }
        }

        if (tvAiHealthNotes != null) {
            if (p.getHealthNotes() != null && !p.getHealthNotes().isEmpty()) {
                tvAiHealthNotes.setText("💡 " + p.getHealthNotes());
                tvAiHealthNotes.setVisibility(View.VISIBLE);
            } else {
                tvAiHealthNotes.setVisibility(View.GONE);
            }
        }

        if (btnQuickLogMeal != null) {
            btnQuickLogMeal.setText("⚡ Log " + p.getDisplayName() + " (" + p.getCalories() + " kcal)");
            btnQuickLogMeal.setOnClickListener(view -> handleQuickLog(p));
        }

        if (btnAnalyzeWithGemini != null) btnAnalyzeWithGemini.setEnabled(true);
        if (layoutGeminiLoading != null) layoutGeminiLoading.setVisibility(View.GONE);
    }

    private void showEditIngredientsDialog() {
        if (getContext() == null || activePrediction == null) return;

        final EditText input = new EditText(getContext());
        input.setSingleLine(false);
        input.setLines(4);
        List<String> current = new ArrayList<>();
        if (activePrediction.getVisibleIngredients() != null) current.addAll(activePrediction.getVisibleIngredients());
        if (activePrediction.getInferredIngredients() != null) current.addAll(activePrediction.getInferredIngredients());
        input.setText(joinStrings(current, ", "));

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

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Ingredients")
                .setMessage("Add, remove, or adjust visible and recipe ingredients (comma-separated):")
                .setView(container)
                .setPositiveButton("Save Ingredients", (dialog, which) -> {
                    String entered = input.getText().toString().trim();
                    List<String> updated = new ArrayList<>();
                    if (!entered.isEmpty()) {
                        for (String s : entered.split(",")) {
                            if (!s.trim().isEmpty()) updated.add(s.trim());
                        }
                    }
                    activePrediction.setVisibleIngredients(updated);
                    activePrediction.setIngredients(updated);
                    renderActiveNutritionUI(activePrediction);
                    Toast.makeText(getContext(), "Ingredients updated!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetScanState() {
        currentBitmap = null;
        activePrediction = null;
        if (ivPreview != null) ivPreview.setImageDrawable(null);
        if (layoutEmptyPreview != null) layoutEmptyPreview.setVisibility(View.VISIBLE);
        if (layoutPredictions != null) layoutPredictions.setVisibility(View.GONE);
        if (layoutGeminiLoading != null) layoutGeminiLoading.setVisibility(View.GONE);
        checkAndLaunchCamera();
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
                    p.getServingWeightGrams() > 0 ? p.getServingWeightGrams() : 150,
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

                // Real-time Cloud Firestore synchronization
                com.smartmealplanner.ai.firebase.FirebaseManager.getInstance(requireContext())
                    .syncMealToCloud(meal, new com.smartmealplanner.ai.firebase.FirebaseManager.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            android.util.Log.i("ScanMealFragment", "✅ Meal synced to Firestore in real-time: " + meal.mealId);
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                try {
                                    db.loggedMealDao().markAsSynced(meal.mealId);
                                } catch (Exception ignored) {}
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            android.util.Log.w("ScanMealFragment", "⚠️ Firestore real-time sync deferred (will sync via WorkManager): " + e.getMessage());
                        }
                    });

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Logged " + p.getDisplayName() + " (" + p.getCalories() + " kcal) & syncing to cloud!", Toast.LENGTH_LONG).show();
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
