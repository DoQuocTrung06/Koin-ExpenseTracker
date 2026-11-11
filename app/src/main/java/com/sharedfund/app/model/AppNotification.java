package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class AppNotification implements Serializable {
    public static final String TYPE_EXPENSE_APPROVAL = "EXPENSE_APPROVAL";
    public static final String TYPE_MEMBER_JOIN = "MEMBER_JOIN";
    public static final String TYPE_RECURRING_DUE = "RECURRING_DUE";
    public static final String TYPE_FUND_ACTIVITY = "FUND_ACTIVITY";

    @SerializedName("id")
    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("type")
    private String type;
    @SerializedName("fund_name")
    private String fundName;
    @SerializedName("title")
    private String title;
    @SerializedName("content")
    private String content;
    @SerializedName("description")
    private String description;
    private transient String timeAgo;
    @SerializedName("is_read")
    private boolean isRead;
    @SerializedName("room_id")
    private String targetRoomId;
    private transient String targetRoomName;
    @SerializedName("created_at")
    private String createdAt;

    public AppNotification() {
    }

    public AppNotification(String id, String type, String fundName, String title, String description, String timeAgo, boolean isRead, String targetRoomId, String targetRoomName) {
        this.id = id;
        this.type = type;
        this.fundName = fundName;
        this.title = title;
        this.description = description;
        this.content = description;
        this.timeAgo = timeAgo;
        this.isRead = isRead;
        this.targetRoomId = targetRoomId;
        this.targetRoomName = targetRoomName;
    }

    public AppNotification(String id, String userId, String type, String fundName, String title, String description, String timeAgo, boolean isRead, String targetRoomId, String targetRoomName) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.fundName = fundName;
        this.title = title;
        this.description = description;
        this.content = description;
        this.timeAgo = timeAgo;
        this.isRead = isRead;
        this.targetRoomId = targetRoomId;
        this.targetRoomName = targetRoomName;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFundName() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName = fundName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        if (content != null && !content.trim().isEmpty()) {
            return content;
        }
        return description;
    }

    public void setContent(String content) {
        this.content = content;
        if (this.description == null || this.description.trim().isEmpty()) {
            this.description = content;
        }
    }

    public String getDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        return content;
    }

    public String getMessage() {
        return getDescription();
    }

    public void setMessage(String message) {
        setDescription(message);
    }

    public void setDescription(String description) {
        this.description = description;
        if (this.content == null || this.content.trim().isEmpty()) {
            this.content = description;
        }
    }

    public String getTimeAgo() {
        if (timeAgo != null && !timeAgo.trim().isEmpty()) {
            return timeAgo;
        }
        if (createdAt != null && !createdAt.trim().isEmpty()) {
            return formatRelativeTime(createdAt);
        }
        return "Vừa xong";
    }

    public static long parseCreatedAtMillis(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) return 0L;
        try {
            String cleanIso = isoString.trim();
            int dotIdx = cleanIso.indexOf('.');
            if (dotIdx != -1) {
                int plusIdx = cleanIso.indexOf('+', dotIdx);
                int zIdx = cleanIso.indexOf('Z', dotIdx);
                cleanIso = cleanIso.substring(0, dotIdx) + (zIdx != -1 ? "Z" : (plusIdx != -1 ? cleanIso.substring(plusIdx) : ""));
            }
            if (cleanIso.endsWith("Z")) {
                cleanIso = cleanIso.substring(0, cleanIso.length() - 1);
            }
            int plusIdx = cleanIso.indexOf('+');
            if (plusIdx != -1) {
                cleanIso = cleanIso.substring(0, plusIdx);
            }
            java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat(
                    cleanIso.contains("T") ? "yyyy-MM-dd'T'HH:mm:ss" : "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date parsed = isoFormat.parse(cleanIso);
            return parsed != null ? parsed.getTime() : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String formatRelativeTime(String isoString) {
        long millis = parseCreatedAtMillis(isoString);
        if (millis > 0L) {
            long diffMs = System.currentTimeMillis() - millis;
            if (diffMs < 60000L) {
                return "Vừa xong";
            }
            long minutes = diffMs / 60000L;
            if (minutes < 60) {
                return minutes + " phút trước";
            }
            long hours = minutes / 60;
            if (hours < 24) {
                return hours + " giờ trước";
            }
            long days = hours / 24;
            if (days < 7) {
                return days + " ngày trước";
            }
            java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            return outFormat.format(new java.util.Date(millis));
        }
        return "Vừa xong";
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getTargetRoomId() {
        return targetRoomId;
    }

    public void setTargetRoomId(String targetRoomId) {
        this.targetRoomId = targetRoomId;
    }

    public String getTargetRoomName() {
        return targetRoomName;
    }

    public void setTargetRoomName(String targetRoomName) {
        this.targetRoomName = targetRoomName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
