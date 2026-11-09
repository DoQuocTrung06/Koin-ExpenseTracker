package com.sharedfund.app.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CustomDateRangeDialog extends Dialog {

    public interface OnDateRangeSelectedListener {
        void onRangeSelected(long startMillis, long endMillis, String formattedRange);
    }

    private final OnDateRangeSelectedListener listener;
    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public CustomDateRangeDialog(@NonNull Context context, Long start, Long end, OnDateRangeSelectedListener listener) {
        super(context);
        this.listener = listener;
        if (start != null && start > 0) startCal.setTimeInMillis(start);
        if (end != null && end > 0) endCal.setTimeInMillis(end);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_date_range_picker);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.5f);
        }

        ImageButton btnDialogClose = findViewById(R.id.btnDialogClose);
        View cardDialogStartDate = findViewById(R.id.cardDialogStartDate);
        View cardDialogEndDate = findViewById(R.id.cardDialogEndDate);
        TextView tvDialogStartDate = findViewById(R.id.tvDialogStartDate);
        TextView tvDialogEndDate = findViewById(R.id.tvDialogEndDate);
        MaterialButton btnDialogCancel = findViewById(R.id.btnDialogCancel);
        MaterialButton btnDialogApply = findViewById(R.id.btnDialogApply);

        tvDialogStartDate.setText(sdf.format(startCal.getTime()));
        tvDialogEndDate.setText(sdf.format(endCal.getTime()));

        if (btnDialogClose != null) btnDialogClose.setOnClickListener(v -> dismiss());
        if (btnDialogCancel != null) btnDialogCancel.setOnClickListener(v -> dismiss());

        if (cardDialogStartDate != null) {
            cardDialogStartDate.setOnClickListener(v -> showSingleDatePicker("Chọn ngày bắt đầu", startCal, tvDialogStartDate));
        }

        if (cardDialogEndDate != null) {
            cardDialogEndDate.setOnClickListener(v -> showSingleDatePicker("Chọn ngày kết thúc", endCal, tvDialogEndDate));
        }

        attachTouchEffect(cardDialogStartDate);
        attachTouchEffect(cardDialogEndDate);
        attachTouchEffect(btnDialogCancel);
        attachTouchEffect(btnDialogApply);
        attachTouchEffect(btnDialogClose);

        if (btnDialogApply != null) {
            btnDialogApply.setOnClickListener(v -> {
                if (listener != null) {
                    String range = sdf.format(startCal.getTime()) + " - " + sdf.format(endCal.getTime());
                    listener.onRangeSelected(startCal.getTimeInMillis(), endCal.getTimeInMillis(), range);
                }
                dismiss();
            });
        }
    }

    private void showSingleDatePicker(String title, Calendar targetCal, TextView targetTv) {
        Dialog pickerDialog = new Dialog(getContext(), R.style.Theme_SharedFund_DatePicker);
        pickerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pickerDialog.setContentView(R.layout.dialog_single_date_picker);

        Window window = pickerDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.5f);
        }

        TextView tvTitle = pickerDialog.findViewById(R.id.tvPickerTitle);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }

        CalendarView cv = pickerDialog.findViewById(R.id.calendarView);
        if (cv != null) {
            cv.setDate(targetCal.getTimeInMillis(), false, true);
            cv.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                targetCal.set(Calendar.YEAR, year);
                targetCal.set(Calendar.MONTH, month);
                targetCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                targetTv.setText(sdf.format(targetCal.getTime()));
                pickerDialog.dismiss();
            });

            final float[] downX = new float[1];
            final float[] downY = new float[1];
            final float density = getContext().getResources().getDisplayMetrics().density;
            final float headerThreshold = 75f * density;

            cv.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX[0] = event.getX();
                        downY[0] = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float diffX = Math.abs(event.getX() - downX[0]);
                        float diffY = Math.abs(event.getY() - downY[0]);
                        if (diffX < 15f * density && diffY < 15f * density && downY[0] >= headerThreshold) {
                            cv.postDelayed(() -> {
                                if (pickerDialog.isShowing()) {
                                    targetTv.setText(sdf.format(targetCal.getTime()));
                                    pickerDialog.dismiss();
                                }
                            }, 120);
                        }
                        break;
                }
                return false;
            });
        }

        View btnClose = pickerDialog.findViewById(R.id.btnPickerClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> pickerDialog.dismiss());
            attachTouchEffect(btnClose);
        }

        pickerDialog.show();
    }

    private void attachTouchEffect(View v) {
        if (v == null) return;
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.setAlpha(0.75f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.setAlpha(1.0f);
                    break;
            }
            return false;
        });
    }
}
