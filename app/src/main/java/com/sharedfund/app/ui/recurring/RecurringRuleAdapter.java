package com.sharedfund.app.ui.recurring;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RecurringRuleAdapter extends RecyclerView.Adapter<RecurringRuleAdapter.RecurringRuleViewHolder> {

    public interface OnRecurringActionListener {
        void onToggleRule(RecurringRule rule, boolean isActive);
        void onDeleteRule(RecurringRule rule);
        void onEditRule(RecurringRule rule);
        void onPayRule(RecurringRule rule);
        void onNudgeRule(RecurringRule rule);
    }

    private static final long NUDGE_COOLDOWN_MS = 60 * 60 * 1000L;

    private final List<RecurringRule> rules = new ArrayList<>();
    private final OnRecurringActionListener listener;
    private boolean isManager = true;
    private boolean isRoomScope = false;
    private String scopeRoomId = "";
    private RecurringRuleViewHolder currentlySwipedHolder = null;
    private final Map<String, Long> nudgeTimestamps = new HashMap<>();
    private final Set<String> pendingRuleKeys = new HashSet<>();
    private final Set<RecurringRuleViewHolder> attachedHolders = new HashSet<>();
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private boolean isTickerRunning = false;

    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            boolean anyActive = false;
            for (RecurringRuleViewHolder holder : new ArrayList<>(attachedHolders)) {
                if (holder != null && holder.updateNudgeCountdownTick()) {
                    anyActive = true;
                }
            }
            if (anyActive) {
                countdownHandler.postDelayed(this, 1000L);
            } else {
                isTickerRunning = false;
            }
        }
    };

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

    public void setScopeRoomId(String scopeRoomId) {
        this.scopeRoomId = scopeRoomId != null ? scopeRoomId.trim() : "";
    }

    public void setNudgeTimestamps(Map<String, Long> timestamps) {
        nudgeTimestamps.clear();
        if (timestamps != null) {
            nudgeTimestamps.putAll(timestamps);
        }
        notifyDataSetChanged();
        startTickerIfNeeded();
    }

    public void setPendingRuleKeys(Set<String> keys) {
        pendingRuleKeys.clear();
        if (keys != null) {
            pendingRuleKeys.addAll(keys);
        }
        notifyDataSetChanged();
    }

    public void stopTimers() {
        countdownHandler.removeCallbacks(countdownRunnable);
        isTickerRunning = false;
        attachedHolders.clear();
    }

    private void startTickerIfNeeded() {
        boolean hasActive = false;
        for (RecurringRuleViewHolder holder : attachedHolders) {
            if (holder != null && holder.hasActiveNudgeCountdown()) {
                hasActive = true;
                break;
            }
        }
        if (!hasActive) {
            long now = System.currentTimeMillis();
            for (Long ts : nudgeTimestamps.values()) {
                if (ts != null && (now - ts) < NUDGE_COOLDOWN_MS && (ts - now) < 300000L) {
                    hasActive = true;
                    break;
                }
            }
        }
        if (hasActive && !isTickerRunning) {
            isTickerRunning = true;
            countdownHandler.postDelayed(countdownRunnable, 1000L);
        } else if (!hasActive && isTickerRunning) {
            countdownHandler.removeCallbacks(countdownRunnable);
            isTickerRunning = false;
        }
    }

    private long getRemainingCooldownMs(RecurringRule rule) {
        if (rule == null) return 0L;
        long latest = 0L;
        String idKey = rule.getId() != null ? rule.getId().trim() : "";
        if (!idKey.isEmpty() && nudgeTimestamps.containsKey(idKey)) {
            Long ts = nudgeTimestamps.get(idKey);
            if (ts != null && ts > latest) latest = ts;
        }
        String titleKey = rule.getTitle() != null ? rule.getTitle().trim().toLowerCase(Locale.ROOT) : "";
        String rIdKey = (rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) ? rule.getRoomId().trim() : scopeRoomId;
        if (!rIdKey.isEmpty() && !titleKey.isEmpty()) {
            String scoped = rIdKey + "|" + titleKey;
            Long ts = nudgeTimestamps.get(scoped);
            if (ts != null && ts > latest) latest = ts;
        }
        if (!titleKey.isEmpty() && nudgeTimestamps.containsKey(titleKey)) {
            Long ts = nudgeTimestamps.get(titleKey);
            if (ts != null && ts > latest) latest = ts;
        }
        if (latest <= 0L) return 0L;
        long now = System.currentTimeMillis();
        if ((latest - now) >= 300000L) return 0L;
        long remaining = NUDGE_COOLDOWN_MS - Math.max(0L, now - latest);
        return remaining > 0L ? remaining : 0L;
    }

    private boolean isRulePendingApproval(RecurringRule rule) {
        if (rule == null || pendingRuleKeys.isEmpty()) return false;
        String idKey = rule.getId() != null ? rule.getId().trim() : "";
        if (!idKey.isEmpty() && pendingRuleKeys.contains(idKey)) return true;
        String titleKey = rule.getTitle() != null ? rule.getTitle().trim().toLowerCase(Locale.ROOT) : "";
        if (titleKey.isEmpty()) return false;
        long amtKey = (long) Math.abs(rule.getAmount());
        String rIdKey = (rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) ? rule.getRoomId().trim() : scopeRoomId;
        if (!rIdKey.isEmpty() && pendingRuleKeys.contains(rIdKey + "|" + titleKey + "|" + amtKey)) {
            return true;
        }
        return pendingRuleKeys.contains("|" + titleKey + "|" + amtKey);
    }

    public void submitList(List<RecurringRule> newRules) {
        rules.clear();
        if (newRules != null) {
            rules.addAll(newRules);
        }
        notifyDataSetChanged();
        startTickerIfNeeded();
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
    public void onViewAttachedToWindow(@NonNull RecurringRuleViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        attachedHolders.add(holder);
        holder.updateNudgeCountdownTick();
        startTickerIfNeeded();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecurringRuleViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        attachedHolders.remove(holder);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        stopTimers();
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
        private RecurringRule boundRule;

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
            cardForeground.setOnTouchListener((v, event) -> {
                if (!isManager) {
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        float deltaX = Math.abs(event.getRawX() - downX);
                        float deltaY = Math.abs(event.getRawY() - downY);
                        if (deltaX < 15 && deltaY < 15) {
                            int pos = getBindingAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION && pos < rules.size() && listener != null) {
                                listener.onEditRule(rules.get(pos));
                            }
                        }
                    }
                    return false;
                }

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
                                    openSwipe();
                                }
                            } else {
                                if (totalDiff < -maxSwipeDistance / 3) {
                                    openSwipe();
                                } else {
                                    closeSwipe(true);
                                }
                            }
                        } else {
                            if (isOpen) {
                                closeSwipe(true);
                            } else {
                                float dX = Math.abs(event.getRawX() - downX);
                                float dY = Math.abs(event.getRawY() - downY);
                                if (dX < 15 && dY < 15) {
                                    int pos = getBindingAdapterPosition();
                                    if (pos != RecyclerView.NO_POSITION && pos < rules.size() && listener != null) {
                                        listener.onEditRule(rules.get(pos));
                                    }
                                }
                            }
                        }
                        return true;
                }
                return false;
            });

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    int pos = getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos < rules.size() && listener != null) {
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
            this.boundRule = rule;
            closeSwipe(false);
            Context context = itemView.getContext();

            tvTitle.setText(rule.getTitle() != null ? rule.getTitle() : "Khoản định kỳ");

            String subText;
            String countdown = rule.getNextCycleCountdownText();
            if (isRoomScope) {
                subText = rule.getFrequencyDescription() + (countdown != null && !countdown.trim().isEmpty() ? " • " + countdown : "");
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

            if (rule.isPaidInCurrentCycle()) {
                tvStatusBadge.setVisibility(View.GONE);
                tvRemainingBadge.setVisibility(View.VISIBLE);
                tvRemainingBadge.setText(rule.getPaidStatusBadgeText());
                tvRemainingBadge.setBackgroundResource(R.drawable.bg_badge_paid);
                tvRemainingBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_income));
                btnActionPay.setVisibility(View.GONE);
                btnActionNudge.setVisibility(View.GONE);
                return;
            }

            if (isRulePendingApproval(rule)) {
                tvStatusBadge.setVisibility(View.GONE);
                tvRemainingBadge.setVisibility(View.VISIBLE);
                tvRemainingBadge.setText("Chờ Chủ quỹ duyệt");
                tvRemainingBadge.setBackgroundResource(R.drawable.bg_pending_badge);
                tvRemainingBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_amber));
                btnActionPay.setVisibility(View.GONE);
                btnActionNudge.setVisibility(View.GONE);
                return;
            }

            int diff = rule.getDaysDiffCurrentCycle();

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
                    bindNudgeButton(context, rule);
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
                    bindNudgeButton(context, rule);
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

        private void bindNudgeButton(Context context, RecurringRule rule) {
            btnActionPay.setVisibility(View.GONE);
            btnActionNudge.setVisibility(View.VISIBLE);
            applyNudgeButtonState(context, getRemainingCooldownMs(rule));
        }

        boolean hasActiveNudgeCountdown() {
            return boundRule != null
                    && btnActionNudge.getVisibility() == View.VISIBLE
                    && getRemainingCooldownMs(boundRule) > 0L;
        }

        boolean updateNudgeCountdownTick() {
            if (boundRule == null || btnActionNudge.getVisibility() != View.VISIBLE) {
                return false;
            }
            long remainingMs = getRemainingCooldownMs(boundRule);
            applyNudgeButtonState(itemView.getContext(), remainingMs);
            return remainingMs > 0L;
        }

        private void applyNudgeButtonState(Context context, long remainingMs) {
            if (remainingMs > 0L) {
                long totalSeconds = Math.max(1L, remainingMs / 1000L);
                if (totalSeconds >= 3600L) totalSeconds = 3599L;
                long mins = totalSeconds / 60L;
                long secs = totalSeconds % 60L;
                btnActionNudge.setEnabled(false);
                btnActionNudge.setClickable(false);
                btnActionNudge.setText(String.format(Locale.US, "Đã nhắc (%02d:%02d)", mins, secs));
                btnActionNudge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_variant)));
                btnActionNudge.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.border)));
                btnActionNudge.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary));
                btnActionNudge.setAlpha(1.0f);
            } else {
                btnActionNudge.setEnabled(true);
                btnActionNudge.setClickable(true);
                btnActionNudge.setText("Nhắc đóng tiền");
                btnActionNudge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent_amber_light)));
                btnActionNudge.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent_amber)));
                btnActionNudge.setTextColor(ContextCompat.getColor(context, R.color.accent_amber));
                btnActionNudge.setAlpha(1.0f);
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
