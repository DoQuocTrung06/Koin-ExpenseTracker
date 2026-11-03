package com.sharedfund.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sharedfund.app.R;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.transaction.TransactionAdapter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WalletStatementAdapter extends RecyclerView.Adapter<WalletStatementAdapter.DateCardViewHolder> {

    public static final int MODE_WALLET = 0;
    public static final int MODE_ROOM = 1;
    private int displayMode = MODE_WALLET;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public static class DateGroup {
        public final String date;
        public final List<Transaction> transactions = new ArrayList<>();

        public DateGroup(String date) {
            this.date = date;
        }
    }

    private final List<DateGroup> dateGroups = new ArrayList<>();
    private final OnTransactionClickListener listener;
    private final DecimalFormat df;
    private String accountNumber = "1047061615";

    public WalletStatementAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        this.df = new DecimalFormat("#,##0", symbols);
    }

    public void setDisplayMode(int displayMode) {
        this.displayMode = displayMode;
        notifyDataSetChanged();
    }

    public void setAccountNumber(String accountNumber) {
        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            this.accountNumber = accountNumber;
            notifyDataSetChanged();
        }
    }

    public void setTransactions(List<Transaction> transactions) {
        dateGroups.clear();
        if (transactions == null || transactions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        List<Transaction> sorted = new ArrayList<>(transactions);
        Collections.sort(sorted, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                Date d1 = TransactionAdapter.parseDate(o1.getCreatedAt());
                Date d2 = TransactionAdapter.parseDate(o2.getCreatedAt());
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d2.compareTo(d1);
            }
        });

        Map<String, DateGroup> map = new LinkedHashMap<>();
        for (Transaction tx : sorted) {
            String dateKey = TransactionAdapter.formatDateOnly(tx.getCreatedAt());
            if (!map.containsKey(dateKey)) {
                map.put(dateKey, new DateGroup(dateKey));
            }
            map.get(dateKey).transactions.add(tx);
        }

        dateGroups.addAll(map.values());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DateCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet_mb_card, parent, false);
        return new DateCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateCardViewHolder holder, int position) {
        DateGroup group = dateGroups.get(position);
        holder.bind(group, listener, df, accountNumber, displayMode);
    }

    @Override
    public int getItemCount() {
        return dateGroups.size();
    }

    public static class DateCardViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMbDateHeader;
        private final LinearLayout layoutMbItemsContainer;

        public DateCardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMbDateHeader = itemView.findViewById(R.id.tvMbDateHeader);
            layoutMbItemsContainer = itemView.findViewById(R.id.layoutMbItemsContainer);
        }

        public void bind(final DateGroup group, final OnTransactionClickListener listener, final DecimalFormat df, final String stk, final int mode) {
            tvMbDateHeader.setText(group.date);
            layoutMbItemsContainer.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            for (int i = 0; i < group.transactions.size(); i++) {
                final Transaction tx = group.transactions.get(i);
                if (i > 0) {
                    View divider = new View(itemView.getContext());
                    divider.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.border_light));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 2);
                    lp.setMargins(40, 0, 40, 0);
                    divider.setLayoutParams(lp);
                    layoutMbItemsContainer.addView(divider);
                }

                View row = inflater.inflate(R.layout.item_wallet_mb_row, layoutMbItemsContainer, false);
                TextView tvMbRowTitle = row.findViewById(R.id.tvMbRowTitle);
                TextView tvMbRowSubtitle = row.findViewById(R.id.tvMbRowSubtitle);
                TextView tvMbRowAmount = row.findViewById(R.id.tvMbRowAmount);
                TextView tvMbRowBalanceAfter = row.findViewById(R.id.tvMbRowBalanceAfter);

                String title = tx.getNote();
                if (mode == MODE_ROOM) {
                    String rawCat = tx.getCategory() != null ? tx.getCategory().trim() : "";
                    String rawNote = tx.getNote() != null ? tx.getNote().trim() : "";
                    if (rawNote.contains("[STK:")) {
                        int idx = rawNote.indexOf("[STK:");
                        int endIdx = rawNote.indexOf("]", idx);
                        String userNotePart = rawNote.substring(0, idx).trim();
                        if (!userNotePart.isEmpty()) {
                            title = userNotePart;
                        } else if (endIdx > idx) {
                            title = "Chuyển tiền (" + rawNote.substring(idx + 1, endIdx) + ")";
                        } else {
                            title = "Chuyển tiền ngân hàng";
                        }
                    } else if (rawNote.isEmpty() || rawNote.equalsIgnoreCase("Góp quỹ từ Ví Koin") || rawNote.equalsIgnoreCase("Rút quỹ về Ví Koin") || rawNote.equalsIgnoreCase("Rút tiền về Ví Koin")) {
                        if (tx.isIncome()) {
                            title = "Góp quỹ";
                        } else if (!rawCat.isEmpty() && !rawCat.equalsIgnoreCase("Giao dịch") && !rawCat.equalsIgnoreCase("Khác")) {
                            title = "Chi " + rawCat.toLowerCase();
                        } else {
                            title = "Chi tiêu quỹ";
                        }
                    }
                } else {
                    if (title == null || title.trim().isEmpty()) {
                        if ("Nạp ví SePay".equalsIgnoreCase(tx.getCategory())) {
                            title = "Nạp tiền vào ví Koin";
                        } else if ("Rút tiền".equalsIgnoreCase(tx.getCategory())) {
                            title = "Rút tiền về ngân hàng";
                        } else if (tx.getRoomId() != null && !tx.getRoomId().trim().isEmpty()) {
                            if (WalletTransactionsFragment.isWalletIncome(tx)) {
                                title = "Rút quỹ về Ví Koin";
                            } else {
                                title = "Góp quỹ";
                            }
                        } else {
                            title = tx.getCategory() != null ? tx.getCategory() : "Giao dịch";
                        }
                    } else if (title.toUpperCase().startsWith("SF NAP")) {
                        title = "Nạp tiền vào ví Koin";
                    } else if (title.toUpperCase().startsWith("SF RUT")) {
                        title = "Rút quỹ về Ví Koin";
                    }
                }
                tvMbRowTitle.setText(title);

                String timeStr = TransactionAdapter.formatOnlyTime(tx.getCreatedAt());
                if (timeStr.isEmpty()) {
                    timeStr = "00:00";
                }
                String sub = timeStr;
                if (mode == MODE_ROOM) {
                    String creator = tx.getCreatedBy();
                    if (creator == null || creator.trim().isEmpty()) {
                        creator = "Thành viên";
                    }
                    sub = timeStr + " · " + creator;
                } else if (stk != null && !stk.trim().isEmpty()) {
                    sub = timeStr + " · TK " + stk;
                }
                tvMbRowSubtitle.setText(sub);

                boolean isPositive;
                if (mode == MODE_WALLET) {
                    isPositive = WalletTransactionsFragment.isWalletIncome(tx);
                } else {
                    isPositive = tx.isIncome();
                }
                String amountStr = (isPositive ? "+" : "-") + df.format(tx.getAmount()) + " ₫";
                tvMbRowAmount.setText(amountStr);
                if (isPositive) {
                    tvMbRowAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_income));
                } else {
                    tvMbRowAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
                }

                if (mode == MODE_WALLET) {
                    tvMbRowBalanceAfter.setVisibility(View.VISIBLE);
                    tvMbRowBalanceAfter.setText("Số dư: " + df.format(tx.getBalanceAfter()) + " ₫");
                } else {
                    if (tx.getBalanceAfter() > 0) {
                        tvMbRowBalanceAfter.setVisibility(View.VISIBLE);
                        tvMbRowBalanceAfter.setText("Số dư: " + df.format(tx.getBalanceAfter()) + " ₫");
                    } else {
                        tvMbRowBalanceAfter.setVisibility(View.GONE);
                    }
                }

                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onTransactionClick(tx);
                        }
                    }
                });

                layoutMbItemsContainer.addView(row);
            }
        }
    }
}
