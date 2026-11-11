package com.sharedfund.app.ui.recurring;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.model.RecurringRule;
import com.sharedfund.app.ui.widget.AppToaster;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CreateRecurringRuleFragment extends Fragment {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_ROOM_NAME = "arg_room_name";
    private static final String ARG_RULE_EDIT = "arg_rule_edit";

    private String roomId;
    private String roomName;
    private RecurringRule ruleToEdit;

    private String selectedFrequency = RecurringRule.FREQUENCY_MONTHLY;
    private int selectedWeekDay = 2;
    private boolean isImeOpen = false;

    private MaterialButton btnFreqMonthly;
    private MaterialButton btnFreqWeekly;
    private MaterialButton btnFreqDaily;

    private View layoutMonthly;
    private LinearLayout layoutWeekly;
    private LinearLayout layoutQuickAmountBar;

    private TextInputEditText etRecurringTitle;
    private TextInputEditText etRecurringAmount;
    private TextInputLayout tilRecurringAmount;
    private TextInputEditText etRecurringDay;

    private final List<MaterialButton> weekDayButtons = new ArrayList<>();
    private final TransactionRepository transactionRepository = new TransactionRepository();

    public static CreateRecurringRuleFragment newInstance(String roomId, String roomName, RecurringRule ruleToEdit) {
        CreateRecurringRuleFragment fragment = new CreateRecurringRuleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putString(ARG_ROOM_NAME, roomName);
        if (ruleToEdit != null) {
            args.putSerializable(ARG_RULE_EDIT, ruleToEdit);
        }
        fragment.setArguments(args);
        return fragment;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_recurring_rule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btnCreateRecurringBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }

        TextView tvTitle = view.findViewById(R.id.tvCreateRecurringScreenTitle);
        etRecurringTitle = view.findViewById(R.id.etRecurringTitle);
        etRecurringAmount = view.findViewById(R.id.etRecurringAmount);
        tilRecurringAmount = view.findViewById(R.id.tilRecurringAmount);
        etRecurringDay = view.findViewById(R.id.etRecurringDay);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmitRecurringRule);

        btnFreqMonthly = view.findViewById(R.id.btnFreqMonthly);
        btnFreqWeekly = view.findViewById(R.id.btnFreqWeekly);
        btnFreqDaily = view.findViewById(R.id.btnFreqDaily);

        layoutMonthly = view.findViewById(R.id.layoutMonthlyDayPicker);
        layoutWeekly = view.findViewById(R.id.layoutWeeklyDayPicker);
        layoutQuickAmountBar = view.findViewById(R.id.layoutQuickAmountBar);

        setupFrequencySegments();
        setupWeekDayPills(view);
        setupQuickAmounts(view);
        setupKeyboardAdaptiveBehavior(view);
        setupAmountFormatter();

        if (ruleToEdit != null) {
            if (tvTitle != null) tvTitle.setText("Chỉnh sửa khoản định kỳ");
            if (btnSubmit != null) btnSubmit.setText("Lưu thay đổi");

            if (etRecurringTitle != null) etRecurringTitle.setText(ruleToEdit.getTitle());
            if (etRecurringAmount != null) {
                DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.GERMANY);
                sym.setGroupingSeparator('.');
                DecimalFormat df = new DecimalFormat("#,###", sym);
                etRecurringAmount.setText(df.format((long) Math.abs(ruleToEdit.getAmount())));
            }

            selectedFrequency = ruleToEdit.getFrequency();
            selectFrequency(selectedFrequency);

            if (RecurringRule.FREQUENCY_MONTHLY.equalsIgnoreCase(selectedFrequency)) {
                if (etRecurringDay != null) etRecurringDay.setText(String.valueOf(ruleToEdit.getExecutionDay()));
            } else if (RecurringRule.FREQUENCY_WEEKLY.equalsIgnoreCase(selectedFrequency)) {
                selectedWeekDay = ruleToEdit.getExecutionDay();
                updateWeekDayPillsState();
            }
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> handleSubmit());
        }
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
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
        selectFrequency(selectedFrequency != null ? selectedFrequency : RecurringRule.FREQUENCY_MONTHLY);
    }

    private void selectFrequency(String frequency) {
        selectedFrequency = frequency;
        if (!isAdded() || getContext() == null) return;
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

    private void setupQuickAmounts(View root) {
        MaterialButton btn50k = root.findViewById(R.id.btnQuick50k);
        MaterialButton btn100k = root.findViewById(R.id.btnQuick100k);
        MaterialButton btn500k = root.findViewById(R.id.btnQuick500k);
        MaterialButton btn1m = root.findViewById(R.id.btnQuick1m);

        if (btn50k != null) btn50k.setOnClickListener(v -> applyQuickAmount(50000));
        if (btn100k != null) btn100k.setOnClickListener(v -> applyQuickAmount(100000));
        if (btn500k != null) btn500k.setOnClickListener(v -> applyQuickAmount(500000));
        if (btn1m != null) btn1m.setOnClickListener(v -> applyQuickAmount(1000000));
    }

    private void applyQuickAmount(long amount) {
        if (etRecurringAmount == null) return;
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.GERMANY);
        sym.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", sym);
        etRecurringAmount.setText(df.format(amount));
        if (tilRecurringAmount != null) {
            tilRecurringAmount.setError(null);
        }
        if (etRecurringAmount.getText() != null) {
            etRecurringAmount.setSelection(etRecurringAmount.getText().length());
        }
    }

    private void setupAmountFormatter() {
        if (etRecurringAmount == null) return;
        etRecurringAmount.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting || s == null) return;
                String cleanString = s.toString().replace(".", "").replace(",", "").trim();
                if (cleanString.isEmpty()) return;
                try {
                    isFormatting = true;
                    long val = Long.parseLong(cleanString);
                    DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.GERMANY);
                    sym.setGroupingSeparator('.');
                    DecimalFormat df = new DecimalFormat("#,###", sym);
                    String formatted = df.format(val);
                    etRecurringAmount.setText(formatted);
                    etRecurringAmount.setSelection(formatted.length());
                } catch (NumberFormatException ignored) {
                } finally {
                    isFormatting = false;
                }
            }
        });
    }

    private void setupKeyboardAdaptiveBehavior(View rootView) {
        if (etRecurringAmount != null) {
            etRecurringAmount.setOnFocusChangeListener((v, hasFocus) -> updateKeyboardAdaptiveDock());
            etRecurringAmount.setOnClickListener(v -> {
                if (layoutQuickAmountBar != null) {
                    layoutQuickAmountBar.setVisibility(View.VISIBLE);
                }
            });
        }

        View.OnFocusChangeListener otherFocusListener = (v, hasFocus) -> {
            if (hasFocus) {
                updateKeyboardAdaptiveDock();
            }
        };

        if (etRecurringTitle != null) {
            etRecurringTitle.setOnFocusChangeListener(otherFocusListener);
        }
        if (etRecurringDay != null) {
            etRecurringDay.setOnFocusChangeListener(otherFocusListener);
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            androidx.core.graphics.Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int imeBottom = imeInsets.bottom;
            isImeOpen = imeVisible || imeBottom > 0;
            int bottomPadding = isImeOpen ? imeBottom : sysBars.bottom;
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, bottomPadding);
            updateKeyboardAdaptiveDock();
            return insets;
        });
    }

    private void updateKeyboardAdaptiveDock() {
        boolean isAmountFocused = etRecurringAmount != null && etRecurringAmount.hasFocus();
        if (layoutQuickAmountBar != null) {
            layoutQuickAmountBar.setVisibility(isImeOpen && isAmountFocused ? View.VISIBLE : View.GONE);
        }
    }

    private void handleSubmit() {
        if (roomId != null && !roomId.trim().isEmpty()) {
            com.sharedfund.app.model.Room room = RoomRepository.getInstance().getRoomSync(roomId);
            if (room != null && !RoomRepository.getInstance().isCurrentUserAdmin(roomId)) {
                if (getActivity() != null) {
                    AppToaster.error(getActivity(), "Bạn không có quyền quản lý khoản định kỳ này");
                }
                return;
            }
        }

        String title = etRecurringTitle.getText() != null ? etRecurringTitle.getText().toString().trim() : "";
        String amountStr = etRecurringAmount.getText() != null ? etRecurringAmount.getText().toString().trim() : "";

        if (title.isEmpty()) {
            etRecurringTitle.setError("Vui lòng nhập tên khoản chi");
            etRecurringTitle.requestFocus();
            return;
        }

        if (amountStr.isEmpty()) {
            if (tilRecurringAmount != null) {
                tilRecurringAmount.setError("Vui lòng nhập số tiền");
            }
            etRecurringAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            if (tilRecurringAmount != null) {
                tilRecurringAmount.setError("Số tiền không hợp lệ");
            }
            etRecurringAmount.requestFocus();
            return;
        }

        int execDay = 15;
        if (RecurringRule.FREQUENCY_MONTHLY.equalsIgnoreCase(selectedFrequency)) {
            String dayStr = etRecurringDay.getText() != null ? etRecurringDay.getText().toString().trim() : "15";
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
        boolean isEdit = (ruleToEdit != null);
        RecurringRule rule = isEdit ? ruleToEdit : new RecurringRule();
        if (!isEdit) {
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

        Context ctx = getContext();
        if (isEdit) {
            transactionRepository.updateRecurringRule(rule, new TransactionRepository.DataCallback<RecurringRule>() {
                @Override
                public void onSuccess(RecurringRule data) {
                    RoomRepository.getInstance().notifyRecurringRulesChanged();
                }

                @Override
                public void onError(String error) {
                    RoomRepository.getInstance().notifyRecurringRulesChanged();
                }
            });
            if (ctx != null && getActivity() != null) {
                AppToaster.success(getActivity(), "Đã lưu thay đổi khoản định kỳ");
            }
        } else {
            transactionRepository.createRecurringRule(rule, new TransactionRepository.DataCallback<RecurringRule>() {
                @Override
                public void onSuccess(RecurringRule created) {
                    RoomRepository.getInstance().notifyRecurringRulesChanged();
                }

                @Override
                public void onError(String error) {
                    RoomRepository.getInstance().notifyRecurringRulesChanged();
                }
            });
            if (ctx != null && getActivity() != null) {
                AppToaster.success(getActivity(), "Đã lưu khoản định kỳ thành công");
            }
        }

        navigateBack();
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
