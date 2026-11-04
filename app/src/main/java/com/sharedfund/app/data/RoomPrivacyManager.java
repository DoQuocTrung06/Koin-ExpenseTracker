package com.sharedfund.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sharedfund.app.SharedFundApp;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RoomPrivacyManager {

    private static final String PREF_PRIVACY = "shared_fund_room_privacy";
    private static final String PREF_REQUESTS = "shared_fund_join_requests";
    private static final Gson gson = new Gson();

    public static class JoinRequestItem {
        public String userId;
        public String applicantName;
        public String email;
        public String time;
        public String message;

        public JoinRequestItem() {
        }

        public JoinRequestItem(String userId, String applicantName, String email, String time, String message) {
            this.userId = userId;
            this.applicantName = applicantName;
            this.email = email;
            this.time = time;
            this.message = message;
        }
    }

    private static Context getContext() {
        return SharedFundApp.getInstance();
    }

    public static boolean isRoomPrivate(String roomId) {
        if (roomId == null) return false;
        Context ctx = getContext();
        if (ctx == null) return false;
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_PRIVACY, Context.MODE_PRIVATE);
        return prefs.getBoolean("privacy_" + roomId, false);
    }

    public static void setRoomPrivate(String roomId, boolean isPrivate) {
        if (roomId == null) return;
        Context ctx = getContext();
        if (ctx == null) return;
        ctx.getSharedPreferences(PREF_PRIVACY, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("privacy_" + roomId, isPrivate)
                .apply();
    }

    public static List<JoinRequestItem> getPendingRequests(String roomId) {
        List<JoinRequestItem> list = new ArrayList<>();
        if (roomId == null) return list;
        Context ctx = getContext();
        if (ctx == null) return list;
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_REQUESTS, Context.MODE_PRIVATE);
        String json = prefs.getString("requests_" + roomId, null);
        if (json != null && !json.trim().isEmpty()) {
            Type type = new TypeToken<List<JoinRequestItem>>() {}.getType();
            try {
                List<JoinRequestItem> parsed = gson.fromJson(json, type);
                if (parsed != null) {
                    list.addAll(parsed);
                }
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    public static void addPendingRequest(String roomId, JoinRequestItem item) {
        if (roomId == null || item == null) return;
        Context ctx = getContext();
        if (ctx == null) return;
        List<JoinRequestItem> list = getPendingRequests(roomId);
        for (JoinRequestItem existing : list) {
            if ((item.userId != null && item.userId.equals(existing.userId)) ||
                    (item.email != null && item.email.equalsIgnoreCase(existing.email))) {
                return;
            }
        }
        list.add(0, item);
        savePendingRequests(roomId, list);
    }

    public static void removePendingRequest(String roomId, String userId, String email) {
        if (roomId == null) return;
        List<JoinRequestItem> list = getPendingRequests(roomId);
        Iterator<JoinRequestItem> it = list.iterator();
        while (it.hasNext()) {
            JoinRequestItem item = it.next();
            if ((userId != null && userId.equals(item.userId)) ||
                    (email != null && email.equalsIgnoreCase(item.email))) {
                it.remove();
                break;
            }
        }
        savePendingRequests(roomId, list);
    }

    private static void savePendingRequests(String roomId, List<JoinRequestItem> list) {
        Context ctx = getContext();
        if (ctx == null) return;
        ctx.getSharedPreferences(PREF_REQUESTS, Context.MODE_PRIVATE)
                .edit()
                .putString("requests_" + roomId, gson.toJson(list))
                .apply();
    }
}
