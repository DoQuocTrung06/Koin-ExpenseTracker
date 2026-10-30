package com.sharedfund.app.ui.transaction;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
        TextView tvDetailCreator = view.findViewById(R.id.tvDetailCreator);
        TextView tvDetailCategory = view.findViewById(R.id.tvDetailCategory);
        TextView tvDetailNote = view.findViewById(R.id.tvDetailNote);
        TextView tvDetailDate = view.findViewById(R.id.tvDetailDate);
        LinearLayout layoutRejectionReason = view.findViewById(R.id.layoutRejectionReason);
        TextView tvRejectionReasonContent = view.findViewById(R.id.tvRejectionReasonContent);
        LinearLayout layoutAdminActions = view.findViewById(R.id.layoutAdminActions);
        MaterialButton btnRejectExpense = view.findViewById(R.id.btnRejectExpense);
        MaterialButton btnApproveExpense = view.findViewById(R.id.btnApproveExpense);
        MaterialButton btnCloseDetail = view.findViewById(R.id.btnCloseDetail);

        tvDetailAmount.setText(transaction.getFormattedAmount());
        tvDetailCreator.setText(transaction.getCreatedBy() != null ? transaction.getCreatedBy() : "Member");
        tvDetailCategory.setText(transaction.getCategory() != null ? transaction.getCategory() : "Khác");
        tvDetailNote.setText(transaction.getNote() != null ? transaction.getNote() : "");
        tvDetailDate.setText(transaction.getCreatedAt() != null ? transaction.getCreatedAt() : "");

        if (transaction.isIncome()) {
            tvDetailAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_income));
        } else {
            tvDetailAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_expense));
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
            tvDetailStatusBadge.setText("Đã phê duyệt");
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
