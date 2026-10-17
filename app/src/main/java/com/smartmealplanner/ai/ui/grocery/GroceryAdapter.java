package com.smartmealplanner.ai.ui.grocery;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.database.entity.GroceryItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.GroceryViewHolder> {

    public interface OnGroceryActionListener {
        void onToggle(GroceryItemEntity item, boolean isChecked);
        void onDelete(GroceryItemEntity item);
    }

    private List<GroceryItemEntity> items = new ArrayList<>();
    private OnGroceryActionListener listener;

    public void setItems(List<GroceryItemEntity> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setListener(OnGroceryActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroceryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grocery, parent, false);
        return new GroceryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GroceryViewHolder holder, int position) {
        GroceryItemEntity item = items.get(position);
        holder.tvName.setText(item.ingredientName);
        holder.tvCategory.setText(item.category != null ? item.category : "Pantry");

        if (item.quantity > 0) {
            holder.tvQuantity.setVisibility(View.VISIBLE);
            holder.tvQuantity.setText(String.format(Locale.US, "%.0f %s", item.quantity, item.unit != null ? item.unit : ""));
        } else {
            holder.tvQuantity.setVisibility(View.GONE);
        }

        // Checkbox strike-through
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isChecked);
        if (item.isChecked) {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvName.setAlpha(0.5f);
        } else {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvName.setAlpha(1.0f);
        }

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onToggle(item, isChecked);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GroceryViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvName, tvCategory, tvQuantity;
        ImageView btnDelete;

        GroceryViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbGroceryChecked);
            tvName = itemView.findViewById(R.id.tvGroceryName);
            tvCategory = itemView.findViewById(R.id.tvGroceryCategory);
            tvQuantity = itemView.findViewById(R.id.tvGroceryQuantity);
            btnDelete = itemView.findViewById(R.id.btnDeleteGrocery);
        }
    }
}
