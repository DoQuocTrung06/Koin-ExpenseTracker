package com.sharedfund.app.model;

import java.io.Serializable;

public class AppNotification implements Serializable {
    public static final String TYPE_EXPENSE_APPROVAL = "EXPENSE_APPROVAL";
    public static final String TYPE_MEMBER_JOIN = "MEMBER_JOIN";
    public static final String TYPE_RECURRING_DUE = "RECURRING_DUE";
    public static final String TYPE_FUND_ACTIVITY = "FUND_ACTIVITY";

    private String id;
    private String type;
    private String fundName;
    private String title;
    private String description;
    private String timeAgo;
    private boolean isRead;
    private String targetRoomId;
    private String targetRoomName;

    public AppNotification() {
    }

    public AppNotification(String id, String type, String fundName, String title, String description, String timeAgo, boolean isRead, String targetRoomId, String targetRoomName) {
        this.id = id;
        this.type = type;
        this.fundName = fundName;
        this.title = title;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
