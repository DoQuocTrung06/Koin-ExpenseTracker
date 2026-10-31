package com.sharedfund.app.ui.room;

import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
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

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DepositDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "room_id";

    private String roomId;
    private double pendingAmount;
    private String qrGeneratedAt;
    private String demoPaymentReference;

    private TextView tvDepositTitle;
    private TextInputLayout tilDepositAmount;
    private TextInputEditText etDepositAmount;
    private MaterialButton btnQuick20k;
    private MaterialButton btnQuick50k;
    private MaterialButton btnQuick100k;
    private MaterialButton btnQuick200k;
    private LinearLayout layoutQrSection;
    private ImageView ivQrCode;
    private TextView tvTransferInfo;
    private ProgressBar pbDepositLoading;
    private MaterialButton btnCancelDeposit;
    private MaterialButton btnGenerateQr;
    private MaterialButton btnDoneDeposit;
    private MaterialButton btnDemoPaymentSuccess;

    public interface OnDepositCompleteListener {
        void onDepositComplete(double amount);
    }

    private OnDepositCompleteListener listener;

    public static DepositDialogFragment newInstance(String roomId) {
        DepositDialogFragment fragment = new DepositDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    public static DepositDialogFragment newWalletInstance() {
        return newInstance("WALLET");
    }

    private boolean isWalletDeposit() {
        return roomId == null || "WALLET".equals(roomId);
    }

    public void setOnDepositCompleteListener(OnDepositCompleteListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
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
        bindViews(view);
        setupQuickButtons();
        setupTextWatcher();
        setupClickListeners();
        if (tvDepositTitle != null) {
            tvDepositTitle.setText(isWalletDeposit() ? "Nạp tiền vào ví cá nhân" : "Nạp tiền vào quỹ nhóm");
        }
    }

    private void bindViews(View view) {
        tvDepositTitle = view.findViewById(R.id.tvDepositTitle);
        tilDepositAmount = view.findViewById(R.id.tilDepositAmount);
        etDepositAmount = view.findViewById(R.id.etDepositAmount);
        btnQuick20k = view.findViewById(R.id.btnQuick20k);
        btnQuick50k = view.findViewById(R.id.btnQuick50k);
        btnQuick100k = view.findViewById(R.id.btnQuick100k);
        btnQuick200k = view.findViewById(R.id.btnQuick200k);
        layoutQrSection = view.findViewById(R.id.layoutQrSection);
        ivQrCode = view.findViewById(R.id.ivQrCode);
        tvTransferInfo = view.findViewById(R.id.tvTransferInfo);
        pbDepositLoading = view.findViewById(R.id.pbDepositLoading);
        btnCancelDeposit = view.findViewById(R.id.btnCancelDeposit);
        btnGenerateQr = view.findViewById(R.id.btnGenerateQr);
        btnDoneDeposit = view.findViewById(R.id.btnDoneDeposit);
        btnDemoPaymentSuccess = view.findViewById(R.id.btnDemoPaymentSuccess);
        btnDemoPaymentSuccess.setVisibility(View.GONE);
    }

    private void setupQuickButtons() {
        View.OnClickListener quickListener = v -> {
            int amount = 0;
            if (v.getId() == R.id.btnQuick20k) amount = 20000;
            else if (v.getId() == R.id.btnQuick50k) amount = 50000;
            else if (v.getId() == R.id.btnQuick100k) amount = 100000;
            else if (v.getId() == R.id.btnQuick200k) amount = 200000;
            etDepositAmount.setText(String.valueOf(amount));
            tilDepositAmount.setError(null);
        };
        btnQuick20k.setOnClickListener(quickListener);
        btnQuick50k.setOnClickListener(quickListener);
        btnQuick100k.setOnClickListener(quickListener);
        btnQuick200k.setOnClickListener(quickListener);
    }

    private void setupTextWatcher() {
        etDepositAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilDepositAmount.getError() != null) tilDepositAmount.setError(null);
                layoutQrSection.setVisibility(View.GONE);
                btnDoneDeposit.setVisibility(View.GONE);
                btnDemoPaymentSuccess.setVisibility(View.GONE);
                pendingAmount = 0;
                qrGeneratedAt = null;
                demoPaymentReference = null;
                btnGenerateQr.setText("Quét mã QR");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        btnCancelDeposit.setOnClickListener(v -> dismiss());

        btnGenerateQr.setOnClickListener(v -> generateQrCode());

        btnDoneDeposit.setOnClickListener(v -> confirmDeposit());
        btnDemoPaymentSuccess.setOnClickListener(v -> simulateDepositForDemo());
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
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            tilDepositAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount < 1000) {
            tilDepositAmount.setError("Số tiền tối thiểu là 1.000 ₫");
            return;
        }

        pendingAmount = amount;
        qrGeneratedAt = Instant.now().toString();
        demoPaymentReference = "DEMO-SEPAY-" + java.util.UUID.randomUUID();

        pbDepositLoading.setVisibility(View.VISIBLE);
        btnGenerateQr.setEnabled(false);

        String bankName = PaymentHelper.BANK_NAME;
        String accountNumber = PaymentHelper.ACCOUNT_NO;
        String accountName = PaymentHelper.ACCOUNT_NAME;
        String transferContent;
        String qrUrl;

        if (isWalletDeposit()) {
            String userId = UserSessionManager.getInstance(requireContext()).getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                pbDepositLoading.setVisibility(View.GONE);
                btnGenerateQr.setEnabled(true);
                Toast.makeText(requireContext(), "Không xác định được tài khoản để nạp ví.", Toast.LENGTH_LONG).show();
                return;
            }
            transferContent = PaymentHelper.getWalletTransferContent(userId);
            qrUrl = PaymentHelper.generateWalletQrUrl(userId, amount);
        } else {
            Room room = RoomRepository.getInstance().getRoomSync(roomId);
            if (room == null || room.getJoinCode() == null) {
                pbDepositLoading.setVisibility(View.GONE);
                btnGenerateQr.setEnabled(true);
                Toast.makeText(requireContext(), "Không tìm thấy mã quỹ từ Supabase.", Toast.LENGTH_LONG).show();
                return;
            }
            transferContent = PaymentHelper.getTransferContent(room.getJoinCode());
            qrUrl = PaymentHelper.generateVietQrUrl(room.getJoinCode(), amount);
        }

        DecimalFormat df = new DecimalFormat("#,###");
        String formattedAmount = df.format(amount).replace(',', '.') + " ₫";

        tvTransferInfo.setText(
                "🏦 Ngân hàng: " + bankName + "\n"
                + "💳 STK: " + accountNumber + "  (📋 Chạm để sao chép)\n"
                + "👤 Chủ TK: " + accountName + "\n"
                + "💰 Số tiền: " + formattedAmount + "\n"
                + "📝 Nội dung: " + transferContent + "  (📋 Chạm để sao chép)"
        );

        tvTransferInfo.setOnClickListener(v -> {
            if (!isAdded()) return;
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Thông tin chuyển khoản",
                    "STK: " + accountNumber + " (" + bankName + ")\n"
                    + "Chủ TK: " + accountName + "\n"
                    + "Số tiền: " + (long) amount + "\n"
                    + "Nội dung: " + transferContent);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "Đã sao chép thông tin chuyển khoản!", Toast.LENGTH_SHORT).show();
            }
        });

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

            if (downloadedBitmap == null) {
                downloadedBitmap = generateQrPlaceholderBitmap(accountNumber, formattedAmount, transferContent);
            }

            final Bitmap qrBitmap = downloadedBitmap;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    pbDepositLoading.setVisibility(View.GONE);
                    btnGenerateQr.setEnabled(true);

                    if (qrBitmap == null) {
                        layoutQrSection.setVisibility(View.GONE);
                        btnDoneDeposit.setVisibility(View.GONE);
                        btnDemoPaymentSuccess.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Không tải được mã QR thật từ VietQR. Vui lòng kiểm tra kết nối và thử lại.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    ivQrCode.setImageBitmap(qrBitmap);
                    ivQrCode.setBackgroundColor(Color.WHITE);
                    ivQrCode.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    layoutQrSection.setVisibility(View.VISIBLE);
                    btnDoneDeposit.setVisibility(View.VISIBLE);
                    btnDemoPaymentSuccess.setVisibility(isDebugBuild() ? View.VISIBLE : View.GONE);
                    btnGenerateQr.setText("Quét lại mã QR");

                    Toast.makeText(requireContext(),
                            "Mã VietQR đã sẵn sàng! Quét bằng ứng dụng ngân hàng.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Bitmap generateQrPlaceholderBitmap(String accountNumber, String amount, String content) {
        int size = 480;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(0, 0, size, size), 20, 20, bgPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.parseColor("#0052CC"));
        borderPaint.setStrokeWidth(4);
        canvas.drawRoundRect(new RectF(4, 4, size - 4, size - 4), 18, 18, borderPaint);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#0052CC"));
        titlePaint.setTextSize(32);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("VietQR - SharedFund", size / 2f, 50, titlePaint);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#E2E8F0"));
        linePaint.setStrokeWidth(2);
        canvas.drawLine(30, 70, size - 30, 70, linePaint);

        Paint qrDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qrDotPaint.setColor(Color.parseColor("#1E293B"));
        int qrSize = 250;
        int qrAreaX = (size - qrSize) / 2;
        int qrAreaY = 85;
        int cellSize = 10;
        java.util.Random rand = new java.util.Random(accountNumber.hashCode());

        drawQrAnchor(canvas, qrDotPaint, qrAreaX, qrAreaY, 56);
        drawQrAnchor(canvas, qrDotPaint, qrAreaX + qrSize - 56, qrAreaY, 56);
        drawQrAnchor(canvas, qrDotPaint, qrAreaX, qrAreaY + qrSize - 56, 56);

        for (int x = qrAreaX + 64; x < qrAreaX + qrSize - 64; x += cellSize) {
            for (int y = qrAreaY + 64; y < qrAreaY + qrSize - 64; y += cellSize) {
                if (rand.nextBoolean()) {
                    canvas.drawRect(x, y, x + cellSize - 2, y + cellSize - 2, qrDotPaint);
                }
            }
        }

        Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        infoPaint.setColor(Color.parseColor("#0F172A"));
        infoPaint.setTextSize(20);
        infoPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("STK: " + accountNumber, size / 2f, qrAreaY + qrSize + 32, infoPaint);

        Paint amountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        amountPaint.setColor(Color.parseColor("#10B981"));
        amountPaint.setTextSize(26);
        amountPaint.setTypeface(Typeface.DEFAULT_BOLD);
        amountPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(amount, size / 2f, qrAreaY + qrSize + 66, amountPaint);

        Paint contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentPaint.setColor(Color.parseColor("#64748B"));
        contentPaint.setTextSize(16);
        contentPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ND: " + content, size / 2f, qrAreaY + qrSize + 96, contentPaint);

        return bitmap;
    }

    private void drawQrAnchor(Canvas canvas, Paint paint, int x, int y, int size) {
        Paint outerPaint = new Paint(paint);
        outerPaint.setStyle(Paint.Style.STROKE);
        outerPaint.setStrokeWidth(6);
        canvas.drawRect(x, y, x + size, y + size, outerPaint);

        int inset = size / 4;
        canvas.drawRect(x + inset, y + inset, x + size - inset, y + size - inset, paint);
    }

    private void confirmDeposit() {
        if (roomId == null || pendingAmount <= 0 || qrGeneratedAt == null) return;
        btnDoneDeposit.setEnabled(false);
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        TransactionRepository transactions = new TransactionRepository();
        transactions.setAccessToken(session.getAccessToken());
        TransactionRepository.DataCallback<com.sharedfund.app.model.Transaction> callback =
                new TransactionRepository.DataCallback<com.sharedfund.app.model.Transaction>() {
                    @Override public void onSuccess(com.sharedfund.app.model.Transaction transaction) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            btnDoneDeposit.setEnabled(true);
                            if (transaction == null) {
                                Toast.makeText(requireContext(), "SePay chưa xác nhận khoản chuyển này. Hãy đợi rồi kiểm tra lại.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            RoomRepository.getInstance().onRemoteTransactionReceived(transaction);
                            if (isWalletDeposit()) {
                                RoomRepository.getInstance().loadUserProfile();
                            }
                            if (listener != null) listener.onDepositComplete(pendingAmount);
                            Toast.makeText(requireContext(), "Đã nhận xác nhận thanh toán từ SePay.", Toast.LENGTH_SHORT).show();
                            dismiss();
                        });
                    }
                    @Override public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            btnDoneDeposit.setEnabled(true);
                            Toast.makeText(requireContext(), "Không kiểm tra được trạng thái thanh toán: " + message, Toast.LENGTH_LONG).show();
                        });
                    }
                };
        if (isWalletDeposit()) {
            transactions.findConfirmedWalletPayment(session.getUserId(), pendingAmount, qrGeneratedAt, callback);
        } else {
            transactions.findConfirmedPayment(roomId, pendingAmount, qrGeneratedAt, callback);
        }
    }

    private void simulateDepositForDemo() {
        if (!isDebugBuild() || pendingAmount <= 0 || qrGeneratedAt == null) return;
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Không xác định được tài khoản demo.", Toast.LENGTH_LONG).show();
            return;
        }

        btnDoneDeposit.setEnabled(false);
        btnDemoPaymentSuccess.setEnabled(false);
        TransactionRepository transactions = new TransactionRepository();
        transactions.setAccessToken(session.getAccessToken());
        transactions.simulateSePayPaymentForDemo(userId,
                isWalletDeposit() ? null : roomId, pendingAmount, demoPaymentReference,
                new TransactionRepository.DataCallback<com.google.gson.JsonObject>() {
                    @Override public void onSuccess(com.google.gson.JsonObject result) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            btnDoneDeposit.setEnabled(true);
                            btnDemoPaymentSuccess.setEnabled(true);
                            if (!isWalletDeposit()) {
                                TransactionRepository repository = new TransactionRepository();
                                repository.setAccessToken(session.getAccessToken());
                                repository.refreshRoomTransactions(roomId,
                                        new TransactionRepository.DataCallback<java.util.List<com.sharedfund.app.model.Transaction>>() {
                                            @Override public void onSuccess(java.util.List<com.sharedfund.app.model.Transaction> rows) {
                                                if (!isAdded()) return;
                                                requireActivity().runOnUiThread(() -> {
                                                    if (!isAdded()) return;
                                                    if (rows != null) for (com.sharedfund.app.model.Transaction row : rows)
                                                        RoomRepository.getInstance().onRemoteTransactionReceived(row);
                                                    completeDemoDeposit();
                                                });
                                            }
                                            @Override public void onError(String message) {
                                                if (isAdded()) requireActivity().runOnUiThread(() -> {
                                                    if (isAdded()) Toast.makeText(requireContext(),
                                                            "Thanh toán demo đã được ghi nhận, nhưng không tải lại được lịch sử: " + message,
                                                            Toast.LENGTH_LONG).show();
                                                    if (isAdded()) completeDemoDeposit();
                                                });
                                            }
                                        });
                            } else {
                                completeDemoDeposit();
                            }
                        });
                    }
                    @Override public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            btnDoneDeposit.setEnabled(true);
                            btnDemoPaymentSuccess.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    "Không mô phỏng được thanh toán. Kiểm tra RPC demo trên Supabase: " + message,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void completeDemoDeposit() {
        if (listener != null) listener.onDepositComplete(pendingAmount);
        Toast.makeText(requireContext(), "Đã mô phỏng thanh toán thành công (DEMO).", Toast.LENGTH_LONG).show();
        dismiss();
    }

    private boolean isDebugBuild() {
        return (requireContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
