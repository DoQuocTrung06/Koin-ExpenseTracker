package com.sharedfund.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sharedfund.app.model.AppNotification;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NotificationManager {
    private static final String PREF_NAME = "koin_notifications_pref";
    private static final String KEY_NOTIFS = "saved_notifications";
    private static final String KEY_DELETED_IDS = "deleted_notification_ids";
    private static final String KEY_INITIALIZED = "notifications_initialized_v2";

    private static NotificationManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final List<AppNotification> cachedList = new ArrayList<>();
    private final Set<NotificationChangeListener> listeners = new HashSet<>();

    public interface NotificationChangeListener {
        void onNotificationsChanged(int unreadCount);
    }

    private NotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        loadNotifications();
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
        for (NotificationChangeListener l : listeners) {
            if (l != null) l.onNotificationsChanged(count);
        }
    }

    public synchronized List<AppNotification> getNotifications() {
        return new ArrayList<>(cachedList);
    }

    public synchronized int getUnreadCount() {
        int count = 0;
        for (AppNotification item : cachedList) {
            if (!item.isRead()) {
                count++;
            }
        }
        return count;
    }

    public synchronized void markAsRead(String id) {
        for (AppNotification item : cachedList) {
            if (item.getId() != null && item.getId().equals(id)) {
                item.setRead(true);
                break;
            }
        }
        saveNotifications();
        notifyListeners();
    }

    public synchronized void markAsUnread(String id) {
        for (AppNotification item : cachedList) {
            if (item.getId() != null && item.getId().equals(id)) {
                item.setRead(false);
                break;
            }
        }
        saveNotifications();
        notifyListeners();
    }

    public synchronized void markAllAsRead() {
        for (AppNotification item : cachedList) {
            item.setRead(true);
        }
        saveNotifications();
        notifyListeners();
    }

    public synchronized void deleteNotification(String id) {
        Iterator<AppNotification> it = cachedList.iterator();
        while (it.hasNext()) {
            AppNotification item = it.next();
            if (item.getId() != null && item.getId().equals(id)) {
                it.remove();
                recordDeletedId(id);
                break;
            }
        }
        saveNotifications();
        notifyListeners();
    }

    public synchronized void addNotification(AppNotification notification) {
        if (notification == null || isDeleted(notification.getId())) return;
        cachedList.add(0, notification);
        saveNotifications();
        notifyListeners();
    }

    private void loadNotifications() {
        cachedList.clear();
        String json = prefs.getString(KEY_NOTIFS, null);
        if (json != null && !json.trim().isEmpty()) {
            Type listType = new TypeToken<List<AppNotification>>() {}.getType();
            List<AppNotification> saved = gson.fromJson(json, listType);
            if (saved != null) {
                cachedList.addAll(saved);
            }
        } else if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            seedDefaultNotifications();
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply();
            saveNotifications();
        }
    }

    private void seedDefaultNotifications() {
        cachedList.add(new AppNotification(
                "notif_1",
                AppNotification.TYPE_EXPENSE_APPROVAL,
                "Quỹ Ăn Tết",
                "Phạm Quốc Cường yêu cầu rút quỹ 20.000 đ",
                "Nội dung: Mua bánh kẹo liên hoan. Bấm để xem hóa đơn và phê duyệt.",
                "14:14 Hôm nay",
                false,
                "room_tet_01",
                "Quỹ Ăn Tết"
        ));

        cachedList.add(new AppNotification(
                "notif_2",
                AppNotification.TYPE_MEMBER_JOIN,
                "Quỹ Ăn Tết",
                "Trương Cao Sang xin tham gia vào quỹ",
                "Thành viên Trương Cao Sang (524H0125) đã gửi yêu cầu tham gia quỹ của bạn. Bấm để phê duyệt.",
                "10:20 Hôm qua",
                false,
                "room_tet_01",
                "Quỹ Ăn Tết"
        ));

        cachedList.add(new AppNotification(
                "notif_3",
                AppNotification.TYPE_RECURRING_DUE,
                "Lịch định kỳ",
                "Đến hạn thanh toán Internet & Truyền hình FPT",
                "Khoản định kỳ tháng của Quỹ Ăn Tết với số tiền 250.000 đ. Nhấn để kiểm tra và xác nhận chi ngay.",
                "08:00 Hôm nay",
                false,
                "room_tet_01",
                "Quỹ Ăn Tết"
        ));
    }

    private void saveNotifications() {
        String json = gson.toJson(cachedList);
        prefs.edit().putString(KEY_NOTIFS, json).apply();
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
}
