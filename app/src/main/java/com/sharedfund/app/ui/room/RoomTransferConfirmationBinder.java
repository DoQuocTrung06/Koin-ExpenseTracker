package com.sharedfund.app.ui.room;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;
import com.sharedfund.app.util.CopyToast;
import com.sharedfund.app.util.VietnameseCurrencyWords;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RoomTransferConfirmationBinder {

    private final DecimalFormat df;

    public RoomTransferConfirmationBinder() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        this.df = new DecimalFormat("#,###", symbols);
    }

    public static class ConfirmViews {
        public View layoutRoomTransferForm;
        public View layoutBottomTransferDock;
        public View layoutProcessingNotice;
        public View layoutConfirmStep;
        public View layoutSuccessNotice;
        public TextView tvRoomTransferScreenTitle;
        public TextView tvConfirmStepAmount;
        public TextView tvConfirmStepAmountWords;
        public TextView tvConfirmStepType;
        public TextView tvConfirmStepSource;
        public TextView tvConfirmStepDest;
        public TextView tvConfirmStepDestAccount;
        public TextView tvConfirmStepDestBank;
        public TextView tvConfirmStepNote;
        public View dividerConfirmTransferReceipt;
        public View layoutConfirmTransferReceiptRow;
        public MaterialButton btnConfirmStepExecute;
        public TextView tvSuccessTitle;
        public TextView tvSuccessAmount;
        public TextView tvSuccessTxId;
        public ImageView btnSuccessCopyTxId;
        public TextView tvSuccessTime;
        public TextView tvSuccessSource;
        public TextView tvSuccessDest;
        public TextView tvSuccessNote;
        public MaterialButton btnSuccessDone;
    }

    public void showVerificationDialog(ConfirmViews views, String type, double amount, String source,
                                       String destName, String destAccount, String destBank,
                                       String note, boolean hasReceipt, Runnable onConfirm) {
        if (views == null) return;
        if (views.layoutRoomTransferForm != null) views.layoutRoomTransferForm.setVisibility(View.GONE);
        if (views.layoutBottomTransferDock != null) views.layoutBottomTransferDock.setVisibility(View.GONE);
        if (views.layoutConfirmStep != null) views.layoutConfirmStep.setVisibility(View.VISIBLE);
        if (views.tvRoomTransferScreenTitle != null) views.tvRoomTransferScreenTitle.setText("Xác nhận giao dịch");

        if (views.tvConfirmStepAmount != null) views.tvConfirmStepAmount.setText(df.format(amount) + " ₫");
        if (views.tvConfirmStepAmountWords != null) {
            views.tvConfirmStepAmountWords.setText(VietnameseCurrencyWords.toWordsWithParentheses((long) amount));
        }
        if (views.tvConfirmStepType != null) views.tvConfirmStepType.setText(type);
        if (views.tvConfirmStepSource != null) views.tvConfirmStepSource.setText(source);

        if (destAccount != null && !destAccount.isEmpty()) {
            if (views.tvConfirmStepDest != null) {
                views.tvConfirmStepDest.setText(destName != null ? destName.toUpperCase(Locale.getDefault()) : "");
            }
            if (views.tvConfirmStepDestAccount != null) {
                views.tvConfirmStepDestAccount.setText(destAccount);
                views.tvConfirmStepDestAccount.setVisibility(View.VISIBLE);
            }
            if (views.tvConfirmStepDestBank != null) {
                views.tvConfirmStepDestBank.setText(destBank != null ? destBank : "");
                views.tvConfirmStepDestBank.setVisibility(View.VISIBLE);
            }
        } else {
            if (views.tvConfirmStepDest != null) {
                views.tvConfirmStepDest.setText(destName != null ? destName : "");
            }
            if (views.tvConfirmStepDestAccount != null) {
                views.tvConfirmStepDestAccount.setVisibility(View.GONE);
            }
            if (views.tvConfirmStepDestBank != null) {
                views.tvConfirmStepDestBank.setVisibility(View.GONE);
            }
        }

        if (views.tvConfirmStepNote != null) views.tvConfirmStepNote.setText(note);

        if (views.dividerConfirmTransferReceipt != null) {
            views.dividerConfirmTransferReceipt.setVisibility(hasReceipt ? View.VISIBLE : View.GONE);
        }
        if (views.layoutConfirmTransferReceiptRow != null) {
            views.layoutConfirmTransferReceiptRow.setVisibility(hasReceipt ? View.VISIBLE : View.GONE);
        }

        if (views.btnConfirmStepExecute != null) {
            views.btnConfirmStepExecute.setEnabled(true);
            views.btnConfirmStepExecute.setOnClickListener(v -> {
                views.btnConfirmStepExecute.setEnabled(false);
                if (views.layoutConfirmStep != null) views.layoutConfirmStep.setVisibility(View.GONE);
                if (onConfirm != null) onConfirm.run();
            });
        }
    }

    public void showSuccess(Context context, View targetView, ConfirmViews views, String title,
                            String amountFormatted, String source, String dest, String note,
                            String rawTxId, Runnable onDone) {
        if (views == null || context == null) return;
        if (views.layoutProcessingNotice != null) views.layoutProcessingNotice.setVisibility(View.GONE);
        if (views.layoutBottomTransferDock != null) views.layoutBottomTransferDock.setVisibility(View.GONE);
        if (views.layoutSuccessNotice != null) views.layoutSuccessNotice.setVisibility(View.VISIBLE);
        if (views.tvRoomTransferScreenTitle != null) views.tvRoomTransferScreenTitle.setText("Kết quả giao dịch");

        if (views.tvSuccessTitle != null) views.tvSuccessTitle.setText(title);
        if (views.tvSuccessAmount != null) {
            views.tvSuccessAmount.setVisibility(View.VISIBLE);
            views.tvSuccessAmount.setText(amountFormatted);
            views.tvSuccessAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }
        if (views.tvSuccessSource != null) views.tvSuccessSource.setText(source);
        if (views.tvSuccessDest != null) views.tvSuccessDest.setText(dest);
        if (views.tvSuccessNote != null) views.tvSuccessNote.setText(note);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault());
        if (views.tvSuccessTime != null) views.tvSuccessTime.setText(timeFormat.format(new Date()));

        String txId = "#SF-" + String.format(Locale.US, "%08X", (int) (System.currentTimeMillis() & 0xFFFFFFFFL));
        if (rawTxId != null && rawTxId.length() >= 8) {
            txId = "#SF-" + rawTxId.substring(0, 8).toUpperCase(Locale.US);
        }
        if (views.tvSuccessTxId != null) views.tvSuccessTxId.setText(txId);

        final String finalTxId = txId;
        if (views.btnSuccessCopyTxId != null) {
            views.btnSuccessCopyTxId.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("TxID", finalTxId);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                if (targetView != null) CopyToast.show(context, targetView);
            });
        }

        if (views.btnSuccessDone != null) {
            views.btnSuccessDone.setOnClickListener(v -> {
                if (onDone != null) onDone.run();
            });
        }
    }
}
