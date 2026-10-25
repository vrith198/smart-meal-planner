package com.smartmealplanner.ai.ui.substitute;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmealplanner.ai.R;
import com.smartmealplanner.ai.model.SubstituteOption;

import java.util.ArrayList;
import java.util.List;

public class SubstituteAdapter extends RecyclerView.Adapter<SubstituteAdapter.SubstituteViewHolder> {

    public interface OnSubstituteActionListener {
        void onViewEvidence(SubstituteOption option);
        void onSwapIngredient(SubstituteOption option);
    }

    private List<SubstituteOption> options = new ArrayList<>();
    private final boolean canSwap;
    private final OnSubstituteActionListener listener;

    public SubstituteAdapter(List<SubstituteOption> options, boolean canSwap, OnSubstituteActionListener listener) {
        this.options = (options != null) ? options : new ArrayList<>();
        this.canSwap = canSwap;
        this.listener = listener;
    }

    public void setOptions(List<SubstituteOption> newOptions) {
        this.options = (newOptions != null) ? newOptions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubstituteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_substitute_option, parent, false);
        return new SubstituteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SubstituteViewHolder holder, int position) {
        SubstituteOption opt = options.get(position);

        holder.tvName.setText(opt.getName());
        holder.tvRatio.setText(opt.getRatio() != null && !opt.getRatio().isEmpty() ? opt.getRatio() : "1:1 ratio");

        if (opt.getDietaryTag() != null && !opt.getDietaryTag().isEmpty()) {
            holder.tvDietaryTag.setVisibility(View.VISIBLE);
            holder.tvDietaryTag.setText(opt.getDietaryTag());
        } else {
            holder.tvDietaryTag.setVisibility(View.GONE);
        }

        if (opt.getCulinaryRole() != null && !opt.getCulinaryRole().isEmpty()) {
            holder.tvCulinaryRole.setVisibility(View.VISIBLE);
            holder.tvCulinaryRole.setText(String.format("• %s", opt.getCulinaryRole()));
        } else {
            holder.tvCulinaryRole.setVisibility(View.GONE);
        }

        holder.tvReason.setText(opt.getReason());

        String sourceLabel = (opt.getSourceName() != null && !opt.getSourceName().isEmpty())
                ? String.format("Proof: %s", opt.getSourceName())
                : "View Proof";
        holder.btnEvidence.setText(sourceLabel);

        holder.btnEvidence.setOnClickListener(v -> {
            if (listener != null) listener.onViewEvidence(opt);
        });

        if (canSwap) {
            holder.btnSwap.setVisibility(View.VISIBLE);
            holder.btnSwap.setOnClickListener(v -> {
                if (listener != null) listener.onSwapIngredient(opt);
            });
        } else {
            holder.btnSwap.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class SubstituteViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRatio, tvDietaryTag, tvCulinaryRole, tvReason;
        MaterialButton btnEvidence, btnSwap;

        SubstituteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubstituteName);
            tvRatio = itemView.findViewById(R.id.tvSubstituteRatio);
            tvDietaryTag = itemView.findViewById(R.id.tvDietaryTag);
            tvCulinaryRole = itemView.findViewById(R.id.tvCulinaryRole);
            tvReason = itemView.findViewById(R.id.tvSubstituteReason);
            btnEvidence = itemView.findViewById(R.id.btnViewEvidence);
            btnSwap = itemView.findViewById(R.id.btnSwapIngredient);
        }
    }
}
