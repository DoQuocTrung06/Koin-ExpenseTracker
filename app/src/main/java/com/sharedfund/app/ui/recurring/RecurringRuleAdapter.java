package com.sharedfund.app.ui.recurring;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.sharedfund.app.R;
import com.sharedfund.app.model.RecurringRule;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecurringRuleAdapter extends RecyclerView.Adapter<RecurringRuleAdapter.RecurringRuleViewHolder> {

    public interface OnRecurringActionListener {
        void onToggleRule(RecurringRule rule, boolean isActive);
        void onDeleteRule(RecurringRule rule);
    }

    private final List<RecurringRule> rules = new ArrayList<>();
    private final OnRecurringActionListener listener;

    public RecurringRuleAdapter(OnRecurringActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<RecurringRule> newRules) {
        rules.clear();
        if (newRules != null) {
            rules.addAll(newRules);
        }
        notifyDataSetChanged();
    }

    public List<RecurringRule> getRules() {
        return rules;
    }

    @NonNull
    @Override
    public RecurringRuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recurring_rule, parent, false);
        return new RecurringRuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecurringRuleViewHolder holder, int position) {
        holder.bind(rules.get(position));
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    class RecurringRuleViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardRoot;
        private final TextView tvTitle;
        private final TextView tvFrequency;
        private final TextView tvAmount;
        private final MaterialSwitch switchActive;

        RecurringRuleViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRecurringRuleRoot);
            tvTitle = itemView.findViewById(R.id.tvRuleTitle);
            tvFrequency = itemView.findViewById(R.id.tvRuleFrequency);
            tvAmount = itemView.findViewById(R.id.tvRuleAmount);
            switchActive = itemView.findViewById(R.id.switchRuleActive);
        }

        void bind(RecurringRule rule) {
            tvTitle.setText(rule.getTitle() != null ? rule.getTitle() : "Khoản định kỳ");

            String scopeLabel = RecurringRule.SCOPE_ROOM.equalsIgnoreCase(rule.getScope()) ? "Quỹ nhóm" : "Ví cá nhân";
            String subText = rule.getFrequencyDescription() + " · " + scopeLabel;
            tvFrequency.setText(subText);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
            symbols.setGroupingSeparator('.');
            DecimalFormat df = new DecimalFormat("#,###", symbols);
            tvAmount.setText(df.format(Math.abs(rule.getAmount())) + " đ");

            applyActiveState(rule.isActive());

            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(rule.isActive());
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                rule.setActive(isChecked);
                applyActiveState(isChecked);
                if (listener != null) listener.onToggleRule(rule, isChecked);
            });
        }

        private void applyActiveState(boolean isActive) {
            int contextColorSurface = ContextCompat.getColor(itemView.getContext(), R.color.surface);
            int contextColorSurfaceVariant = ContextCompat.getColor(itemView.getContext(), R.color.surface_variant);
            int colorPrimary = ContextCompat.getColor(itemView.getContext(), R.color.text_primary);
            int colorSecondary = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
            int colorDisabled = ContextCompat.getColor(itemView.getContext(), R.color.text_tertiary);

            if (isActive) {
                cardRoot.setCardBackgroundColor(ColorStateList.valueOf(contextColorSurface));
                cardRoot.setAlpha(1.0f);
                tvTitle.setTextColor(colorPrimary);
                tvFrequency.setTextColor(colorSecondary);
                tvAmount.setTextColor(colorPrimary);
            } else {
                cardRoot.setCardBackgroundColor(ColorStateList.valueOf(contextColorSurfaceVariant));
                cardRoot.setAlpha(0.65f);
                tvTitle.setTextColor(colorDisabled);
                tvFrequency.setTextColor(colorDisabled);
                tvAmount.setTextColor(colorDisabled);
            }
        }
    }
}
