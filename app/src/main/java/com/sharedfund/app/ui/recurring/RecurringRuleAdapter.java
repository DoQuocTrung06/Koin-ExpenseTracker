package com.sharedfund.app.ui.recurring;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
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
        void onEditRule(RecurringRule rule);
        void onPayRule(RecurringRule rule);
        void onNudgeRule(RecurringRule rule);
    }

    private final List<RecurringRule> rules = new ArrayList<>();
    private final OnRecurringActionListener listener;
    private boolean isManager = true;
    private boolean isRoomScope = false;
    private RecurringRuleViewHolder currentlySwipedHolder = null;

    public RecurringRuleAdapter(OnRecurringActionListener listener) {
        this.listener = listener;
    }

    public void setManager(boolean isManager) {
        this.isManager = isManager;
        notifyDataSetChanged();
    }

    public void setRoomScope(boolean isRoomScope) {
        this.isRoomScope = isRoomScope;
        notifyDataSetChanged();
    }

    public void submitList(List<RecurringRule> newRules) {
        rules.clear();
        if (newRules != null) {
            rules.addAll(newRules);
        }
        notifyDataSetChanged();
    }

    public void closeAnySwipedCard() {
        if (currentlySwipedHolder != null) {
            currentlySwipedHolder.closeSwipe(true);
            currentlySwipedHolder = null;
        }
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
        private final MaterialCardView cardForeground;
        private final View layoutDeleteBackground;
        private final View btnDelete;
        private final TextView tvTitle;
        private final TextView tvFrequency;
        private final TextView tvStatusBadge;
        private final TextView tvAmount;
        private final TextView tvRemainingBadge;
        private final MaterialButton btnActionPay;
        private final MaterialButton btnActionNudge;
        private final MaterialSwitch switchActive;

        private float downX = 0f;
        private float downY = 0f;
        private boolean isSwiping = false;
        private boolean isOpen = false;
        private final float maxSwipeDistance;

        RecurringRuleViewHolder(@NonNull View itemView) {
            super(itemView);
            cardForeground = itemView.findViewById(R.id.cardRecurringRuleRoot);
            layoutDeleteBackground = itemView.findViewById(R.id.layoutDeleteActionBackground);
            btnDelete = itemView.findViewById(R.id.btnSwipeDeleteRule);
            tvTitle = itemView.findViewById(R.id.tvRuleTitle);
            tvFrequency = itemView.findViewById(R.id.tvRuleFrequency);
            tvStatusBadge = itemView.findViewById(R.id.tvRuleStatusBadge);
            tvAmount = itemView.findViewById(R.id.tvRuleAmount);
            tvRemainingBadge = itemView.findViewById(R.id.tvRuleRemainingBadge);
            btnActionPay = itemView.findViewById(R.id.btnActionPay);
            btnActionNudge = itemView.findViewById(R.id.btnActionNudge);
            switchActive = itemView.findViewById(R.id.switchRuleActive);

            if (layoutDeleteBackground != null) {
                layoutDeleteBackground.setVisibility(View.GONE);
            }

            maxSwipeDistance = 76f * itemView.getResources().getDisplayMetrics().density;
            setupTouchGestures();
        }

        private void setupTouchGestures() {
            cardForeground.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEditRule(rules.get(pos));
                }
            });

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && listener != null) {
                        listener.onDeleteRule(rules.get(pos));
                    }
                });
            }
        }

        void openSwipe() {
            isOpen = true;
            currentlySwipedHolder = this;
            if (layoutDeleteBackground != null) {
                layoutDeleteBackground.setVisibility(View.VISIBLE);
            }
            cardForeground.animate()
                    .translationX(-maxSwipeDistance)
                    .setDuration(180)
                    .withEndAction(() -> {
                        switchActive.setVisibility(View.GONE);
                    })
                    .start();
        }

        void closeSwipe(boolean animated) {
            isOpen = false;
            if (currentlySwipedHolder == this) {
                currentlySwipedHolder = null;
            }
            if (isManager) {
                switchActive.setVisibility(View.VISIBLE);
            }
            if (animated) {
                cardForeground.animate().translationX(0f).setDuration(220).withEndAction(() -> {
                    if (!isOpen && layoutDeleteBackground != null) {
                        layoutDeleteBackground.setVisibility(View.GONE);
                    }
                }).start();
                if (isManager) {
                    switchActive.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                }
            } else {
                cardForeground.setTranslationX(0f);
                if (layoutDeleteBackground != null) {
                    layoutDeleteBackground.setVisibility(View.GONE);
                }
                if (isManager) {
                    switchActive.setAlpha(1.0f);
                    switchActive.setScaleX(1.0f);
                    switchActive.setScaleY(1.0f);
                }
            }
        }

        void bind(RecurringRule rule) {
            closeSwipe(false);
            Context context = itemView.getContext();

            tvTitle.setText(rule.getTitle() != null ? rule.getTitle() : "Khoản định kỳ");

            String subText;
            if (isRoomScope) {
                subText = rule.getFrequencyDescription();
            } else if (rule.getRoomName() != null && !rule.getRoomName().trim().isEmpty()) {
                subText = rule.getFrequencyDescription() + " · " + rule.getRoomName();
            } else if (RecurringRule.SCOPE_ROOM.equalsIgnoreCase(rule.getScope())) {
                subText = rule.getFrequencyDescription() + " · Quỹ nhóm";
            } else {
                subText = rule.getFrequencyDescription() + " · Ví cá nhân";
            }
            tvFrequency.setText(subText);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
            symbols.setGroupingSeparator('.');
            DecimalFormat df = new DecimalFormat("#,###", symbols);
            tvAmount.setText(df.format(Math.abs(rule.getAmount())) + " đ");

            applyActiveState(rule.isActive());

            if (!isManager) {
                switchActive.setVisibility(View.GONE);
            } else {
                switchActive.setVisibility(View.VISIBLE);
                switchActive.setOnCheckedChangeListener(null);
                switchActive.setChecked(rule.isActive());
                switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    rule.setActive(isChecked);
                    applyActiveState(isChecked);
                    if (listener != null) listener.onToggleRule(rule, isChecked);
                });
            }

            btnActionPay.setOnClickListener(v -> {
                if (listener != null) listener.onPayRule(rule);
            });

            btnActionNudge.setOnClickListener(v -> {
                if (listener != null) listener.onNudgeRule(rule);
            });

            bindStatusAndActions(context, rule);
        }

        private void bindStatusAndActions(Context context, RecurringRule rule) {
            if (!rule.isActive()) {
                tvStatusBadge.setVisibility(View.GONE);
                tvRemainingBadge.setVisibility(View.VISIBLE);
                tvRemainingBadge.setText("Đang tạm dừng");
                tvRemainingBadge.setBackgroundResource(R.drawable.bg_badge_remaining);
                tvRemainingBadge.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary));
                btnActionPay.setVisibility(View.GONE);
                btnActionNudge.setVisibility(View.GONE);
                return;
            }

            if (rule.isPaidInCurrentMonth()) {
                tvStatusBadge.setVisibility(View.GONE);
                tvRemainingBadge.setVisibility(View.VISIBLE);
                tvRemainingBadge.setText("Đã thanh toán tháng này");
                tvRemainingBadge.setBackgroundResource(R.drawable.bg_badge_paid);
                tvRemainingBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_income));
                btnActionPay.setVisibility(View.GONE);
                btnActionNudge.setVisibility(View.GONE);
                return;
            }

            int diff = rule.getDaysDiffCurrentMonth();

            if (diff > 0) {
                tvStatusBadge.setVisibility(View.VISIBLE);
                tvStatusBadge.setText("Quá hạn " + diff + " ngày");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_overdue);
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_expense));

                tvRemainingBadge.setVisibility(View.GONE);
                if (isManager) {
                    btnActionPay.setVisibility(View.VISIBLE);
                    btnActionNudge.setVisibility(View.GONE);
                } else {
                    btnActionPay.setVisibility(View.GONE);
                    btnActionNudge.setVisibility(View.VISIBLE);
                }
            } else if (diff == 0) {
                tvStatusBadge.setVisibility(View.VISIBLE);
                tvStatusBadge.setText("Đến hạn hôm nay");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_overdue);
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_expense));

                tvRemainingBadge.setVisibility(View.GONE);
                if (isManager) {
                    btnActionPay.setVisibility(View.VISIBLE);
                    btnActionNudge.setVisibility(View.GONE);
                } else {
                    btnActionPay.setVisibility(View.GONE);
                    btnActionNudge.setVisibility(View.VISIBLE);
                }
            } else {
                tvStatusBadge.setVisibility(View.GONE);
                btnActionPay.setVisibility(View.GONE);
                btnActionNudge.setVisibility(View.GONE);

                tvRemainingBadge.setVisibility(View.VISIBLE);
                tvRemainingBadge.setText("Còn " + Math.abs(diff) + " ngày");
                tvRemainingBadge.setBackgroundResource(R.drawable.bg_badge_remaining);
                tvRemainingBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }
        }

        private void applyActiveState(boolean isActive) {
            int contextColorSurface = ContextCompat.getColor(itemView.getContext(), R.color.surface);
            int contextColorSurfaceVariant = ContextCompat.getColor(itemView.getContext(), R.color.surface_variant);
            int colorPrimary = ContextCompat.getColor(itemView.getContext(), R.color.text_primary);
            int colorSecondary = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
            int colorDisabled = ContextCompat.getColor(itemView.getContext(), R.color.text_tertiary);

            if (isActive) {
                cardForeground.setCardBackgroundColor(ColorStateList.valueOf(contextColorSurface));
                cardForeground.setAlpha(1.0f);
                tvTitle.setTextColor(colorPrimary);
                tvFrequency.setTextColor(colorSecondary);
                tvAmount.setTextColor(colorPrimary);
            } else {
                cardForeground.setCardBackgroundColor(ColorStateList.valueOf(contextColorSurfaceVariant));
                cardForeground.setAlpha(1.0f);
                tvTitle.setTextColor(colorDisabled);
                tvFrequency.setTextColor(colorDisabled);
                tvAmount.setTextColor(colorDisabled);
            }
        }
    }
}
