package com.sharedfund.app.util;

import java.util.ArrayList;
import java.util.List;

public final class VietnameseCurrencyWords {

    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] UNITS = {
            "", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"
    };

    private VietnameseCurrencyWords() {}

    private static String readThreeDigits(int number, boolean hasHigher) {
        int hundreds = number / 100;
        int tens = (number % 100) / 10;
        int ones = number % 10;
        List<String> res = new ArrayList<>();

        if (hundreds > 0 || hasHigher) {
            res.add(DIGITS[hundreds] + " trăm");
        }

        if (tens > 1) {
            res.add(DIGITS[tens] + " mươi");
            if (ones == 1) {
                res.add("mốt");
            } else if (ones == 5) {
                res.add("lăm");
            } else if (ones > 0) {
                res.add(DIGITS[ones]);
            }
        } else if (tens == 1) {
            res.add("mười");
            if (ones == 5) {
                res.add("lăm");
            } else if (ones > 0) {
                res.add(DIGITS[ones]);
            }
        } else {
            if ((hundreds > 0 || hasHigher) && ones > 0) {
                res.add("linh " + DIGITS[ones]);
            } else if (ones > 0) {
                res.add(DIGITS[ones]);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < res.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(res.get(i));
        }
        return sb.toString();
    }

    public static String toWords(long amount) {
        if (amount <= 0) {
            return "Không đồng";
        }

        List<Integer> groups = new ArrayList<>();
        long temp = amount;
        while (temp > 0) {
            groups.add((int) (temp % 1000));
            temp /= 1000;
        }

        List<String> resGroups = new ArrayList<>();
        for (int i = groups.size() - 1; i >= 0; i--) {
            int val = groups.get(i);
            if (val > 0) {
                boolean hasHigher = (i < groups.size() - 1);
                String text = readThreeDigits(val, hasHigher);
                if (!UNITS[i].isEmpty()) {
                    text += " " + UNITS[i];
                }
                resGroups.add(text);
            }
        }

        StringBuilder full = new StringBuilder();
        for (int i = 0; i < resGroups.size(); i++) {
            if (i > 0) full.append(" ");
            full.append(resGroups.get(i));
        }
        full.append(" đồng");

        String str = full.toString().trim();
        if (!str.isEmpty()) {
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        }
        return str;
    }

    public static String toWordsWithParentheses(long amount) {
        return "(" + toWords(amount) + ")";
    }
}
