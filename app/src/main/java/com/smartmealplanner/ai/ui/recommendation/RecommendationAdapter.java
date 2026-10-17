package com.smartmealplanner.ai.ui.recommendation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.entity.FoodItemEntity;
import com.smartmealplanner.ai.recommendation.MealRanker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.RecViewHolder> {

    public interface OnRecommendationActionListener {
        void onLike(MealRanker.RankedMeal meal);
        void onDislike(MealRanker.RankedMeal meal);
        void onAddToPlan(MealRanker.RankedMeal meal);
    }

    private List<MealRanker.RankedMeal> meals = new ArrayList<>();
    private OnRecommendationActionListener listener;

    public void setMeals(List<MealRanker.RankedMeal> newMeals) {
        this.meals = (newMeals != null) ? newMeals : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setListener(OnRecommendationActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommendation_card, parent, false);
        return new RecViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecViewHolder holder, int position) {
        MealRanker.RankedMeal item = meals.get(position);
        FoodItemEntity food = item.getFood();

        holder.tvName.setText(food.name);
        holder.tvDietCategory.setText(String.format(Locale.US, "%s • ₹%.0f", food.dietaryType, food.estimatedCostInr));
        holder.tvFuzzyBadge.setText(String.format(Locale.US, "Suitability: %d%%", item.getFuzzyScore()));

        // Serving calories & macros
        int grams = food.mediumServingGrams > 0 ? food.mediumServingGrams : 100;
        double factor = grams / 100.0;
        holder.tvCalories.setText(String.format(Locale.US, "%.0f kcal", food.caloriesPer100g * factor));
        holder.tvProtein.setText(String.format(Locale.US, "P: %.1fg", food.proteinPer100g * factor));
        holder.tvCarbs.setText(String.format(Locale.US, "C: %.1fg", food.carbsPer100g * factor));
        holder.tvFat.setText(String.format(Locale.US, "F: %.1fg", food.fatPer100g * factor));

        // Format grounded Explainable AI bullet points
        StringBuilder sb = new StringBuilder();
        if (item.getExplanations() != null) {
            for (String reason : item.getExplanations()) {
                sb.append("• ").append(reason).append("\n");
            }
        }
        holder.tvExplanations.setText(sb.toString().trim());

        holder.btnLike.setOnClickListener(v -> {
            if (listener != null) listener.onLike(item);
        });

        holder.btnDislike.setOnClickListener(v -> {
            if (listener != null) listener.onDislike(item);
        });

        holder.btnAddToPlan.setOnClickListener(v -> {
            if (listener != null) listener.onAddToPlan(item);
        });
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    static class RecViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDietCategory, tvFuzzyBadge, tvCalories, tvProtein, tvCarbs, tvFat, tvExplanations;
        MaterialButton btnLike, btnDislike, btnAddToPlan;

        RecViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecFoodName);
            tvDietCategory = itemView.findViewById(R.id.tvRecDietCategory);
            tvFuzzyBadge = itemView.findViewById(R.id.tvFuzzyScoreBadge);
            tvCalories = itemView.findViewById(R.id.tvRecCalories);
            tvProtein = itemView.findViewById(R.id.tvRecProtein);
            tvCarbs = itemView.findViewById(R.id.tvRecCarbs);
            tvFat = itemView.findViewById(R.id.tvRecFat);
            tvExplanations = itemView.findViewById(R.id.tvExplanationsText);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnDislike = itemView.findViewById(R.id.btnDislike);
            btnAddToPlan = itemView.findViewById(R.id.btnAddToPlan);
        }
    }
}
