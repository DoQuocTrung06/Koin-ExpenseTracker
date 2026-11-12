package com.sharedfund.app.ui.room;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sharedfund.app.R;
import com.sharedfund.app.ui.widget.DonutChartView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomCategoryBreakdownBinder {

    private final DonutChartView donutHistoryChartView;
    private final TextView tvEmptyCategoryBars;
    private final LinearLayout layoutHistoryCategoryBars;
    private final DecimalFormat df;

    private boolean isCategoryListExpanded = false;

    public RoomCategoryBreakdownBinder(
            DonutChartView donutHistoryChartView,
            TextView tvEmptyCategoryBars,
            LinearLayout layoutHistoryCategoryBars,
            DecimalFormat df
    ) {
        this.donutHistoryChartView = donutHistoryChartView;
        this.tvEmptyCategoryBars = tvEmptyCategoryBars;
        this.layoutHistoryCategoryBars = layoutHistoryCategoryBars;
        this.df = df != null ? df : new DecimalFormat("#,##0");
    }

    public void renderCategoryBreakdown(Context context, Map<String, Double> map) {
        if (context == null || layoutHistoryCategoryBars == null) return;
        layoutHistoryCategoryBars.removeAllViews();

        int[] palette = getFintechPalette(context);

        if (map == null || map.isEmpty()) {
            if (donutHistoryChartView != null) {
                donutHistoryChartView.setData(map, palette);
            }
            if (tvEmptyCategoryBars != null) {
                tvEmptyCategoryBars.setVisibility(View.VISIBLE);
            }
            return;
        }

        double totalExpense = 0.0;
        for (Double amt : map.values()) {
            if (amt != null && amt > 0) {
                totalExpense += amt;
            }
        }

        if (totalExpense <= 0) {
            if (donutHistoryChartView != null) {
                donutHistoryChartView.setData(map, palette);
            }
            if (tvEmptyCategoryBars != null) {
                tvEmptyCategoryBars.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (tvEmptyCategoryBars != null) {
            tvEmptyCategoryBars.setVisibility(View.GONE);
        }

        List<DonutChartView.Slice> sliceList = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            double amt = entry.getValue() != null ? entry.getValue() : 0.0;
            if (amt <= 0) continue;
            float percent = (float) ((amt / totalExpense) * 100.0);
            int color = getCategoryColor(context, entry.getKey(), colorIdx);
            sliceList.add(new DonutChartView.Slice(entry.getKey(), amt, color, percent));
            colorIdx++;
        }

        Collections.sort(sliceList, (a, b) -> Double.compare(b.amount, a.amount));

        if (donutHistoryChartView != null) {
            donutHistoryChartView.setSlices(sliceList);
        }

        int maxItemsToShow = isCategoryListExpanded ? sliceList.size() : Math.min(3, sliceList.size());
        for (int i = 0; i < maxItemsToShow; i++) {
            DonutChartView.Slice slice = sliceList.get(i);
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpToPx(context, 6), 0, dpToPx(context, 6));

            View square = new View(context);
            LinearLayout.LayoutParams sqParams = new LinearLayout.LayoutParams(dpToPx(context, 10), dpToPx(context, 10));
            sqParams.setMarginEnd(dpToPx(context, 8));
            square.setLayoutParams(sqParams);
            GradientDrawable sqDrawable = new GradientDrawable();
            sqDrawable.setShape(GradientDrawable.OVAL);
            sqDrawable.setColor(slice.color);
            square.setBackground(sqDrawable);

            TextView tvCategoryInfo = new TextView(context);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tvCategoryInfo.setLayoutParams(infoParams);
            tvCategoryInfo.setText(slice.label + " (" + String.format(Locale.US, "%.1f", slice.percentage) + "%)");
            tvCategoryInfo.setTextSize(13.5f);
            tvCategoryInfo.setTextColor(ContextCompat.getColor(context, R.color.text_primary));

            TextView tvCategoryAmount = new TextView(context);
            tvCategoryAmount.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tvCategoryAmount.setText("-" + df.format(slice.amount).replace(',', '.') + " ₫");
            tvCategoryAmount.setTextSize(13.5f);
            tvCategoryAmount.setTypeface(null, Typeface.BOLD);
            tvCategoryAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary));

            row.addView(square);
            row.addView(tvCategoryInfo);
            row.addView(tvCategoryAmount);
            layoutHistoryCategoryBars.addView(row);
        }

        if (sliceList.size() > 3) {
            TextView btnToggle = new TextView(context);
            LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            toggleParams.setMargins(0, dpToPx(context, 4), 0, dpToPx(context, 2));
            btnToggle.setLayoutParams(toggleParams);
            btnToggle.setGravity(Gravity.CENTER);
            btnToggle.setPadding(0, dpToPx(context, 6), 0, dpToPx(context, 6));
            btnToggle.setTextSize(12.5f);
            btnToggle.setTextColor(ContextCompat.getColor(context, R.color.primary));
            btnToggle.setTypeface(null, Typeface.BOLD);
            btnToggle.setBackgroundResource(R.drawable.bg_card);

            if (!isCategoryListExpanded) {
                int remaining = sliceList.size() - 3;
                double remainingAmount = 0.0;
                for (int i = 3; i < sliceList.size(); i++) {
                    remainingAmount += sliceList.get(i).amount;
                }
                btnToggle.setText("Xem thêm " + remaining + " danh mục khác (" + df.format(remainingAmount).replace(',', '.') + " ₫)  ▼");
            } else {
                btnToggle.setText("Thu gọn danh mục  ▲");
            }

            btnToggle.setOnClickListener(v -> {
                isCategoryListExpanded = !isCategoryListExpanded;
                renderCategoryBreakdown(context, map);
            });

            layoutHistoryCategoryBars.addView(btnToggle);
        }
    }

    private int[] getFintechPalette(Context context) {
        return new int[]{
                ContextCompat.getColor(context, R.color.accent_expense),
                ContextCompat.getColor(context, R.color.accent_amber),
                ContextCompat.getColor(context, R.color.primary),
                ContextCompat.getColor(context, R.color.accent_income),
                ContextCompat.getColor(context, R.color.role_owner_text),
                ContextCompat.getColor(context, R.color.role_vice_text),
                ContextCompat.getColor(context, R.color.text_secondary)
        };
    }

    private int getCategoryColor(Context context, String categoryName, int fallbackIndex) {
        if (categoryName == null) return ContextCompat.getColor(context, R.color.text_secondary);
        String lower = categoryName.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("ăn") || lower.contains("uống") || lower.contains("food")) {
            return ContextCompat.getColor(context, R.color.accent_expense);
        }
        if (lower.contains("di chuyển") || lower.contains("xe") || lower.contains("xăng") || lower.contains("đi lại")) {
            return ContextCompat.getColor(context, R.color.accent_amber);
        }
        if (lower.contains("phòng") || lower.contains("nhà")) {
            return ContextCompat.getColor(context, R.color.primary);
        }
        if (lower.contains("mua") || lower.contains("sắm") || lower.contains("chợ") || lower.contains("siêu thị")) {
            return ContextCompat.getColor(context, R.color.role_vice_text);
        }
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet") || lower.contains("mạng") || lower.contains("4g") || lower.contains("5g")) {
            return ContextCompat.getColor(context, R.color.role_owner_text);
        }
        if (lower.contains("điện") || lower.contains("nước") || lower.contains("hóa đơn")) {
            return ContextCompat.getColor(context, R.color.accent_income);
        }
        if (lower.contains("giải trí") || lower.contains("phim") || lower.contains("party") || lower.contains("liên hoan")) {
            return ContextCompat.getColor(context, R.color.accent_expense);
        }
        if (lower.contains("rút")) {
            return ContextCompat.getColor(context, R.color.text_secondary);
        }
        int[] palette = getFintechPalette(context);
        return palette[Math.abs(fallbackIndex) % palette.length];
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
