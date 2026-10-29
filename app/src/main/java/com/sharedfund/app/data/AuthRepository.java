package com.sharedfund.app.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sharedfund.app.model.Profile;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(Profile profile);
        void onError(String errorMessage);
    }

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Context context;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    public void signUp(String email, String password, String fullName, AuthCallback callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);

        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("full_name", fullName);
        payload.add("data", dataObj);

        RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(SupabaseConfig.SUPABASE_URL + "/auth/v1/signup")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
                        String userId = "";
                        String token = "";

                        if (root.has("access_token")) {
                            token = root.get("access_token").getAsString();
                        }
                        if (root.has("user")) {
                            JsonObject userObj = root.getAsJsonObject("user");
                            userId = userObj.get("id").getAsString();
                        } else if (root.has("id")) {
                            userId = root.get("id").getAsString();
                        }

                        Profile profile = new Profile(userId, email, fullName, "");
                        saveProfileToDatabase(profile, token, callback);
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                } else {
                    callback.onError("Sign up failed: " + responseBody);
                }
            }
        });
    }

    public void signIn(String email, String password, AuthCallback callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);

        RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(SupabaseConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
                        String token = root.has("access_token") ? root.get("access_token").getAsString() : "";
                        String userId = "";

                        if (root.has("user")) {
                            JsonObject userObj = root.getAsJsonObject("user");
                            userId = userObj.get("id").getAsString();
                        }

                        fetchProfileFromDatabase(userId, email, token, callback);
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                } else {
                    callback.onError("Sign in failed: " + responseBody);
                }
            }
        });
    }

    private void saveProfileToDatabase(Profile profile, String token, AuthCallback callback) {
        if (token == null || token.trim().isEmpty()) {
            // Email confirmation is enabled: Auth user exists, but there is no authenticated
            // session yet, so the client must not pretend the profile row was persisted.
            callback.onSuccess(profile);
            return;
        }
        RequestBody body = RequestBody.create(gson.toJson(profile), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/profiles")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + (token.isEmpty() ? SupabaseConfig.SUPABASE_ANON_KEY : token))
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                UserSessionManager.getInstance(context).saveSession(
                        profile.getId(), profile.getEmail(), profile.getFullName(), profile.getAvatarUrl(), token
                );
                callback.onSuccess(profile);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    String details = "";
                    try { details = response.body() != null ? response.body().string() : ""; }
                    catch (IOException ignored) { }
                    callback.onError("Cannot save profile: " + details);
                    return;
                }
                UserSessionManager.getInstance(context).saveSession(
                        profile.getId(), profile.getEmail(), profile.getFullName(), profile.getAvatarUrl(), token);
                callback.onSuccess(profile);
            }
        });
    }

    private void fetchProfileFromDatabase(String userId, String email, String token, AuthCallback callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/profiles?id=eq." + userId + "&select=*")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + (token.isEmpty() ? SupabaseConfig.SUPABASE_ANON_KEY : token))
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Cannot load profile: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("Cannot load profile (HTTP " + response.code() + "): " + responseBody);
                    return;
                }
                Profile profile = null;
                try {
                    Profile[] list = gson.fromJson(responseBody, Profile[].class);
                    if (list != null && list.length > 0) {
                        profile = list[0];
                    }
                } catch (Exception ignored) {
                }

                if (profile == null) {
                    callback.onError("Profile not found for authenticated user.");
                    return;
                }

                UserSessionManager.getInstance(context).saveSession(
                        profile.getId(), profile.getEmail(), profile.getFullName(), profile.getAvatarUrl(), token
                );
                callback.onSuccess(profile);
            }
        });
    }
}
