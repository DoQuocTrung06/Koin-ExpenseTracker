package com.sharedfund.app.ui.room;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FundActionBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_ROOM_BALANCE = "arg_room_balance";

    public interface OnFundActionListener {
        void onActionSuccess();
    }

    private String roomId;
    private double roomBalance;
    private double walletBalance;
    private OnFundActionListener listener;

    private TextView tvFundActionRoomBalance;
    private TextView tvWalletAvailableBalance;
    private TabLayout tabLayoutFundAction;
    private LinearLayout layoutContributeTab;
    private LinearLayout layoutWithdrawTab;

    private MaterialButton btnQuickContribute50k;
    private MaterialButton btnQuickContribute100k;
    private MaterialButton btnQuickContribute200k;
    private MaterialButton btnQuickContributeAll;
    private TextInputLayout tilContributeAmount;
    private TextInputEditText etContributeAmount;
    private TextInputLayout tilContributeNote;
    private TextInputEditText etContributeNote;
    private MaterialButton btnConfirmContribute;

    private MaterialButton btnQuickWithdraw50k;
    private MaterialButton btnQuickWithdraw100k;
    private MaterialButton btnQuickWithdraw200k;
    private MaterialButton btnQuickWithdrawAll;
    private TextInputLayout tilWithdrawAmount;
    private TextInputEditText etWithdrawAmount;
    private TextInputLayout tilWithdrawNote;
    private TextInputEditText etWithdrawNote;
    private MaterialButton btnConfirmWithdraw;

    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    private DecimalFormat df;

    public static FundActionBottomSheet newInstance(String roomId, double roomBalance) {
        FundActionBottomSheet sheet = new FundActionBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putDouble(ARG_ROOM_BALANCE, roomBalance);
        sheet.setArguments(args);
        return sheet;
    }

    public static FundActionBottomSheet newInstance(double roomBalance) {
        return newInstance("", roomBalance);
    }

    public void setListener(OnFundActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        symbols.setGroupingSeparator('.');
        df = new DecimalFormat("#,##0", symbols);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID, "");
            roomBalance = getArguments().getDouble(ARG_ROOM_BALANCE, 0.0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_fund_action, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabs();
        setupListeners();
    }

    private void initViews(View view) {
        tvFundActionRoomBalance = view.findViewById(R.id.tvFundActionRoomBalance);
        tvWalletAvailableBalance = view.findViewById(R.id.tvWalletAvailableBalance);
        tabLayoutFundAction = view.findViewById(R.id.tabLayoutFundAction);
        layoutContributeTab = view.findViewById(R.id.layoutContributeTab);
        layoutWithdrawTab = view.findViewById(R.id.layoutWithdrawTab);

        btnQuickContribute50k = view.findViewById(R.id.btnQuickContribute50k);
        btnQuickContribute100k = view.findViewById(R.id.btnQuickContribute100k);
        btnQuickContribute200k = view.findViewById(R.id.btnQuickContribute200k);
        btnQuickContributeAll = view.findViewById(R.id.btnQuickContributeAll);
        tilContributeAmount = view.findViewById(R.id.tilContributeAmount);
        etContributeAmount = view.findViewById(R.id.etContributeAmount);
        tilContributeNote = view.findViewById(R.id.tilContributeNote);
        etContributeNote = view.findViewById(R.id.etContributeNote);
        btnConfirmContribute = view.findViewById(R.id.btnConfirmContribute);

        btnQuickWithdraw50k = view.findViewById(R.id.btnQuickWithdraw50k);
        btnQuickWithdraw100k = view.findViewById(R.id.btnQuickWithdraw100k);
        btnQuickWithdraw200k = view.findViewById(R.id.btnQuickWithdraw200k);
        btnQuickWithdrawAll = view.findViewById(R.id.btnQuickWithdrawAll);
        tilWithdrawAmount = view.findViewById(R.id.tilWithdrawAmount);
        etWithdrawAmount = view.findViewById(R.id.etWithdrawAmount);
        tilWithdrawNote = view.findViewById(R.id.tilWithdrawNote);
        etWithdrawNote = view.findViewById(R.id.etWithdrawNote);
        btnConfirmWithdraw = view.findViewById(R.id.btnConfirmWithdraw);

        walletBalance = RoomRepository.getInstance().getUserWalletBalance();
        tvFundActionRoomBalance.setText("Số dư quỹ hiện tại: " + df.format(roomBalance) + " ₫");
        tvWalletAvailableBalance.setText("Số dư Ví Koin khả dụng: " + df.format(walletBalance) + " ₫");
    }

    private void setupTabs() {
        tabLayoutFundAction.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                layoutContributeTab.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
                layoutWithdrawTab.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {
        btnQuickContribute50k.setOnClickListener(v -> etContributeAmount.setText("50000"));
        btnQuickContribute100k.setOnClickListener(v -> etContributeAmount.setText("100000"));
        btnQuickContribute200k.setOnClickListener(v -> etContributeAmount.setText("200000"));
        btnQuickContributeAll.setOnClickListener(v -> {
            long maxWhole = (long) Math.floor(walletBalance);
            if (maxWhole > 0) etContributeAmount.setText(String.valueOf(maxWhole));
        });

        btnQuickWithdraw50k.setOnClickListener(v -> etWithdrawAmount.setText("50000"));
        btnQuickWithdraw100k.setOnClickListener(v -> etWithdrawAmount.setText("100000"));
        btnQuickWithdraw200k.setOnClickListener(v -> etWithdrawAmount.setText("200000"));
        btnQuickWithdrawAll.setOnClickListener(v -> {
            long maxWhole = (long) Math.floor(roomBalance);
            if (maxWhole > 0) etWithdrawAmount.setText(String.valueOf(maxWhole));
        });

        btnConfirmContribute.setOnClickListener(v -> processContribute());
        btnConfirmWithdraw.setOnClickListener(v -> processWithdraw());
    }

    private void processContribute() {
        String amountStr = etContributeAmount.getText() != null ? etContributeAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilContributeAmount.setError("Vui lòng nhập số tiền muốn góp");
            return;
        }
        tilContributeAmount.setError(null);

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            tilContributeAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) {
            tilContributeAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        if (amount > walletBalance) {
            tilContributeAmount.setError("Số dư Ví Koin không đủ (Còn " + df.format(walletBalance) + " ₫)");
            return;
        }

        String note = etContributeNote.getText() != null ? etContributeNote.getText().toString().trim() : "";
        if (note.isEmpty()) note = "Góp quỹ nhóm";

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();

        btnConfirmContribute.setEnabled(false);

        Transaction tx = new Transaction();
        tx.setRoomId(roomId);
        tx.setUserId(userId);
        tx.setCreatedBy(userName != null ? userName : "Bạn");
        tx.setType(Transaction.TYPE_INCOME);
        tx.setAmount(amount);
        tx.setCategory("Đóng quỹ");
        tx.setNote(note);
        tx.setStatus("APPROVED");

        RoomRepository.getInstance().addTransaction(tx);
        RoomRepository.getInstance().setUserWalletBalance(walletBalance - amount);

        TransactionRepository remote = new TransactionRepository();
        remote.setAccessToken(session.getAccessToken());
        remote.transferWalletToRoom(userId, roomId, amount, note, new TransactionRepository.DataCallback<com.google.gson.JsonObject>() {
            @Override public void onSuccess(com.google.gson.JsonObject data) {}
            @Override public void onError(String errorMessage) {}
        });

        Toast.makeText(requireContext(), "Đã góp " + df.format(amount) + " ₫ vào quỹ từ Ví Koin!", Toast.LENGTH_SHORT).show();
        if (listener != null) listener.onActionSuccess();
        dismiss();
    }

    private void processWithdraw() {
        String amountStr = etWithdrawAmount.getText() != null ? etWithdrawAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilWithdrawAmount.setError("Vui lòng nhập số tiền muốn rút");
            return;
        }
        tilWithdrawAmount.setError(null);

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            tilWithdrawAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) {
            tilWithdrawAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        if (amount > roomBalance) {
            tilWithdrawAmount.setError("Số dư quỹ không đủ (Còn " + df.format(roomBalance) + " ₫)");
            return;
        }

        String note = etWithdrawNote.getText() != null ? etWithdrawNote.getText().toString().trim() : "";
        if (note.isEmpty()) note = "Rút tiền về ví Koin";

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();

        btnConfirmWithdraw.setEnabled(false);

        Transaction tx = new Transaction();
        tx.setRoomId(roomId);
        tx.setUserId(userId);
        tx.setCreatedBy(userName != null ? userName : "Bạn");
        tx.setType(Transaction.TYPE_EXPENSE);
        tx.setAmount(amount);
        tx.setCategory("Rút về ví");
        tx.setNote(note);
        tx.setStatus("APPROVED");

        RoomRepository.getInstance().addTransaction(tx);
        RoomRepository.getInstance().setUserWalletBalance(walletBalance + amount);

        TransactionRepository remote = new TransactionRepository();
        remote.setAccessToken(session.getAccessToken());
        remote.submitWithdrawal(userId, userName, roomId, amount, "Rút về ví", note, true, new TransactionRepository.DataCallback<Transaction>() {
            @Override public void onSuccess(Transaction created) {}
            @Override public void onError(String errorMessage) {}
        });

        Toast.makeText(requireContext(), "Đã rút " + df.format(amount) + " ₫ về Ví Koin thành công!", Toast.LENGTH_SHORT).show();
        if (listener != null) listener.onActionSuccess();
        dismiss();
    }
}
