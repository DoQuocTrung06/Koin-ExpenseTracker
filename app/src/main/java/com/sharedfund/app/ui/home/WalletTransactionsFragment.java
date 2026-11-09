package com.sharedfund.app.ui.home;



import android.content.ClipData;

import android.content.ClipboardManager;

import android.content.Context;

import android.content.res.ColorStateList;

import android.os.Bundle;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.EditText;

import android.widget.ImageButton;

import android.widget.LinearLayout;

import android.widget.ProgressBar;

import android.widget.TextView;

import android.widget.Toast;



import androidx.annotation.NonNull;

import androidx.annotation.Nullable;

import androidx.core.content.ContextCompat;

import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;



import com.google.android.material.button.MaterialButton;

import com.google.android.material.card.MaterialCardView;

import com.sharedfund.app.R;

import com.sharedfund.app.data.RoomRepository;

import com.sharedfund.app.data.TransactionRepository;

import com.sharedfund.app.data.UserSessionManager;

import com.sharedfund.app.model.Transaction;

import com.sharedfund.app.ui.transaction.TransactionAdapter;

import com.sharedfund.app.ui.transaction.TransactionDetailBottomSheet;

import com.sharedfund.app.ui.widget.CustomDateRangeDialog;
import com.sharedfund.app.ui.report.StatementReportFragment;
import com.sharedfund.app.MainActivity;

import com.sharedfund.app.ui.widget.TimeFilterBottomSheet;

import com.sharedfund.app.util.CopyToast;

import com.sharedfund.app.util.ErrorMessageHelper;



import java.text.DecimalFormat;

import java.text.DecimalFormatSymbols;

import java.util.ArrayList;

import java.util.Calendar;

import java.util.Collections;

import java.util.Comparator;

import java.util.Date;

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

    private int currentTimeFilter = 0;

    private Long customStartMillis = null;

    private Long customEndMillis = null;

    private String currentSearchKeyword = "";

    private EditText etWalletSearch;

    private ImageButton btnClearWalletSearch;



    private MaterialButton btnTimeAll;

    private MaterialButton btnTimeThisWeek;

    private MaterialButton btnTimeThisMonth;

    private MaterialButton btnTimeCustom;



    private MaterialCardView cardWalletCustomRangeBadge;

    private TextView tvWalletCustomRange;

    private ImageButton btnClearWalletCustomRange;



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

        RoomRepository.getInstance().getUserWalletLiveData().observe(getViewLifecycleOwner(), balance -> {

            if (balance != null) {

                if (tvWalletCurrentBalance != null) {

                    tvWalletCurrentBalance.setText(df.format(balance) + " ₫");

                }

                recalculateBalancesAndNotify(balance);

            }

        });

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

        etWalletSearch = view.findViewById(R.id.etWalletSearch);

        btnClearWalletSearch = view.findViewById(R.id.btnClearWalletSearch);



        btnTimeAll = view.findViewById(R.id.btnTimeAll);

        btnTimeThisWeek = view.findViewById(R.id.btnTimeThisWeek);

        btnTimeThisMonth = view.findViewById(R.id.btnTimeThisMonth);

        btnTimeCustom = view.findViewById(R.id.btnTimeCustom);



        cardWalletCustomRangeBadge = view.findViewById(R.id.cardWalletCustomRangeBadge);

        tvWalletCustomRange = view.findViewById(R.id.tvWalletCustomRange);

        btnClearWalletCustomRange = view.findViewById(R.id.btnClearWalletCustomRange);



        pbWalletLoading = view.findViewById(R.id.pbWalletLoading);

        rvWalletTransactions = view.findViewById(R.id.rvWalletTransactions);

        layoutEmptyWallet = view.findViewById(R.id.layoutEmptyWallet);



        updateTimeFilterButtons();

        updateFilterButtons();

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

                    CopyToast.show(requireContext(), requireView());

                }

            });

        }



        btnWalletFilterAll.setOnClickListener(v -> setFilter(0));

        btnWalletFilterIncome.setOnClickListener(v -> setFilter(1));

        btnWalletFilterExpense.setOnClickListener(v -> setFilter(2));



        attachTouchEffect(btnWalletFilterAll);

        attachTouchEffect(btnWalletFilterIncome);

        attachTouchEffect(btnWalletFilterExpense);



        if (btnTimeAll != null) {

            btnTimeAll.setOnClickListener(v -> setTimeFilter(0));

            attachTouchEffect(btnTimeAll);

        }

        if (btnTimeThisWeek != null) {

            btnTimeThisWeek.setOnClickListener(v -> setTimeFilter(2));

            attachTouchEffect(btnTimeThisWeek);

        }

        if (btnTimeThisMonth != null) {

            btnTimeThisMonth.setOnClickListener(v -> setTimeFilter(3));

            attachTouchEffect(btnTimeThisMonth);

        }

        if (btnTimeCustom != null) {

            btnTimeCustom.setOnClickListener(v -> showDateRangePicker());

            attachTouchEffect(btnTimeCustom);

        }



        MaterialButton btnWalletExport = view.findViewById(R.id.btnWalletExport);
        if (btnWalletExport != null) {
            btnWalletExport.setOnClickListener(v -> openStatementReportScreen());
            attachTouchEffect(btnWalletExport);
        }

        if (btnClearWalletCustomRange != null) {

            btnClearWalletCustomRange.setOnClickListener(v -> {

                customStartMillis = null;

                customEndMillis = null;

                if (cardWalletCustomRangeBadge != null) {

                    cardWalletCustomRangeBadge.setVisibility(View.GONE);

                }

                setTimeFilter(0);

            });

        }



        if (etWalletSearch != null) {

            etWalletSearch.addTextChangedListener(new android.text.TextWatcher() {

                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {

                    currentSearchKeyword = s != null ? s.toString().trim().toLowerCase(java.util.Locale.ROOT) : "";

                    if (btnClearWalletSearch != null) {

                        btnClearWalletSearch.setVisibility(currentSearchKeyword.isEmpty() ? View.GONE : View.VISIBLE);

                    }

                    applyFilter();

                }

                @Override public void afterTextChanged(android.text.Editable s) {}

            });

        }



        if (btnClearWalletSearch != null) {

            btnClearWalletSearch.setOnClickListener(v -> {

                if (etWalletSearch != null) etWalletSearch.setText("");

            });

        }

    }



    private void openStatementReportScreen() {
        if (!isAdded() || getContext() == null) return;
        if (displayedTransactions == null || displayedTransactions.isEmpty()) {
            Toast.makeText(requireContext(), "Không có giao dịch nào để xuất báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = tvWalletAccountName != null ? tvWalletAccountName.getText().toString() : "ĐỖ QUỐC TRUNG";
        String stk = tvWalletAccountNumber != null ? tvWalletAccountNumber.getText().toString() : "1047061615";
        String targetTitle = "Ví Koin Cá Nhân (" + name + ")";
        String targetSubtitle = "Số tài khoản: " + stk;

        String dateRangeLabel;
        if (currentTimeFilter == 0) {
            dateRangeLabel = "Toàn bộ thời gian";
        } else if (currentTimeFilter == 2) {
            dateRangeLabel = "Tuần này";
        } else if (currentTimeFilter == 3) {
            dateRangeLabel = "Tháng này";
        } else {
            dateRangeLabel = (tvWalletCustomRange != null && tvWalletCustomRange.getText() != null)
                    ? tvWalletCustomRange.getText().toString()
                    : "Khoảng thời gian tùy chọn";
        }

        StatementReportFragment reportFragment = StatementReportFragment.newInstance(
                targetTitle,
                targetSubtitle,
                dateRangeLabel,
                name,
                "Chủ tài khoản",
                new ArrayList<>(displayedTransactions)
        );

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(reportFragment, true);
        } else {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, reportFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showDateRangePicker() {

        CustomDateRangeDialog dialog = new CustomDateRangeDialog(requireContext(), customStartMillis, customEndMillis, (startMillis, endMillis, displayLabel) -> {

            currentTimeFilter = 4;

            customStartMillis = startMillis;

            customEndMillis = endMillis;

            if (tvWalletCustomRange != null) {

                tvWalletCustomRange.setText(displayLabel);

            }

            if (cardWalletCustomRangeBadge != null) {

                cardWalletCustomRangeBadge.setVisibility(View.VISIBLE);

            }

            updateTimeFilterButtons();

            applyFilter();

        });

        dialog.show();

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



    private void recalculateBalancesAndNotify(double balance) {

        if (allPersonalTransactions == null || allPersonalTransactions.isEmpty()) {

            return;

        }

        double runningBalance = balance;

        for (Transaction tx : allPersonalTransactions) {

            tx.setBalanceAfter(Math.max(0, runningBalance));

            double amt = tx.getAmount();

            if (isWalletIncome(tx)) {

                runningBalance = runningBalance - amt;

            } else {

                runningBalance = runningBalance + amt;

            }

        }

        if (adapter != null) {

            adapter.notifyDataSetChanged();

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

                        List<Transaction> filtered = new ArrayList<>();

                        for (Transaction tx : data) {

                            if (isRetainedWalletTransaction(tx)) {

                                filtered.add(tx);

                            }

                        }



                        Collections.sort(filtered, new Comparator<Transaction>() {

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



                        Double liveBalance = RoomRepository.getInstance().getUserWalletLiveData().getValue();

                        double runningBalance = liveBalance != null ? liveBalance : RoomRepository.getInstance().getUserWalletBalance();

                        for (Transaction tx : filtered) {

                            tx.setBalanceAfter(Math.max(0, runningBalance));

                            double amt = tx.getAmount();

                            if (isWalletIncome(tx)) {

                                runningBalance = runningBalance - amt;

                            } else {

                                runningBalance = runningBalance + amt;

                            }

                        }



                        allPersonalTransactions.addAll(filtered);

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

                    Toast.makeText(requireContext(), ErrorMessageHelper.getFriendlyMessage(errorMessage), Toast.LENGTH_SHORT).show();

                });

            }

        });

    }



    public static boolean isWithdrawToWallet(Transaction tx) {

        if (tx == null) return false;

        String note = tx.getNote() != null ? tx.getNote().toLowerCase() : "";

        String cat = tx.getCategory() != null ? tx.getCategory().toLowerCase() : "";

        if (note.contains("rút quỹ") || note.contains("rut quy")

                || note.contains("rút tiền về ví") || note.contains("rut tien ve vi")

                || note.contains("rút về ví") || note.contains("rut ve vi")

                || note.contains("rút ví") || note.contains("rut vi")

                || note.contains("rút") || note.contains("rut")

                || note.startsWith("sf rut") || note.startsWith("sf_rut")) {

            return true;

        }

        if (cat.contains("rút") || cat.contains("rut")) {

            return true;

        }

        if ((cat.equals("khác") || cat.equals("khac")) && (note.contains("ví") || note.contains("vi") || note.contains("koin") || note.isEmpty())) {

            return true;

        }

        return false;

    }



    public static boolean isWalletIncome(Transaction tx) {

        if (tx == null) return false;

        boolean hasRoom = tx.getRoomId() != null && !tx.getRoomId().trim().isEmpty();

        if (!hasRoom) {

            return tx.isIncome();

        } else {

            return tx.isExpense() && isWithdrawToWallet(tx);

        }

    }



    public static boolean isRetainedWalletTransaction(Transaction tx) {

        if (tx == null) return false;

        boolean hasRoom = tx.getRoomId() != null && !tx.getRoomId().trim().isEmpty();

        if (!hasRoom) {

            return tx.isIncome() || tx.isExpense();

        } else {

            if (tx.isIncome()) {

                return true;

            }

            if (!tx.isApproved()) {

                return false;

            }

            return tx.isExpense() && isWithdrawToWallet(tx);

        }

    }



    private boolean isIncomeTx(Transaction tx) {

        return isWalletIncome(tx);

    }



    private void setFilter(int filter) {

        currentFilter = filter;

        updateFilterButtons();

        applyFilter();

    }



    private void updateFilterButtons() {

        if (!isAdded() || getContext() == null) return;

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);

        int surfaceColor = ContextCompat.getColor(requireContext(), R.color.surface);

        int borderColor = ContextCompat.getColor(requireContext(), R.color.border);

        int textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary);

        int accentBlueLight = ContextCompat.getColor(requireContext(), R.color.accent_blue_light);



        applySubChipState(btnWalletFilterAll, currentFilter == 0, primaryColor, accentBlueLight, surfaceColor, borderColor, textPrimary);

        applySubChipState(btnWalletFilterIncome, currentFilter == 1, primaryColor, accentBlueLight, surfaceColor, borderColor, textPrimary);

        applySubChipState(btnWalletFilterExpense, currentFilter == 2, primaryColor, accentBlueLight, surfaceColor, borderColor, textPrimary);

    }



    private void applySubChipState(MaterialButton btn, boolean selected, int activeColor, int activeBgColor, int surfaceColor, int borderColor, int unselectedTextColor) {

        if (btn == null) return;

        btn.setBackgroundTintList(ColorStateList.valueOf(selected ? activeBgColor : surfaceColor));

        btn.setStrokeColor(ColorStateList.valueOf(selected ? activeColor : borderColor));

        btn.setStrokeWidth(dpToPx(1));

        btn.setTextColor(selected ? activeColor : unselectedTextColor);

    }



    private void setTimeFilter(int filter) {

        currentTimeFilter = filter;

        if (filter != 4 && filter != 1) {

            customStartMillis = null;

            customEndMillis = null;

            if (cardWalletCustomRangeBadge != null) {

                cardWalletCustomRangeBadge.setVisibility(View.GONE);

            }

        }

        updateTimeFilterButtons();

        applyFilter();

    }



    private void updateTimeFilterButtons() {

        if (!isAdded() || getContext() == null) return;

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);

        int surfaceColor = ContextCompat.getColor(requireContext(), R.color.surface);

        int transparentColor = ContextCompat.getColor(requireContext(), android.R.color.transparent);

        int textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary);



        applySegmentTabState(btnTimeAll, currentTimeFilter == 0, primaryColor, surfaceColor, transparentColor, textPrimary);

        applySegmentTabState(btnTimeThisWeek, currentTimeFilter == 2, primaryColor, surfaceColor, transparentColor, textPrimary);

        applySegmentTabState(btnTimeThisMonth, currentTimeFilter == 3, primaryColor, surfaceColor, transparentColor, textPrimary);

        applySegmentTabState(btnTimeCustom, (currentTimeFilter == 1 || currentTimeFilter == 4), primaryColor, surfaceColor, transparentColor, textPrimary);

    }



    private void applySegmentTabState(MaterialButton btn, boolean selected, int primaryColor, int surfaceColor, int transparentColor, int textPrimary) {

        if (btn == null) return;

        if (selected) {

            btn.setBackgroundResource(R.drawable.bg_segment_tab_glass);

            btn.setBackgroundTintList(null);

            btn.setTextColor(primaryColor);

            btn.setElevation(dpToPx(2));

        } else {

            btn.setBackgroundResource(0);

            btn.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            btn.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));

            btn.setTextColor(textPrimary);

            btn.setElevation(0);

        }

        if (btn == btnTimeCustom) {
            btn.setIconTint(ColorStateList.valueOf(selected ? primaryColor : textPrimary));
        }
    }



    private int dpToPx(int dp) {

        float density = getResources().getDisplayMetrics().density;

        return Math.round((float) dp * density);

    }



    private void attachTouchEffect(View v) {

        if (v == null) return;

        v.setOnTouchListener((view, event) -> {

            switch (event.getAction()) {

                case android.view.MotionEvent.ACTION_DOWN:

                    view.setAlpha(0.75f);

                    break;

                case android.view.MotionEvent.ACTION_UP:

                case android.view.MotionEvent.ACTION_CANCEL:

                    view.setAlpha(1.0f);

                    break;

            }

            return false;

        });

    }



    private boolean matchTimeFilter(Transaction tx) {

        if (currentTimeFilter == 0 || tx == null) return true;

        Date txDate = TransactionAdapter.parseDate(tx.getCreatedAt());

        if (txDate == null) return true;

        long txMillis = txDate.getTime();



        if (currentTimeFilter == 1) {

            Calendar cal = Calendar.getInstance();

            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);

            long start = cal.getTimeInMillis();

            long end = start + (24L * 60L * 60L * 1000L) - 1;

            return txMillis >= start && txMillis <= end;

        } else if (currentTimeFilter == 2) {

            Calendar cal = Calendar.getInstance();

            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);

            long start = cal.getTimeInMillis();

            long end = start + (7L * 24L * 60L * 60L * 1000L) - 1;

            return txMillis >= start && txMillis <= end;

        } else if (currentTimeFilter == 3) {

            Calendar cal = Calendar.getInstance();

            cal.set(Calendar.DAY_OF_MONTH, 1);

            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);

            long start = cal.getTimeInMillis();

            cal.add(Calendar.MONTH, 1);

            long end = cal.getTimeInMillis() - 1;

            return txMillis >= start && txMillis <= end;

        } else if (currentTimeFilter == 4) {

            if (customStartMillis != null && customEndMillis != null) {

                Calendar calStart = Calendar.getInstance();

                calStart.setTimeInMillis(customStartMillis);

                calStart.set(Calendar.HOUR_OF_DAY, 0); calStart.set(Calendar.MINUTE, 0); calStart.set(Calendar.SECOND, 0); calStart.set(Calendar.MILLISECOND, 0);



                Calendar calEnd = Calendar.getInstance();

                calEnd.setTimeInMillis(customEndMillis);

                calEnd.set(Calendar.HOUR_OF_DAY, 23); calEnd.set(Calendar.MINUTE, 59); calEnd.set(Calendar.SECOND, 59); calEnd.set(Calendar.MILLISECOND, 999);



                return txMillis >= calStart.getTimeInMillis() && txMillis <= calEnd.getTimeInMillis();

            }

        }

        return true;

    }



    private void applyFilter() {

        displayedTransactions.clear();

        for (Transaction tx : allPersonalTransactions) {

            boolean matchesType = (currentFilter == 0) || (currentFilter == 1 && isIncomeTx(tx)) || (currentFilter == 2 && !isIncomeTx(tx));

            boolean matchesTime = matchTimeFilter(tx);

            boolean matchesSearch = true;

            if (!currentSearchKeyword.isEmpty()) {

                String note = tx.getNote() != null ? tx.getNote().toLowerCase(java.util.Locale.ROOT) : "";

                String cat = tx.getCategory() != null ? tx.getCategory().toLowerCase(java.util.Locale.ROOT) : "";

                String creator = tx.getCreatedBy() != null ? tx.getCreatedBy().toLowerCase(java.util.Locale.ROOT) : "";

                matchesSearch = note.contains(currentSearchKeyword) || cat.contains(currentSearchKeyword) || creator.contains(currentSearchKeyword);

            }



            if (matchesType && matchesTime && matchesSearch) {

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

