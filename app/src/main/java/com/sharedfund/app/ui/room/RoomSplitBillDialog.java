package com.sharedfund.app.ui.room;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sharedfund.app.R;
import com.sharedfund.app.util.CopyToast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class RoomSplitBillDialog {

    public static void show(Context context, View anchorView, String roomName, int memberCount) {
        if (context == null) return;
        final int safeCount = Math.max(1, memberCount);
        final String safeRoomName = roomName != null ? roomName : "Quỹ nhóm";

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        final DecimalFormat df = new DecimalFormat("#,###", symbols);

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_koin_split_bill);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvSubtitle = dialog.findViewById(R.id.tvSplitDialogSubtitle);
        TextInputEditText etAmount = dialog.findViewById(R.id.etSplitAmount);
        TextView tvResultAmount = dialog.findViewById(R.id.tvSplitResultAmount);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnSplitCancel);
        MaterialButton btnCopy = dialog.findViewById(R.id.btnSplitCopy);

        if (tvSubtitle != null) {
            tvSubtitle.setText("Chia đều hóa đơn cho " + safeCount + " thành viên trong quỹ");
        }

        final double[] currentEach = {0.0};
        final double[] currentAmt = {0.0};

        if (etAmount != null) {
            etAmount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String str = s != null ? s.toString().trim() : "";
                    try {
                        double amt = Double.parseDouble(str);
                        currentAmt[0] = amt;
                        currentEach[0] = amt / safeCount;
                        if (tvResultAmount != null) {
                            tvResultAmount.setText(df.format(currentEach[0]).replace(',', '.') + " ₫");
                        }
                    } catch (Exception ex) {
                        currentAmt[0] = 0.0;
                        currentEach[0] = 0.0;
                        if (tvResultAmount != null) {
                            tvResultAmount.setText("0 ₫");
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                String text = "🧾 CHIA TIỀN NHÓM - " + safeRoomName + "\n"
                        + "• Tổng hóa đơn: " + df.format(currentAmt[0]).replace(',', '.') + " ₫\n"
                        + "• Số thành viên: " + safeCount + " người\n"
                        + "👉 Mỗi người: " + df.format(currentEach[0]).replace(',', '.') + " ₫";
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Split Bill", text));
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
