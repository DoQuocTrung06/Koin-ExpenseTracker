package com.sharedfund.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sharedfund.app.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DonutChartView extends View {

    public static class Slice {
        public final String label;
        public final double amount;
        public final int color;
        public final float percentage;

        public Slice(String label, double amount, int color, float percentage) {
            this.label = label;
            this.amount = amount;
            this.color = color;
            this.percentage = percentage;
        }
    }

    private final List<Slice> slices = new ArrayList<>();
    private double totalAmount = 0.0;
    private String centerTitle = "Tổng chi";

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private final DecimalFormat df;

    public DonutChartView(Context context) {
        this(context, null);
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        df = new DecimalFormat("#,##0", symbols);
    }

    public void setData(Map<String, Double> categoryMap, int[] colors) {
        slices.clear();
        totalAmount = 0.0;
        if (categoryMap != null) {
            for (Double amt : categoryMap.values()) {
                if (amt != null && amt > 0) {
                    totalAmount += amt;
                }
            }
            if (totalAmount > 0 && colors != null && colors.length > 0) {
                int colorIndex = 0;
                for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                    double amt = entry.getValue() != null ? entry.getValue() : 0.0;
                    if (amt <= 0) continue;
                    float pct = (float) ((amt / totalAmount) * 100.0);
                    int color = colors[colorIndex % colors.length];
                    slices.add(new Slice(entry.getKey(), amt, color, pct));
                    colorIndex++;
                }
            }
        }
        invalidate();
    }

    public void setSlices(List<Slice> newSlices) {
        slices.clear();
        totalAmount = 0.0;
        if (newSlices != null) {
            for (Slice s : newSlices) {
                if (s != null && s.amount > 0) {
                    totalAmount += s.amount;
                    slices.add(s);
                }
            }
        }
        invalidate();
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int density = (int) getResources().getDisplayMetrics().density;
        int defaultSize = 120 * density;
        int width = widthMode == MeasureSpec.EXACTLY ? widthSize : defaultSize;
        int height = heightMode == MeasureSpec.EXACTLY ? heightSize : defaultSize;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float padding = 8f * getResources().getDisplayMetrics().density;
        float diameter = Math.min(width, height) - (padding * 2);
        if (diameter <= 0) return;

        float strokeWidth = diameter * 0.18f;
        arcPaint.setStrokeWidth(strokeWidth);

        float cx = width / 2f;
        float cy = height / 2f;
        float radius = (diameter - strokeWidth) / 2f;
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        if (slices.isEmpty() || totalAmount <= 0) {
            arcPaint.setColor(ContextCompat.getColor(getContext(), R.color.border));
            canvas.drawArc(oval, 0f, 360f, false, arcPaint);

            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            textPaint.setTextSize(10.5f * getResources().getDisplayMetrics().scaledDensity);
            textPaint.setTypeface(Typeface.DEFAULT);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Chưa có chi", cx, cy - 2f * getResources().getDisplayMetrics().density, textPaint);
            canvas.drawText("0 ₫", cx, cy + 12f * getResources().getDisplayMetrics().density, textPaint);
            return;
        }

        float startAngle = -90f;
        float gap = slices.size() > 1 ? 2.5f : 0f;
        for (Slice slice : slices) {
            float sweepAngle = (slice.percentage / 100f) * 360f;
            float actualSweep = Math.max(1f, sweepAngle - gap);
            arcPaint.setColor(slice.color);
            canvas.drawArc(oval, startAngle + (gap / 2f), actualSweep, false, arcPaint);
            startAngle += sweepAngle;
        }

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textPaint.setTextSize(10f * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTypeface(Typeface.DEFAULT);
        canvas.drawText(centerTitle, cx, cy - 4f * getResources().getDisplayMetrics().density, textPaint);

        String formattedTotal = df.format(totalAmount) + " ₫";
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        float totalTextSizeSp = 12f;
        if (formattedTotal.length() > 14) {
            totalTextSizeSp = 8.5f;
        } else if (formattedTotal.length() > 10) {
            totalTextSizeSp = 10f;
        }
        textPaint.setTextSize(totalTextSizeSp * getResources().getDisplayMetrics().scaledDensity);
        float maxAvailableWidth = (diameter - strokeWidth * 2) * 0.9f;
        while (totalTextSizeSp > 6.5f && textPaint.measureText(formattedTotal) > maxAvailableWidth) {
            totalTextSizeSp -= 0.5f;
            textPaint.setTextSize(totalTextSizeSp * getResources().getDisplayMetrics().scaledDensity);
        }
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(formattedTotal, cx, cy + 12f * getResources().getDisplayMetrics().density, textPaint);
    }
}
