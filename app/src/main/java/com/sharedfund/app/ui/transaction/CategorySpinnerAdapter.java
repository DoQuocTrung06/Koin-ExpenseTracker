package com.sharedfund.app.ui.transaction;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sharedfund.app.R;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

/** Spinner adapter that shows a recognizable icon next to each Supabase category. */
final class CategorySpinnerAdapter extends ArrayAdapter<String> {
    private final Context context;

    CategorySpinnerAdapter(Context context, String[] categories) {
        super(context, android.R.layout.simple_spinner_item, categories);
        this.context = context;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createCategoryRow(position);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createCategoryRow(position);
    }

    private View createCategoryRow(int position) {
        String category = getItem(position);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setMinimumHeight(dp(48));

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconFor(category));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconParams.setMarginEnd(dp(12));
        row.addView(icon, iconParams);

        TextView label = new TextView(context);
        label.setText(category);
        label.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        label.setTextSize(15);
        row.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private int iconFor(String category) {
        String key = Normalizer.normalize(category == null ? "" : category, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .toLowerCase(Locale.ROOT);
        if (containsAny(key, "an uong", "thuc an", "do an", "food", "restaurant", "cafe", "an"))
            return R.drawable.ic_restaurant;
        if (containsAny(key, "mua sam", "shopping", "hang hoa"))
            return R.drawable.ic_shopping;
        if (containsAny(key, "xe", "di chuyen", "xang", "transport", "travel"))
            return R.drawable.ic_transport;
        if (containsAny(key, "quy", "gop", "dong quy", "fund"))
            return R.drawable.ic_group;
        if (containsAny(key, "vi", "wallet", "tien"))
            return R.drawable.ic_wallet;
        return R.drawable.ic_home_category;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (keyword.contains(" ") ? value.contains(keyword)
                    : Arrays.asList(value.split("[^a-z0-9]+")).contains(keyword)) return true;
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
