package com.sharedfund.app.ui.recurring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.sharedfund.app.R;
import com.sharedfund.app.model.RecurringRule;

import java.util.UUID;

public class CreateRecurringRuleBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "arg_room_id";

    public interface OnRuleCreatedListener {
        void onRuleCreated(RecurringRule rule);
    }

    private String roomId;
    private OnRuleCreatedListener listener;

    private String selectedFrequency = RecurringRule.FREQUENCY_MONTHLY;
    private int selectedExecutionDay = 15;

    public static CreateRecurringRuleBottomSheet newInstance(String roomId) {
        CreateRecurringRuleBottomSheet sheet = new CreateRecurringRuleBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
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

        TextInputEditText etTitle = view.findViewById(R.id.etRecurringTitle);
        TextInputEditText etAmount = view.findViewById(R.id.etRecurringAmount);
        TextInputEditText etCategory = view.findViewById(R.id.etRecurringCategory);
        TextInputEditText etDay = view.findViewById(R.id.etRecurringDay);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmitRecurringRule);

        ChipGroup chipGroupFrequency = view.findViewById(R.id.chipGroupFrequency);
        ChipGroup chipGroupWeekDays = view.findViewById(R.id.chipGroupWeekDays);
        LinearLayout layoutMonthly = view.findViewById(R.id.layoutMonthlyDayPicker);
        LinearLayout layoutWeekly = view.findViewById(R.id.layoutWeeklyDayPicker);

        if (chipGroupFrequency != null) {
            chipGroupFrequency.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                if (id == R.id.chipMonthly) {
                    selectedFrequency = RecurringRule.FREQUENCY_MONTHLY;
                    if (layoutMonthly != null) layoutMonthly.setVisibility(View.VISIBLE);
                    if (layoutWeekly != null) layoutWeekly.setVisibility(View.GONE);
                } else if (id == R.id.chipWeekly) {
                    selectedFrequency = RecurringRule.FREQUENCY_WEEKLY;
                    if (layoutMonthly != null) layoutMonthly.setVisibility(View.GONE);
                    if (layoutWeekly != null) layoutWeekly.setVisibility(View.VISIBLE);
                } else if (id == R.id.chipDaily) {
                    selectedFrequency = RecurringRule.FREQUENCY_DAILY;
                    if (layoutMonthly != null) layoutMonthly.setVisibility(View.GONE);
                    if (layoutWeekly != null) layoutWeekly.setVisibility(View.GONE);
                }
            });
        }

        if (chipGroupWeekDays != null) {
            chipGroupWeekDays.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                Chip chip = view.findViewById(id);
                if (chip != null && chip.getTag() != null) {
                    try {
                        selectedExecutionDay = Integer.parseInt(chip.getTag().toString());
                    } catch (NumberFormatException ignored) {}
                }
            });
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "Tiện ích & Nhà";

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
                    execDay = selectedExecutionDay > 0 ? selectedExecutionDay : 2;
                } else {
                    execDay = 1;
                }

                RecurringRule rule = new RecurringRule();
                rule.setId(UUID.randomUUID().toString());
                rule.setTitle(title);
                rule.setAmount(amount);
                rule.setCategoryName(category);
                rule.setFrequency(selectedFrequency);
                rule.setExecutionDay(execDay);
                rule.setType("Chi");
                rule.setActive(true);

                if (roomId != null && !roomId.trim().isEmpty()) {
                    rule.setRoomId(roomId);
                    rule.setScope(RecurringRule.SCOPE_ROOM);
                } else {
                    rule.setScope(RecurringRule.SCOPE_PERSONAL);
                }

                if (listener != null) {
                    listener.onRuleCreated(rule);
                }
                dismiss();
            });
        }
    }
}
