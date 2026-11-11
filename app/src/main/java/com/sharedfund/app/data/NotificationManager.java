package com.sharedfund.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sharedfund.app.model.AppNotification;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationManager {
    private static final String PREF_NAME = "koin_notifications_pref";
    private static final String KEY_NOTIFS = "saved_notifications";
    private static final String KEY_DELETED_IDS = "deleted_notification_ids";
    private static final String KEY_INITIALIZED = "notifications_initialized_v3";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static NotificationManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final OkHttpClient httpClient;
    private final Handler mainHandler;
    private final List<AppNotification> cachedList = new ArrayList<>();
    private final Set<NotificationChangeListener> listeners = new HashSet<>();

    public interface NotificationChangeListener {
        void onNotificationsChanged(int unreadCount);
    }

    private NotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
        loadNotifications();
        String currentUserId = getCurrentUserId();
        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            fetchNotificationsFromCloud(currentUserId, null);
        }
    }

    public static synchronized NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager(context);
        }
        return instance;
    }

    public void addListener(NotificationChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
            listener.onNotificationsChanged(getUnreadCount());
        }
    }

    public void removeListener(NotificationChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        int count = getUnreadCount();
        mainHandler.post(() -> {
            for (NotificationChangeListener l : listeners) {
                if (l != null) l.onNotificationsChanged(count);
            }
        });
    }

    public synchronized List<AppNotification> getNotifications() {
        String myId = getCurrentUserId();
        List<AppNotification> list = new ArrayList<>();
        for (AppNotification item : cachedList) {
            if (myId == null || item.getUserId() == null || myId.equals(item.getUserId())) {
                list.add(item);
            }
        }
        return list;
    }

    public synchronized int getUnreadCount() {
        String myId = getCurrentUserId();
        int count = 0;
        for (AppNotification item : cachedList) {
            if (myId == null || item.getUserId() == null || myId.equals(item.getUserId())) {
                if (!item.isRead()) {
                    count++;
                }
            }
        }
        return count;
    }

    public synchronized void onUserSessionChanged(String newUserId) {
        cachedList.clear();
        loadNotifications();
        notifyListeners();
        if (newUserId != null && !newUserId.trim().isEmpty()) {
            fetchNotificationsFromCloud(newUserId, null);
        }
    }

    public synchronized void markAsRead(String id) {
        if (id == null) return;
        for (AppNotification item : cachedList) {
            if (id.equals(item.getId())) {
                item.setRead(true);
                break;
            }
        }
        saveNotifications();
        notifyListeners();

        patchRemoteNotification(id, "{\"is_read\": true}");
    }

    public synchronized void markAsUnread(String id) {
        if (id == null) return;
        for (AppNotification item : cachedList) {
            if (id.equals(item.getId())) {
                item.setRead(false);
                break;
            }
        }
        saveNotifications();
        notifyListeners();

        patchRemoteNotification(id, "{\"is_read\": false}");
    }

    public synchronized void markAllAsRead() {
        for (AppNotification item : cachedList) {
            item.setRead(true);
        }
        saveNotifications();
        notifyListeners();

        String currentUserId = getCurrentUserId();
        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            String url = SupabaseConfig.REST_URL + "/notifications?user_id=eq." + currentUserId;
            RequestBody body = RequestBody.create("{\"is_read\": true}", JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + getEffectiveToken())
                    .addHeader("Content-Type", "application/json")
                    .patch(body)
                    .build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() != null) response.body().close();
                }
            });
        }
    }

    public synchronized void deleteNotification(String id) {
        if (id == null) return;
        Iterator<AppNotification> it = cachedList.iterator();
        while (it.hasNext()) {
            AppNotification item = it.next();
            if (id.equals(item.getId())) {
                it.remove();
                recordDeletedId(id);
                break;
            }
        }
        saveNotifications();
        notifyListeners();

        String url = SupabaseConfig.REST_URL + "/notifications?id=eq." + id;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .delete()
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) response.body().close();
            }
        });
    }

    public synchronized void addNotification(AppNotification notification) {
        if (notification == null || isDeleted(notification.getId())) return;
        if (notification.getId() == null || notification.getId().trim().isEmpty()) {
            notification.setId(UUID.randomUUID().toString());
        }
        String myId = getCurrentUserId();
        if (notification.getUserId() == null || notification.getUserId().trim().isEmpty()) {
            notification.setUserId(myId != null ? myId : "anonymous");
        }
        if (notification.getCreatedAt() == null || notification.getCreatedAt().trim().isEmpty()) {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            notification.setCreatedAt(isoFormat.format(new Date()));
        }

        cachedList.add(0, notification);
        saveNotifications();
        notifyListeners();

        sendToCloud(notification);
    }

    public void sendNotificationToUser(AppNotification notification, String recipientUserId) {
        if (notification == null || recipientUserId == null || recipientUserId.trim().isEmpty()) return;
        notification.setUserId(recipientUserId);
        if (notification.getId() == null || notification.getId().trim().isEmpty()) {
            notification.setId(UUID.randomUUID().toString());
        }
        if (notification.getCreatedAt() == null || notification.getCreatedAt().trim().isEmpty()) {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            notification.setCreatedAt(isoFormat.format(new Date()));
        }

        String myId = getCurrentUserId();
        if (recipientUserId.equals(myId)) {
            synchronized (this) {
                cachedList.add(0, notification);
                saveNotifications();
            }
            notifyListeners();
        }

        sendToCloud(notification);
    }

    public void fetchNotificationsFromCloud(String userId, Runnable onComplete) {
        String targetUserId = (userId != null && !userId.trim().isEmpty()) ? userId : getCurrentUserId();
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            if (onComplete != null) mainHandler.post(onComplete);
            return;
        }

        String url = SupabaseConfig.REST_URL + "/notifications?user_id=eq." + targetUserId + "&order=created_at.desc&select=*";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (onComplete != null) mainHandler.post(onComplete);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful() && json != null && !json.trim().isEmpty()) {
                    Type listType = new TypeToken<List<AppNotification>>() {}.getType();
                    List<AppNotification> remoteList = gson.fromJson(json, listType);
                    if (remoteList != null) {
                        String currentMyId = getCurrentUserId();
                        if (targetUserId.equals(currentMyId)) {
                            synchronized (NotificationManager.this) {
                                cachedList.clear();
                                for (AppNotification item : remoteList) {
                                    if (!isDeleted(item.getId())) {
                                        cachedList.add(item);
                                    }
                                }
                                saveNotifications();
                            }
                            notifyListeners();
                        }
                    }
                }
                if (onComplete != null) mainHandler.post(onComplete);
            }
        });
    }

    public synchronized void onRemoteNotificationReceived(String eventType, AppNotification remoteNotif) {
        if (remoteNotif == null) return;
        String myId = getCurrentUserId();
        if (remoteNotif.getUserId() != null && myId != null && !myId.trim().isEmpty() && !myId.equals(remoteNotif.getUserId())) {
            return;
        }

        if ("DELETE".equalsIgnoreCase(eventType)) {
            if (remoteNotif.getId() != null) {
                Iterator<AppNotification> it = cachedList.iterator();
                while (it.hasNext()) {
                    if (remoteNotif.getId().equals(it.next().getId())) {
                        it.remove();
                        break;
                    }
                }
                saveNotifications();
                notifyListeners();
            }
            return;
        }

        boolean found = false;
        for (int i = 0; i < cachedList.size(); i++) {
            if (cachedList.get(i).getId() != null && cachedList.get(i).getId().equals(remoteNotif.getId())) {
                cachedList.set(i, remoteNotif);
                found = true;
                break;
            }
        }
        if (!found && !isDeleted(remoteNotif.getId())) {
            cachedList.add(0, remoteNotif);
        }
        saveNotifications();
        notifyListeners();
    }

    private void sendToCloud(AppNotification notification) {
        String url = SupabaseConfig.REST_URL + "/notifications";
        String jsonBody = gson.toJson(notification);
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) response.body().close();
            }
        });
    }

    public synchronized void updateNotificationsForRule(String roomId, String ruleTitle, double newAmount) {
        if (ruleTitle == null) return;
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###", symbols);
        String formattedAmount = df.format(Math.abs(newAmount)) + " đ";

        boolean changed = false;
        for (AppNotification notif : cachedList) {
            if (notif.getTitle() != null && notif.getTitle().contains(ruleTitle)) {
                String oldMessage = notif.getDescription();
                if (oldMessage != null && oldMessage.contains("đ")) {
                    String updatedMessage = oldMessage.replaceAll("[0-9.]+\\s*đ", formattedAmount);
                    notif.setDescription(updatedMessage);
                    changed = true;
                    String patchJson = "{\"description\":\"" + updatedMessage + "\",\"content\":\"" + updatedMessage + "\"}";
                    patchRemoteNotification(notif.getId(), patchJson);
                }
            }
        }
        if (changed) {
            saveNotifications();
            notifyListeners();
        }
    }

    private void patchRemoteNotification(String id, String patchJson) {
        String url = SupabaseConfig.REST_URL + "/notifications?id=eq." + id;
        RequestBody body = RequestBody.create(patchJson, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .addHeader("Content-Type", "application/json")
                .patch(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) response.body().close();
            }
        });
    }

    private String getCacheKey() {
        String uid = getCurrentUserId();
        return KEY_NOTIFS + "_" + (uid != null && !uid.trim().isEmpty() ? uid : "guest");
    }

    private synchronized void loadNotifications() {
        cachedList.clear();
        String uid = getCurrentUserId();
        String json = prefs.getString(getCacheKey(), null);
        if (json != null && !json.trim().isEmpty()) {
            Type listType = new TypeToken<List<AppNotification>>() {}.getType();
            List<AppNotification> saved = gson.fromJson(json, listType);
            if (saved != null) {
                for (AppNotification item : saved) {
                    if (uid == null || item.getUserId() == null || uid.equals(item.getUserId())) {
                        cachedList.add(item);
                    }
                }
            }
        }
    }

    private synchronized void saveNotifications() {
        String json = gson.toJson(cachedList);
        prefs.edit().putString(getCacheKey(), json).apply();
    }

    private void recordDeletedId(String id) {
        if (id == null) return;
        Set<String> deleted = new HashSet<>(prefs.getStringSet(KEY_DELETED_IDS, new HashSet<>()));
        deleted.add(id);
        prefs.edit().putStringSet(KEY_DELETED_IDS, deleted).apply();
    }

    private boolean isDeleted(String id) {
        if (id == null) return false;
        Set<String> deleted = prefs.getStringSet(KEY_DELETED_IDS, new HashSet<>());
        return deleted.contains(id);
    }

    private String getCurrentUserId() {
        try {
            UserSessionManager session = UserSessionManager.getInstance(context);
            if (session != null) {
                return session.getUserId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getEffectiveToken() {
        try {
            UserSessionManager session = UserSessionManager.getInstance(context);
            if (session != null && session.getAccessToken() != null && !session.getAccessToken().trim().isEmpty()) {
                return session.getAccessToken();
            }
        } catch (Exception ignored) {}
        return SupabaseConfig.SUPABASE_ANON_KEY;
    }
}
