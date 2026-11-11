package com.sharedfund.app.ui.recurring;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
        void onEditRule(RecurringRule rule);
    }

    private final List<RecurringRule> rules = new ArrayList<>();
    private final OnRecurringActionListener listener;
    private RecurringRuleViewHolder currentlySwipedHolder = null;

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
        private final TextView tvAmount;
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
            tvAmount = itemView.findViewById(R.id.tvRuleAmount);
            switchActive = itemView.findViewById(R.id.switchRuleActive);

            if (layoutDeleteBackground != null) {
                layoutDeleteBackground.setVisibility(View.GONE);
            }

            maxSwipeDistance = 76f * itemView.getResources().getDisplayMetrics().density;
            setupTouchGestures();
        }

        private void setupTouchGestures() {
            cardForeground.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        isSwiping = false;
                        if (currentlySwipedHolder != null && currentlySwipedHolder != this) {
                            currentlySwipedHolder.closeSwipe(true);
                            currentlySwipedHolder = null;
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - downX;
                        float deltaY = event.getRawY() - downY;

                        if (!isSwiping) {
                            if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 15) {
                                isSwiping = true;
                                if (layoutDeleteBackground != null) {
                                    layoutDeleteBackground.setVisibility(View.VISIBLE);
                                }
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }

                        if (isSwiping) {
                            float currentTranslation = isOpen ? -maxSwipeDistance + deltaX : deltaX;
                            if (currentTranslation > 0) currentTranslation = 0;
                            if (currentTranslation < -maxSwipeDistance) currentTranslation = -maxSwipeDistance;

                            cardForeground.setTranslationX(currentTranslation);

                            float progress = Math.abs(currentTranslation) / maxSwipeDistance;
                            switchActive.setAlpha(1.0f - progress);
                            switchActive.setScaleX(1.0f - (0.4f * progress));
                            switchActive.setScaleY(1.0f - (0.4f * progress));
                            switchActive.setVisibility(progress >= 0.95f ? View.GONE : View.VISIBLE);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        if (isSwiping) {
                            float totalDiff = event.getRawX() - downX;
                            if (isOpen) {
                                if (totalDiff > maxSwipeDistance / 3) {
                                    closeSwipe(true);
                                } else {
                                    openSwipe(true);
                                }
                            } else {
                                if (totalDiff < -maxSwipeDistance / 3) {
                                    openSwipe(true);
                                } else {
                                    closeSwipe(true);
                                }
                            }
                        } else {
                            if (isOpen) {
                                closeSwipe(true);
                            } else {
                                int pos = getBindingAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION && pos < rules.size()) {
                                    if (listener != null) listener.onEditRule(rules.get(pos));
                                }
                            }
                        }
                        return true;
                }
                return false;
            });

            btnDelete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < rules.size()) {
                    RecurringRule rule = rules.get(pos);
                    closeSwipe(false);
                    if (listener != null) listener.onDeleteRule(rule);
                }
            });
        }

        void openSwipe(boolean animated) {
            isOpen = true;
            currentlySwipedHolder = this;
            if (layoutDeleteBackground != null) {
                layoutDeleteBackground.setVisibility(View.VISIBLE);
            }
            if (animated) {
                cardForeground.animate().translationX(-maxSwipeDistance).setDuration(220).start();
                switchActive.animate().alpha(0f).scaleX(0.6f).scaleY(0.6f).setDuration(200).withEndAction(() -> switchActive.setVisibility(View.GONE)).start();
            } else {
                cardForeground.setTranslationX(-maxSwipeDistance);
                switchActive.setAlpha(0f);
                switchActive.setVisibility(View.GONE);
            }
        }

        void closeSwipe(boolean animated) {
            isOpen = false;
            if (currentlySwipedHolder == this) {
                currentlySwipedHolder = null;
            }
            switchActive.setVisibility(View.VISIBLE);
            if (animated) {
                cardForeground.animate().translationX(0f).setDuration(220).withEndAction(() -> {
                    if (!isOpen && layoutDeleteBackground != null) {
                        layoutDeleteBackground.setVisibility(View.GONE);
                    }
                }).start();
                switchActive.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            } else {
                cardForeground.setTranslationX(0f);
                if (layoutDeleteBackground != null) {
                    layoutDeleteBackground.setVisibility(View.GONE);
                }
                switchActive.setAlpha(1.0f);
                switchActive.setScaleX(1.0f);
                switchActive.setScaleY(1.0f);
            }
        }

        void bind(RecurringRule rule) {
            closeSwipe(false);

            tvTitle.setText(rule.getTitle() != null ? rule.getTitle() : "Khoản định kỳ");

            String scopeLabel;
            if (rule.getRoomName() != null && !rule.getRoomName().trim().isEmpty()) {
                scopeLabel = rule.getRoomName();
            } else if (RecurringRule.SCOPE_ROOM.equalsIgnoreCase(rule.getScope())) {
                scopeLabel = "Quỹ nhóm";
            } else {
                scopeLabel = "Ví cá nhân";
            }
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
