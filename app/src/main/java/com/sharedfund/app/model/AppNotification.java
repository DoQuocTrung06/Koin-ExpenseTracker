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

    public void setDescription(String description) {
        this.description = description;
        if (this.content == null || this.content.trim().isEmpty()) {
            this.content = description;
        }
    }

    public String getTimeAgo() {
        return timeAgo;
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
