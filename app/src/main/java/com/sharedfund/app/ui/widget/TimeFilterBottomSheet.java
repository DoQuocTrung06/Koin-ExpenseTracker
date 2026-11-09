package com.sharedfund.app.ui.widget;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sharedfund.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeFilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(int filterType, Long startMillis, Long endMillis, String displayLabel);
    }

    private OnFilterAppliedListener listener;
    private int selectedPreset = 0;
    private Calendar startCal = Calendar.getInstance();
    private Calendar endCal = Calendar.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private MaterialButton btnPresetAll;
    private MaterialButton btnPresetToday;
    private MaterialButton btnPresetWeek;
    private MaterialButton btnPresetMonth;

    private MaterialCardView cardSelectStartDate;
    private MaterialCardView cardSelectEndDate;
    private TextView tvSheetStartDate;
    private TextView tvSheetEndDate;

    private MaterialButton btnSheetReset;
    private MaterialButton btnSheetApply;

    public static TimeFilterBottomSheet newInstance(int currentFilter, Long start, Long end) {
        TimeFilterBottomSheet sheet = new TimeFilterBottomSheet();
        Bundle args = new Bundle();
        args.putInt("current_filter", currentFilter);
        if (start != null) args.putLong("start_millis", start);
        if (end != null) args.putLong("end_millis", end);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_time_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnPresetAll = view.findViewById(R.id.btnPresetAll);
        btnPresetToday = view.findViewById(R.id.btnPresetToday);
        btnPresetWeek = view.findViewById(R.id.btnPresetWeek);
        btnPresetMonth = view.findViewById(R.id.btnPresetMonth);

        cardSelectStartDate = view.findViewById(R.id.cardSelectStartDate);
        cardSelectEndDate = view.findViewById(R.id.cardSelectEndDate);
        tvSheetStartDate = view.findViewById(R.id.tvSheetStartDate);
        tvSheetEndDate = view.findViewById(R.id.tvSheetEndDate);

        btnSheetReset = view.findViewById(R.id.btnSheetReset);
        btnSheetApply = view.findViewById(R.id.btnSheetApply);

        if (getArguments() != null) {
            selectedPreset = getArguments().getInt("current_filter", 0);
            long s = getArguments().getLong("start_millis", -1);
            long e = getArguments().getLong("end_millis", -1);
            if (s > 0) startCal.setTimeInMillis(s);
            if (e > 0) endCal.setTimeInMillis(e);
        }

        updateDateTexts();
        updatePresetButtons();

        btnPresetAll.setOnClickListener(v -> {
            selectedPreset = 0;
            updatePresetButtons();
        });
        btnPresetToday.setOnClickListener(v -> {
            selectedPreset = 1;
            setRangeToday();
            updatePresetButtons();
        });
        btnPresetWeek.setOnClickListener(v -> {
            selectedPreset = 2;
            setRangeThisWeek();
            updatePresetButtons();
        });
        btnPresetMonth.setOnClickListener(v -> {
            selectedPreset = 3;
            setRangeThisMonth();
            updatePresetButtons();
        });

        cardSelectStartDate.setOnClickListener(v -> openDatePicker(true));
        cardSelectEndDate.setOnClickListener(v -> openDatePicker(false));

        btnSheetReset.setOnClickListener(v -> {
            selectedPreset = 0;
            if (listener != null) {
                listener.onFilterApplied(0, null, null, "Toàn bộ");
            }
            dismiss();
        });

        btnSheetApply.setOnClickListener(v -> {
            if (listener != null) {
                if (selectedPreset == 0) {
                    listener.onFilterApplied(0, null, null, "Toàn bộ");
                } else if (selectedPreset == 1) {
                    listener.onFilterApplied(1, null, null, "Hôm nay");
                } else if (selectedPreset == 2) {
                    listener.onFilterApplied(2, null, null, "Tuần này");
                } else if (selectedPreset == 3) {
                    listener.onFilterApplied(3, null, null, "Tháng này");
                } else {
                    String label = sdf.format(startCal.getTime()) + " - " + sdf.format(endCal.getTime());
                    listener.onFilterApplied(4, startCal.getTimeInMillis(), endCal.getTimeInMillis(), label);
                }
            }
            dismiss();
        });
    }

    private void setRangeToday() {
        startCal = Calendar.getInstance();
        endCal = Calendar.getInstance();
        updateDateTexts();
    }

    private void setRangeThisWeek() {
        startCal = Calendar.getInstance();
        startCal.set(Calendar.DAY_OF_WEEK, startCal.getFirstDayOfWeek());
        endCal = (Calendar) startCal.clone();
        endCal.add(Calendar.DAY_OF_WEEK, 6);
        updateDateTexts();
    }

    private void setRangeThisMonth() {
        startCal = Calendar.getInstance();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        endCal = (Calendar) startCal.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        updateDateTexts();
    }

    private void updateDateTexts() {
        if (tvSheetStartDate != null) tvSheetStartDate.setText(sdf.format(startCal.getTime()));
        if (tvSheetEndDate != null) tvSheetEndDate.setText(sdf.format(endCal.getTime()));
    }

    private void openDatePicker(boolean isStart) {
        Calendar target = isStart ? startCal : endCal;
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            target.set(Calendar.YEAR, year);
            target.set(Calendar.MONTH, month);
            target.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedPreset = 4;
            updateDateTexts();
            updatePresetButtons();
        }, target.get(Calendar.YEAR), target.get(Calendar.MONTH), target.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updatePresetButtons() {
        if (!isAdded() || getContext() == null) return;
        int primary = ContextCompat.getColor(requireContext(), R.color.primary);
        int surface = ContextCompat.getColor(requireContext(), R.color.surface);
        int surfaceVariant = ContextCompat.getColor(requireContext(), R.color.surface_variant);
        int textSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int white = ContextCompat.getColor(requireContext(), R.color.white);

        applyButtonStyle(btnPresetAll, selectedPreset == 0, primary, surfaceVariant, white, textSecondary);
        applyButtonStyle(btnPresetToday, selectedPreset == 1, primary, surfaceVariant, white, textSecondary);
        applyButtonStyle(btnPresetWeek, selectedPreset == 2, primary, surfaceVariant, white, textSecondary);
        applyButtonStyle(btnPresetMonth, selectedPreset == 3, primary, surfaceVariant, white, textSecondary);
    }

    private void applyButtonStyle(MaterialButton btn, boolean active, int primary, int surfaceVariant, int white, int textSecondary) {
        if (btn == null) return;
        btn.setBackgroundTintList(ColorStateList.valueOf(active ? primary : surfaceVariant));
        btn.setTextColor(active ? white : textSecondary);
    }
}
