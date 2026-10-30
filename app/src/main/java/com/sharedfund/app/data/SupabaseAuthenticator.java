package com.sharedfund.app.data;

import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class SupabaseAuthenticator implements Authenticator {

    public interface SessionExpiredListener {
        void onSessionExpired();
    }

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static volatile SessionExpiredListener sessionExpiredListener;

    private final Context context;
    private final OkHttpClient refreshClient;

    public static void setSessionExpiredListener(SessionExpiredListener listener) {
        sessionExpiredListener = listener;
    }

    public static void notifyExpired(Context ctx) {
        if (ctx != null) {
            UserSessionManager.getInstance(ctx).clearSession();
        }
        if (sessionExpiredListener != null) {
            sessionExpiredListener.onSessionExpired();
        }
    }

    public SupabaseAuthenticator(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.refreshClient = new OkHttpClient.Builder()
                .dns(SafeDns.getInstance())
                .build();
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (responseCount(response) >= 2) {
            return null;
        }

        UserSessionManager session = getUserSession();
        if (session == null) {
            return null;
        }

        String refreshToken = session.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            notifySessionExpired(session);
            return null;
        }

        synchronized (this) {
            String currentAccessToken = session.getAccessToken();
            String requestHeader = response.request().header("Authorization");
            if (requestHeader != null && !requestHeader.equals("Bearer " + currentAccessToken) && !currentAccessToken.isEmpty()) {
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + currentAccessToken)
                        .build();
            }

            String newAccessToken = performTokenRefresh(session, refreshToken);
            if (newAccessToken != null && !newAccessToken.trim().isEmpty()) {
                RoomRepository.getInstance().configureAccessToken(newAccessToken);
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newAccessToken)
                        .build();
            } else {
                notifySessionExpired(session);
                return null;
            }
        }
    }

    private UserSessionManager getUserSession() {
        if (context != null) {
            return UserSessionManager.getInstance(context);
        }
        return UserSessionManager.getInstance();
    }

    private String performTokenRefresh(UserSessionManager session, String refreshToken) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("refresh_token", refreshToken);

            RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(SupabaseConfig.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token")
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response res = refreshClient.newCall(request).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    String resStr = res.body().string();
                    JsonObject root = JsonParser.parseString(resStr).getAsJsonObject();
                    String newAccess = root.has("access_token") ? root.get("access_token").getAsString() : "";
                    String newRefresh = root.has("refresh_token") ? root.get("refresh_token").getAsString() : refreshToken;
                    long expiresIn = root.has("expires_in") ? root.get("expires_in").getAsLong() : 3600;

                    if (!newAccess.isEmpty()) {
                        session.updateTokens(newAccess, newRefresh, expiresIn);
                        return newAccess;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void notifySessionExpired(UserSessionManager session) {
        session.clearSession();
        if (sessionExpiredListener != null) {
            sessionExpiredListener.onSessionExpired();
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}