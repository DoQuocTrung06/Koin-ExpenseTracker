package com.sharedfund.app.ui.transaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sharedfund.app.R;
import com.sharedfund.app.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách giao dịch Thu/Chi
 * Thống nhất theo mục 4 & 5 tài liệu Giai đoạn 3
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    private final List<Transaction> transactions = new ArrayList<>();
    private final OnTransactionClickListener listener;

    public TransactionAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<Transaction> newTransactions) {
        this.transactions.clear();
        if (newTransactions != null) {
            this.transactions.addAll(newTransactions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction tx = transactions.get(position);
        holder.bind(tx, listener);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivTxIcon;
        private final TextView tvTxNote;
        private final TextView tvTxCreator;
        private final TextView tvTxTime;
        private final TextView tvTxAmount;
        private final TextView tvTxCategory;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTxIcon = itemView.findViewById(R.id.ivTxIcon);
            tvTxNote = itemView.findViewById(R.id.tvTxNote);
            tvTxCreator = itemView.findViewById(R.id.tvTxCreator);
            tvTxTime = itemView.findViewById(R.id.tvTxTime);
            tvTxAmount = itemView.findViewById(R.id.tvTxAmount);
            tvTxCategory = itemView.findViewById(R.id.tvTxCategory);
        }

        public void bind(final Transaction tx, final OnTransactionClickListener listener) {
            tvTxNote.setText(tx.getNote());
            tvTxCreator.setText(tx.getCreatedBy());
            tvTxTime.setText(tx.getCreatedAt());
            tvTxAmount.setText(tx.getFormattedAmount());
            tvTxCategory.setText(tx.getCategory() != null ? tx.getCategory() : "Khác");

            int contextColor;
            if (tx.isIncome()) {
                contextColor = ContextCompat.getColor(itemView.getContext(), R.color.accent_income);
                tvTxAmount.setTextColor(contextColor);
                ivTxIcon.setImageResource(R.drawable.ic_trending_up);
            } else {
                contextColor = ContextCompat.getColor(itemView.getContext(), R.color.accent_expense);
                tvTxAmount.setTextColor(contextColor);

                // Set category icon
                String cat = tx.getCategory() != null ? tx.getCategory().toLowerCase() : "";
                if (cat.contains("ăn") || cat.contains("uống") || cat.contains("cà phê")) {
                    ivTxIcon.setImageResource(R.drawable.ic_restaurant);
                } else if (cat.contains("xe") || cat.contains("di chuyển") || cat.contains("xăng")) {
                    ivTxIcon.setImageResource(R.drawable.ic_transport);
                } else if (cat.contains("sắm") || cat.contains("mua") || cat.contains("chợ")) {
                    ivTxIcon.setImageResource(R.drawable.ic_shopping);
                } else if (cat.contains("nhà") || cat.contains("phòng") || cat.contains("điện")) {
                    ivTxIcon.setImageResource(R.drawable.ic_home_category);
                } else {
                    ivTxIcon.setImageResource(R.drawable.ic_receipt);
                }
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onTransactionClick(tx);
                    }
                }
            });
        }
    }
}
