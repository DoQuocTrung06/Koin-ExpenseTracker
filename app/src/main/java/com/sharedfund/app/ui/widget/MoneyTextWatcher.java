package com.sharedfund.app.ui.widget;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MoneyTextWatcher implements TextWatcher {
    private final EditText editText;
    private final OnAmountChangedListener listener;
    private boolean isFormatting = false;
    private final DecimalFormat formatter;

    public interface OnAmountChangedListener {
        void onAmountChanged(double amount, String raw);
    }

    public MoneyTextWatcher(EditText editText) {
        this(editText, null);
    }

    public MoneyTextWatcher(EditText editText, OnAmountChangedListener listener) {
        this.editText = editText;
        this.listener = listener;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        this.formatter = new DecimalFormat("#,###", symbols);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (isFormatting || s == null) return;
        String cleanString = s.toString().replace(".", "").replace(",", "").trim();
        if (cleanString.isEmpty()) {
            if (listener != null) listener.onAmountChanged(0.0, "");
            return;
        }
        try {
            isFormatting = true;
            long val = Long.parseLong(cleanString);
            String formatted = formatter.format(val);
            editText.setText(formatted);
            editText.setSelection(formatted.length());
            if (listener != null) listener.onAmountChanged(val, cleanString);
        } catch (NumberFormatException ignored) {
        } finally {
            isFormatting = false;
        }
    }

    public static double parseAmount(EditText editText) {
        if (editText == null || editText.getText() == null) return 0.0;
        return parseAmount(editText.getText().toString());
    }

    public static double parseAmount(String raw) {
        if (raw == null) return 0.0;
        String clean = raw.replace(".", "").replace(",", "").trim();
        if (clean.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static String format(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount);
    }
}
