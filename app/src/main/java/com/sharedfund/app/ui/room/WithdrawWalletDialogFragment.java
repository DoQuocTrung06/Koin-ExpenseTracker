package com.sharedfund.app.ui.room;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class WithdrawWalletDialogFragment extends BottomSheetDialogFragment {

    public interface OnWithdrawCompleteListener {
        void onWithdrawComplete();
    }

    private OnWithdrawCompleteListener listener;
    private double currentBalance = 0.0;
    private final TransactionRepository transactionRepository = new TransactionRepository();

    private TextView tvAvailableBalance;
    private TextInputLayout tilWithdrawAmount;
    private TextInputEditText etWithdrawAmount;
    private TextInputLayout tilWithdrawBank;
    private TextInputEditText etWithdrawBank;
    private TextInputLayout tilWithdrawAccountNumber;
    private TextInputEditText etWithdrawAccountNumber;
    private TextInputLayout tilWithdrawAccountHolder;
    private TextInputEditText etWithdrawAccountHolder;
    private ProgressBar pbWithdrawLoading;
    private MaterialButton btnCancelWithdraw;
    private MaterialButton btnConfirmWithdraw;

    private MaterialButton btnQuick50k;
    private MaterialButton btnQuick100k;
    private MaterialButton btnQuick200k;
    private MaterialButton btnQuickAll;

    public static WithdrawWalletDialogFragment newInstance() {
        return new WithdrawWalletDialogFragment();
    }

    public void setOnWithdrawCompleteListener(OnWithdrawCompleteListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_withdraw_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupData();
        setupListeners();
    }

    private void initViews(View view) {
        tvAvailableBalance = view.findViewById(R.id.tvAvailableBalance);
        tilWithdrawAmount = view.findViewById(R.id.tilWithdrawAmount);
        etWithdrawAmount = view.findViewById(R.id.etWithdrawAmount);
        tilWithdrawBank = view.findViewById(R.id.tilWithdrawBank);
        etWithdrawBank = view.findViewById(R.id.etWithdrawBank);
        tilWithdrawAccountNumber = view.findViewById(R.id.tilWithdrawAccountNumber);
        etWithdrawAccountNumber = view.findViewById(R.id.etWithdrawAccountNumber);
        tilWithdrawAccountHolder = view.findViewById(R.id.tilWithdrawAccountHolder);
        etWithdrawAccountHolder = view.findViewById(R.id.etWithdrawAccountHolder);
        pbWithdrawLoading = view.findViewById(R.id.pbWithdrawLoading);
        btnCancelWithdraw = view.findViewById(R.id.btnCancelWithdraw);
        btnConfirmWithdraw = view.findViewById(R.id.btnConfirmWithdraw);

        btnQuick50k = view.findViewById(R.id.btnQuick50k);
        btnQuick100k = view.findViewById(R.id.btnQuick100k);
        btnQuick200k = view.findViewById(R.id.btnQuick200k);
        btnQuickAll = view.findViewById(R.id.btnQuickAll);
    }

    private void setupData() {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String fullName = session.getCurrentUserName();
        if (fullName != null && !fullName.isEmpty()) {
            etWithdrawAccountHolder.setText(fullName.toUpperCase(Locale.ROOT));
        }

        currentBalance = RoomRepository.getInstance().getUserWalletBalance();
        updateBalanceDisplay(currentBalance);

        if (session.isLoggedIn()) {
            RoomRepository.getInstance().loadWalletBalance(session.getUserId(), new RoomRepository.DataCallback<Double>() {
                @Override
                public void onSuccess(Double balance) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            currentBalance = balance != null ? balance : 0.0;
                            updateBalanceDisplay(currentBalance);
                        });
                    }
                }

                @Override
                public void onError(String message) {}
            });
        }
    }

    private void updateBalanceDisplay(double balance) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        tvAvailableBalance.setText("Số dư ví khả dụng: " + df.format(balance) + " ₫");
    }

    private void setupListeners() {
        btnQuick50k.setOnClickListener(v -> etWithdrawAmount.setText("50000"));
        btnQuick100k.setOnClickListener(v -> etWithdrawAmount.setText("100000"));
        btnQuick200k.setOnClickListener(v -> etWithdrawAmount.setText("200000"));
        btnQuickAll.setOnClickListener(v -> {
            long wholeAmount = (long) Math.floor(currentBalance);
            if (wholeAmount > 0) {
                etWithdrawAmount.setText(String.valueOf(wholeAmount));
            }
        });

        btnCancelWithdraw.setOnClickListener(v -> dismiss());

        btnConfirmWithdraw.setOnClickListener(v -> executeWithdrawal());
    }

    private void executeWithdrawal() {
        String amountStr = etWithdrawAmount.getText() != null ? etWithdrawAmount.getText().toString().trim() : "";
        String bankName = etWithdrawBank.getText() != null ? etWithdrawBank.getText().toString().trim() : "";
        String accountNumber = etWithdrawAccountNumber.getText() != null ? etWithdrawAccountNumber.getText().toString().trim() : "";
        String accountHolder = etWithdrawAccountHolder.getText() != null ? etWithdrawAccountHolder.getText().toString().trim() : "";

        if (amountStr.isEmpty()) {
            tilWithdrawAmount.setError("Vui lòng nhập số tiền rút");
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
            tilWithdrawAmount.setError("Số tiền phải lớn hơn 0 ₫");
            return;
        }

        if (amount > currentBalance) {
            tilWithdrawAmount.setError("Số dư ví không đủ để rút số tiền này");
            return;
        }

        if (bankName.isEmpty()) {
            tilWithdrawBank.setError("Vui lòng nhập tên ngân hàng");
            return;
        }
        tilWithdrawBank.setError(null);

        if (accountNumber.isEmpty()) {
            tilWithdrawAccountNumber.setError("Vui lòng nhập số tài khoản");
            return;
        }
        tilWithdrawAccountNumber.setError(null);

        if (accountHolder.isEmpty()) {
            tilWithdrawAccountHolder.setError("Vui lòng nhập tên chủ tài khoản");
            return;
        }
        tilWithdrawAccountHolder.setError(null);

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = RoomRepository.getInstance().getCurrentUserId();
        }

        setLoading(true);

        transactionRepository.setAccessToken(session.getAccessToken());
        transactionRepository.withdrawWallet(userId, amount, bankName, accountNumber, accountHolder, new TransactionRepository.DataCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject value) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Yêu cầu rút tiền thành công!", Toast.LENGTH_SHORT).show();
                    RoomRepository.getInstance().loadUserProfile();
                    if (listener != null) {
                        listener.onWithdrawComplete();
                    }
                    dismiss();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Rút tiền thất bại: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        pbWithdrawLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnConfirmWithdraw.setEnabled(!loading);
        btnCancelWithdraw.setEnabled(!loading);
    }
}
