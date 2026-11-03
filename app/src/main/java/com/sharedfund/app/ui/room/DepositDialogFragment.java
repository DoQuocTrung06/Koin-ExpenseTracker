package com.sharedfund.app.ui.room;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.R;
import com.sharedfund.app.data.PaymentHelper;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.SafeDns;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.util.CopyToast;
import com.sharedfund.app.util.ErrorMessageHelper;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DepositDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "room_id";

    private String roomId;
    private double pendingAmount;
    private String qrGeneratedAt;

    private TextView tvDepositTitle;
    private TextInputLayout tilDepositAmount;
    private TextInputEditText etDepositAmount;
    private MaterialButton btnQuick50k;
    private MaterialButton btnQuick100k;
    private MaterialButton btnQuick200k;
    private MaterialButton btnQuick500k;
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
    private TextView tvQrSuccessTitle;
    private TextView tvQrSuccessAmount;
    private MaterialButton btnQrSuccessDone;

    private MaterialButton btnCancelDeposit;
    private MaterialButton btnGenerateQr;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private boolean isPolling = false;

    private final TransactionRepository transactionRepository = new TransactionRepository();

    public static DepositDialogFragment newInstance(String roomId) {
        DepositDialogFragment fragment = new DepositDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    private boolean isWalletDeposit() {
        return roomId == null || roomId.trim().isEmpty();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID, "");
        }
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_deposit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupPresetButtons();
        setupClipboardCopy();
        setupTextWatcher();
        setupClickListeners();
    }

    @Override
    public void onDestroyView() {
        stopPaymentPolling();
        super.onDestroyView();
    }

    private void initViews(View view) {
        tvDepositTitle = view.findViewById(R.id.tvDepositTitle);
        tilDepositAmount = view.findViewById(R.id.tilDepositAmount);
        etDepositAmount = view.findViewById(R.id.etDepositAmount);
        btnQuick50k = view.findViewById(R.id.btnQuick50k);
        btnQuick100k = view.findViewById(R.id.btnQuick100k);
        btnQuick200k = view.findViewById(R.id.btnQuick200k);
        btnQuick500k = view.findViewById(R.id.btnQuick500k);
        pbDepositLoading = view.findViewById(R.id.pbDepositLoading);
        layoutQrSection = view.findViewById(R.id.layoutQrSection);
        ivQrCode = view.findViewById(R.id.ivQrCode);
        tvQrBankName = view.findViewById(R.id.tvQrBankName);
        tvQrRecipientName = view.findViewById(R.id.tvQrRecipientName);
        tvQrAccountNumber = view.findViewById(R.id.tvQrAccountNumber);
        tvQrAmount = view.findViewById(R.id.tvQrAmount);
        btnCopyQrAccountNumber = view.findViewById(R.id.btnCopyQrAccountNumber);
        layoutQrProcessingNotice = view.findViewById(R.id.layoutQrProcessingNotice);
        layoutQrSuccessNotice = view.findViewById(R.id.layoutQrSuccessNotice);
        layoutQrInfoCard = view.findViewById(R.id.layoutQrInfoCard);
        tvQrSuccessTitle = view.findViewById(R.id.tvQrSuccessTitle);
        tvQrSuccessAmount = view.findViewById(R.id.tvQrSuccessAmount);
        btnQrSuccessDone = view.findViewById(R.id.btnQrSuccessDone);

        btnCancelDeposit = view.findViewById(R.id.btnCancelDeposit);
        btnGenerateQr = view.findViewById(R.id.btnGenerateQr);

        if (isWalletDeposit()) {
            tvDepositTitle.setText("Nạp tiền vào Ví Koin");
        } else {
            tvDepositTitle.setText("Đóng góp vào quỹ nhóm");
        }
    }

    private void setupPresetButtons() {
        btnQuick50k.setOnClickListener(v -> { etDepositAmount.setText("50000"); tilDepositAmount.setError(null); });
        btnQuick100k.setOnClickListener(v -> { etDepositAmount.setText("100000"); tilDepositAmount.setError(null); });
        btnQuick200k.setOnClickListener(v -> { etDepositAmount.setText("200000"); tilDepositAmount.setError(null); });
        btnQuick500k.setOnClickListener(v -> { etDepositAmount.setText("500000"); tilDepositAmount.setError(null); });
    }

    private void setupClipboardCopy() {
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
    }

    private void setupTextWatcher() {
        etDepositAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilDepositAmount.getError() != null) tilDepositAmount.setError(null);
                stopPaymentPolling();
                layoutQrSection.setVisibility(View.GONE);
                pendingAmount = 0;
                qrGeneratedAt = null;
                btnGenerateQr.setText("Tạo mã VietQR");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        btnCancelDeposit.setOnClickListener(v -> dismiss());
        btnGenerateQr.setOnClickListener(v -> generateQrCode());
    }

    private void generateQrCode() {
        String amountStr = etDepositAmount.getText() != null
                ? etDepositAmount.getText().toString().trim() : "";

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

        stopPaymentPolling();
        pendingAmount = amount;
        qrGeneratedAt = Instant.now().minusSeconds(120).toString();

        pbDepositLoading.setVisibility(View.VISIBLE);
        btnGenerateQr.setEnabled(false);

        String qrUrl;

        if (isWalletDeposit()) {
            String userId = UserSessionManager.getInstance(requireContext()).getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                userId = RoomRepository.getInstance().getCurrentUserId();
            }
            if (userId == null || userId.trim().isEmpty()) {
                pbDepositLoading.setVisibility(View.GONE);
                btnGenerateQr.setEnabled(true);
                Toast.makeText(requireContext(), "Không xác định được tài khoản người dùng.", Toast.LENGTH_LONG).show();
                return;
            }
            qrUrl = PaymentHelper.generateWalletQrUrl(userId, amount);
        } else {
            Room room = RoomRepository.getInstance().getRoomSync(roomId);
            if (room == null || room.getJoinCode() == null) {
                pbDepositLoading.setVisibility(View.GONE);
                btnGenerateQr.setEnabled(true);
                Toast.makeText(requireContext(), "Không tìm thấy mã quỹ.", Toast.LENGTH_LONG).show();
                return;
            }
            qrUrl = PaymentHelper.generateVietQrUrl(room.getJoinCode(), amount);
        }

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
            } catch (Exception ignored) {
            }

            final Bitmap qrBitmap = downloadedBitmap;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    pbDepositLoading.setVisibility(View.GONE);
                    btnGenerateQr.setEnabled(true);

                    if (qrBitmap == null) {
                        layoutQrSection.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Không tải được mã QR. Vui lòng kiểm tra kết nối mạng.", Toast.LENGTH_LONG).show();
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
                    btnGenerateQr.setText("Tạo lại mã VietQR");
                    startPaymentPolling(amount, qrGeneratedAt);
                });
            }
        });
    }

    private void startPaymentPolling(double expectedAmount, String sinceIsoTime) {
        stopPaymentPolling();
        isPolling = true;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling || !isAdded() || getContext() == null) return;
                UserSessionManager session = UserSessionManager.getInstance(requireContext());
                transactionRepository.setAccessToken(session.getAccessToken());
                TransactionRepository.DataCallback<Transaction> callback = new TransactionRepository.DataCallback<Transaction>() {
                    @Override
                    public void onSuccess(Transaction transaction) {
                        if (!isPolling || !isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isPolling || !isAdded()) return;
                            if (transaction != null) {
                                handlePaymentSuccess(transaction.getAmount());
                            } else {
                                pollHandler.postDelayed(pollRunnable, 2000);
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isPolling || !isAdded()) return;
                        pollHandler.postDelayed(pollRunnable, 2000);
                    }
                };
                if (isWalletDeposit()) {
                    String userId = session.getUserId();
                    if (userId == null || userId.isEmpty()) userId = RoomRepository.getInstance().getCurrentUserId();
                    transactionRepository.findConfirmedWalletPayment(userId, expectedAmount, sinceIsoTime, callback);
                } else {
                    transactionRepository.findConfirmedPayment(roomId, expectedAmount, sinceIsoTime, callback);
                }
            }
        };
        pollHandler.postDelayed(pollRunnable, 2000);
    }

    private void handlePaymentSuccess(double amount) {
        stopPaymentPolling();
        if (layoutQrInfoCard != null) layoutQrInfoCard.setVisibility(View.GONE);
        if (ivQrCode != null) ivQrCode.setVisibility(View.GONE);
        if (btnGenerateQr != null) btnGenerateQr.setVisibility(View.GONE);
        if (btnCancelDeposit != null) btnCancelDeposit.setVisibility(View.GONE);

        if (layoutQrProcessingNotice != null) {
            layoutQrProcessingNotice.setVisibility(View.VISIBLE);
        }

        pollHandler.postDelayed(() -> {
            if (!isAdded()) return;
            if (layoutQrProcessingNotice != null) {
                layoutQrProcessingNotice.setVisibility(View.GONE);
            }

            DecimalFormatSymbols curSymbols = new DecimalFormatSymbols(Locale.US);
            curSymbols.setGroupingSeparator('.');
            DecimalFormat curDf = new DecimalFormat("#,##0", curSymbols);

            if (tvQrSuccessTitle != null) {
                tvQrSuccessTitle.setText(isWalletDeposit() ? "Nạp ví thành công!" : "Đóng quỹ thành công!");
            }
            if (tvQrSuccessAmount != null) {
                tvQrSuccessAmount.setText("+" + curDf.format(amount) + " ₫");
            }
            if (layoutQrSuccessNotice != null) {
                layoutQrSuccessNotice.setVisibility(View.VISIBLE);
            }

            if (isWalletDeposit()) {
                RoomRepository.getInstance().loadUserProfile();
            } else {
                RoomRepository.getInstance().refreshRoomTransactions(roomId, new RoomRepository.DataCallback<List<Transaction>>() {
                    @Override public void onSuccess(List<Transaction> value) {}
                    @Override public void onError(String message) {}
                });
            }


            if (btnQrSuccessDone != null) {
                btnQrSuccessDone.setOnClickListener(v -> dismiss());
            }
            pollHandler.postDelayed(() -> {
                if (isAdded()) dismiss();
            }, 3500);
        }, 1200);
    }

    private void stopPaymentPolling() {
        isPolling = false;
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }
}
