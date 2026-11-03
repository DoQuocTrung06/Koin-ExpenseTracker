package com.sharedfund.app.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.transaction.TransactionDetailBottomSheet;
import com.sharedfund.app.ui.widget.AppToaster;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletTransactionsFragment extends Fragment {

    private final TransactionRepository transactionRepository = new TransactionRepository();
    private final List<Transaction> allPersonalTransactions = new ArrayList<>();
    private final List<Transaction> displayedTransactions = new ArrayList<>();
    private WalletStatementAdapter adapter;

    private TextView tvWalletAccountName;
    private TextView tvWalletAccountNumber;
    private LinearLayout layoutWalletCopyStk;
    private TextView tvWalletCurrentBalance;

    private MaterialButton btnWalletFilterAll;
    private MaterialButton btnWalletFilterIncome;
    private MaterialButton btnWalletFilterExpense;
    private ProgressBar pbWalletLoading;
    private RecyclerView rvWalletTransactions;
    private LinearLayout layoutEmptyWallet;

    private int currentFilter = 0;
    private final DecimalFormat df;

    public WalletTransactionsFragment() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        df = new DecimalFormat("#,##0", symbols);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners(view);
        setupRecyclerView();
        loadWalletBalance();
        loadHistory();
    }

    private void initViews(View view) {
        tvWalletAccountName = view.findViewById(R.id.tvWalletAccountName);
        tvWalletAccountNumber = view.findViewById(R.id.tvWalletAccountNumber);
        layoutWalletCopyStk = view.findViewById(R.id.layoutWalletCopyStk);
        tvWalletCurrentBalance = view.findViewById(R.id.tvWalletCurrentBalance);

        btnWalletFilterAll = view.findViewById(R.id.btnWalletFilterAll);
        btnWalletFilterIncome = view.findViewById(R.id.btnWalletFilterIncome);
        btnWalletFilterExpense = view.findViewById(R.id.btnWalletFilterExpense);
        pbWalletLoading = view.findViewById(R.id.pbWalletLoading);
        rvWalletTransactions = view.findViewById(R.id.rvWalletTransactions);
        layoutEmptyWallet = view.findViewById(R.id.layoutEmptyWallet);
    }

    private void setupListeners(View view) {
        ImageButton btnWalletBack = view.findViewById(R.id.btnWalletBack);
        if (btnWalletBack != null) {
            btnWalletBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        if (layoutWalletCopyStk != null) {
            layoutWalletCopyStk.setOnClickListener(v -> {
                String stk = tvWalletAccountNumber != null ? tvWalletAccountNumber.getText().toString() : "1047061615";
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Số tài khoản", stk);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    AppToaster.info(requireActivity(), "Đã sao chép số tài khoản: " + stk);
                }
            });
        }

        btnWalletFilterAll.setOnClickListener(v -> setFilter(0));
        btnWalletFilterIncome.setOnClickListener(v -> setFilter(1));
        btnWalletFilterExpense.setOnClickListener(v -> setFilter(2));
    }

    private void setupRecyclerView() {
        adapter = new WalletStatementAdapter(transaction -> {
            TransactionDetailBottomSheet detailSheet = TransactionDetailBottomSheet.newInstance(transaction, true);
            detailSheet.show(getParentFragmentManager(), "TransactionDetailBottomSheet");
        });
        rvWalletTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWalletTransactions.setAdapter(adapter);
    }

    private String formatAccountNumber(String name, String userId) {
        if ("DO QUOC TRUNG".equalsIgnoreCase(name)) {
            return "1047061615";
        }
        if (userId == null || userId.trim().isEmpty()) {
            return "1047061615";
        }
        long hash = Math.abs((long) userId.hashCode());
        long num = 1000000000L + (hash % 9000000000L);
        return String.valueOf(num);
    }

    private void loadWalletBalance() {
        Double balance = RoomRepository.getInstance().getUserWalletLiveData().getValue();
        if (balance == null) {
            balance = 0.0;
        }
        if (tvWalletCurrentBalance != null) {
            tvWalletCurrentBalance.setText(df.format(balance) + " ₫");
        }

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String name = session.getCurrentUserName();
        if (tvWalletAccountName != null) {
            tvWalletAccountName.setText(name != null && !name.trim().isEmpty() ? name.toUpperCase(Locale.ROOT) : "DO QUOC TRUNG");
        }
        if (tvWalletAccountNumber != null) {
            String uid = session.getUserId();
            String stk = formatAccountNumber(name != null ? name.trim() : "", uid);
            tvWalletAccountNumber.setText(stk);
            if (adapter != null) {
                adapter.setAccountNumber(stk);
            }
        }
    }

    private void loadHistory() {
        String userId = UserSessionManager.getInstance(requireContext()).getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            userId = RoomRepository.getInstance().getCurrentUserId();
        }
        if (userId == null || userId.trim().isEmpty()) {
            layoutEmptyWallet.setVisibility(View.VISIBLE);
            return;
        }

        pbWalletLoading.setVisibility(View.VISIBLE);
        layoutEmptyWallet.setVisibility(View.GONE);

        transactionRepository.getUserTransactions(userId, new TransactionRepository.DataCallback<List<Transaction>>() {
            @Override
            public void onSuccess(List<Transaction> data) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    pbWalletLoading.setVisibility(View.GONE);

                    allPersonalTransactions.clear();
                    if (data != null) {
                        allPersonalTransactions.addAll(data);
                    }
                    applyFilter();
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    pbWalletLoading.setVisibility(View.GONE);
                    layoutEmptyWallet.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private boolean isIncomeTx(Transaction tx) {
        if (tx == null) return false;
        if (tx.isIncome()) return true;
        String type = tx.getType();
        return type != null && "INCOME".equalsIgnoreCase(type);
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        updateFilterButtons();
        applyFilter();
    }

    private void updateFilterButtons() {
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int surfaceContainer = ContextCompat.getColor(requireContext(), R.color.surface_variant);
        int white = ContextCompat.getColor(requireContext(), R.color.white);
        int secondaryText = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        btnWalletFilterAll.setBackgroundTintList(ColorStateList.valueOf(currentFilter == 0 ? primaryColor : surfaceContainer));
        btnWalletFilterAll.setTextColor(currentFilter == 0 ? white : secondaryText);

        btnWalletFilterIncome.setBackgroundTintList(ColorStateList.valueOf(currentFilter == 1 ? primaryColor : surfaceContainer));
        btnWalletFilterIncome.setTextColor(currentFilter == 1 ? white : secondaryText);

        btnWalletFilterExpense.setBackgroundTintList(ColorStateList.valueOf(currentFilter == 2 ? primaryColor : surfaceContainer));
        btnWalletFilterExpense.setTextColor(currentFilter == 2 ? white : secondaryText);
    }

    private void applyFilter() {
        displayedTransactions.clear();
        for (Transaction tx : allPersonalTransactions) {
            if (currentFilter == 0) {
                displayedTransactions.add(tx);
            } else if (currentFilter == 1 && isIncomeTx(tx)) {
                displayedTransactions.add(tx);
            } else if (currentFilter == 2 && !isIncomeTx(tx)) {
                displayedTransactions.add(tx);
            }
        }
        adapter.setTransactions(displayedTransactions);

        if (displayedTransactions.isEmpty()) {
            layoutEmptyWallet.setVisibility(View.VISIBLE);
            rvWalletTransactions.setVisibility(View.GONE);
        } else {
            layoutEmptyWallet.setVisibility(View.GONE);
            rvWalletTransactions.setVisibility(View.VISIBLE);
        }
    }
}
