package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Model biểu diễn Giao dịch (Transaction)
 * Thống nhất theo mục 6.2 tài liệu Giai đoạn 3
 */
public class Transaction implements Serializable {
    public static final String TYPE_INCOME = "Thu";
    public static final String TYPE_EXPENSE = "Chi";

    private String id;
    @SerializedName("room_id")
    private String roomId;
    private double amount;
    private String type; // "Thu" hoặc "Chi"
    private String category; // Ăn uống, Di chuyển, Mua sắm, Thuê phòng, Đóng quỹ,...
    private String note; // Lời tựa
    @SerializedName("user_name")
    private String createdBy; // ID hoặc tên hiển thị người tạo
    @SerializedName("user_id")
    private String userId;
    @SerializedName("created_at")
    private String createdAt; // Thời gian tạo
    @SerializedName("transaction_date")
    private String transactionDate;
    @SerializedName("bill_image_url")
    private String billImageUrl;
    @SerializedName("balance_after")
    private double balanceAfter;
    @SerializedName("status")
    private String status;

    public Transaction() {
    }

    public Transaction(String roomId, String createdBy, double amount, String type, String category, String note) {
        this(null, roomId, amount, type, category, note, createdBy, null);
    }

    public Transaction(String id, String roomId, double amount, String type, String category, String note, String createdBy, String createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.note = note;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public String getBillImageUrl() { return billImageUrl; }
    public void setBillImageUrl(String billImageUrl) { this.billImageUrl = billImageUrl; }
    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isIncome() {
        return TYPE_INCOME.equalsIgnoreCase(type) || "INCOME".equalsIgnoreCase(type);
    }

    public boolean isExpense() {
        return TYPE_EXPENSE.equalsIgnoreCase(type) || "EXPENSE".equalsIgnoreCase(type);
    }

    /**
     * Định dạng số tiền có tiền tố (+) hoặc (-) theo loại giao dịch
     * Sử dụng DecimalFormatSymbols độc lập locale, đảm bảo không bị cắt số tiền dài.
     */
    public String getFormattedAmount() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        String formatted = formatter.format(amount) + " ₫";
        if (isIncome()) {
            return "+" + formatted;
        } else {
            return "-" + formatted;
        }
    }
}
