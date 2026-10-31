package com.sharedfund.app.ui.home;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.sharedfund.app.R;
import com.sharedfund.app.data.PaymentHelper;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.SafeDns;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WalletTransferBottomSheet extends BottomSheetDialogFragment {

    public interface OnWalletTransferListener {
        void onTransferComplete();
    }

    private OnWalletTransferListener listener;

    private View btnTabDeposit;
    private View btnTabWithdraw;
    private ImageView ivTabDeposit;
    private ImageView ivTabWithdraw;
    private TextView tvTabDeposit;
    private TextView tvTabWithdraw;
    private LinearLayout layoutTabDeposit;
    private LinearLayout layoutTabWithdraw;
    private ImageButton btnTransferClose;

    private TextInputLayout tilDepositAmount;
    private TextInputEditText etDepositAmount;
    private MaterialButton btnQuick20k;
    private MaterialButton btnQuick50k;
    private MaterialButton btnQuick100k;
    private MaterialButton btnQuick200k;
    private MaterialButton btnGenerateQr;
    private ProgressBar pbDepositLoading;
    private LinearLayout layoutQrSection;
    private ImageView ivQrCode;

    private double pendingDepositAmount = 0.0;
    private String depositQrGeneratedAt;
    private final Handler sepayPollHandler = new Handler(Looper.getMainLooper());
    private Runnable sepayPollRunnable;
    private boolean isPollingSePay = false;

    private TextView tvAvailableBalance;
    private TextInputLayout tilWithdrawAmount;
    private TextInputEditText etWithdrawAmount;
    private MaterialButton btnWithdrawQuick50k;
    private MaterialButton btnWithdrawQuick100k;
    private MaterialButton btnWithdrawQuick200k;
    private MaterialButton btnWithdrawQuickAll;
    private TextInputLayout tilWithdrawBank;
    private TextInputEditText etWithdrawBank;
    private TextInputLayout tilWithdrawAccountNumber;
    private TextInputEditText etWithdrawAccountNumber;
    private TextInputLayout tilWithdrawAccountHolder;
    private TextInputEditText etWithdrawAccountHolder;
    private ProgressBar pbWithdrawLoading;
    private MaterialButton btnConfirmWithdraw;

    private double currentWalletBalance = 0.0;
    private final TransactionRepository transactionRepository = new TransactionRepository();

    public static WalletTransferBottomSheet newInstance() {
        return new WalletTransferBottomSheet();
    }

    public void setOnWalletTransferListener(OnWalletTransferListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bsd = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_wallet_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabs();
        setupDepositSection();
        setupWithdrawSection();
    }

    @Override
    public void onDestroyView() {
        stopSePayPolling();
        super.onDestroyView();
    }

    private void initViews(View view) {
        btnTransferClose = view.findViewById(R.id.btnTransferClose);
        btnTabDeposit = view.findViewById(R.id.btnTabDeposit);
        btnTabWithdraw = view.findViewById(R.id.btnTabWithdraw);
        ivTabDeposit = view.findViewById(R.id.ivTabDeposit);
        ivTabWithdraw = view.findViewById(R.id.ivTabWithdraw);
        tvTabDeposit = view.findViewById(R.id.tvTabDeposit);
        tvTabWithdraw = view.findViewById(R.id.tvTabWithdraw);
        layoutTabDeposit = view.findViewById(R.id.layoutTabDeposit);
        layoutTabWithdraw = view.findViewById(R.id.layoutTabWithdraw);

        tilDepositAmount = view.findViewById(R.id.tilDepositAmount);
        etDepositAmount = view.findViewById(R.id.etDepositAmount);
        btnQuick20k = view.findViewById(R.id.btnQuick20k);
        btnQuick50k = view.findViewById(R.id.btnQuick50k);
        btnQuick100k = view.findViewById(R.id.btnQuick100k);
        btnQuick200k = view.findViewById(R.id.btnQuick200k);
        btnGenerateQr = view.findViewById(R.id.btnGenerateQr);
        pbDepositLoading = view.findViewById(R.id.pbDepositLoading);
        layoutQrSection = view.findViewById(R.id.layoutQrSection);
        ivQrCode = view.findViewById(R.id.ivQrCode);

        tvAvailableBalance = view.findViewById(R.id.tvAvailableBalance);
        tilWithdrawAmount = view.findViewById(R.id.tilWithdrawAmount);
        etWithdrawAmount = view.findViewById(R.id.etWithdrawAmount);
        btnWithdrawQuick50k = view.findViewById(R.id.btnWithdrawQuick50k);
        btnWithdrawQuick100k = view.findViewById(R.id.btnWithdrawQuick100k);
        btnWithdrawQuick200k = view.findViewById(R.id.btnWithdrawQuick200k);
        btnWithdrawQuickAll = view.findViewById(R.id.btnWithdrawQuickAll);
        tilWithdrawBank = view.findViewById(R.id.tilWithdrawBank);
        etWithdrawBank = view.findViewById(R.id.etWithdrawBank);
        tilWithdrawAccountNumber = view.findViewById(R.id.tilWithdrawAccountNumber);
        etWithdrawAccountNumber = view.findViewById(R.id.etWithdrawAccountNumber);
        tilWithdrawAccountHolder = view.findViewById(R.id.tilWithdrawAccountHolder);
        etWithdrawAccountHolder = view.findViewById(R.id.etWithdrawAccountHolder);
        pbWithdrawLoading = view.findViewById(R.id.pbWithdrawLoading);
        btnConfirmWithdraw = view.findViewById(R.id.btnConfirmWithdraw);

        btnTransferClose.setOnClickListener(v -> dismiss());
    }

    private void setupTabs() {
        btnTabDeposit.setOnClickListener(v -> selectTab(true));
        btnTabWithdraw.setOnClickListener(v -> selectTab(false));
        selectTab(true);
    }

    private void selectTab(boolean isDeposit) {
        int secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int whiteColor = ContextCompat.getColor(requireContext(), R.color.white);

        btnTabDeposit.setBackgroundResource(isDeposit ? R.drawable.bg_wallet_tab_active : R.drawable.bg_wallet_tab_inactive);
        tvTabDeposit.setTextColor(isDeposit ? whiteColor : secondaryColor);
        ivTabDeposit.setImageTintList(ColorStateList.valueOf(isDeposit ? whiteColor : secondaryColor));

        btnTabWithdraw.setBackgroundResource(!isDeposit ? R.drawable.bg_wallet_tab_active : R.drawable.bg_wallet_tab_inactive);
        tvTabWithdraw.setTextColor(!isDeposit ? whiteColor : secondaryColor);
        ivTabWithdraw.setImageTintList(ColorStateList.valueOf(!isDeposit ? whiteColor : secondaryColor));

        layoutTabDeposit.setVisibility(isDeposit ? View.VISIBLE : View.GONE);
        layoutTabWithdraw.setVisibility(isDeposit ? View.GONE : View.VISIBLE);

        if (!isDeposit) {
            stopSePayPolling();
            loadWalletBalanceForWithdraw();
        }
    }

    private void setupDepositSection() {
        btnQuick20k.setOnClickListener(v -> { etDepositAmount.setText("20000"); tilDepositAmount.setError(null); });
        btnQuick50k.setOnClickListener(v -> { etDepositAmount.setText("50000"); tilDepositAmount.setError(null); });
        btnQuick100k.setOnClickListener(v -> { etDepositAmount.setText("100000"); tilDepositAmount.setError(null); });
        btnQuick200k.setOnClickListener(v -> { etDepositAmount.setText("200000"); tilDepositAmount.setError(null); });

        etDepositAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilDepositAmount.getError() != null) tilDepositAmount.setError(null);
                stopSePayPolling();
                layoutQrSection.setVisibility(View.GONE);
                pendingDepositAmount = 0.0;
                depositQrGeneratedAt = null;
                btnGenerateQr.setText("Tạo mã VietQR");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnGenerateQr.setOnClickListener(v -> generateDepositQr());
    }

    private void generateDepositQr() {
        String amountStr = etDepositAmount.getText() != null ? etDepositAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilDepositAmount.setError("Vui lòng nhập số tiền");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            tilDepositAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount < 1000) {
            tilDepositAmount.setError("Số tiền tối thiểu là 1.000 ₫");
            return;
        }

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Không xác định được tài khoản người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        stopSePayPolling();
        pendingDepositAmount = amount;
        depositQrGeneratedAt = Instant.now().toString();

        pbDepositLoading.setVisibility(View.VISIBLE);
        btnGenerateQr.setEnabled(false);

        String qrUrl = PaymentHelper.generateWalletQrUrl(userId, amount);

        Executors.newSingleThreadExecutor().execute(() -> {
            Bitmap downloadedBitmap = null;
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .dns(SafeDns.getInstance())
                        .build();
                Request request = new Request.Builder().url(qrUrl).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        downloadedBitmap = BitmapFactory.decodeStream(response.body().byteStream());
                    }
                }
            } catch (Exception ignored) {}

            final Bitmap qrBitmap = downloadedBitmap;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    pbDepositLoading.setVisibility(View.GONE);
                    btnGenerateQr.setEnabled(true);

                    if (qrBitmap == null) {
                        layoutQrSection.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Không tải được mã VietQR. Vui lòng kiểm tra kết nối mạng.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ivQrCode.setImageBitmap(qrBitmap);
                    ivQrCode.setBackgroundColor(Color.WHITE);
                    ivQrCode.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    layoutQrSection.setVisibility(View.VISIBLE);
                    btnGenerateQr.setText("Tạo lại mã VietQR");
                    startSePayPolling(userId, amount, depositQrGeneratedAt);
                });
            }
        });
    }

    private void startSePayPolling(String userId, double expectedAmount, String sinceIsoTime) {
        stopSePayPolling();
        isPollingSePay = true;
        sepayPollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPollingSePay || !isAdded() || getContext() == null) return;
                UserSessionManager session = UserSessionManager.getInstance(requireContext());
                transactionRepository.setAccessToken(session.getAccessToken());
                transactionRepository.findConfirmedWalletPayment(userId, expectedAmount, sinceIsoTime,
                        new TransactionRepository.DataCallback<Transaction>() {
                            @Override
                            public void onSuccess(Transaction transaction) {
                                if (!isPollingSePay || !isAdded()) return;
                                requireActivity().runOnUiThread(() -> {
                                    if (!isPollingSePay || !isAdded()) return;
                                    if (transaction != null) {
                                        stopSePayPolling();
                                        RoomRepository.getInstance().onRemoteTransactionReceived(transaction);
                                        RoomRepository.getInstance().loadUserProfile();
                                        if (listener != null) listener.onTransferComplete();
                                        Toast.makeText(requireContext(), "Giao dịch thành công! Đã cập nhật số dư ví Koin.", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    } else {
                                        sepayPollHandler.postDelayed(sepayPollRunnable, 3000);
                                    }
                                });
                            }

                            @Override
                            public void onError(String message) {
                                if (!isPollingSePay || !isAdded()) return;
                                sepayPollHandler.postDelayed(sepayPollRunnable, 3000);
                            }
                        });
            }
        };
        sepayPollHandler.postDelayed(sepayPollRunnable, 3000);
    }

    private void stopSePayPolling() {
        isPollingSePay = false;
        if (sepayPollRunnable != null) {
            sepayPollHandler.removeCallbacks(sepayPollRunnable);
            sepayPollRunnable = null;
        }
    }

    private void setupWithdrawSection() {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String fullName = session.getCurrentUserName();
        if (fullName != null && !fullName.isEmpty()) {
            etWithdrawAccountHolder.setText(fullName.toUpperCase(Locale.ROOT));
        }

        btnWithdrawQuick50k.setOnClickListener(v -> etWithdrawAmount.setText("50000"));
        btnWithdrawQuick100k.setOnClickListener(v -> etWithdrawAmount.setText("100000"));
        btnWithdrawQuick200k.setOnClickListener(v -> etWithdrawAmount.setText("200000"));
        btnWithdrawQuickAll.setOnClickListener(v -> {
            long wholeAmount = (long) Math.floor(currentWalletBalance);
            if (wholeAmount > 0) {
                etWithdrawAmount.setText(String.valueOf(wholeAmount));
            }
        });

        btnConfirmWithdraw.setOnClickListener(v -> executeWithdrawal());
    }

    private void loadWalletBalanceForWithdraw() {
        currentWalletBalance = RoomRepository.getInstance().getUserWalletBalance();
        updateWithdrawBalanceDisplay(currentWalletBalance);

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        if (session.isLoggedIn()) {
            RoomRepository.getInstance().loadWalletBalance(session.getUserId(), new RoomRepository.DataCallback<Double>() {
                @Override
                public void onSuccess(Double balance) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            currentWalletBalance = balance != null ? balance : 0.0;
                            updateWithdrawBalanceDisplay(currentWalletBalance);
                        });
                    }
                }

                @Override
                public void onError(String message) {}
            });
        }
    }

    private void updateWithdrawBalanceDisplay(double balance) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        tvAvailableBalance.setText("Số dư ví khả dụng: " + df.format(balance) + " ₫");
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

        if (amount > currentWalletBalance) {
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

        setWithdrawLoading(true);

        transactionRepository.setAccessToken(session.getAccessToken());
        transactionRepository.withdrawWallet(userId, amount, bankName, accountNumber, accountHolder,
                new TransactionRepository.DataCallback<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject value) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            setWithdrawLoading(false);
                            Toast.makeText(requireContext(), "Đã rút tiền từ ví về ngân hàng thành công!", Toast.LENGTH_SHORT).show();
                            RoomRepository.getInstance().loadUserProfile();
                            if (listener != null) listener.onTransferComplete();
                            dismiss();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            setWithdrawLoading(false);
                            Toast.makeText(requireContext(), "Rút tiền thất bại: " + message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void setWithdrawLoading(boolean loading) {
        pbWithdrawLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnConfirmWithdraw.setEnabled(!loading);
    }
}
