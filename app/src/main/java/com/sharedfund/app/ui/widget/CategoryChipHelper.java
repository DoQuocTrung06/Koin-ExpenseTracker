package com.sharedfund.app.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sharedfund.app.R;

import java.util.ArrayList;
import java.util.List;

public class CategoryChipHelper {

    public interface OnCategorySelectedListener {
        void onSelected(String categoryName);
    }

    public static class CategoryItem {
        public final String name;
        public final int iconRes;

        public CategoryItem(String name, int iconRes) {
            this.name = name;
            this.iconRes = iconRes;
        }
    }

    public static List<CategoryItem> getDefaultExpenseCategories() {
        List<CategoryItem> list = new ArrayList<>();
        list.add(new CategoryItem("Ăn uống", R.drawable.ic_restaurant));
        list.add(new CategoryItem("Mua sắm", R.drawable.ic_shopping));
        list.add(new CategoryItem("Liên hoan", R.drawable.ic_sparkles));
        list.add(new CategoryItem("Tiền phòng", R.drawable.ic_home_category));
        list.add(new CategoryItem("Điện nước", R.drawable.ic_receipt));
        list.add(new CategoryItem("Di chuyển", R.drawable.ic_transport));
        list.add(new CategoryItem("Rút về ví", R.drawable.ic_wallet));
        list.add(new CategoryItem("Khác", R.drawable.ic_receipt));
        return list;
    }

    public static void populateChips(Context context, LinearLayout container, List<CategoryItem> categories, String selectedCategory, OnCategorySelectedListener listener) {
        if (container == null || context == null || categories == null) return;
        container.removeAllViews();
        float density = context.getResources().getDisplayMetrics().density;
        int height = Math.round(38 * density);
        int padH = Math.round(12 * density);
        int marginE = Math.round(8 * density);
        int iconSize = Math.round(18 * density);
        int iconMargin = Math.round(6 * density);

        for (CategoryItem item : categories) {
            boolean isSelected = selectedCategory != null && item.name.equalsIgnoreCase(selectedCategory);

            LinearLayout chip = new LinearLayout(context);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(padH, 0, padH, 0);
            chip.setBackgroundResource(isSelected ? R.drawable.bg_category_chip_selected : R.drawable.bg_category_chip_unselected);
            chip.setClickable(true);
            chip.setFocusable(true);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
            lp.setMarginEnd(marginE);
            chip.setLayoutParams(lp);

            ImageView iv = new ImageView(context);
            iv.setImageResource(item.iconRes);
            int iconColor = isSelected ? ContextCompat.getColor(context, R.color.primary) : ContextCompat.getColor(context, R.color.text_secondary);
            iv.setColorFilter(iconColor);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            ivLp.setMarginEnd(iconMargin);
            chip.addView(iv, ivLp);

            TextView tv = new TextView(context);
            tv.setText(item.name);
            tv.setTextSize(13);
            tv.setTextColor(isSelected ? ContextCompat.getColor(context, R.color.primary) : ContextCompat.getColor(context, R.color.text_primary));
            if (isSelected) {
                tv.setTypeface(null, Typeface.BOLD);
            }
            chip.addView(tv);

            chip.setOnClickListener(v -> {
                if (listener != null) {
                    if (isSelected) {
                        listener.onSelected(null);
                    } else {
                        listener.onSelected(item.name);
                    }
                }
            });

            container.addView(chip);
        }
    }
}
