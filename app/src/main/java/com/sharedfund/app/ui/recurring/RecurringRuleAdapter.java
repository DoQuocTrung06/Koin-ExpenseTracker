package com.sharedfund.app.ui.recurring;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.sharedfund.app.R;
import com.sharedfund.app.model.RecurringRule;

import java.util.ArrayList;
import java.util.List;

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
        private final TextView tvTitle;
        private final TextView tvFrequency;
        private final TextView tvAmount;
        private final MaterialSwitch switchActive;
        private final ImageButton btnDelete;

        RecurringRuleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvRuleTitle);
            tvFrequency = itemView.findViewById(R.id.tvRuleFrequency);
            tvAmount = itemView.findViewById(R.id.tvRuleAmount);
            switchActive = itemView.findViewById(R.id.switchRuleActive);
            btnDelete = itemView.findViewById(R.id.btnDeleteRule);
        }

        void bind(RecurringRule rule) {
            tvTitle.setText(rule.getTitle() != null ? rule.getTitle() : "Khoản định kỳ");

            String scopeLabel = RecurringRule.SCOPE_ROOM.equalsIgnoreCase(rule.getScope()) ? "Quỹ nhóm" : "Ví cá nhân";
            String subText = rule.getFrequencyDescription() + " · " + scopeLabel;
            tvFrequency.setText(subText);

            tvAmount.setText(rule.getFormattedAmount());

            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(rule.isActive());
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                rule.setActive(isChecked);
                if (listener != null) listener.onToggleRule(rule, isChecked);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteRule(rule);
            });
        }
    }
}
