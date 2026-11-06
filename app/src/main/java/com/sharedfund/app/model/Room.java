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
        if (raw == null) return "";
        String clean = raw;
        int idx1 = clean.indexOf(TREASURER_TAG);
        if (idx1 >= 0) clean = clean.substring(0, idx1);
        int idx2 = clean.indexOf(PRIVATE_TAG);
        if (idx2 >= 0) clean = clean.substring(0, idx2);
        int idx3 = clean.indexOf(REQUESTS_TAG);
        if (idx3 >= 0) clean = clean.substring(0, idx3);
        if (clean.startsWith("treasurers:")) return "";
        return clean.trim();
    }

    public static String stripTreasurerTag(String raw) {
        return stripMetadata(raw);
    }

    public boolean hasServerTreasurers() {
        return description != null && (description.contains(TREASURER_TAG) || description.startsWith("treasurers:"));
    }

    public java.util.Set<String> getTreasurerIds() {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (description == null) return set;
        String rawIds = extractTagValue(description, TREASURER_TAG);
        if (rawIds == null && description.startsWith("treasurers:")) {
            int end = description.indexOf("||");
            rawIds = end >= 0 ? description.substring("treasurers:".length(), end) : description.substring("treasurers:".length());
        }
        if (rawIds != null && !rawIds.trim().isEmpty()) {
            for (String p : rawIds.split(",")) {
                if (!p.trim().isEmpty()) set.add(p.trim());
            }
        }
        return set;
    }

    public boolean isPrivateRoom() {
        if (description == null) return false;
        String val = extractTagValue(description, PRIVATE_TAG);
        return "true".equalsIgnoreCase(val);
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
    }

    public java.util.List<JoinRequestMeta> getPendingJoinRequests() {
        java.util.List<JoinRequestMeta> list = new java.util.ArrayList<>();
        if (description == null) return list;
        String rawJson = extractTagValue(description, REQUESTS_TAG);
        if (rawJson != null && !rawJson.trim().isEmpty()) {
            try {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<JoinRequestMeta>>(){}.getType();
                java.util.List<JoinRequestMeta> parsed = new com.google.gson.Gson().fromJson(rawJson, type);
                if (parsed != null) list.addAll(parsed);
            } catch (Exception ignored) {}
        }
        return list;
    }

    private static String extractTagValue(String text, String tag) {
        if (text == null || tag == null) return null;
        int start = text.indexOf(tag);
        if (start < 0) return null;
        start += tag.length();
        int end = text.indexOf("||", start);
        return end >= 0 ? text.substring(start, end).trim() : text.substring(start).trim();
    }

    public static String buildFullDescription(String cleanDesc, java.util.Set<String> treasurers, boolean isPrivate, java.util.List<JoinRequestMeta> requests) {
        StringBuilder sb = new StringBuilder(stripMetadata(cleanDesc));
        if (treasurers != null && !treasurers.isEmpty()) {
            sb.append(TREASURER_TAG);
            boolean first = true;
            for (String t : treasurers) {
                if (t != null && !t.trim().isEmpty()) {
                    if (!first) sb.append(",");
                    sb.append(t.trim());
                    first = false;
                }
            }
        }
        if (isPrivate) {
            sb.append(PRIVATE_TAG).append("true");
        }
        if (requests != null && !requests.isEmpty()) {
            sb.append(REQUESTS_TAG).append(new com.google.gson.Gson().toJson(requests));
        }
        return sb.toString();
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