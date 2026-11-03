package com.sharedfund.app.util;

import java.util.regex.Pattern;

public class PasswordStrengthEvaluator {

    public enum Level {
        EMPTY,
        WEAK,
        MEDIUM,
        STRONG
    }

    public static class Evaluation {
        private final Level level;
        private final int progressPercent;
        private final String label;
        private final boolean minLength;
        private final boolean upperAndLower;
        private final boolean digit;
        private final boolean special;

        public Evaluation(Level level, int progressPercent, String label,
                          boolean minLength, boolean upperAndLower, boolean digit, boolean special) {
            this.level = level;
            this.progressPercent = progressPercent;
            this.label = label;
            this.minLength = minLength;
            this.upperAndLower = upperAndLower;
            this.digit = digit;
            this.special = special;
        }

        public Level getLevel() {
            return level;
        }

        public int getProgressPercent() {
            return progressPercent;
        }

        public String getLabel() {
            return label;
        }

        public boolean hasMinLength() {
            return minLength;
        }

        public boolean hasUpperAndLower() {
            return upperAndLower;
        }

        public boolean hasDigit() {
            return digit;
        }

        public boolean hasSpecial() {
            return special;
        }
    }

    private static final Pattern UPPER_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]");

    public static Evaluation evaluate(String password) {
        if (password == null || password.isEmpty()) {
            return new Evaluation(Level.EMPTY, 0, "Chưa nhập",
                    false, false, false, false);
        }

        boolean minLength = password.length() >= 8;
        boolean hasUpper = UPPER_PATTERN.matcher(password).find();
        boolean hasLower = LOWER_PATTERN.matcher(password).find();
        boolean upperAndLower = hasUpper && hasLower;
        boolean digit = DIGIT_PATTERN.matcher(password).find();
        boolean special = SPECIAL_PATTERN.matcher(password).find();
        boolean hasLetterAndDigit = (hasUpper || hasLower) && digit;

        Level level;
        int progress;
        String label;

        if (!minLength || !hasLetterAndDigit) {
            level = Level.WEAK;
            progress = 33;
            label = "Mật khẩu yếu";
        } else if (upperAndLower && digit && special) {
            level = Level.STRONG;
            progress = 100;
            label = "Mật khẩu mạnh";
        } else {
            level = Level.MEDIUM;
            progress = 66;
            label = "Mật khẩu trung bình";
        }

        return new Evaluation(level, progress, label,
                minLength, upperAndLower, digit, special);
    }

    public static boolean hasLettersAndNumbers(String password) {
        if (password == null) return false;
        boolean hasLetter = LETTER_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        return hasLetter && hasDigit;
    }
}
