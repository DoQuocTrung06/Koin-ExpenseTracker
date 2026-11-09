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
        return frequency;
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
            return "Thứ " + (executionDay == 1 ? "CN" : String.valueOf(executionDay)) + " hằng tuần";
        }
        return "Ngày " + executionDay + " hằng tháng";
    }

    public int getCategoryIconRes() {
        if (categoryName == null) return R.drawable.ic_receipt;
        String lower = categoryName.toLowerCase(Locale.ROOT);
        if (lower.contains("ăn") || lower.contains("uống")) return R.drawable.ic_restaurant;
        if (lower.contains("sắm")) return R.drawable.ic_shopping;
        if (lower.contains("phòng") || lower.contains("nhà") || lower.contains("trọ")) return R.drawable.ic_home_category;
        if (lower.contains("xe") || lower.contains("chuyển")) return R.drawable.ic_transport;
        if (lower.contains("lương")) return R.drawable.ic_wallet;
        if (lower.contains("thưởng") || lower.contains("liên hoan")) return R.drawable.ic_sparkles;
        return R.drawable.ic_receipt;
    }
}
