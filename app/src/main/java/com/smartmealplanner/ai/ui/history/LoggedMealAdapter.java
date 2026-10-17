package com.smartmealplanner.ai.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.entity.LoggedMealEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoggedMealAdapter extends RecyclerView.Adapter<LoggedMealAdapter.MealViewHolder> {

    public interface OnMealClickListener {
        void onMealClick(LoggedMealEntity meal);
        void onMealLongClick(LoggedMealEntity meal);
    }

    private List<LoggedMealEntity> meals = new ArrayList<>();
    private OnMealClickListener listener;

    public void setMeals(List<LoggedMealEntity> newMeals) {
        this.meals = (newMeals != null) ? newMeals : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setListener(OnMealClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_logged_meal, parent, false);
        return new MealViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        LoggedMealEntity meal = meals.get(position);
        holder.tvFoodName.setText(meal.foodName);
        holder.tvDetails.setText(String.format(Locale.US, "%s • %dg", meal.mealSlot, meal.servingWeightGrams));
        holder.tvCalories.setText(String.format(Locale.US, "%.0f kcal", meal.calories));
        holder.tvMacros.setText(String.format(Locale.US, "P: %.1fg • C: %.1fg • F: %.1fg",
                meal.proteinGrams, meal.carbsGrams, meal.fatGrams));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMealClick(meal);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onMealLongClick(meal);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvDetails, tvCalories, tvMacros;

        MealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvMealFoodName);
            tvDetails = itemView.findViewById(R.id.tvMealDetails);
            tvCalories = itemView.findViewById(R.id.tvMealCalories);
            tvMacros = itemView.findViewById(R.id.tvMealMacros);
        }
    }
}
