package com.sharedfund.app.ui.recurring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
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

        TextInputEditText etTitle = view.findViewById(R.id.etRecurringTitle);
        TextInputEditText etAmount = view.findViewById(R.id.etRecurringAmount);
        TextInputEditText etCategory = view.findViewById(R.id.etRecurringCategory);
        TextInputEditText etDay = view.findViewById(R.id.etRecurringDay);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmitRecurringRule);

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "Tiện ích & Nhà";
                String dayStr = etDay.getText() != null ? etDay.getText().toString().trim() : "1";

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

                int day = 1;
                try {
                    day = Integer.parseInt(dayStr);
                    if (day < 1 || day > 31) day = 1;
                } catch (NumberFormatException ignored) {}

                RecurringRule rule = new RecurringRule();
                rule.setId(UUID.randomUUID().toString());
                rule.setTitle(title);
                rule.setAmount(amount);
                rule.setCategoryName(category);
                rule.setFrequency(RecurringRule.FREQUENCY_MONTHLY);
                rule.setExecutionDay(day);
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
