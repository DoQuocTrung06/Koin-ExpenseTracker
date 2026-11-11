package com.sharedfund.app.ui.recurring;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sharedfund.app.R;
import com.sharedfund.app.model.RecurringRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CreateRecurringRuleBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_ROOM_NAME = "arg_room_name";
    private static final String ARG_RULE_EDIT = "arg_rule_edit";

    public interface OnRuleCreatedListener {
        void onRuleCreated(RecurringRule rule);
    }

    private String roomId;
    private String roomName;
    private RecurringRule ruleToEdit;
    private OnRuleCreatedListener listener;

    private String selectedFrequency = RecurringRule.FREQUENCY_MONTHLY;
    private int selectedExecutionDay = 15;
    private int selectedWeekDay = 2;

    private MaterialButton btnFreqMonthly;
    private MaterialButton btnFreqWeekly;
    private MaterialButton btnFreqDaily;

    private LinearLayout layoutMonthly;
    private LinearLayout layoutWeekly;

    private final List<MaterialButton> weekDayButtons = new ArrayList<>();

    public static CreateRecurringRuleBottomSheet newInstance(String roomId) {
        return newInstance(roomId, null, null);
    }

    public static CreateRecurringRuleBottomSheet newInstance(String roomId, String roomName) {
        return newInstance(roomId, roomName, null);
    }

    public static CreateRecurringRuleBottomSheet newInstance(String roomId, String roomName, RecurringRule ruleToEdit) {
        CreateRecurringRuleBottomSheet sheet = new CreateRecurringRuleBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putString(ARG_ROOM_NAME, roomName);
        if (ruleToEdit != null) {
            args.putSerializable(ARG_RULE_EDIT, ruleToEdit);
        }
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(OnRuleCreatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
            roomName = getArguments().getString(ARG_ROOM_NAME);
            ruleToEdit = (RecurringRule) getArguments().getSerializable(ARG_RULE_EDIT);
        }
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_recurring_rule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnCreateRecurringClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        MaterialButton btnCancel = view.findViewById(R.id.btnCancelRecurringRule);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        TextView tvSheetHeader = view.findViewById(R.id.tvSheetHeaderTitle);
        TextInputEditText etTitle = view.findViewById(R.id.etRecurringTitle);
        TextInputEditText etAmount = view.findViewById(R.id.etRecurringAmount);
        TextInputEditText etDay = view.findViewById(R.id.etRecurringDay);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmitRecurringRule);

        btnFreqMonthly = view.findViewById(R.id.btnFreqMonthly);
        btnFreqWeekly = view.findViewById(R.id.btnFreqWeekly);
        btnFreqDaily = view.findViewById(R.id.btnFreqDaily);

        layoutMonthly = view.findViewById(R.id.layoutMonthlyDayPicker);
        layoutWeekly = view.findViewById(R.id.layoutWeeklyDayPicker);

        setupFrequencySegments();
        setupWeekDayPills(view);

        if (ruleToEdit != null) {
            if (tvSheetHeader != null) tvSheetHeader.setText("Chỉnh sửa khoản định kỳ");
            if (btnSubmit != null) btnSubmit.setText("Lưu thay đổi");

            if (etTitle != null) etTitle.setText(ruleToEdit.getTitle());
            if (etAmount != null) {
                java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols(Locale.GERMANY);
                sym.setGroupingSeparator('.');
                java.text.DecimalFormat df = new java.text.DecimalFormat("#,###", sym);
                etAmount.setText(df.format((long) Math.abs(ruleToEdit.getAmount())));
            }

            selectedFrequency = ruleToEdit.getFrequency();
            selectFrequency(selectedFrequency);

            if (RecurringRule.FREQUENCY_MONTHLY.equalsIgnoreCase(selectedFrequency)) {
                if (etDay != null) etDay.setText(String.valueOf(ruleToEdit.getExecutionDay()));
            } else if (RecurringRule.FREQUENCY_WEEKLY.equalsIgnoreCase(selectedFrequency)) {
                selectedWeekDay = ruleToEdit.getExecutionDay();
                updateWeekDayPillsState();
            }
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

                if (title.isEmpty()) {
                    etTitle.setError("Vui lòng nhập tên khoản chi");
                    etTitle.requestFocus();
                    return;
                }

                if (amountStr.isEmpty()) {
                    etAmount.setError("Vui lòng nhập số tiền");
                    etAmount.requestFocus();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
                } catch (NumberFormatException e) {
                    etAmount.setError("Số tiền không hợp lệ");
                    etAmount.requestFocus();
                    return;
                }

                int execDay = 1;
                if (RecurringRule.FREQUENCY_MONTHLY.equalsIgnoreCase(selectedFrequency)) {
                    String dayStr = etDay.getText() != null ? etDay.getText().toString().trim() : "15";
                    try {
                        execDay = Integer.parseInt(dayStr);
                        if (execDay < 1) execDay = 1;
                        if (execDay > 31) execDay = 31;
                    } catch (NumberFormatException e) {
                        execDay = 15;
                    }
                } else if (RecurringRule.FREQUENCY_WEEKLY.equalsIgnoreCase(selectedFrequency)) {
                    execDay = selectedWeekDay > 0 ? selectedWeekDay : 2;
                } else {
                    execDay = 1;
                }

                String inferredCategory = inferCategory(title);

                RecurringRule rule = (ruleToEdit != null) ? ruleToEdit : new RecurringRule();
                if (ruleToEdit == null) {
                    rule.setId(UUID.randomUUID().toString());
                    rule.setActive(true);
                }
                rule.setTitle(title);
                rule.setAmount(amount);
                rule.setCategoryName(inferredCategory);
                rule.setFrequency(selectedFrequency);
                rule.setExecutionDay(execDay);
                rule.setNextRunDate(rule.computeNextRunDateIso());
                rule.setType("Chi");

                if (roomId != null && !roomId.trim().isEmpty()) {
                    rule.setRoomId(roomId);
                    rule.setRoomName(roomName != null && !roomName.trim().isEmpty() ? roomName : "Quỹ nhóm");
                    rule.setScope(RecurringRule.SCOPE_ROOM);
                } else {
                    rule.setScope(RecurringRule.SCOPE_PERSONAL);
                    rule.setRoomName("Ví cá nhân");
                }

                if (listener != null) {
                    listener.onRuleCreated(rule);
                }
                dismiss();
            });
        }
    }

    private void setupFrequencySegments() {
        if (btnFreqMonthly != null) {
            btnFreqMonthly.setOnClickListener(v -> selectFrequency(RecurringRule.FREQUENCY_MONTHLY));
        }
        if (btnFreqWeekly != null) {
            btnFreqWeekly.setOnClickListener(v -> selectFrequency(RecurringRule.FREQUENCY_WEEKLY));
        }
        if (btnFreqDaily != null) {
            btnFreqDaily.setOnClickListener(v -> selectFrequency(RecurringRule.FREQUENCY_DAILY));
        }
        selectFrequency(RecurringRule.FREQUENCY_MONTHLY);
    }

    private void selectFrequency(String frequency) {
        selectedFrequency = frequency;
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary);

        boolean isMonthly = RecurringRule.FREQUENCY_MONTHLY.equalsIgnoreCase(frequency);
        boolean isWeekly = RecurringRule.FREQUENCY_WEEKLY.equalsIgnoreCase(frequency);
        boolean isDaily = RecurringRule.FREQUENCY_DAILY.equalsIgnoreCase(frequency);

        applySegmentTab(btnFreqMonthly, isMonthly, primaryColor, textPrimary);
        applySegmentTab(btnFreqWeekly, isWeekly, primaryColor, textPrimary);
        applySegmentTab(btnFreqDaily, isDaily, primaryColor, textPrimary);

        if (layoutMonthly != null) {
            layoutMonthly.setVisibility(isMonthly ? View.VISIBLE : View.GONE);
        }
        if (layoutWeekly != null) {
            layoutWeekly.setVisibility(isWeekly ? View.VISIBLE : View.GONE);
        }
    }

    private void applySegmentTab(MaterialButton btn, boolean selected, int primaryColor, int textPrimary) {
        if (btn == null) return;
        if (selected) {
            btn.setBackgroundResource(R.drawable.bg_segment_tab_glass);
            btn.setBackgroundTintList(null);
            btn.setTextColor(primaryColor);
            btn.setElevation(dpToPx(2));
        } else {
            btn.setBackgroundResource(0);
            btn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btn.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            btn.setTextColor(textPrimary);
            btn.setElevation(0);
        }
    }

    private void setupWeekDayPills(View root) {
        weekDayButtons.clear();
        int[] buttonIds = {
                R.id.btnDayMon, R.id.btnDayTue, R.id.btnDayWed,
                R.id.btnDayThu, R.id.btnDayFri, R.id.btnDaySat, R.id.btnDaySun
        };

        for (int id : buttonIds) {
            MaterialButton btn = root.findViewById(id);
            if (btn != null) {
                weekDayButtons.add(btn);
                btn.setOnClickListener(v -> {
                    if (btn.getTag() != null) {
                        try {
                            selectedWeekDay = Integer.parseInt(btn.getTag().toString());
                        } catch (NumberFormatException ignored) {}
                    }
                    updateWeekDayPillsState();
                });
            }
        }
        updateWeekDayPillsState();
    }

    private void updateWeekDayPillsState() {
        if (!isAdded() || getContext() == null) return;
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int surfaceColor = ContextCompat.getColor(requireContext(), R.color.surface);
        int borderColor = ContextCompat.getColor(requireContext(), R.color.border);
        int textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int accentBlueLight = ContextCompat.getColor(requireContext(), R.color.accent_blue_light);

        for (MaterialButton btn : weekDayButtons) {
            if (btn == null) continue;
            int tagVal = 2;
            if (btn.getTag() != null) {
                try {
                    tagVal = Integer.parseInt(btn.getTag().toString());
                } catch (NumberFormatException ignored) {}
            }
            boolean selected = (tagVal == selectedWeekDay);
            btn.setBackgroundTintList(ColorStateList.valueOf(selected ? accentBlueLight : surfaceColor));
            btn.setStrokeColor(ColorStateList.valueOf(selected ? primaryColor : borderColor));
            btn.setStrokeWidth(dpToPx(1));
            btn.setTextColor(selected ? primaryColor : textPrimary);
        }
    }

    private int dpToPx(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    private String inferCategory(String title) {
        if (title == null) return "Tiện ích & Nhà";
        String lower = title.toLowerCase(Locale.ROOT);
        if (lower.contains("phòng") || lower.contains("trọ") || lower.contains("nhà") || lower.contains("thuê")) {
            return "Tiền phòng";
        }
        if (lower.contains("điện") || lower.contains("nước") || lower.contains("mạng") || lower.contains("internet") || lower.contains("wifi") || lower.contains("fpt") || lower.contains("viettel")) {
            return "Điện nước";
        }
        if (lower.contains("ăn") || lower.contains("uống") || lower.contains("chợ") || lower.contains("cơm") || lower.contains("thực phẩm")) {
            return "Ăn uống";
        }
        if (lower.contains("xe") || lower.contains("xăng") || lower.contains("gửi xe") || lower.contains("vé")) {
            return "Di chuyển";
        }
        if (lower.contains("sắm") || lower.contains("đồ")) {
            return "Mua sắm";
        }
        return "Tiện ích & Nhà";
    }
}
