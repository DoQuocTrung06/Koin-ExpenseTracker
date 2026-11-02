package com.sharedfund.app.ui.transaction;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TransactionDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TRANSACTION = "arg_transaction";
    private static final String ARG_IS_ADMIN = "arg_is_admin";

    public interface OnTransactionActionListener {
        void onApprove(Transaction transaction);
        void onReject(Transaction transaction, String reason);
    }

    private Transaction transaction;
    private boolean isAdmin;
    private OnTransactionActionListener actionListener;

    public static TransactionDetailBottomSheet newInstance(Transaction transaction, boolean isAdmin) {
        TransactionDetailBottomSheet sheet = new TransactionDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRANSACTION, transaction);
        args.putBoolean(ARG_IS_ADMIN, isAdmin);
        sheet.setArguments(args);
        return sheet;
    }

    public void setActionListener(OnTransactionActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transaction = (Transaction) getArguments().getSerializable(ARG_TRANSACTION);
            isAdmin = getArguments().getBoolean(ARG_IS_ADMIN, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_transaction_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (transaction == null) {
            dismiss();
            return;
        }

        TextView tvDetailAmount = view.findViewById(R.id.tvDetailAmount);
        TextView tvDetailStatusBadge = view.findViewById(R.id.tvDetailStatusBadge);
        TextView tvDetailTxCode = view.findViewById(R.id.tvDetailTxCode);
        ImageButton btnCopyTxCode = view.findViewById(R.id.btnCopyTxCode);
        TextView tvDetailType = view.findViewById(R.id.tvDetailType);
        TextView tvDetailCreator = view.findViewById(R.id.tvDetailCreator);
        TextView tvDetailRecipient = view.findViewById(R.id.tvDetailRecipient);
        TextView tvDetailCategory = view.findViewById(R.id.tvDetailCategory);
        TextView tvDetailNote = view.findViewById(R.id.tvDetailNote);
        TextView tvDetailDate = view.findViewById(R.id.tvDetailDate);
        View dividerBalanceAfter = view.findViewById(R.id.dividerBalanceAfter);
        LinearLayout layoutBalanceAfter = view.findViewById(R.id.layoutBalanceAfter);
        TextView tvDetailBalanceAfter = view.findViewById(R.id.tvDetailBalanceAfter);

        LinearLayout layoutRejectionReason = view.findViewById(R.id.layoutRejectionReason);
        TextView tvRejectionReasonContent = view.findViewById(R.id.tvRejectionReasonContent);
        LinearLayout layoutAdminActions = view.findViewById(R.id.layoutAdminActions);
        MaterialButton btnRejectExpense = view.findViewById(R.id.btnRejectExpense);
        MaterialButton btnApproveExpense = view.findViewById(R.id.btnApproveExpense);
        MaterialButton btnCloseDetail = view.findViewById(R.id.btnCloseDetail);

        final String code = formatTxCode(transaction.getId());
        tvDetailTxCode.setText(code);

        btnCopyTxCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Transaction Code", code);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "Đã sao chép mã: " + code, Toast.LENGTH_SHORT).show();
            }
        });

        tvDetailAmount.setText(transaction.getFormattedAmount());

        if (transaction.getRoomId() == null || transaction.getRoomId().trim().isEmpty()) {
            if (transaction.isIncome()) {
                tvDetailType.setText("Nạp tiền ví Koin");
                tvDetailRecipient.setText("Ví Koin cá nhân");
            } else {
                tvDetailType.setText("Rút tiền về ngân hàng");
                tvDetailRecipient.setText("Tài khoản ngân hàng");
            }
        } else {
            if (transaction.isIncome()) {
                tvDetailType.setText("Góp tiền vào quỹ");
                tvDetailRecipient.setText("Quỹ nhóm");
            } else {
                tvDetailType.setText("Chi tiêu quỹ nhóm");
                tvDetailRecipient.setText("Thụ hưởng khoản chi");
            }
        }

        String creator = transaction.getCreatedBy();
        if (creator == null || creator.trim().isEmpty() || "Member".equalsIgnoreCase(creator)) {
            creator = transaction.getRoomId() == null ? "Ví cá nhân" : "Thành viên";
        }
        tvDetailCreator.setText(creator);

        tvDetailCategory.setText(transaction.getCategory() != null ? transaction.getCategory() : "Khác");
        tvDetailNote.setText(transaction.getNote() != null && !transaction.getNote().trim().isEmpty() ? transaction.getNote() : "Không có ghi chú");
        tvDetailDate.setText(TransactionAdapter.formatDateTime(transaction.getCreatedAt()));

        if (transaction.getBalanceAfter() > 0) {
            dividerBalanceAfter.setVisibility(View.VISIBLE);
            layoutBalanceAfter.setVisibility(View.VISIBLE);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator('.');
            DecimalFormat df = new DecimalFormat("#,##0", symbols);
            tvDetailBalanceAfter.setText(df.format(transaction.getBalanceAfter()) + " ₫");
        } else {
            dividerBalanceAfter.setVisibility(View.GONE);
            layoutBalanceAfter.setVisibility(View.GONE);
        }

        if (transaction.isIncome()) {
            tvDetailAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_income));
        } else {
            tvDetailAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }

        if (transaction.isPending()) {
            tvDetailStatusBadge.setVisibility(View.VISIBLE);
            tvDetailStatusBadge.setText("Chờ phê duyệt");
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_pending_badge);
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber));
        } else if (transaction.isRejected()) {
            tvDetailStatusBadge.setVisibility(View.VISIBLE);
            tvDetailStatusBadge.setText("Đã bị từ chối");
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_expense_badge);
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_expense));
            layoutRejectionReason.setVisibility(View.VISIBLE);
            String reason = transaction.getRejectionReason();
            tvRejectionReasonContent.setText(reason != null && !reason.trim().isEmpty() ? reason : "Không có lý do cụ thể");
        } else {
            tvDetailStatusBadge.setVisibility(View.VISIBLE);
            tvDetailStatusBadge.setText("Thành công");
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_income_badge);
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_income));
        }

        if (isAdmin && transaction.isPending() && transaction.isExpense()) {
            layoutAdminActions.setVisibility(View.VISIBLE);

            btnApproveExpense.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onApprove(transaction);
                }
                dismiss();
            });

            btnRejectExpense.setOnClickListener(v -> showRejectInputDialog());
        } else {
            layoutAdminActions.setVisibility(View.GONE);
        }

        btnCloseDetail.setOnClickListener(v -> dismiss());
    }

    private String formatTxCode(String id) {
        if (id == null || id.trim().isEmpty()) return "SF-" + System.currentTimeMillis() % 1000000;
        String clean = id.replace("-", "").toUpperCase();
        if (clean.length() >= 8) {
            return "SF-" + clean.substring(0, 8);
        }
        return "SF-" + clean;
    }

    private void showRejectInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Lý do từ chối yêu cầu chi");

        final EditText input = new EditText(requireContext());
        input.setHint("Nhập lý do cụ thể (bắt buộc)...");
        input.setPadding(40, 30, 40, 30);
        builder.setView(input);

        builder.setPositiveButton("Xác nhận từ chối", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(requireContext(), "Bắt buộc phải nhập lý do từ chối!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (actionListener != null) {
                actionListener.onReject(transaction, reason);
            }
            dismiss();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
