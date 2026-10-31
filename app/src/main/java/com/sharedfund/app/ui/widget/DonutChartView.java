package com.sharedfund.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
    private String centerTitle = "Tổng chi tiêu";

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Rect textBounds = new Rect();

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

    public void setCenterTitle(String title) {
        this.centerTitle = title;
        invalidate();
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

    public List<Slice> getSlices() {
        return slices;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int density = (int) getResources().getDisplayMetrics().density;
        int defaultHeight = 220 * density;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = heightMode == MeasureSpec.EXACTLY ? MeasureSpec.getSize(heightMeasureSpec) : defaultHeight;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float padding = 16f * getResources().getDisplayMetrics().density;
        float diameter = Math.min(width, height) - (padding * 2);
        if (diameter <= 0) return;

        float strokeWidth = diameter * 0.16f;
        arcPaint.setStrokeWidth(strokeWidth);

        float cx = width / 2f;
        float cy = height / 2f;
        float radius = (diameter - strokeWidth) / 2f;
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        if (slices.isEmpty() || totalAmount <= 0) {
            arcPaint.setColor(ContextCompat.getColor(getContext(), R.color.border));
            canvas.drawArc(oval, 0f, 360f, false, arcPaint);

            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            textPaint.setTextSize(13f * getResources().getDisplayMetrics().scaledDensity);
            textPaint.setTypeface(Typeface.DEFAULT);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Chưa có chi tiêu", cx, cy + (textPaint.getTextSize() / 3f), textPaint);
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
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        textPaint.setTextSize(12f * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTypeface(Typeface.DEFAULT);
        canvas.drawText(centerTitle, cx, cy - 8f * getResources().getDisplayMetrics().density, textPaint);

        String formattedTotal = df.format(totalAmount) + " ₫";
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textPaint.setTextSize(16f * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(formattedTotal, cx, cy + 14f * getResources().getDisplayMetrics().density, textPaint);
    }
}
