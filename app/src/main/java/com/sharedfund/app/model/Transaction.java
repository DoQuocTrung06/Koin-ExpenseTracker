package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.text.DecimalFormat;

public class Transaction implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("room_id")
    private String roomId;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("user_name")
    private String userName;

    @SerializedName("amount")
    private double amount;

    @SerializedName("type")
    private String type;

    @SerializedName("category")
    private String category;

    @SerializedName("note")
    private String note;

    @SerializedName("bill_image_url")
    private String billImageUrl;

    @SerializedName("balance_after")
    private double balanceAfter;

    @SerializedName("status")
    private String status;

    @SerializedName("transaction_date")
    private String transactionDate;

    @SerializedName("created_at")
    private String createdAt;

    public Transaction() {
        this.status = "COMPLETED";
    }

    public Transaction(String roomId, String userName, double amount, String type, String category, String note) {
        this.roomId = roomId;
        this.userName = userName;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.note = note;
        this.status = "COMPLETED";
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

    public String getBillImageUrl() {
        return billImageUrl;
    }

    public void setBillImageUrl(String billImageUrl) {
        this.billImageUrl = billImageUrl;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isExpense() {
        return "EXPENSE".equalsIgnoreCase(type);
    }

    public String getFormattedAmount() {
        DecimalFormat formatter = new DecimalFormat("#,###");
        if (isExpense()) {
            return "- " + formatter.format(amount) + " ₫";
        } else {
            return "+ " + formatter.format(amount) + " ₫";
        }
    }

    public String getFormattedBalanceAfter() {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(balanceAfter) + " ₫";
    }
}
