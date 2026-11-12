package com.sharedfund.app.ui.room;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;
import com.sharedfund.app.util.CopyToast;

public class RoomReminderDialog {

    public static void show(Context context, View anchorView, String roomName, String joinCode) {
        if (context == null) return;
        final String safeRoomName = roomName != null ? roomName : "Quỹ nhóm";
        final String safeJoinCode = joinCode != null ? joinCode : "";

        String shareMsg = "🔔 LỜI NHẮC ĐÓNG QUỸ CHUNG\n"
                + "Quỹ: \"" + safeRoomName + "\"\n"
                + "Chào cả nhà, quỹ đã đến kỳ hạn đóng góp. Mọi người kiểm tra và hoàn thành phần quỹ đúng hạn nhé!\n"
                + "👉 Mã tham gia: " + safeJoinCode + "\n"
                + "Cảm ơn mọi người nhiều!";

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_koin_remind);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvMsg = dialog.findViewById(R.id.tvRemindDialogMessage);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnRemindCancel);
        MaterialButton btnShare = dialog.findViewById(R.id.btnRemindShare);
        MaterialButton btnCopy = dialog.findViewById(R.id.btnRemindCopy);

        if (tvMsg != null) {
            tvMsg.setText(shareMsg);
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareMsg);
                sendIntent.setType("text/plain");
                context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ lời nhắc đóng quỹ"));
                dialog.dismiss();
            });
        }

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Fund Reminder", shareMsg));
                    if (anchorView != null) {
                        CopyToast.show(context, anchorView);
                    }
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}
