package com.smartmealplanner.ai.ui.scan;

import android.content.Intent;
import android.graphics.Bitmap;
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
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.FoodItemEntity;
import com.smartmealplanner.ai.ml.FoodClassifier;
import com.smartmealplanner.ai.model.PredictionResult;
import com.smartmealplanner.ai.ui.analysis.MealAnalysisActivity;

import java.util.ArrayList;
import java.util.List;

public class ScanMealFragment extends Fragment {

    private ImageView ivPreview;
    private View layoutEmptyPreview;
    private ProgressBar pbInference;
    private LinearLayout layoutPredictions;

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
    private List<FoodItemEntity> allCatalogFoods = new ArrayList<>();
    private List<PredictionResult> currentPredictions = new ArrayList<>();

    // Activity Result Launchers
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                        decoder.setTargetSampleSize(maxDim / 1024);
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

        tvLowConfidenceWarning = v.findViewById(R.id.tvLowConfidenceWarning);
        spinnerCorrection = v.findViewById(R.id.spinnerFoodCorrection);
        btnConfirmFood = v.findViewById(R.id.btnConfirmFood);

        MaterialButton btnCapture = v.findViewById(R.id.btnCaptureCamera);
        MaterialButton btnGallery = v.findViewById(R.id.btnChooseGallery);

        btnCapture.setOnClickListener(view -> cameraLauncher.launch(null));
        btnGallery.setOnClickListener(view -> galleryLauncher.launch("image/*"));

        btnConfirmFood.setOnClickListener(view -> handleConfirmFood());

        // Initialize food classifier
        foodClassifier = new FoodClassifier(getContext());

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
        ivPreview.setImageBitmap(bitmap);
        layoutEmptyPreview.setVisibility(View.GONE);
        pbInference.setVisibility(View.VISIBLE);
        layoutPredictions.setVisibility(View.GONE);

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

        // Top 1
        PredictionResult p1 = predictions.get(0);
        tvPred1Name.setText("1. " + p1.getDisplayName());
        tvPred1Conf.setText(p1.getConfidencePercentage() + "%");
        pbPred1.setProgress(p1.getConfidencePercentage());

        // Top 2
        if (predictions.size() > 1) {
            PredictionResult p2 = predictions.get(1);
            tvPred2Name.setText("2. " + p2.getDisplayName());
            tvPred2Conf.setText(p2.getConfidencePercentage() + "%");
            pbPred2.setProgress(p2.getConfidencePercentage());
        }

        // Top 3
        if (predictions.size() > 2) {
            PredictionResult p3 = predictions.get(2);
            tvPred3Name.setText("3. " + p3.getDisplayName());
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
        for (int i = 0; i < allCatalogFoods.size(); i++) {
            if (allCatalogFoods.get(i).classifierLabel != null &&
                allCatalogFoods.get(i).classifierLabel.equalsIgnoreCase(p1.getLabel())) {
                spinnerCorrection.setSelection(i);
                break;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (foodClassifier != null) {
            foodClassifier.close();
        }
    }
}
