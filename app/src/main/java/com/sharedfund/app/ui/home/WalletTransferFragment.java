package com.sharedfund.app.ui.home;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import com.sharedfund.app.util.CopyToast;
import com.sharedfund.app.util.ErrorMessageHelper;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WalletTransferFragment extends androidx.fragment.app.Fragment {


    private View layoutWalletTransferForm;
    private TextView tvWalletTransferScreenTitle;
    private View btnTabDeposit;
    private View btnTabWithdraw;
    private ImageView ivTabDeposit;
    private ImageView ivTabWithdraw;
    private TextView tvTabDeposit;
    private TextView tvTabWithdraw;
    private LinearLayout layoutTabDeposit;
    private LinearLayout layoutTabWithdraw;

    private TextInputLayout tilDepositAmount;
    private TextInputEditText etDepositAmount;
    private ProgressBar pbDepositLoading;
    private LinearLayout layoutQrSection;
    private ImageView ivQrCode;
    private TextView tvQrBankName;
    private TextView tvQrRecipientName;
    private TextView tvQrAccountNumber;
    private TextView tvQrAmount;
    private View btnCopyQrAccountNumber;
    private View layoutQrProcessingNotice;
    private View layoutQrSuccessNotice;
    private View layoutQrInfoCard;
    private TextView tvQrSuccessAmount;
    private MaterialButton btnQrSuccessDone;

    private View layoutWalletSuccessNotice;
    private TextView tvWalletSuccessTitle;
    private TextView tvWalletSuccessAmount;
    private TextView tvWalletSuccessTxId;
    private View btnWalletSuccessCopyTxId;
    private TextView tvWalletSuccessTime;
    private TextView tvWalletSuccessType;
    private TextView tvWalletSuccessMethod;
    private TextView tvWalletSuccessBalance;
    private MaterialButton btnWalletSuccessDone;

    private double pendingDepositAmount = 0.0;
    private String depositQrGeneratedAt;
    private final Handler sepayPollHandler = new Handler(Looper.getMainLooper());
    private Runnable sepayPollRunnable;
    private boolean isPollingSePay = false;

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

    private View layoutBottomWalletDock;
    private View layoutQuickAmountBarWallet;
    private MaterialButton btnQuickWallet50k;
    private MaterialButton btnQuickWallet100k;
    private MaterialButton btnQuickWallet500k;
    private MaterialButton btnQuickWalletAll;
    private MaterialButton btnWalletSubmitUnified;
    private boolean isImeOpen = false;

    private double currentWalletBalance = 0.0;
    private final TransactionRepository transactionRepository = new TransactionRepository();

    public static WalletTransferFragment newInstance() {
        return new WalletTransferFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabs();
        setupDepositSection();
        setupWithdrawSection();
        setupQuickAmountButtons();
        setupKeyboardAdaptiveBehavior(view);
    }

    @Override
    public void onDestroyView() {
        stopSePayPolling();
        super.onDestroyView();
    }

    private void initViews(View view) {
        ImageButton btnBack = view.findViewById(R.id.btnWalletTransferBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        layoutWalletTransferForm = view.findViewById(R.id.layoutWalletTransferForm);
        tvWalletTransferScreenTitle = view.findViewById(R.id.tvWalletTransferScreenTitle);
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
        pbDepositLoading = view.findViewById(R.id.pbDepositLoading);
        layoutQrSection = view.findViewById(R.id.layoutQrSection);
        layoutQrProcessingNotice = view.findViewById(R.id.layoutQrProcessingNotice);
        layoutQrSuccessNotice = view.findViewById(R.id.layoutQrSuccessNotice);
        layoutQrInfoCard = view.findViewById(R.id.layoutQrInfoCard);
        tvQrSuccessAmount = view.findViewById(R.id.tvQrSuccessAmount);
        btnQrSuccessDone = view.findViewById(R.id.btnQrSuccessDone);

        ivQrCode = view.findViewById(R.id.ivQrCode);
        tvQrBankName = view.findViewById(R.id.tvQrBankName);
        tvQrRecipientName = view.findViewById(R.id.tvQrRecipientName);
        tvQrAccountNumber = view.findViewById(R.id.tvQrAccountNumber);
        tvQrAmount = view.findViewById(R.id.tvQrAmount);
        btnCopyQrAccountNumber = view.findViewById(R.id.btnCopyQrAccountNumber);

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

        layoutWalletSuccessNotice = view.findViewById(R.id.layoutWalletSuccessNotice);
        tvWalletSuccessTitle = view.findViewById(R.id.tvWalletSuccessTitle);
        tvWalletSuccessAmount = view.findViewById(R.id.tvWalletSuccessAmount);
        tvWalletSuccessTxId = view.findViewById(R.id.tvWalletSuccessTxId);
        btnWalletSuccessCopyTxId = view.findViewById(R.id.btnWalletSuccessCopyTxId);
        tvWalletSuccessTime = view.findViewById(R.id.tvWalletSuccessTime);
        tvWalletSuccessType = view.findViewById(R.id.tvWalletSuccessType);
        tvWalletSuccessMethod = view.findViewById(R.id.tvWalletSuccessMethod);
        tvWalletSuccessBalance = view.findViewById(R.id.tvWalletSuccessBalance);
        btnWalletSuccessDone = view.findViewById(R.id.btnWalletSuccessDone);

        if (btnWalletSuccessDone != null) {
            btnWalletSuccessDone.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }
        if (btnWalletSuccessCopyTxId != null) {
            btnWalletSuccessCopyTxId.setOnClickListener(v -> {
                if (tvWalletSuccessTxId != null) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("TxId", tvWalletSuccessTxId.getText().toString());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        android.widget.Toast.makeText(requireContext(), "Đã sao chép mã giao dịch", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        layoutBottomWalletDock = view.findViewById(R.id.layoutBottomWalletDock);
        layoutQuickAmountBarWallet = view.findViewById(R.id.layoutQuickAmountBarWallet);
        btnQuickWallet50k = view.findViewById(R.id.btnQuickWallet50k);
        btnQuickWallet100k = view.findViewById(R.id.btnQuickWallet100k);
        btnQuickWallet500k = view.findViewById(R.id.btnQuickWallet500k);
        btnQuickWalletAll = view.findViewById(R.id.btnQuickWalletAll);
        btnWalletSubmitUnified = view.findViewById(R.id.btnWalletSubmitUnified);

        btnWalletSubmitUnified.setOnClickListener(v -> {
            if (layoutTabDeposit.getVisibility() == View.VISIBLE) {
                generateDepositQr();
            } else {
                performWithdraw();
            }
        });
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

        if (isDeposit) {
            btnWalletSubmitUnified.setText("Tạo mã VietQR");
            btnWalletSubmitUnified.setIconResource(R.drawable.ic_qr_code);
            btnQuickWalletAll.setText("2.000.000");
        } else {
            stopSePayPolling();
            loadWalletBalanceForWithdraw();
            btnWalletSubmitUnified.setText("Xác nhận rút tiền");
            btnWalletSubmitUnified.setIcon(null);
            btnQuickWalletAll.setText("Tất cả ví");
        }
        updateKeyboardAdaptiveDock();
    }

    private void setupQuickAmountButtons() {
        btnQuickWallet50k.setOnClickListener(v -> applyQuickAmount(50000));
        btnQuickWallet100k.setOnClickListener(v -> applyQuickAmount(100000));
        btnQuickWallet500k.setOnClickListener(v -> applyQuickAmount(500000));
        btnQuickWalletAll.setOnClickListener(v -> {
            if (layoutTabDeposit.getVisibility() == View.VISIBLE) {
                applyQuickAmount(2000000);
            } else {
                long wholeAmount = (long) Math.floor(currentWalletBalance);
                String val = wholeAmount > 0 ? String.valueOf(wholeAmount) : "0";
                etWithdrawAmount.setText(val);
                tilWithdrawAmount.setError(null);
                etWithdrawAmount.setSelection(val.length());
            }
        });
    }

    private void applyQuickAmount(long amount) {
        String val = String.valueOf(amount);
        if (layoutTabDeposit.getVisibility() == View.VISIBLE) {
            etDepositAmount.setText(val);
            tilDepositAmount.setError(null);
            etDepositAmount.setSelection(val.length());
        } else {
            etWithdrawAmount.setText(val);
            tilWithdrawAmount.setError(null);
            etWithdrawAmount.setSelection(val.length());
        }
    }

    private void setupDepositSection() {
        if (btnCopyQrAccountNumber != null) {
            btnCopyQrAccountNumber.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Số tài khoản", PaymentHelper.ACCOUNT_NO);
                    clipboard.setPrimaryClip(clip);
                    CopyToast.show(requireContext(), requireView());
                }
            });
        }

        etDepositAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilDepositAmount.getError() != null) tilDepositAmount.setError(null);
                stopSePayPolling();
                layoutQrSection.setVisibility(View.GONE);
                pendingDepositAmount = 0.0;
                depositQrGeneratedAt = null;
                btnWalletSubmitUnified.setText("Tạo mã VietQR");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
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
        String rawUserId = session.getUserId();
        if (rawUserId == null || rawUserId.trim().isEmpty()) {
            rawUserId = RoomRepository.getInstance().getCurrentUserId();
        }
        if (rawUserId == null || rawUserId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Không xác định được tài khoản người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        stopSePayPolling();
        pendingDepositAmount = amount;
        try {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getView() != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
        depositQrGeneratedAt = Instant.now().minusSeconds(120).toString();

        pbDepositLoading.setVisibility(View.VISIBLE);
        btnWalletSubmitUnified.setEnabled(false);

        final String userId = rawUserId;
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
                    btnWalletSubmitUnified.setEnabled(true);

                    if (qrBitmap == null) {
                        layoutQrSection.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Không tải được mã VietQR. Vui lòng kiểm tra kết nối mạng.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ivQrCode.setImageBitmap(qrBitmap);
                    android.graphics.drawable.GradientDrawable qrBg = new android.graphics.drawable.GradientDrawable();
                    qrBg.setColor(Color.WHITE);
                    qrBg.setCornerRadius(16f * getResources().getDisplayMetrics().density);
                    ivQrCode.setBackground(qrBg);
                    int pad = (int) (12 * getResources().getDisplayMetrics().density);
                    ivQrCode.setPadding(pad, pad, pad, pad);
                    ivQrCode.setClipToOutline(true);
                    ivQrCode.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    DecimalFormatSymbols curSymbols = new DecimalFormatSymbols(Locale.US);
                    curSymbols.setGroupingSeparator('.');
                    DecimalFormat curDf = new DecimalFormat("#,##0", curSymbols);

                    if (tvQrBankName != null) tvQrBankName.setText("Koin");
                    if (tvQrRecipientName != null) tvQrRecipientName.setText(PaymentHelper.ACCOUNT_NAME);
                    if (tvQrAccountNumber != null) tvQrAccountNumber.setText(PaymentHelper.ACCOUNT_NO);
                    if (tvQrAmount != null) tvQrAmount.setText(curDf.format(amount) + " ₫");
                    if (layoutQrProcessingNotice != null) layoutQrProcessingNotice.setVisibility(View.GONE);
                    if (layoutQrSuccessNotice != null) layoutQrSuccessNotice.setVisibility(View.GONE);
                    if (layoutQrInfoCard != null) layoutQrInfoCard.setVisibility(View.VISIBLE);
                    if (ivQrCode != null) ivQrCode.setVisibility(View.VISIBLE);

                    layoutQrSection.setVisibility(View.VISIBLE);
                    btnWalletSubmitUnified.setText("Tạo lại mã VietQR");
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
                                        handleDepositSuccess(transaction.getAmount());
                                    } else {
                                        sepayPollHandler.postDelayed(sepayPollRunnable, 2000);
                                    }
                                });
                            }

                            @Override
                            public void onError(String message) {
                                if (!isPollingSePay || !isAdded()) return;
                                sepayPollHandler.postDelayed(sepayPollRunnable, 2000);
                            }
                        });
            }
        };
        sepayPollHandler.postDelayed(sepayPollRunnable, 2000);
    }

    private void handleDepositSuccess(double amount) {
        stopSePayPolling();
        if (layoutQrInfoCard != null) layoutQrInfoCard.setVisibility(View.GONE);
        if (ivQrCode != null) ivQrCode.setVisibility(View.GONE);
        if (layoutBottomWalletDock != null) layoutBottomWalletDock.setVisibility(View.GONE);

        if (layoutQrProcessingNotice != null) {
            layoutQrProcessingNotice.setVisibility(View.VISIBLE);
        }

        sepayPollHandler.postDelayed(() -> {
            if (!isAdded()) return;
            showFullSuccessNotice(true, amount, null);
        }, 1200);
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
    }

    private void loadWalletBalanceForWithdraw() {
        Double balance = RoomRepository.getInstance().getUserWalletLiveData().getValue();
        currentWalletBalance = balance != null ? balance : 0.0;
        DecimalFormatSymbols curSymbols = new DecimalFormatSymbols(Locale.US);
        curSymbols.setGroupingSeparator('.');
        DecimalFormat curDf = new DecimalFormat("#,##0", curSymbols);
        tvAvailableBalance.setText("Số dư ví khả dụng: " + curDf.format(currentWalletBalance) + " ₫");
    }

    private void performWithdraw() {
        String amountStr = etWithdrawAmount.getText() != null ? etWithdrawAmount.getText().toString().trim() : "";
        String bankName = etWithdrawBank.getText() != null ? etWithdrawBank.getText().toString().trim() : "";
        String accountNumber = etWithdrawAccountNumber.getText() != null ? etWithdrawAccountNumber.getText().toString().trim() : "";
        String accountHolder = etWithdrawAccountHolder.getText() != null ? etWithdrawAccountHolder.getText().toString().trim() : "";

        if (amountStr.isEmpty()) {
            tilWithdrawAmount.setError("Vui lòng nhập số tiền cần rút");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            tilWithdrawAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount < 1000) {
            tilWithdrawAmount.setError("Số tiền rút tối thiểu là 1.000 ₫");
            return;
        }

        if (amount > currentWalletBalance) {
            tilWithdrawAmount.setError("Số dư ví không đủ để rút số tiền này");
            return;
        }
        tilWithdrawAmount.setError(null);

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

        showWithdrawVerificationDialog(amount, bankName, accountNumber, accountHolder);
    }

    private void showWithdrawVerificationDialog(double amount, String bankName, String accountNumber, String accountHolder) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_transaction_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator('.');
        DecimalFormat curDf = new DecimalFormat("#,##0", sym);

        TextView tvConfirmTitle = dialog.findViewById(R.id.tvConfirmTitle);
        TextView tvConfirmAmount = dialog.findViewById(R.id.tvConfirmAmount);
        TextView tvConfirmType = dialog.findViewById(R.id.tvConfirmType);
        TextView tvConfirmNote = dialog.findViewById(R.id.tvConfirmNote);
        MaterialButton btnDialogCancel = dialog.findViewById(R.id.btnDialogCancel);
        MaterialButton btnDialogConfirm = dialog.findViewById(R.id.btnDialogConfirm);

        if (tvConfirmTitle != null) tvConfirmTitle.setText("Xác nhận rút tiền về ngân hàng");
        if (tvConfirmAmount != null) tvConfirmAmount.setText(curDf.format(amount) + " VND");
        if (tvConfirmType != null) tvConfirmType.setText("Rút tiền về tài khoản ngân hàng");
        if (tvConfirmNote != null) tvConfirmNote.setText("Rút tiền từ ví Koin về ngân hàng");

        if (btnDialogCancel != null) btnDialogCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnDialogConfirm != null) {
            btnDialogConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                executeWithdraw(amount, bankName, accountNumber, accountHolder);
            });
        }

        dialog.show();
    }

    private void executeWithdraw(double amount, String bankName, String accountNumber, String accountHolder) {
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
                            showFullSuccessNotice(false, amount, null);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            setWithdrawLoading(false);
                            String friendly = ErrorMessageHelper.getFriendlyMessage(message);
                            tilWithdrawAmount.setError(friendly);
                            Toast.makeText(requireContext(), friendly, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void setWithdrawLoading(boolean loading) {
        pbWithdrawLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnWalletSubmitUnified.setEnabled(!loading);
    }

    private void setupKeyboardAdaptiveBehavior(View rootView) {
        View.OnFocusChangeListener amountFocusListener = (v, hasFocus) -> updateKeyboardAdaptiveDock();
        etDepositAmount.setOnFocusChangeListener(amountFocusListener);
        etWithdrawAmount.setOnFocusChangeListener(amountFocusListener);

        View.OnFocusChangeListener otherFocusListener = (v, hasFocus) -> updateKeyboardAdaptiveDock();
        etWithdrawBank.setOnFocusChangeListener(otherFocusListener);
        etWithdrawAccountNumber.setOnFocusChangeListener(otherFocusListener);
        etWithdrawAccountHolder.setOnFocusChangeListener(otherFocusListener);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            androidx.core.graphics.Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int imeBottom = imeInsets.bottom;
            isImeOpen = imeVisible || imeBottom > 0;
            int bottomPadding = isImeOpen ? imeBottom : sysBars.bottom;
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, bottomPadding);

            updateKeyboardAdaptiveDock();
            return insets;
        });
    }

    private void updateKeyboardAdaptiveDock() {
        boolean isAmountFocused = (etDepositAmount != null && etDepositAmount.hasFocus())
                || (etWithdrawAmount != null && etWithdrawAmount.hasFocus());
        if (layoutQuickAmountBarWallet != null) {
            layoutQuickAmountBarWallet.setVisibility(isImeOpen && isAmountFocused ? View.VISIBLE : View.GONE);
        }
    }
    private void showFullSuccessNotice(boolean isDeposit, double amount, String txId) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;
            if (layoutWalletTransferForm != null) layoutWalletTransferForm.setVisibility(View.GONE);
            if (layoutBottomWalletDock != null) layoutBottomWalletDock.setVisibility(View.GONE);
            if (layoutQuickAmountBarWallet != null) layoutQuickAmountBarWallet.setVisibility(View.GONE);
            if (layoutQrProcessingNotice != null) layoutQrProcessingNotice.setVisibility(View.GONE);
            if (layoutQrSection != null) layoutQrSection.setVisibility(View.GONE);
            if (tvWalletTransferScreenTitle != null) {
                tvWalletTransferScreenTitle.setText("Kết quả giao dịch");
            }
            if (layoutWalletSuccessNotice != null) {
                layoutWalletSuccessNotice.setVisibility(View.VISIBLE);
            }

            DecimalFormatSymbols curSymbols = new DecimalFormatSymbols(Locale.US);
            curSymbols.setGroupingSeparator('.');
            DecimalFormat curDf = new DecimalFormat("#,##0", curSymbols);

            String formattedAmount = (isDeposit ? "+" : "-") + curDf.format(amount) + " ₫";
            if (tvWalletSuccessAmount != null) {
                tvWalletSuccessAmount.setText(formattedAmount);
            }
            if (tvWalletSuccessTitle != null) {
                tvWalletSuccessTitle.setText(isDeposit ? "Nạp tiền thành công!" : "Rút tiền thành công!");
            }

            String finalTxId = (txId != null && !txId.isEmpty()) ? txId : ("#SF-" + String.format(Locale.US, "%08X", (int) (System.currentTimeMillis() & 0xFFFFFFFFL)));
            if (tvWalletSuccessTxId != null) {
                tvWalletSuccessTxId.setText(finalTxId);
            }
            if (btnWalletSuccessCopyTxId != null) {
                btnWalletSuccessCopyTxId.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Transaction ID", finalTxId);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        CopyToast.show(requireContext(), requireView());
                    }
                });
            }

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault());
            if (tvWalletSuccessTime != null) {
                tvWalletSuccessTime.setText(timeFormat.format(new Date()));
            }

            if (tvWalletSuccessType != null) {
                tvWalletSuccessType.setText(isDeposit ? "Nạp tiền vào Ví Koin" : "Rút tiền về tài khoản ngân hàng");
            }

            if (tvWalletSuccessMethod != null) {
                tvWalletSuccessMethod.setText(isDeposit ? "Chuyển khoản VietQR" : "Tài khoản ngân hàng liên kết");
            }

            double currentBal = RoomRepository.getInstance().getUserWalletBalance();
            if (tvWalletSuccessBalance != null) {
                tvWalletSuccessBalance.setText(curDf.format(currentBal) + " ₫");
            }

            RoomRepository.getInstance().loadUserProfile();
        });
    }

}
