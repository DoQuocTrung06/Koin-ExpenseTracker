package com.sharedfund.app.ui.room;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DepositDialogFragment - Dialog nạp tiền VietQR động
 * Người dùng nhập số tiền -> Nhấn "Tạo mã QR" -> Hiển thị QR thanh toán + hướng dẫn.
 * Giai đoạn sau: Gọi API Backend sinh link VietQR thật và lắng nghe webhook Realtime.
 */
public class DepositDialogFragment extends DialogFragment {

    private static final String ARG_ROOM_ID = "room_id";

    private String roomId;

    // Views
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

    public void setOnDepositCompleteListener(OnDepositCompleteListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_App_BottomSheet);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
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
    }

    private void bindViews(View view) {
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

        // TODO: Giai đoạn sau - gọi API Backend để lấy link VietQR thật
        // Hiện tại: Tạo link VietQR tĩnh từ thông tin mock
        btnGenerateQr.postDelayed(() -> {
            if (!isAdded()) return;
            pbDepositLoading.setVisibility(View.GONE);
            btnGenerateQr.setEnabled(true);

            // Lấy thông tin quỹ
            Room room = RoomRepository.getInstance().getRoomSync(roomId);
            String roomCode = room != null ? room.getJoinCode() : "SFROOM";
            String userId = UserSessionManager.getInstance(requireContext()).getCurrentUserId();

            // Thông tin chuyển khoản mock (sẽ thay bằng dữ liệu thật từ Backend)
            String bankName = "Vietcombank (VCB)";
            String accountNumber = "1019281234";
            String accountName = "NGUYEN VAN A";
            String transferContent = "SF " + roomCode + " " + userId;

            tvTransferInfo.setText(
                    "🏦 Ngân hàng: " + bankName + "\n"
                    + "💳 STK: " + accountNumber + "\n"
                    + "👤 Chủ TK: " + accountName + "\n"
                    + "📝 Nội dung: " + transferContent
            );

            // Hiển thị QR (dùng VietQR public URL với tham số)
            // Format: https://img.vietqr.io/image/{bank}-{account}-{template}.png?amount={amount}&addInfo={info}
            String vietQrUrl = "https://img.vietqr.io/image/VCB-" + accountNumber
                    + "-compact2.png?amount=" + (int) amount
                    + "&addInfo=" + transferContent.replace(" ", "%20")
                    + "&accountName=" + accountName.replace(" ", "%20");

            // Load QR image bằng thư viện (thay bằng Glide nếu có dependency)
            // Hiện tại: hiển thị placeholder icon
            ivQrCode.setImageResource(R.drawable.ic_wallet);
            ivQrCode.setBackgroundColor(
                    requireContext().getColor(R.color.primary_light));

            layoutQrSection.setVisibility(View.VISIBLE);
            btnDoneDeposit.setVisibility(View.VISIBLE);
            btnGenerateQr.setText("Tạo lại QR");

            Toast.makeText(requireContext(),
                    "Mã QR đã tạo! Quét bằng ứng dụng ngân hàng.", Toast.LENGTH_SHORT).show();

        }, 1000);
    }

    private void confirmDeposit() {
        String amountStr = etDepositAmount.getText() != null
                ? etDepositAmount.getText().toString().trim() : "0";
        double amount = 0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ignored) {}

        // TODO: Giai đoạn sau - lắng nghe Realtime webhook xác nhận từ Backend
        // Hiện tại: thêm transaction thu ngay vào mock data
        String creatorName = UserSessionManager.getInstance(requireContext()).getCurrentUserName();
        String timeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        Transaction tx = new Transaction(
                "", roomId, amount, Transaction.TYPE_INCOME, "Nạp quỹ",
                "Nạp tiền qua VietQR - " + creatorName, creatorName, timeStr
        );
        RoomRepository.getInstance().addTransaction(tx);

        if (listener != null) listener.onDepositComplete(amount);
        Toast.makeText(requireContext(), "Xác nhận nạp tiền thành công!", Toast.LENGTH_SHORT).show();
        dismiss();
    }
}
