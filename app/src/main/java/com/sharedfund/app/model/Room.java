package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.text.DecimalFormat;

public class Room implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("join_code")
    private String joinCode;

    @SerializedName("balance")
    private double balance;

    @SerializedName("target_amount")
    private double targetAmount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("created_by")
    private String createdBy;

    @SerializedName("is_archived")
    private boolean isArchived;

    @SerializedName("created_at")
    private String createdAt;

    public Room() {
        this.currency = "VND";
    }

    public Room(String name, String description, String joinCode, double balance, String createdBy) {
        this.name = name;
        this.description = description;
        this.joinCode = joinCode;
        this.balance = balance;
        this.createdBy = createdBy;
        this.currency = "VND";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedBalance() {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(balance) + " ₫";
    }
}
