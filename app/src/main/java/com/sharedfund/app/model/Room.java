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
    private transient String currentUserRole;

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

    public static final String TREASURER_TAG = "||TREASURERS:";
    public static final String PRIVATE_TAG = "||PRIVATE:";
    public static final String REQUESTS_TAG = "||REQUESTS:";
    public static final String NUDGE_TAG = "||NUDGES:";

    public String getRawDescription() {
        return description;
    }

    public String getDescription() {
        return stripMetadata(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static String stripMetadata(String raw) {
        return RoomMetadataCodec.stripMetadata(raw);
    }

    public static String stripTreasurerTag(String raw) {
        return RoomMetadataCodec.stripMetadata(raw);
    }

    public boolean hasServerTreasurers() {
        return description != null && (description.contains(TREASURER_TAG) || description.startsWith("treasurers:"));
    }

    public java.util.Set<String> getTreasurerIds() {
        return RoomMetadataCodec.parseTreasurers(description);
    }

    public boolean isPrivateRoom() {
        return RoomMetadataCodec.parseIsPrivate(description);
    }

    public static class JoinRequestMeta {
        public String userId;
        public String applicantName;
        public String email;
        public String time;
        public String message;

        public JoinRequestMeta() {}
        public JoinRequestMeta(String userId, String applicantName, String email, String time, String message) {
            this.userId = userId;
            this.applicantName = applicantName;
            this.email = email;
            this.time = time;
            this.message = message;
        }

        public String getName() {
            return applicantName;
        }
    }

    public java.util.List<JoinRequestMeta> getPendingJoinRequests() {
        return RoomMetadataCodec.parsePendingJoinRequests(description);
    }

    public java.util.Map<String, Long> getRecurringNudgeTimestamps() {
        return RoomMetadataCodec.parseRecurringNudgeTimestamps(description);
    }

    public static String buildFullDescription(String cleanDesc, java.util.Set<String> treasurers, boolean isPrivate, java.util.List<JoinRequestMeta> requests) {
        return RoomMetadataCodec.buildFullDescription(cleanDesc, treasurers, isPrivate, requests, null);
    }

    public static String buildFullDescription(String cleanDesc, java.util.Set<String> treasurers, boolean isPrivate, java.util.List<JoinRequestMeta> requests, java.util.Map<String, Long> nudges) {
        return RoomMetadataCodec.buildFullDescription(cleanDesc, treasurers, isPrivate, requests, nudges);
    }

    public static String buildDescriptionWithTreasurers(String cleanDesc, java.util.Set<String> treasurerIds) {
        return buildFullDescription(cleanDesc, treasurerIds, false, null);
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

    public String getOwnerId() {
        return createdBy;
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