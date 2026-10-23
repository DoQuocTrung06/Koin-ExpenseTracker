package com.sharedfund.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.sharedfund.app.model.Profile;

public class UserSessionManager {

    private static final String PREF_NAME = "shared_fund_user_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private static volatile UserSessionManager instance;
    private final SharedPreferences preferences;

    private UserSessionManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized UserSessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserSessionManager(context);
        }
        return instance;
    }

    public void saveSession(String userId, String email, String fullName, String avatarUrl) {
        saveSession(userId, email, fullName, avatarUrl, "");
    }

    public void saveSession(String userId, String email, String fullName, String avatarUrl, String accessToken) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_AVATAR_URL, avatarUrl);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, "");
    }

    public String getCurrentUserId() {
        return getUserId();
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, "");
    }

    public String getCurrentUserEmail() {
        return getEmail();
    }

    public String getFullName() {
        return preferences.getString(KEY_FULL_NAME, "Người dùng");
    }

    public String getCurrentUserName() {
        return getFullName();
    }

    public String getAvatarUrl() {
        return preferences.getString(KEY_AVATAR_URL, "");
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, "");
    }

    public Profile getCurrentProfile() {
        if (!isLoggedIn()) {
            return null;
        }
        return new Profile(getUserId(), getEmail(), getFullName(), getAvatarUrl());
    }

    public Profile getCurrentUser() {
        return getCurrentProfile();
    }

    public void clearSession() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
}
