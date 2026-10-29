package com.sharedfund.app.model;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Model biểu diễn Quỹ chung (Room / Shared Fund)
 * Thống nhất theo mục 6.1 tài liệu Giai đoạn 3
 */
public class Room implements Serializable {
    private String id;
    private String name;
    private String joinCode;
    private double balance;
    private String description;
    private int memberCount;
    private List<String> members;
    private String createdAt;

    public Room() {
        this.members = new ArrayList<>();
    }

    public Room(String name, String description, String joinCode, double balance, List<String> members) {
        this(null, name, joinCode, balance, description, members != null ? members.size() : 0, members, null);
    }

    public Room(String id, String name, String joinCode, double balance, String description, int memberCount, List<String> members, String createdAt) {
        this.id = id;
        this.name = name;
        this.joinCode = joinCode;
        this.balance = balance;
        this.description = description;
        this.memberCount = memberCount;
        this.members = members != null ? members : new ArrayList<>();
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
        return memberCount;
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

    /**
     * Định dạng số tiền sang định dạng tiền tệ VNĐ chuẩn hóa (ví dụ: 1.500.000 ₫)
     * Sử dụng DecimalFormatSymbols độc lập với cài đặt ngôn ngữ hệ điều hành để đảm bảo
     * không bị lỗi dấu phân cách hay cắt số tiền dài.
     */
    public String getFormattedBalance() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        return formatter.format(balance) + " ₫";
    }
}
