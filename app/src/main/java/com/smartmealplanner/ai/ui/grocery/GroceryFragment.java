package com.smartmealplanner.ai.ui.grocery;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.AppDatabase;
import com.smartmealplanner.ai.database.entity.FoodItemEntity;
import com.smartmealplanner.ai.database.entity.GroceryItemEntity;
import com.smartmealplanner.ai.database.entity.MealPlanEntity;
import com.smartmealplanner.ai.firebase.FirebaseManager;
import com.smartmealplanner.ai.model.SubstituteOption;
import com.smartmealplanner.ai.ui.substitute.IngredientSubstituteBottomSheet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class GroceryFragment extends Fragment implements GroceryAdapter.OnGroceryActionListener {

    private RecyclerView rvGrocery;
    private GroceryAdapter adapter;
    private TextView tvEmpty;
    private MaterialButton btnGenerate, btnAddCustom, btnClearCompleted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_grocery, container, false);

        rvGrocery = v.findViewById(R.id.rvGroceryList);
        tvEmpty = v.findViewById(R.id.tvEmptyGrocery);
        btnGenerate = v.findViewById(R.id.btnGenerateFromPlan);
        btnAddCustom = v.findViewById(R.id.btnAddCustomGrocery);
        btnClearCompleted = v.findViewById(R.id.btnClearCompletedGrocery);

        rvGrocery.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GroceryAdapter();
        adapter.setListener(this);
        rvGrocery.setAdapter(adapter);

        btnGenerate.setOnClickListener(view -> generateGroceryFromMealPlan());
        btnAddCustom.setOnClickListener(view -> showAddCustomItemDialog());
        btnClearCompleted.setOnClickListener(view -> clearCompletedItems());

        observeGroceryItems();
        return v;
    }

    private void observeGroceryItems() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();

        db.groceryDao().getAllItemsLive(userId).observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                adapter.setItems(null);
            } else {
                tvEmpty.setVisibility(View.GONE);
                adapter.setItems(items);
            }
        });
    }

    private void generateGroceryFromMealPlan() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();
        String todayString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            MealPlanEntity plan = db.mealPlanDao().getPlanForDateSync(userId, todayString);
            if (plan == null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "No meal plan found for today. Generate a plan first!", Toast.LENGTH_SHORT).show();
                    });
                }
                return;
            }

            List<String> plannedFoodIds = Arrays.asList(
                    plan.breakfastFoodId, plan.lunchFoodId, plan.snackFoodId, plan.dinnerFoodId
            );

            // Consolidate ingredient amounts
            Map<String, Double> ingredientAmounts = new HashMap<>();
            Map<String, String> ingredientUnits = new HashMap<>();

            for (String fId : plannedFoodIds) {
                if (fId == null) continue;
                FoodItemEntity food = db.foodItemDao().getFoodById(fId);
                if (food != null && food.ingredientsJson != null) {
                    try {
                        JSONArray array = new JSONArray(food.ingredientsJson);
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject ing = array.getJSONObject(i);
                            String name = ing.getString("name");
                            double amount = ing.optDouble("amount", 1.0);
                            String unit = ing.optString("unit", "g");

                            double current = ingredientAmounts.getOrDefault(name, 0.0);
                            ingredientAmounts.put(name, current + amount);
                            ingredientUnits.put(name, unit);
                        }
                    } catch (Exception ignored) {}
                }
            }

            List<GroceryItemEntity> groceryList = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Double> entry : ingredientAmounts.entrySet()) {
                String name = entry.getKey();
                double amount = entry.getValue();
                String unit = ingredientUnits.getOrDefault(name, "g");
                String category = categorizeIngredient(name);

                groceryList.add(new GroceryItemEntity(
                        UUID.randomUUID().toString(), userId, name, amount, unit, category, false, 1, now
                ));
            }

            if (!groceryList.isEmpty()) {
                db.groceryDao().insertAll(groceryList);

                // Sync each grocery item to Firestore
                FirebaseManager fm = FirebaseManager.getInstance(getContext());
                for (GroceryItemEntity gItem : groceryList) {
                    fm.syncGroceryToCloud(gItem, new FirebaseManager.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                try {
                                    db.groceryDao().markAsSynced(gItem.itemId);
                                } catch (Exception ignored) {}
                            });
                        }
                        @Override
                        public void onFailure(Exception e) {}
                    });
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Generated " + groceryList.size() + " grocery items from meal plan!", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "No verified ingredients found in planned meals.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private String categorizeIngredient(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("atta") || lower.contains("rice") || lower.contains("dal") || lower.contains("flour") || lower.contains("poha")) {
            return "Grains & Lentils";
        } else if (lower.contains("milk") || lower.contains("curd") || lower.contains("paneer") || lower.contains("cheese") || lower.contains("butter")) {
            return "Dairy";
        } else if (lower.contains("onion") || lower.contains("tomato") || lower.contains("potato") || lower.contains("spinach") || lower.contains("apple")) {
            return "Fresh Produce";
        } else if (lower.contains("seeds") || lower.contains("spice") || lower.contains("masala") || lower.contains("chili")) {
            return "Spices & Seasoning";
        }
        return "Pantry";
    }

    private void showAddCustomItemDialog() {
        if (getContext() == null) return;
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText etName = new EditText(getContext());
        etName.setHint("Item name (e.g. Olive Oil)");
        layout.addView(etName);

        EditText etQty = new EditText(getContext());
        etQty.setHint("Quantity & unit (e.g. 500 ml)");
        layout.addView(etQty);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Grocery Item")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String qtyStr = etQty.getText().toString().trim();
                    if (!TextUtils.isEmpty(name)) {
                        addCustomItem(name, qtyStr);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addCustomItem(String name, String qtyStr) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();
        long now = System.currentTimeMillis();

        double qty = 1.0;
        String unit = "pcs";
        try {
            String[] parts = qtyStr.split(" ");
            if (parts.length > 0) qty = Double.parseDouble(parts[0]);
            if (parts.length > 1) unit = parts[1];
        } catch (Exception ignored) {}

        GroceryItemEntity item = new GroceryItemEntity(
                UUID.randomUUID().toString(), userId, name, qty, unit, "Pantry", false, 1, now
        );

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.groceryDao().insert(item);
            FirebaseManager.getInstance(getContext()).syncGroceryToCloud(item, new FirebaseManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            db.groceryDao().markAsSynced(item.itemId);
                        } catch (Exception ignored) {}
                    });
                }
                @Override
                public void onFailure(Exception e) {}
            });
        });
    }

    private void clearCompletedItems() {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        String userId = FirebaseManager.getInstance(getContext()).getCurrentUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.groceryDao().clearCompleted(userId);
        });
    }

    @Override
    public void onToggle(GroceryItemEntity item, boolean isChecked) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.groceryDao().toggleChecked(item.itemId, isChecked, System.currentTimeMillis());
        });
    }

    @Override
    public void onDelete(GroceryItemEntity item) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.groceryDao().delete(item);
        });
    }

    @Override
    public void onFindSubstitute(GroceryItemEntity item) {
        if (getContext() == null || item == null) return;
        IngredientSubstituteBottomSheet sheet = IngredientSubstituteBottomSheet.newInstance(
                item.ingredientName,
                item.quantity,
                item.unit,
                true,
                item.itemId
        );
        sheet.setOnSubstituteSelectedListener((targetId, originalIngredient, chosenSubstitute) -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            item.ingredientName = chosenSubstitute.getName();
            item.syncState = GroceryItemEntity.PENDING_UPDATE;
            item.updatedAtMs = System.currentTimeMillis();
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.groceryDao().update(item);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                "Replaced \"" + originalIngredient + "\" with \"" + chosenSubstitute.getName() + "\"",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        sheet.show(getChildFragmentManager(), "substitute_sheet");
    }
}
