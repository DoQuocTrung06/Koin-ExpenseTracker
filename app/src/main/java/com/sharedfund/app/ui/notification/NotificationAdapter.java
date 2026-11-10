package com.sharedfund.app.ui.notification;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

            btnMore.setOnClickListener(v -> showCustomPopupMenu(context, btnMore, item));
        }

        private void showCustomPopupMenu(Context context, View anchor, AppNotification item) {
            View popupView = LayoutInflater.from(context).inflate(R.layout.popup_notification_menu, null);

            final PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.setElevation(16f);
            popupWindow.setOutsideTouchable(true);

            ImageView ivToggleIcon = popupView.findViewById(R.id.ivPopupToggleIcon);
            TextView tvToggleText = popupView.findViewById(R.id.tvPopupToggleText);
            View btnToggle = popupView.findViewById(R.id.layoutPopupToggleRead);
            View btnDelete = popupView.findViewById(R.id.layoutPopupDelete);

            if (item.isRead()) {
                tvToggleText.setText("Đánh dấu chưa đọc");
                ivToggleIcon.setImageResource(R.drawable.ic_check);
                ivToggleIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
            } else {
                tvToggleText.setText("Đánh dấu đã đọc");
                ivToggleIcon.setImageResource(R.drawable.ic_check);
                ivToggleIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary));
            }

            btnToggle.setOnClickListener(v -> {
                popupWindow.dismiss();
                if (listener != null) listener.onToggleReadStatus(item);
            });

            btnDelete.setOnClickListener(v -> {
                popupWindow.dismiss();
                if (listener != null) listener.onDeleteNotification(item);
            });

            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int xOffset = -(popupView.getMeasuredWidth() - anchor.getWidth());
            popupWindow.showAsDropDown(anchor, xOffset, 4);
        }
    }
}
