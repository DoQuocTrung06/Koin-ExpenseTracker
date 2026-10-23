package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;

public class RoomMember implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("room_id")
    private String roomId;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("role")
    private String role;

    @SerializedName("total_contributed")
    private double totalContributed;

    @SerializedName("joined_at")
    private String joinedAt;

    private String fullName;
    private String avatarUrl;
    private String email;

    public RoomMember() {
    }

    public RoomMember(String userId, String displayName, String avatarUrl, double contributedAmount, boolean isAdmin, String joinedAt) {
        this.userId = userId;
        this.fullName = displayName;
        this.avatarUrl = avatarUrl;
        this.totalContributed = contributedAmount;
        this.role = isAdmin ? "ADMIN" : "MEMBER";
        this.joinedAt = joinedAt;
    }

    public RoomMember(String id, String roomId, String userId, String role, double totalContributed, String joinedAt) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.role = role;
        this.totalContributed = totalContributed;
        this.joinedAt = joinedAt;
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

    public String getRole() {
        return role != null ? role : "MEMBER";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }

    public void setAdmin(boolean admin) {
        this.role = admin ? "ADMIN" : "MEMBER";
    }

    public double getTotalContributed() {
        return totalContributed;
    }

    public void setTotalContributed(double totalContributed) {
        this.totalContributed = totalContributed;
    }

    public double getContributedAmount() {
        return totalContributed;
    }

    public void setContributedAmount(double contributedAmount) {
        this.totalContributed = contributedAmount;
    }

    public String getDisplayName() {
        return getFullName();
    }

    public void setDisplayName(String displayName) {
        this.fullName = displayName;
    }

    public String getFormattedContributed() {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(totalContributed) + " đ";
    }

    public String getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(String joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getFullName() {
        return (fullName != null && !fullName.trim().isEmpty()) ? fullName : "Thành viên";
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInitials() {
        String name = getDisplayName();
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split(" ");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return String.valueOf(name.charAt(0)).toUpperCase();
    }
}
