package com.smartmealplanner.ai.ui.substitute;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.data.repository.IngredientSubstituteRepository;
import com.smartmealplanner.ai.model.IngredientSubstitute;
import com.smartmealplanner.ai.model.SubstituteOption;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class IngredientSubstituteBottomSheet extends BottomSheetDialogFragment
        implements SubstituteAdapter.OnSubstituteActionListener {

    public interface OnSubstituteSelectedListener {
        void onSubstituteSwapped(String targetId, String originalIngredient, SubstituteOption chosenSubstitute);
    }

    private static final String ARG_INGREDIENT_NAME = "arg_ingredient_name";
    private static final String ARG_QUANTITY = "arg_quantity";
    private static final String ARG_UNIT = "arg_unit";
    private static final String ARG_CAN_SWAP = "arg_can_swap";
    private static final String ARG_TARGET_ID = "arg_target_id";

    private String ingredientName;
    private double quantity;
    private String unit;
    private boolean canSwap;
    private String targetId;

    private OnSubstituteSelectedListener swapListener;

    public static IngredientSubstituteBottomSheet newInstance(
            String ingredientName, double quantity, String unit, boolean canSwap, String targetId) {
        IngredientSubstituteBottomSheet fragment = new IngredientSubstituteBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_INGREDIENT_NAME, ingredientName);
        args.putDouble(ARG_QUANTITY, quantity);
        args.putString(ARG_UNIT, unit);
        args.putBoolean(ARG_CAN_SWAP, canSwap);
        args.putString(ARG_TARGET_ID, targetId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnSubstituteSelectedListener(OnSubstituteSelectedListener listener) {
        this.swapListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ingredientName = getArguments().getString(ARG_INGREDIENT_NAME, "");
            quantity = getArguments().getDouble(ARG_QUANTITY, 0);
            unit = getArguments().getString(ARG_UNIT, "");
            canSwap = getArguments().getBoolean(ARG_CAN_SWAP, false);
            targetId = getArguments().getString(ARG_TARGET_ID, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_ingredient_substitute, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvOriginal = view.findViewById(R.id.tvOriginalIngredient);
        ImageView btnClose = view.findViewById(R.id.btnCloseSheet);
        RecyclerView rvOptions = view.findViewById(R.id.rvSubstituteOptions);
        LinearLayout layoutEmpty = view.findViewById(R.id.layoutEmptySubstitutes);
        TextView tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        MaterialButton btnSearchOnline = view.findViewById(R.id.btnSearchOnline);

        btnClose.setOnClickListener(v -> dismiss());

        if (quantity > 0) {
            tvOriginal.setText(String.format(Locale.US, "For %s (%.0f %s)", ingredientName, quantity, unit != null ? unit : ""));
        } else {
            tvOriginal.setText(String.format(Locale.US, "For %s", ingredientName));
        }

        IngredientSubstituteRepository repo = IngredientSubstituteRepository.getInstance(requireContext());
        IngredientSubstitute match = repo.findSubstituteFor(ingredientName);
        List<SubstituteOption> options = (match != null) ? match.getSubstitutes() : null;

        if (options != null && !options.isEmpty()) {
            layoutEmpty.setVisibility(View.GONE);
            rvOptions.setVisibility(View.VISIBLE);
            rvOptions.setLayoutManager(new LinearLayoutManager(requireContext()));
            SubstituteAdapter adapter = new SubstituteAdapter(options, canSwap, this);
            rvOptions.setAdapter(adapter);
        } else {
            rvOptions.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText(String.format(Locale.US, "No pre-bundled substitute entry found for \"%s\".", ingredientName));
            btnSearchOnline.setOnClickListener(v -> {
                try {
                    String query = URLEncoder.encode(ingredientName + " culinary substitute", StandardCharsets.UTF_8.name());
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + query));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Could not launch web search.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onViewEvidence(SubstituteOption option) {
        if (option == null || option.getSourceUrl() == null || option.getSourceUrl().isEmpty()) {
            Toast.makeText(requireContext(), "No source URL available.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(option.getSourceUrl()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not open evidence link.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSwapIngredient(SubstituteOption option) {
        if (swapListener != null) {
            swapListener.onSubstituteSwapped(targetId, ingredientName, option);
        }
        dismiss();
    }
}
