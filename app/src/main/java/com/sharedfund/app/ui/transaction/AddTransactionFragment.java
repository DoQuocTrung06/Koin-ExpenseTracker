package com.sharedfund.app.ui.transaction;

import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Category;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.model.ScannedQrPayment;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.text.DecimalFormat;

/**
 * Màn hình Thêm giao dịch (AddTransactionFragment)
 * Quản lý form nhập liệu: loại giao dịch, số tiền, danh mục, ghi chú và người thực hiện.
 * Đã chuẩn hóa: form validation trực quan, xóa lỗi tức thì khi gõ, phản hồi nút bấm khi đang lưu.
 */
public class AddTransactionFragment extends Fragment {

    private static final String ARG_ROOM_ID = "arg_room_id";

    private String roomId;
    private TransactionViewModel viewModel;

    private ImageButton btnAddTxBack;
    private RadioGroup rgTxType;
    private RadioButton rbExpense;
    private RadioButton rbIncome;
    private TextInputLayout tilAmount;
    private TextInputEditText etAmount;
    private TextView tvCategoryLabel;
    private Spinner spinnerCategory;
    private TextInputLayout tilNote;
    private TextInputEditText etNote;
    private TextInputLayout tilCreatedBy;
    private TextInputEditText etCreatedBy;
    private MaterialButton btnSaveTransaction;
    private MaterialButton btnScanRecipientQr;
    private TextView tvQrRecipientSummary;
    private ScannedQrPayment scannedQrPayment;
    private final ActivityResultLauncher<String> imagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) decodeQrFromImage(uri);
            });
    private final ActivityResultLauncher<ScanOptions> qrScanner = registerForActivityResult(
            new ScanContract(), result -> {
                if (result.getContents() != null) handleScannedPayload(result.getContents());
            });

    private final List<String> expenseCategories = new ArrayList<>();

    public static AddTransactionFragment newInstance(String roomId) {
        AddTransactionFragment fragment = new AddTransactionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupSpinner(new String[0]);
        setupTextWatchers();
        setupViewModel();
        rbIncome.setText(R.string.tx_contribute_from_wallet);
        rbExpense.setText(viewModel.isCurrentUserOwner()
                ? R.string.tx_withdraw_to_wallet : R.string.tx_withdraw_request);
        tvCategoryLabel.setText(rbIncome.isChecked()
                ? R.string.category_contribution_label : R.string.category_withdrawal_label);
        setupClickListeners();
    }

    private void initViews(View view) {
        btnAddTxBack = view.findViewById(R.id.btnAddTxBack);
        rgTxType = view.findViewById(R.id.rgTxType);
        rbExpense = view.findViewById(R.id.rbExpense);
        rbIncome = view.findViewById(R.id.rbIncome);
        tilAmount = view.findViewById(R.id.tilAmount);
        etAmount = view.findViewById(R.id.etAmount);
        tvCategoryLabel = view.findViewById(R.id.tvCategoryLabel);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        tilNote = view.findViewById(R.id.tilNote);
        etNote = view.findViewById(R.id.etNote);
        tilCreatedBy = view.findViewById(R.id.tilCreatedBy);
        etCreatedBy = view.findViewById(R.id.etCreatedBy);
        // Người thực hiện được xác định từ phiên đăng nhập ở backend, không nhập từ client.
        tilCreatedBy.setVisibility(View.GONE);
        btnSaveTransaction = view.findViewById(R.id.btnSaveTransaction);
        btnScanRecipientQr = view.findViewById(R.id.btnScanRecipientQr);
        tvQrRecipientSummary = view.findViewById(R.id.tvQrRecipientSummary);
    }

    private void setupSpinner(String[] categories) {
        ArrayAdapter<String> adapter = new CategorySpinnerAdapter(requireContext(), categories);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupTextWatchers() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilAmount.getError() != null) {
                    tilAmount.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilNote.getError() != null) {
                    tilNote.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etCreatedBy.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilCreatedBy.getError() != null) {
                    tilCreatedBy.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        viewModel.setAccessToken(session.getAccessToken());
        viewModel.setRoomId(roomId);
        btnScanRecipientQr.setVisibility(viewModel.isCurrentUserOwner() && rbExpense.isChecked()
                ? View.VISIBLE : View.GONE);
        RoomRepository.getInstance().configureAccessToken(session.getAccessToken());
        RoomRepository.getInstance().loadCategories(new RoomRepository.DataCallback<List<Category>>() {
            @Override public void onSuccess(List<Category> categories) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    expenseCategories.clear();
                    for (Category category : categories) {
                        if (category.getName() == null || category.getType() == null) continue;
                        if ("expense".equalsIgnoreCase(category.getType()) || "chi".equalsIgnoreCase(category.getType()))
                            expenseCategories.add(category.getName());
                    }
                    // Góp từ ví và rút khỏi quỹ dùng chung danh mục chi tiêu đã cấu hình trên Supabase.
                    setupSpinner(rbExpense.isChecked()
                            ? expenseCategories.toArray(new String[0])
                            : expenseCategories.toArray(new String[0]));
                });
            }
            @Override public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                        "Không tải được danh mục từ Supabase: " + message, Toast.LENGTH_LONG).show());
            }
        });

        // Lắng nghe lỗi hiển thị
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty()) {
                    btnSaveTransaction.setEnabled(true);
                    btnSaveTransaction.setText(scannedQrPayment == null
                            ? getString(R.string.btn_save_transaction) : "Xác nhận chi từ quỹ");
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Lắng nghe trạng thái tải
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                if (loading != null && loading) {
                    btnSaveTransaction.setEnabled(false);
                    btnSaveTransaction.setText("Đang lưu giao dịch...");
                } else {
                    btnSaveTransaction.setEnabled(true);
                    btnSaveTransaction.setText(scannedQrPayment == null
                            ? getString(R.string.btn_save_transaction) : "Xác nhận chi từ quỹ");
                }
            }
        });

        // Lắng nghe sự kiện thêm thành công
        viewModel.getAddSuccessEvent().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    String message;
                    if (rbIncome.isChecked()) message = "Đã góp tiền từ ví cá nhân vào quỹ.";
                    else if (scannedQrPayment != null) message = "Đã ghi nhận khoản chi nội bộ từ quỹ. App không chuyển tiền ngân hàng.";
                    else if (viewModel.isCurrentUserOwner()) message = "Đã rút tiền vào ví cá nhân.";
                    else message = "Đã gửi yêu cầu rút quỹ. Vui lòng chờ chủ quỹ duyệt.";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    // Chuyển thẳng về Lịch sử giao dịch theo đúng sơ đồ luồng
                    if (getActivity() instanceof MainActivity) {
                        requireActivity().getSupportFragmentManager().popBackStack();
                        ((MainActivity) getActivity()).openTransactions(roomId);
                    } else {
                        requireActivity().onBackPressed();
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        btnAddTxBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });

        btnScanRecipientQr.setOnClickListener(v -> showQrSourceOptions());

        rgTxType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbExpense) {
                    rbExpense.setBackgroundResource(R.drawable.bg_expense_badge);
                    rbExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_expense));
                    rbIncome.setBackgroundResource(R.drawable.bg_card);
                    rbIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    setupSpinner(expenseCategories.toArray(new String[0]));
                    rbExpense.setText(viewModel.isCurrentUserOwner()
                            ? R.string.tx_withdraw_to_wallet : R.string.tx_withdraw_request);
                    tvCategoryLabel.setText(R.string.category_withdrawal_label);
                    btnScanRecipientQr.setVisibility(viewModel.isCurrentUserOwner() ? View.VISIBLE : View.GONE);
                } else {
                    rbIncome.setBackgroundResource(R.drawable.bg_income_badge);
                    rbIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_income));
                    rbExpense.setBackgroundResource(R.drawable.bg_card);
                    rbExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    setupSpinner(expenseCategories.toArray(new String[0]));
                    rbIncome.setText(R.string.tx_contribute_from_wallet);
                    tvCategoryLabel.setText(R.string.category_contribution_label);
                    btnScanRecipientQr.setVisibility(View.GONE);
                    scannedQrPayment = null;
                    tvQrRecipientSummary.setVisibility(View.GONE);
                }
            }
        });

        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scannedQrPayment != null) confirmAndSaveQrExpense();
                else validateAndSave();
            }
        });
    }

    private void showQrSourceOptions() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Quét QR người nhận")
                .setItems(new String[]{"Mở camera quét mã", "Chọn ảnh QR từ máy"}, (dialog, which) -> {
                    if (which == 0) {
                        ScanOptions options = new ScanOptions()
                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                .setPrompt("Đưa mã QR VietQR vào khung quét")
                                .setBeepEnabled(false)
                                .setOrientationLocked(false)
                                .setCaptureActivity(CaptureActivity.class);
                        qrScanner.launch(options);
                    } else imagePicker.launch("image/*");
                }).show();
    }

    private void decodeQrFromImage(Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (java.io.InputStream boundsInput = requireContext().getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(boundsInput, null, bounds);
            }
            int sample = 1;
            while (bounds.outWidth / sample > 1800 || bounds.outHeight / sample > 1800) sample *= 2;
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sample;
            Bitmap bitmap;
            try (java.io.InputStream imageInput = requireContext().getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(imageInput, null, decodeOptions);
            }
            if (bitmap == null) throw new IllegalArgumentException("Không đọc được ảnh.");
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binary = new BinaryBitmap(new HybridBinarizer(source));
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            Result result = new MultiFormatReader().decode(binary, hints);
            handleScannedPayload(result.getText());
        } catch (Exception error) {
            Toast.makeText(requireContext(), "Không đọc được QR trong ảnh. Hãy chọn ảnh rõ, đủ sáng.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleScannedPayload(String payload) {
        ScannedQrPayment parsed = ScannedQrPayment.parse(payload);
        if (parsed == null || parsed.accountNumber == null || parsed.accountNumber.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Mã này không chứa thông tin tài khoản VietQR được hỗ trợ.", Toast.LENGTH_LONG).show();
            return;
        }
        scannedQrPayment = parsed;
        if (parsed.amount != null && parsed.amount > 0) etAmount.setText(String.valueOf(parsed.amount));
        if (parsed.transferContent != null && !parsed.transferContent.trim().isEmpty()
                && (etNote.getText() == null || etNote.getText().toString().trim().isEmpty())) {
            etNote.setText(parsed.transferContent);
        }
        String bank = parsed.bankBin == null ? "Không nhận diện được mã ngân hàng" : "Ngân hàng / BIN: " + bankName(parsed.bankBin);
        String name = parsed.recipientName == null ? "Chưa có tên người nhận trong QR" : "Người nhận: " + parsed.recipientName;
        String content = parsed.transferContent == null ? "" : "\nNội dung QR: " + parsed.transferContent;
        tvQrRecipientSummary.setText("Đã đọc thông tin QR\n" + bank + "\nSố tài khoản: "
                + parsed.accountNumber + "\n" + name + content
                + "\n\nQR chỉ điền thông tin người nhận; app không chuyển tiền ngân hàng.");
        tvQrRecipientSummary.setVisibility(View.VISIBLE);
        btnSaveTransaction.setText("Xác nhận chi từ quỹ");
    }

    private String bankName(String bin) {
        switch (bin) {
            case "970436": return "Vietcombank (970436)";
            case "970422": return "MB Bank (970422)";
            case "970407": return "Techcombank (970407)";
            case "970418": return "BIDV (970418)";
            case "970405": return "Agribank (970405)";
            case "970415": return "VietinBank (970415)";
            case "970432": return "VPBank (970432)";
            case "970423": return "TPBank (970423)";
            case "970416": return "ACB (970416)";
            case "970403": return "Sacombank (970403)";
            case "970441": return "VIB (970441)";
            default: return "BIN " + bin;
        }
    }

    private void confirmAndSaveQrExpense() {
        String amountText = etAmount.getText() == null ? "" : etAmount.getText().toString().trim();
        String note = etNote.getText() == null ? "" : etNote.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem() == null ? "Chi QR" : spinnerCategory.getSelectedItem().toString();
        if (amountText.isEmpty() || note.isEmpty()) {
            validateAndSave();
            return;
        }
        String amountDisplay = amountText + " ₫";
        com.sharedfund.app.model.Room room = RoomRepository.getInstance().getRoomSync(roomId);
        String roomBalance = room == null ? "Chưa tải được" : new DecimalFormat("#,###").format(room.getBalance()) + " ₫";
        String recipient = "Người nhận: " + (scannedQrPayment.recipientName == null ? "Chưa xác định" : scannedQrPayment.recipientName)
                + "\nNgân hàng: " + (scannedQrPayment.bankBin == null ? "Chưa xác định" : bankName(scannedQrPayment.bankBin))
                + "\nSố tài khoản: " + scannedQrPayment.accountNumber
                + "\nSố tiền: " + amountDisplay
                + "\nNguồn tiền: Quỹ nhóm (số dư hiện tại: " + roomBalance + ")\nNội dung: " + note
                + "\n\nXác nhận sẽ ghi một khoản chi nội bộ và trừ số dư quỹ. Ứng dụng không chuyển tiền thật đến ngân hàng.";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận chi qua QR")
                .setMessage(recipient)
                .setNegativeButton("Kiểm tra lại", null)
                .setPositiveButton("Ghi nhận chi", (dialog, which) -> {
                    btnSaveTransaction.setEnabled(false);
                    btnSaveTransaction.setText("Đang ghi nhận...");
                    viewModel.submitQrExpense(roomId, amountText, category, note,
                            scannedQrPayment.bankBin, scannedQrPayment.accountNumber,
                            scannedQrPayment.recipientName, scannedQrPayment.transferContent);
                }).show();
    }

    private void validateAndSave() {
        // Xóa thông báo lỗi trước đó
        tilAmount.setError(null);
        tilNote.setError(null);

        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
        String category = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "";
        String type = rbExpense.isChecked() ? Transaction.TYPE_EXPENSE : Transaction.TYPE_INCOME;

        boolean isValid = true;

        if (amountStr.isEmpty()) {
            tilAmount.setError(getString(R.string.err_amount_empty));
            isValid = false;
        } else {
            try {
                double val = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
                if (val <= 0) {
                    tilAmount.setError(getString(R.string.err_amount_invalid));
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                tilAmount.setError(getString(R.string.err_amount_invalid));
                isValid = false;
            }
        }

        if (note.isEmpty()) {
            tilNote.setError(getString(R.string.err_note_empty));
            isValid = false;
        }

        if (isValid) {
            btnSaveTransaction.setEnabled(false);
            btnSaveTransaction.setText("Đang lưu...");
            viewModel.submitTransaction(roomId, type, amountStr, category, note);
        }
    }
}
