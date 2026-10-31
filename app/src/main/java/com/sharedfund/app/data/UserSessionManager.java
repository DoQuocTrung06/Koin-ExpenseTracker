package com.sharedfund.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sharedfund.app.model.Profile;

public class UserSessionManager {

    private static final String PREF_NAME = "shared_fund_user_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_OCCUPATION = "occupation";
    private static final String KEY_EDUCATION = "education";
    private static final String KEY_BIO = "bio";

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

    public static synchronized UserSessionManager getInstance() {
        return instance;
    }

    public void saveSession(String userId, String email, String fullName, String avatarUrl) {
        saveSession(userId, email, fullName, avatarUrl, "", "", 0);
    }

    public void saveSession(String userId, String email, String fullName, String avatarUrl, String accessToken) {
        saveSession(userId, email, fullName, avatarUrl, accessToken, "", 0);
    }

    public void saveSession(String userId, String email, String fullName, String avatarUrl,
                            String accessToken, String refreshToken, long expiresInSeconds) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_AVATAR_URL, avatarUrl);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);

        long expTime = extractExpiresAt(accessToken, expiresInSeconds);
        editor.putLong(KEY_EXPIRES_AT, expTime);
        editor.apply();
    }

    public void updateProfile(String fullName, String email) {
        SharedPreferences.Editor editor = preferences.edit();
        if (fullName != null) {
            editor.putString(KEY_FULL_NAME, fullName);
        }
        if (email != null) {
            editor.putString(KEY_EMAIL, email);
        }
        editor.apply();
    }

    public void updateTokens(String accessToken, String refreshToken, long expiresInSeconds) {
        SharedPreferences.Editor editor = preferences.edit();
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
        }
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        long expTime = extractExpiresAt(accessToken, expiresInSeconds);
        editor.putLong(KEY_EXPIRES_AT, expTime);
        editor.apply();
    }

    private long extractExpiresAt(String token, long expiresInSeconds) {
        if (expiresInSeconds > 0) {
            return (System.currentTimeMillis() / 1000) + expiresInSeconds;
        }
        if (token != null && !token.trim().isEmpty()) {
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE);
                    JsonObject obj = JsonParser.parseString(new String(decoded)).getAsJsonObject();
                    if (obj.has("exp")) {
                        return obj.get("exp").getAsLong();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return (System.currentTimeMillis() / 1000) + 3600;
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean isTokenExpired() {
        String token = getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            return true;
        }
        long expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0);
        if (expiresAt <= 0) {
            expiresAt = extractExpiresAt(token, 0);
            preferences.edit().putLong(KEY_EXPIRES_AT, expiresAt).apply();
        }
        return (System.currentTimeMillis() / 1000) >= (expiresAt - 60);
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

    public void saveAvatarUrl(String avatarUrl) {
        preferences.edit().putString(KEY_AVATAR_URL, avatarUrl != null ? avatarUrl : "").apply();
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, "");
    }

    public String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, "");
    }

    public String getNickname() {
        return preferences.getString(KEY_NICKNAME, "");
    }

    public String getOccupation() {
        return preferences.getString(KEY_OCCUPATION, "");
    }

    public String getEducation() {
        return preferences.getString(KEY_EDUCATION, "");
    }

    public String getBio() {
        return preferences.getString(KEY_BIO, "");
    }

    public void updateCustomProfile(String nickname, String occupation, String education, String bio) {
        SharedPreferences.Editor editor = preferences.edit();
        if (nickname != null) {
            editor.putString(KEY_NICKNAME, nickname);
        }
        if (occupation != null) {
            editor.putString(KEY_OCCUPATION, occupation);
        }
        if (education != null) {
            editor.putString(KEY_EDUCATION, education);
        }
        if (bio != null) {
            editor.putString(KEY_BIO, bio);
        }
        editor.apply();
    }

    public Profile getCurrentProfile() {
        if (!isLoggedIn()) {
            return null;
        }
        Profile profile = new Profile(getUserId(), getEmail(), getFullName(), getAvatarUrl());
        profile.setNickname(getNickname());
        profile.setOccupation(getOccupation());
        profile.setEducation(getEducation());
        profile.setBio(getBio());
        return profile;
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