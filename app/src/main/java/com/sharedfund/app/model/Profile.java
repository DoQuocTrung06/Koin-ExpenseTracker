package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Profile implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("wallet_balance")
    private double walletBalance;

    public Profile() {
    }

    public Profile(String id, String email, String fullName, String avatarUrl) {
        this(id, email, fullName, avatarUrl, 0.0);
    }

    public Profile(String id, String email, String fullName, String avatarUrl, double walletBalance) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.walletBalance = walletBalance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
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

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }
}
