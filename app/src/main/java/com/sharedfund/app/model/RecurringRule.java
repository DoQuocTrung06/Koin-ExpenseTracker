package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;
import com.sharedfund.app.R;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class RecurringRule implements Serializable {
    public static final String SCOPE_PERSONAL = "PERSONAL";
    public static final String SCOPE_ROOM = "ROOM";
    public static final String FREQUENCY_DAILY = "DAILY";
    public static final String FREQUENCY_WEEKLY = "WEEKLY";
    public static final String FREQUENCY_MONTHLY = "MONTHLY";

    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("user_name")
    private String userName;
    @SerializedName("room_id")
    private String roomId;
    @SerializedName("room_name")
    private String roomName;
    private String scope;
    private String title;
    private double amount;
    private String currency;
    private String type;
    @SerializedName("category_name")
    private String categoryName;
    private String frequency;
    @SerializedName("execution_day")
    private int executionDay;
    @SerializedName("next_run_date")
    private String nextRunDate;
    @SerializedName("last_run_date")
    private String lastRunDate;
    @SerializedName("is_active")
    private boolean isActive;
    @SerializedName("created_at")
    private String createdAt;

    public RecurringRule() {
        this.currency = "VND";
        this.frequency = FREQUENCY_MONTHLY;
        this.isActive = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getFrequency() {
        return frequency != null ? frequency : FREQUENCY_MONTHLY;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public int getExecutionDay() {
        return executionDay;
    }

    public void setExecutionDay(int executionDay) {
        this.executionDay = executionDay;
    }

    public String getNextRunDate() {
        return nextRunDate;
    }

    public void setNextRunDate(String nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    public String getLastRunDate() {
        return lastRunDate;
    }

    public void setLastRunDate(String lastRunDate) {
        this.lastRunDate = lastRunDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isIncome() {
        return "Thu".equalsIgnoreCase(type) || "INCOME".equalsIgnoreCase(type);
    }

    public String getFormattedAmount() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        String formatted = formatter.format(amount) + " ₫";
        return isIncome() ? ("+" + formatted) : ("-" + formatted);
    }

    public String getFrequencyDescription() {
        if (FREQUENCY_DAILY.equalsIgnoreCase(frequency)) {
            return "Hằng ngày";
        }
        if (FREQUENCY_WEEKLY.equalsIgnoreCase(frequency)) {
            String dayLabel;
            switch (executionDay) {
                case 1:
                case 8:
                    dayLabel = "Chủ nhật";
                    break;
                case 2:
                    dayLabel = "Thứ 2";
                    break;
                case 3:
                    dayLabel = "Thứ 3";
                    break;
                case 4:
                    dayLabel = "Thứ 4";
                    break;
                case 5:
                    dayLabel = "Thứ 5";
                    break;
                case 6:
                    dayLabel = "Thứ 6";
                    break;
                case 7:
                    dayLabel = "Thứ 7";
                    break;
                default:
                    dayLabel = "Thứ " + executionDay;
                    break;
            }
            return dayLabel + " hàng tuần";
        }
        if (executionDay >= 31) {
            return "Ngày 31 (hoặc cuối tháng)";
        }
        if (executionDay >= 29) {
            return "Ngày " + executionDay + " (hoặc cuối tháng)";
        }
        return "Ngày " + (executionDay > 0 ? executionDay : 1) + " hàng tháng";
    }

    public static int getSafeExecutionDay(int year, int month1Based, int desiredDay) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.YEAR, year);
        cal.set(java.util.Calendar.MONTH, month1Based - 1);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        int maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        return Math.min(Math.max(desiredDay, 1), maxDay);
    }

    public java.util.Date computeNextRunDate(java.util.Date fromDate) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (fromDate != null) {
            cal.setTime(fromDate);
        }
        if (FREQUENCY_DAILY.equalsIgnoreCase(frequency)) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if (FREQUENCY_WEEKLY.equalsIgnoreCase(frequency)) {
            int currentDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
            int targetDayOfWeek = executionDay;
            int diff = (targetDayOfWeek - currentDayOfWeek + 7) % 7;
            if (diff == 0) diff = 7;
            cal.add(java.util.Calendar.DAY_OF_YEAR, diff);
        } else {
            int currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH);
            int maxDayCurrentMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
            int safeDayCurrent = Math.min(Math.max(executionDay, 1), maxDayCurrentMonth);

            if (currentDay < safeDayCurrent) {
                cal.set(java.util.Calendar.DAY_OF_MONTH, safeDayCurrent);
            } else {
                cal.add(java.util.Calendar.MONTH, 1);
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                int maxDayNextMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
                int safeDayNext = Math.min(Math.max(executionDay, 1), maxDayNextMonth);
                cal.set(java.util.Calendar.DAY_OF_MONTH, safeDayNext);
            }
        }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 8);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public String computeNextRunDateIso() {
        java.util.Date next = computeNextRunDate(new java.util.Date());
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(next);
    }

    public int getCategoryIconRes() {
        if (categoryName == null) return R.drawable.ic_recurring_calendar;
        String lower = categoryName.toLowerCase(Locale.ROOT);
        if (lower.contains("ăn") || lower.contains("uống")) return R.drawable.ic_restaurant;
        if (lower.contains("sắm")) return R.drawable.ic_shopping;
        if (lower.contains("phòng") || lower.contains("nhà") || lower.contains("trọ")) return R.drawable.ic_home_category;
        if (lower.contains("xe") || lower.contains("chuyển")) return R.drawable.ic_transport;
        if (lower.contains("lương")) return R.drawable.ic_wallet;
        if (lower.contains("thưởng") || lower.contains("liên hoan")) return R.drawable.ic_sparkles;
        return R.drawable.ic_recurring_calendar;
    }
}
