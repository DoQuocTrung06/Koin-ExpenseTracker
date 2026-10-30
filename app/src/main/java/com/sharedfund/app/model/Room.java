package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Room implements Serializable {
    private String id;
    private String name;
    @SerializedName("join_code")
    private String joinCode;
    private double balance;
    private String description;
    @SerializedName("member_count")
    private int memberCount;
    private List<String> members;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("created_by")
    private String createdBy;
    @SerializedName("target_amount")
    private double targetAmount;
    private String currency;
    @SerializedName("is_archived")
    private boolean archived;
    private String currentUserRole = "MEMBER";

    public Room() {
        this.members = new ArrayList<>();
    }

    public Room(String name, double balance) {
        this.name = name;
        this.balance = balance;
        this.members = new ArrayList<>();
    }

    public Room(String id, String name, String joinCode, double balance, String description, int memberCount, List<String> members) {
        this.id = id;
        this.name = name;
        this.joinCode = joinCode;
        this.balance = balance;
        this.description = description;
        this.memberCount = memberCount;
        this.members = members != null ? members : new ArrayList<String>();
    }

    public Room(String id, String name, String joinCode, double balance, String description, int memberCount, List<String> members, String createdAt) {
        this(id, name, joinCode, balance, description, memberCount, members);
        this.createdAt = createdAt;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMemberCount() {
        if (members != null && !members.isEmpty()) {
            return members.size();
        }
        return memberCount > 0 ? memberCount : 1;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public String getCurrentUserRole() { return currentUserRole; }
    public void setCurrentUserRole(String currentUserRole) { this.currentUserRole = currentUserRole; }

    public String getFormattedBalance() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        return formatter.format(balance) + " ₫";
    }
}