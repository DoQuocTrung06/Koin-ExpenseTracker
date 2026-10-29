package com.sharedfund.app.ui.room;

import android.app.Dialog;
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
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * DepositDialogFragment - BottomSheet nạp tiền VietQR động
 * Người dùng nhập số tiền -> Nhấn "Tạo mã QR" -> Hiển thị QR thanh toán + hướng dẫn.
 * Giai đoạn sau: Gọi API Backend sinh link VietQR thật và lắng nghe webhook Realtime.
 */
public class DepositDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "room_id";

    private String roomId;

    // Views
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

    // Callback để thông báo khi nạp tiền thành công
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
            tvDepositTitle.setText(isWalletDeposit() ? "Nạp tiền vào tài khoản" : "Nạp tiền vào quỹ");
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
                // Ẩn QR nếu người dùng chỉnh sửa số tiền
                layoutQrSection.setVisibility(View.GONE);
                btnDoneDeposit.setVisibility(View.GONE);
                btnGenerateQr.setText("Tạo mã QR");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        btnCancelDeposit.setOnClickListener(v -> dismiss());

        btnGenerateQr.setOnClickListener(v -> generateQrCode());

        btnDoneDeposit.setOnClickListener(v -> confirmDeposit());
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

        // Hiện loading
        pbDepositLoading.setVisibility(View.VISIBLE);
        btnGenerateQr.setEnabled(false);

        // Sử dụng dữ liệu thật từ PaymentHelper
        String bankName = PaymentHelper.BANK_NAME;
        String accountNumber = PaymentHelper.ACCOUNT_NO;
        String accountName = PaymentHelper.ACCOUNT_NAME;
        String transferContent;
        String qrUrl;

        if (isWalletDeposit()) {
            String userId = UserSessionManager.getInstance(requireContext()).getCurrentUserId();
            if (userId == null || userId.isEmpty()) userId = "USER";
            transferContent = PaymentHelper.getWalletTransferContent(userId);
            qrUrl = PaymentHelper.generateWalletQrUrl(userId, amount);
        } else {
            Room room = RoomRepository.getInstance().getRoomSync(roomId);
            String roomCode = (room != null && room.getJoinCode() != null) ? room.getJoinCode() : "SFROOM";
            transferContent = PaymentHelper.getTransferContent(roomCode);
            qrUrl = PaymentHelper.generateVietQrUrl(roomCode, amount);
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

        // Cho phép sao chép thông tin thanh toán nhanh
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

        // Đã tạo qrUrl theo loại nạp tiền

        // Tải ảnh VietQR thực tế từ API VietQR qua OkHttp trên background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            Bitmap downloadedBitmap = null;
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(qrUrl).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        downloadedBitmap = BitmapFactory.decodeStream(response.body().byteStream());
                    }
                }
            } catch (Exception ignored) {
            }

            final Bitmap finalBitmap = (downloadedBitmap != null)
                    ? downloadedBitmap
                    : generateQrPlaceholderBitmap(accountNumber, formattedAmount, transferContent);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    pbDepositLoading.setVisibility(View.GONE);
                    btnGenerateQr.setEnabled(true);

                    ivQrCode.setImageBitmap(finalBitmap);
                    ivQrCode.setBackgroundColor(Color.WHITE);
                    ivQrCode.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    layoutQrSection.setVisibility(View.VISIBLE);
                    btnDoneDeposit.setVisibility(View.VISIBLE);
                    btnGenerateQr.setText("Tạo lại QR");

                    Toast.makeText(requireContext(),
                            "Mã VietQR đã sẵn sàng! Quét bằng ứng dụng ngân hàng.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Tạo ảnh QR placeholder hiển thị thông tin chuyển khoản
     * Giai đoạn sau: thay bằng ảnh QR thật từ VietQR API
     */
    private Bitmap generateQrPlaceholderBitmap(String accountNumber, String amount, String content) {
        int size = 480;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Nền trắng bo góc
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(0, 0, size, size), 20, 20, bgPaint);

        // Border xanh nhạt
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.parseColor("#4F46E5"));
        borderPaint.setStrokeWidth(4);
        canvas.drawRoundRect(new RectF(4, 4, size - 4, size - 4), 18, 18, borderPaint);

        // Tiêu đề "VietQR"
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#4F46E5"));
        titlePaint.setTextSize(32);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("VietQR - SharedFund", size / 2f, 50, titlePaint);

        // Divider
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#E2E8F0"));
        linePaint.setStrokeWidth(2);
        canvas.drawLine(30, 70, size - 30, 70, linePaint);

        // Vùng QR giả (grid pattern)
        Paint qrDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qrDotPaint.setColor(Color.parseColor("#1E293B"));
        int qrSize = 250;
        int qrAreaX = (size - qrSize) / 2;
        int qrAreaY = 85;
        int cellSize = 10;
        java.util.Random rand = new java.util.Random(accountNumber.hashCode());
        // Vẽ corner anchors
        drawQrAnchor(canvas, qrDotPaint, qrAreaX, qrAreaY, 56);
        drawQrAnchor(canvas, qrDotPaint, qrAreaX + qrSize - 56, qrAreaY, 56);
        drawQrAnchor(canvas, qrDotPaint, qrAreaX, qrAreaY + qrSize - 56, 56);
        // Vẽ random dots cho phần giữa
        for (int x = qrAreaX + 64; x < qrAreaX + qrSize - 64; x += cellSize) {
            for (int y = qrAreaY + 64; y < qrAreaY + qrSize - 64; y += cellSize) {
                if (rand.nextBoolean()) {
                    canvas.drawRect(x, y, x + cellSize - 2, y + cellSize - 2, qrDotPaint);
                }
            }
        }

        // Thông tin bên dưới QR
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

    /** Vẽ anchor cho góc QR code */
    private void drawQrAnchor(Canvas canvas, Paint paint, int x, int y, int size) {
        // Outer square
        Paint outerPaint = new Paint(paint);
        outerPaint.setStyle(Paint.Style.STROKE);
        outerPaint.setStrokeWidth(6);
        canvas.drawRect(x, y, x + size, y + size, outerPaint);
        // Inner solid square
        int inset = size / 4;
        canvas.drawRect(x + inset, y + inset, x + size - inset, y + size - inset, paint);
    }

    private void confirmDeposit() {
        String amountStr = etDepositAmount.getText() != null
                ? etDepositAmount.getText().toString().trim() : "0";
        double amount = 0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ignored) {}

        if (amount < 1000) {
            tilDepositAmount.setError("Vui lòng nhập số tiền hợp lệ (tối thiểu 1.000 ₫)");
            return;
        }

        // TODO: Giai đoạn sau - lắng nghe Realtime webhook xác nhận từ Backend
        if (isWalletDeposit()) {
            RoomRepository.getInstance().depositToUserWallet(amount);
            if (listener != null) listener.onDepositComplete(amount);
            Toast.makeText(requireContext(), "Nạp tiền vào tài khoản thành công!", Toast.LENGTH_SHORT).show();
            dismiss();
        } else {
            Room room = RoomRepository.getInstance().getRoomSync(roomId);
            String roomCode = (room != null && room.getJoinCode() != null) ? room.getJoinCode() : "SFROOM";
            String transferContent = PaymentHelper.getTransferContent(roomCode);
            String creatorName = UserSessionManager.getInstance(requireContext()).getCurrentUserName();
            String timeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            Transaction tx = new Transaction(
                    "", roomId, amount, Transaction.TYPE_INCOME, "Nạp quỹ",
                    "Nạp VietQR (" + transferContent + ") - " + creatorName, creatorName, timeStr
            );
            RoomRepository.getInstance().addTransaction(tx);

            if (listener != null) listener.onDepositComplete(amount);
            Toast.makeText(requireContext(), "Xác nhận nạp tiền thành công!", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }
}
