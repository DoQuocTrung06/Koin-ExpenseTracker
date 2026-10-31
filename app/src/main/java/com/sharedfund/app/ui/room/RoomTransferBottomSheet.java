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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.ScannedQrPayment;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class RoomTransferBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_ROOM_BALANCE = "arg_room_balance";

    public interface OnTransferListener {
        void onTransferComplete();
        void onRequestQrScan();
    }

    private String roomId;
    private double roomBalance;
    private OnTransferListener listener;

    private TextView tvTransferRoomBalance;
    private TabLayout tabLayoutRoomTransfer;
    private LinearLayout layoutTransferWallet;
    private LinearLayout layoutTransferBank;
    private LinearLayout layoutTransferQr;

    private MaterialButton btnQuickWallet50k;
    private MaterialButton btnQuickWallet100k;
    private MaterialButton btnQuickWallet200k;
    private MaterialButton btnQuickWalletAll;
    private TextInputLayout tilTransferWalletAmount;
    private TextInputEditText etTransferWalletAmount;
    private TextInputLayout tilTransferWalletNote;
    private TextInputEditText etTransferWalletNote;
    private MaterialButton btnConfirmTransferWallet;

    private TextInputLayout tilTransferBankName;
    private TextInputEditText etTransferBankName;
    private TextInputLayout tilTransferBankAccNum;
    private TextInputEditText etTransferBankAccNum;
    private TextInputLayout tilTransferBankAccHolder;
    private TextInputEditText etTransferBankAccHolder;
    private TextInputLayout tilTransferBankAmount;
    private TextInputEditText etTransferBankAmount;
    private TextInputLayout tilTransferBankNote;
    private TextInputEditText etTransferBankNote;
    private MaterialButton btnConfirmTransferBank;

    private MaterialButton btnScanQrTransfer;
    private MaterialCardView cardQrResult;
    private TextView tvQrResultDetails;
    private TextInputLayout tilQrAmount;
    private TextInputEditText etQrAmount;
    private TextInputLayout tilQrNote;
    private TextInputEditText etQrNote;
    private MaterialButton btnConfirmTransferQr;
    private ScannedQrPayment pendingQr;

    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    private DecimalFormat df;

    public static RoomTransferBottomSheet newInstance(String roomId, double roomBalance) {
        RoomTransferBottomSheet sheet = new RoomTransferBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putDouble(ARG_ROOM_BALANCE, roomBalance);
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(OnTransferListener listener) {
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
            roomId = getArguments().getString(ARG_ROOM_ID);
            roomBalance = getArguments().getDouble(ARG_ROOM_BALANCE, 0.0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_room_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabs();
        setupListeners();
    }

    private void initViews(View view) {
        tvTransferRoomBalance = view.findViewById(R.id.tvTransferRoomBalance);
        tabLayoutRoomTransfer = view.findViewById(R.id.tabLayoutRoomTransfer);
        layoutTransferWallet = view.findViewById(R.id.layoutTransferWallet);
        layoutTransferBank = view.findViewById(R.id.layoutTransferBank);
        layoutTransferQr = view.findViewById(R.id.layoutTransferQr);

        btnQuickWallet50k = view.findViewById(R.id.btnQuickWallet50k);
        btnQuickWallet100k = view.findViewById(R.id.btnQuickWallet100k);
        btnQuickWallet200k = view.findViewById(R.id.btnQuickWallet200k);
        btnQuickWalletAll = view.findViewById(R.id.btnQuickWalletAll);
        tilTransferWalletAmount = view.findViewById(R.id.tilTransferWalletAmount);
        etTransferWalletAmount = view.findViewById(R.id.etTransferWalletAmount);
        tilTransferWalletNote = view.findViewById(R.id.tilTransferWalletNote);
        etTransferWalletNote = view.findViewById(R.id.etTransferWalletNote);
        btnConfirmTransferWallet = view.findViewById(R.id.btnConfirmTransferWallet);

        tilTransferBankName = view.findViewById(R.id.tilTransferBankName);
        etTransferBankName = view.findViewById(R.id.etTransferBankName);
        tilTransferBankAccNum = view.findViewById(R.id.tilTransferBankAccNum);
        etTransferBankAccNum = view.findViewById(R.id.etTransferBankAccNum);
        tilTransferBankAccHolder = view.findViewById(R.id.tilTransferBankAccHolder);
        etTransferBankAccHolder = view.findViewById(R.id.etTransferBankAccHolder);
        tilTransferBankAmount = view.findViewById(R.id.tilTransferBankAmount);
        etTransferBankAmount = view.findViewById(R.id.etTransferBankAmount);
        tilTransferBankNote = view.findViewById(R.id.tilTransferBankNote);
        etTransferBankNote = view.findViewById(R.id.etTransferBankNote);
        btnConfirmTransferBank = view.findViewById(R.id.btnConfirmTransferBank);

        btnScanQrTransfer = view.findViewById(R.id.btnScanQrTransfer);
        cardQrResult = view.findViewById(R.id.cardQrResult);
        tvQrResultDetails = view.findViewById(R.id.tvQrResultDetails);
        tilQrAmount = view.findViewById(R.id.tilQrAmount);
        etQrAmount = view.findViewById(R.id.etQrAmount);
        tilQrNote = view.findViewById(R.id.tilQrNote);
        etQrNote = view.findViewById(R.id.etQrNote);
        btnConfirmTransferQr = view.findViewById(R.id.btnConfirmTransferQr);

        tvTransferRoomBalance.setText("Số dư quỹ khả dụng: " + df.format(roomBalance) + " ₫");
    }

    private void setupTabs() {
        tabLayoutRoomTransfer.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                layoutTransferWallet.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
                layoutTransferBank.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
                layoutTransferQr.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {
        btnQuickWallet50k.setOnClickListener(v -> etTransferWalletAmount.setText("50000"));
        btnQuickWallet100k.setOnClickListener(v -> etTransferWalletAmount.setText("100000"));
        btnQuickWallet200k.setOnClickListener(v -> etTransferWalletAmount.setText("200000"));
        btnQuickWalletAll.setOnClickListener(v -> {
            long maxWhole = (long) Math.floor(roomBalance);
            if (maxWhole > 0) etTransferWalletAmount.setText(String.valueOf(maxWhole));
        });

        btnConfirmTransferWallet.setOnClickListener(v -> processWalletTransfer());
        btnConfirmTransferBank.setOnClickListener(v -> processBankTransfer());

        btnScanQrTransfer.setOnClickListener(v -> {
            if (listener != null) listener.onRequestQrScan();
        });

        btnConfirmTransferQr.setOnClickListener(v -> processQrTransfer());
    }

    public void setScannedQrData(ScannedQrPayment parsed) {
        this.pendingQr = parsed;
        if (tabLayoutRoomTransfer != null && tabLayoutRoomTransfer.getTabCount() > 2) {
            TabLayout.Tab qrTab = tabLayoutRoomTransfer.getTabAt(2);
            if (qrTab != null) qrTab.select();
        }
        if (cardQrResult != null && parsed != null) {
            cardQrResult.setVisibility(View.VISIBLE);
            String info = "Tài khoản nhận: " + parsed.accountNumber
                    + "\nNgân hàng/BIN: " + (parsed.bankBin != null ? parsed.bankBin : "Không xác định")
                    + "\nNgười nhận: " + (parsed.recipientName != null ? parsed.recipientName : "Chưa xác định");
            tvQrResultDetails.setText(info);
            if (parsed.amount != null && parsed.amount > 0) {
                etQrAmount.setText(String.valueOf(parsed.amount.longValue()));
            }
            if (parsed.transferContent != null && !parsed.transferContent.trim().isEmpty()) {
                etQrNote.setText(parsed.transferContent);
            }
        }
    }

    private void processWalletTransfer() {
        String amountStr = etTransferWalletAmount.getText() != null ? etTransferWalletAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilTransferWalletAmount.setError("Vui lòng nhập số tiền muốn chuyển");
            return;
        }
        tilTransferWalletAmount.setError(null);
        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            tilTransferWalletAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) {
            tilTransferWalletAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        if (amount > roomBalance) {
            tilTransferWalletAmount.setError("Số dư quỹ không đủ (Còn " + df.format(roomBalance) + " ₫)");
            return;
        }

        String note = etTransferWalletNote.getText() != null ? etTransferWalletNote.getText().toString().trim() : "";
        if (note.isEmpty()) note = "Rút tiền về ví Koin";

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();

        btnConfirmTransferWallet.setEnabled(false);

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
        double currentWallet = RoomRepository.getInstance().getUserWalletBalance();
        RoomRepository.getInstance().setUserWalletBalance(currentWallet + amount);

        TransactionRepository remote = new TransactionRepository();
        remote.setAccessToken(session.getAccessToken());
        remote.submitWithdrawal(userId, userName, roomId, amount, "Rút về ví", note, true, new TransactionRepository.DataCallback<Transaction>() {
            @Override public void onSuccess(Transaction created) {}
            @Override public void onError(String errorMessage) {}
        });

        Toast.makeText(requireContext(), "Đã chuyển " + df.format(amount) + " ₫ về Ví Koin thành công!", Toast.LENGTH_SHORT).show();
        if (listener != null) listener.onTransferComplete();
        dismiss();
    }

    private void processBankTransfer() {
        String bank = etTransferBankName.getText() != null ? etTransferBankName.getText().toString().trim() : "";
        String accNum = etTransferBankAccNum.getText() != null ? etTransferBankAccNum.getText().toString().trim() : "";
        String holder = etTransferBankAccHolder.getText() != null ? etTransferBankAccHolder.getText().toString().trim() : "";
        String amountStr = etTransferBankAmount.getText() != null ? etTransferBankAmount.getText().toString().trim() : "";
        String note = etTransferBankNote.getText() != null ? etTransferBankNote.getText().toString().trim() : "";

        if (bank.isEmpty()) {
            tilTransferBankName.setError("Vui lòng nhập tên ngân hàng");
            return;
        }
        tilTransferBankName.setError(null);

        if (accNum.isEmpty()) {
            tilTransferBankAccNum.setError("Vui lòng nhập số tài khoản");
            return;
        }
        tilTransferBankAccNum.setError(null);

        if (amountStr.isEmpty()) {
            tilTransferBankAmount.setError("Vui lòng nhập số tiền");
            return;
        }
        tilTransferBankAmount.setError(null);

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            tilTransferBankAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) {
            tilTransferBankAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        if (amount > roomBalance) {
            tilTransferBankAmount.setError("Số dư quỹ không đủ (Còn " + df.format(roomBalance) + " ₫)");
            return;
        }

        if (note.isEmpty()) note = "Chuyển tiền quỹ đến " + bank;

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();

        btnConfirmTransferBank.setEnabled(false);

        Transaction tx = new Transaction();
        tx.setRoomId(roomId);
        tx.setUserId(userId);
        tx.setCreatedBy(userName != null ? userName : "Bạn");
        tx.setType(Transaction.TYPE_EXPENSE);
        tx.setAmount(amount);
        tx.setCategory("Chuyển ngân hàng");
        tx.setNote(note + " (" + bank + " - " + accNum + (holder.isEmpty() ? "" : " - " + holder) + ")");
        tx.setStatus("APPROVED");

        RoomRepository.getInstance().addTransaction(tx);

        TransactionRepository remote = new TransactionRepository();
        remote.setAccessToken(session.getAccessToken());
        remote.submitWithdrawal(userId, userName, roomId, amount, "Chuyển ngân hàng", tx.getNote(), true, new TransactionRepository.DataCallback<Transaction>() {
            @Override public void onSuccess(Transaction created) {}
            @Override public void onError(String errorMessage) {}
        });

        Toast.makeText(requireContext(), "Đã lập lệnh chuyển " + df.format(amount) + " ₫ đến ngân hàng!", Toast.LENGTH_SHORT).show();
        if (listener != null) listener.onTransferComplete();
        dismiss();
    }

    private void processQrTransfer() {
        String amountStr = etQrAmount.getText() != null ? etQrAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilQrAmount.setError("Vui lòng nhập số tiền");
            return;
        }
        tilQrAmount.setError(null);

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            tilQrAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) {
            tilQrAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        if (amount > roomBalance) {
            tilQrAmount.setError("Số dư quỹ không đủ (Còn " + df.format(roomBalance) + " ₫)");
            return;
        }

        String note = etQrNote.getText() != null ? etQrNote.getText().toString().trim() : "";
        if (note.isEmpty()) note = "Thanh toán QR từ quỹ";

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();

        btnConfirmTransferQr.setEnabled(false);

        Transaction tx = new Transaction();
        tx.setRoomId(roomId);
        tx.setUserId(userId);
        tx.setCreatedBy(userName != null ? userName : "Bạn");
        tx.setType(Transaction.TYPE_EXPENSE);
        tx.setAmount(amount);
        tx.setCategory("Chi QR");
        String recipientNote = pendingQr != null && pendingQr.accountNumber != null
                ? " (" + pendingQr.accountNumber + (pendingQr.recipientName != null ? " - " + pendingQr.recipientName : "") + ")"
                : "";
        tx.setNote(note + recipientNote);
        tx.setStatus("APPROVED");

        RoomRepository.getInstance().addTransaction(tx);

        if (pendingQr != null) {
            TransactionRepository remote = new TransactionRepository();
            remote.setAccessToken(session.getAccessToken());
            remote.payRoomRecipient(roomId, amount, "Chi QR", note, pendingQr.bankBin, pendingQr.accountNumber,
                    pendingQr.recipientName, pendingQr.transferContent, new TransactionRepository.DataCallback<com.google.gson.JsonObject>() {
                        @Override public void onSuccess(com.google.gson.JsonObject data) {}
                        @Override public void onError(String errorMessage) {}
                    });
        }

        Toast.makeText(requireContext(), "Đã thanh toán " + df.format(amount) + " ₫ từ quỹ thành công!", Toast.LENGTH_SHORT).show();
        if (listener != null) listener.onTransferComplete();
        dismiss();
    }
}
