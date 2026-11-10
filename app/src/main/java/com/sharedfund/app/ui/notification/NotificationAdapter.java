package com.sharedfund.app.ui.notification;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sharedfund.app.R;
import com.sharedfund.app.model.AppNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationActionListener {
        void onNotificationClick(AppNotification item);
        void onToggleReadStatus(AppNotification item);
        void onDeleteNotification(AppNotification item);
    }

    private final List<AppNotification> items = new ArrayList<>();
    private final OnNotificationActionListener listener;

    public NotificationAdapter(OnNotificationActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AppNotification> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final View layoutRoot;
        private final TextView tvFundName;
        private final TextView tvTime;
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final ImageButton btnMore;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutRoot = itemView.findViewById(R.id.layoutNotifItemRoot);
            tvFundName = itemView.findViewById(R.id.tvNotifFundName);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvDescription = itemView.findViewById(R.id.tvNotifDescription);
            btnMore = itemView.findViewById(R.id.btnNotifMore);
        }

        void bind(AppNotification item) {
            Context context = itemView.getContext();
            tvFundName.setText(item.getFundName() != null ? item.getFundName() : "Quỹ");
            tvTime.setText(item.getTimeAgo() != null ? item.getTimeAgo() : "");
            tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
            tvDescription.setText(item.getDescription() != null ? item.getDescription() : "");

            if (item.isRead()) {
                layoutRoot.setBackgroundColor(ContextCompat.getColor(context, R.color.surface));
            } else {
                layoutRoot.setBackgroundColor(ContextCompat.getColor(context, R.color.accent_blue_light));
            }

            layoutRoot.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(item);
            });

            btnMore.setOnClickListener(v -> showPopupMenu(context, btnMore, item));
        }

        private void showPopupMenu(Context context, View anchor, AppNotification item) {
            PopupMenu popup = new PopupMenu(context, anchor);

            if (item.isRead()) {
                popup.getMenu().add(0, 1, 0, "Đánh dấu chưa đọc");
            } else {
                popup.getMenu().add(0, 1, 0, "Đánh dấu đã đọc");
            }

            SpannableString deleteText = new SpannableString("Xóa thông báo");
            deleteText.setSpan(new ForegroundColorSpan(Color.parseColor("#EF4444")), 0, deleteText.length(), 0);
            MenuItem deleteItem = popup.getMenu().add(0, 2, 1, deleteText);

            popup.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == 1) {
                    if (listener != null) listener.onToggleReadStatus(item);
                    return true;
                } else if (menuItem.getItemId() == 2) {
                    if (listener != null) listener.onDeleteNotification(item);
                    return true;
                }
                return false;
            });

            popup.show();
        }
    }
}
