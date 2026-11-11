package com.sharedfund.app.ui.notification;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.data.NotificationManager;
import com.sharedfund.app.model.AppNotification;
import com.sharedfund.app.ui.widget.AppToaster;

import java.util.List;

public class NotificationFragment extends Fragment implements NotificationAdapter.OnNotificationActionListener, NotificationManager.NotificationChangeListener {

    private RecyclerView rvNotifications;
    private View layoutEmptyNotification;
    private NotificationAdapter adapter;
    private NotificationManager notificationManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationManager = NotificationManager.getInstance(requireContext());

        ImageButton btnBack = view.findViewById(R.id.btnNotificationBack);
        View btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);
        rvNotifications = view.findViewById(R.id.rvNotifications);
        layoutEmptyNotification = view.findViewById(R.id.layoutEmptyNotification);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                notificationManager.markAllAsRead();
                refreshList();
                if (isAdded()) {
                    AppToaster.success(requireActivity(), "Đã đọc tất cả thông báo");
                }
            });
        }

        adapter = new NotificationAdapter(this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);

        refreshList();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (notificationManager != null) {
            notificationManager.addListener(this);
            notificationManager.fetchNotificationsFromCloud(null, this::refreshList);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (notificationManager != null) {
            notificationManager.removeListener(this);
        }
    }

    @Override
    public void onNotificationsChanged(int unreadCount) {
        if (isAdded()) {
            requireActivity().runOnUiThread(this::refreshList);
        }
    }

    private void refreshList() {
        if (notificationManager == null || adapter == null) return;
        List<AppNotification> list = notificationManager.getNotifications();
        adapter.submitList(list);

        if (layoutEmptyNotification != null) {
            layoutEmptyNotification.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (rvNotifications != null) {
            rvNotifications.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onNotificationClick(AppNotification item) {
        if (item == null) return;
        notificationManager.markAsRead(item.getId());
        refreshList();

        if (!(getActivity() instanceof MainActivity)) return;
        MainActivity activity = (MainActivity) getActivity();

        String type = item.getType();
        String roomId = item.getTargetRoomId();

        if (AppNotification.TYPE_EXPENSE_APPROVAL.equals(type)) {
            if (roomId != null && !roomId.trim().isEmpty()) {
                activity.openExpenseApproval(roomId);
            }
        } else if (AppNotification.TYPE_RECURRING_DUE.equals(type)) {
            activity.openRecurringRules(roomId, item.getTargetRoomName());
        } else if (roomId != null && !roomId.trim().isEmpty()) {
            activity.openRoomDetail(roomId);
        }
    }

    @Override
    public void onToggleReadStatus(AppNotification item) {
        if (item == null) return;
        if (item.isRead()) {
            notificationManager.markAsUnread(item.getId());
        } else {
            notificationManager.markAsRead(item.getId());
        }
        refreshList();
    }

    @Override
    public void onDeleteNotification(AppNotification item) {
        if (item == null) return;
        notificationManager.deleteNotification(item.getId());
        refreshList();
        if (isAdded()) {
            AppToaster.success(requireActivity(), "Đã xóa thông báo");
        }
    }
}
