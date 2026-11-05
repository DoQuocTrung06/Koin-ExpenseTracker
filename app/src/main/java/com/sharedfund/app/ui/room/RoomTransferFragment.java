package com.sharedfund.app.ui.room;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.ui.widget.CategoryChipHelper;
import com.sharedfund.app.util.VietnameseCurrencyWords;
import com.sharedfund.app.util.CopyToast;
import com.sharedfund.app.util.ErrorMessageHelper;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.ScannedQrPayment;
import com.sharedfund.app.model.Transaction;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class RoomTransferFragment extends Fragment {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_ROOM_BALANCE = "arg_room_balance";

    public interface OnTransferListener {
        void onTransferComplete();
        void onRequestQrScan();
    }

    private String roomId;
    private double roomBalance;
    private String roomName = "Quỹ nhóm";
    private OnTransferListener listener;

    private View layoutRoomTransferForm;
    private View layoutProcessingNotice;
    private View layoutSuccessNotice;

    private TextView tvTransferRoomBalance;
    private TabLayout tabLayoutRoomTransfer;
    private LinearLayout layoutTransferWallet;
    private LinearLayout layoutTransferBank;
    private LinearLayout layoutTransferQr;

    private View layoutBottomTransferDock;
    private View layoutQuickAmountBarTransfer;
    private MaterialButton btnQuickTransfer50k;
    private MaterialButton btnQuickTransfer100k;
    private MaterialButton btnQuickTransfer500k;
    private MaterialButton btnQuickTransferAll;
    private MaterialButton btnTransferSubmitUnified;

    private TextInputLayout tilTransferWalletAmount;
    private TextInputEditText etTransferWalletAmount;
    private TextInputLayout tilTransferWalletNote;
    private TextInputEditText etTransferWalletNote;

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

    private MaterialButton btnScanQrTransfer;
    private MaterialButton btnPickQrImageTransfer;
    private MaterialCardView cardQrResult;
    private TextView tvQrResultDetails;
    private TextInputLayout tilQrAmount;
    private TextInputEditText etQrAmount;
    private TextInputLayout tilQrNote;
    private TextInputEditText etQrNote;
    private boolean isImeOpen = false;
    private ScannedQrPayment pendingQr;
    private LinearLayout layoutCategoryChipsWallet;
    private LinearLayout layoutCategoryChipsBank;
    private LinearLayout layoutCategoryChipsQr;
    private String selectedCategory = null;

    private View layoutConfirmStep;
    private TextView tvRoomTransferScreenTitle;
    private TextView tvConfirmStepAmount;
    private TextView tvConfirmStepAmountWords;
    private TextView tvConfirmStepType;
    private TextView tvConfirmStepSource;
    private TextView tvConfirmStepDest;
    private TextView tvConfirmStepDestAccount;
    private TextView tvConfirmStepDestBank;
    private TextView tvConfirmStepNote;
    private MaterialButton btnConfirmStepBackForm;
    private MaterialButton btnConfirmStepExecute;

    private TextView tvSuccessTitle;
    private TextView tvSuccessAmount;
    private TextView tvSuccessTxId;
    private ImageView btnSuccessCopyTxId;
    private TextView tvSuccessTime;
    private TextView tvSuccessSource;
    private TextView tvSuccessDest;
    private TextView tvSuccessNote;
    private MaterialButton btnSuccessDone;

    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    private DecimalFormat df;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<ScanOptions> cameraScanLauncher;
    private View layoutQrEmptyPrompt;
    private View layoutQrPaymentForm;

    public static RoomTransferFragment newInstance(String roomId, double roomBalance) {
        RoomTransferFragment sheet = new RoomTransferFragment();
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        symbols.setGroupingSeparator('.');
        df = new DecimalFormat("#,##0", symbols);

        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID, "");
            roomBalance = getArguments().getDouble(ARG_ROOM_BALANCE, 0.0);
        }

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                parseQrFromUri(uri);
            }
        });

        cameraScanLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result != null && result.getContents() != null) {
                ScannedQrPayment parsed = ScannedQrPayment.parse(result.getContents());
                if (parsed != null && parsed.accountNumber != null) {
                    setScannedQrData(parsed);
                } else {
                    Toast.makeText(requireContext(), "Không tìm thấy thông tin chuyển khoản VietQR trong mã vừa quét", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabs();
        setupQuickAmounts();
        setupQrTransferSection();
        setupAllCategoryChips();
        setupKeyboardAdaptiveBehavior(view);
        updateBalanceUi();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
    }

    private void handleBackNavigation() {
        if (layoutConfirmStep != null && layoutConfirmStep.getVisibility() == View.VISIBLE) {
            layoutConfirmStep.setVisibility(View.GONE);
            layoutRoomTransferForm.setVisibility(View.VISIBLE);
            if (layoutBottomTransferDock != null) {
                layoutBottomTransferDock.setVisibility(View.VISIBLE);
            }
            if (tvRoomTransferScreenTitle != null) {
                tvRoomTransferScreenTitle.setText("Chuyển tiền từ quỹ");
            }
        } else {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void initViews(View view) {
        tvRoomTransferScreenTitle = view.findViewById(R.id.tvRoomTransferScreenTitle);
        ImageButton btnBack = view.findViewById(R.id.btnRoomTransferBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        layoutRoomTransferForm = view.findViewById(R.id.layoutRoomTransferForm);
        layoutProcessingNotice = view.findViewById(R.id.layoutProcessingNotice);
        layoutSuccessNotice = view.findViewById(R.id.layoutSuccessNotice);

        tvTransferRoomBalance = view.findViewById(R.id.tvTransferRoomBalance);
        tabLayoutRoomTransfer = view.findViewById(R.id.tabLayoutRoomTransfer);
        layoutTransferWallet = view.findViewById(R.id.layoutTransferWallet);
        layoutTransferBank = view.findViewById(R.id.layoutTransferBank);
        layoutTransferQr = view.findViewById(R.id.layoutTransferQr);

        layoutBottomTransferDock = view.findViewById(R.id.layoutBottomTransferDock);
        layoutQuickAmountBarTransfer = view.findViewById(R.id.layoutQuickAmountBarTransfer);
        btnQuickTransfer50k = view.findViewById(R.id.btnQuickTransfer50k);
        btnQuickTransfer100k = view.findViewById(R.id.btnQuickTransfer100k);
        btnQuickTransfer500k = view.findViewById(R.id.btnQuickTransfer500k);
        btnQuickTransferAll = view.findViewById(R.id.btnQuickTransferAll);
        btnTransferSubmitUnified = view.findViewById(R.id.btnTransferSubmitUnified);
        if (btnTransferSubmitUnified != null) {
            btnTransferSubmitUnified.setOnClickListener(v -> {
                int pos = tabLayoutRoomTransfer != null ? tabLayoutRoomTransfer.getSelectedTabPosition() : 0;
                if (pos == 0) {
                    validateAndConfirmWalletTransfer();
                } else if (pos == 1) {
                    validateAndConfirmBankTransfer();
                } else {
                    validateAndConfirmQrTransfer();
                }
            });
        }

        tilTransferWalletAmount = view.findViewById(R.id.tilTransferWalletAmount);
        etTransferWalletAmount = view.findViewById(R.id.etTransferWalletAmount);
        tilTransferWalletNote = view.findViewById(R.id.tilTransferWalletNote);
        etTransferWalletNote = view.findViewById(R.id.etTransferWalletNote);

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

        btnScanQrTransfer = view.findViewById(R.id.btnScanQrTransfer);
        btnPickQrImageTransfer = view.findViewById(R.id.btnPickQrImageTransfer);
        layoutQrEmptyPrompt = view.findViewById(R.id.layoutQrEmptyPrompt);
        layoutQrPaymentForm = view.findViewById(R.id.layoutQrPaymentForm);
        cardQrResult = view.findViewById(R.id.cardQrResult);
        tvQrResultDetails = view.findViewById(R.id.tvQrResultDetails);
        tilQrAmount = view.findViewById(R.id.tilQrAmount);
        etQrAmount = view.findViewById(R.id.etQrAmount);
        tilQrNote = view.findViewById(R.id.tilQrNote);
        etQrNote = view.findViewById(R.id.etQrNote);
        layoutCategoryChipsWallet = view.findViewById(R.id.layoutCategoryChipsWallet);
        layoutCategoryChipsBank = view.findViewById(R.id.layoutCategoryChipsBank);
        layoutCategoryChipsQr = view.findViewById(R.id.layoutCategoryChipsQr);

        TextWatcher noteClearChipWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0 && selectedCategory != null) {
                    selectedCategory = null;
                    setupAllCategoryChips();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etTransferWalletNote.addTextChangedListener(noteClearChipWatcher);
        etTransferBankNote.addTextChangedListener(noteClearChipWatcher);
        etQrNote.addTextChangedListener(noteClearChipWatcher);

        layoutConfirmStep = view.findViewById(R.id.layoutConfirmStep);
        tvConfirmStepAmount = view.findViewById(R.id.tvConfirmStepAmount);
        tvConfirmStepAmountWords = view.findViewById(R.id.tvConfirmStepAmountWords);
        tvConfirmStepType = view.findViewById(R.id.tvConfirmStepType);
        tvConfirmStepSource = view.findViewById(R.id.tvConfirmStepSource);
        tvConfirmStepDest = view.findViewById(R.id.tvConfirmStepDest);
        tvConfirmStepDestAccount = view.findViewById(R.id.tvConfirmStepDestAccount);
        tvConfirmStepDestBank = view.findViewById(R.id.tvConfirmStepDestBank);
        tvConfirmStepNote = view.findViewById(R.id.tvConfirmStepNote);
        btnConfirmStepBackForm = view.findViewById(R.id.btnConfirmStepBackForm);
        if (btnConfirmStepBackForm != null) {
            btnConfirmStepBackForm.setOnClickListener(v -> handleBackNavigation());
        }
        btnConfirmStepExecute = view.findViewById(R.id.btnConfirmStepExecute);

        tvSuccessTitle = view.findViewById(R.id.tvSuccessTitle);
        tvSuccessAmount = view.findViewById(R.id.tvSuccessAmount);
        tvSuccessTxId = view.findViewById(R.id.tvSuccessTxId);
        btnSuccessCopyTxId = view.findViewById(R.id.btnSuccessCopyTxId);
        tvSuccessTime = view.findViewById(R.id.tvSuccessTime);
        tvSuccessSource = view.findViewById(R.id.tvSuccessSource);
        tvSuccessDest = view.findViewById(R.id.tvSuccessDest);
        tvSuccessNote = view.findViewById(R.id.tvSuccessNote);
        btnSuccessDone = view.findViewById(R.id.btnSuccessDone);
    }

    private void updateBalanceUi() {
        Room r = RoomRepository.getInstance().getRoomSync(roomId);
        if (r != null) {
            roomBalance = r.getBalance();
            if (r.getName() != null && !r.getName().isEmpty()) {
                roomName = r.getName();
            }
        }
        tvTransferRoomBalance.setText(df.format(roomBalance) + " ₫");
    }

        private void setupAllCategoryChips() {
        if (getContext() == null) return;
        List<CategoryChipHelper.CategoryItem> categories = CategoryChipHelper.getDefaultExpenseCategories();
        if (layoutCategoryChipsWallet != null) {
            CategoryChipHelper.populateChips(requireContext(), layoutCategoryChipsWallet, categories, selectedCategory, cat -> {
                selectedCategory = cat;
                setupAllCategoryChips();
            });
        }
        if (layoutCategoryChipsBank != null) {
            CategoryChipHelper.populateChips(requireContext(), layoutCategoryChipsBank, categories, selectedCategory, cat -> {
                selectedCategory = cat;
                setupAllCategoryChips();
            });
        }
        if (layoutCategoryChipsQr != null) {
            CategoryChipHelper.populateChips(requireContext(), layoutCategoryChipsQr, categories, selectedCategory, cat -> {
                selectedCategory = cat;
                setupAllCategoryChips();
            });
        }
    }

    private void setupTabs() {
        tabLayoutRoomTransfer.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                layoutTransferWallet.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
                layoutTransferBank.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
                layoutTransferQr.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);
                if (btnTransferSubmitUnified != null) {
                    if (pos == 0) {
                        btnTransferSubmitUnified.setVisibility(View.VISIBLE);
                        btnTransferSubmitUnified.setText("Tiếp tục chuyển về Ví Koin");
                    } else if (pos == 1) {
                        btnTransferSubmitUnified.setVisibility(View.VISIBLE);
                        btnTransferSubmitUnified.setText("Tiếp tục chuyển đến Ngân hàng");
                    } else {
                        btnTransferSubmitUnified.setVisibility(pendingQr != null ? View.VISIBLE : View.GONE);
                        btnTransferSubmitUnified.setText("Tiếp tục thanh toán từ quỹ");
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupQuickAmounts() {
        if (btnQuickTransfer50k != null) {
            btnQuickTransfer50k.setOnClickListener(v -> applyQuickAmount(50000));
        }
        if (btnQuickTransfer100k != null) {
            btnQuickTransfer100k.setOnClickListener(v -> applyQuickAmount(100000));
        }
        if (btnQuickTransfer500k != null) {
            btnQuickTransfer500k.setOnClickListener(v -> applyQuickAmount(500000));
        }
        if (btnQuickTransferAll != null) {
            btnQuickTransferAll.setOnClickListener(v -> {
                long wholeAmount = (long) Math.floor(roomBalance);
                if (wholeAmount > 0) {
                    applyQuickAmount(wholeAmount);
                }
            });
        }
    }

    private void applyQuickAmount(long amount) {
        int pos = tabLayoutRoomTransfer != null ? tabLayoutRoomTransfer.getSelectedTabPosition() : 0;
        String text = String.valueOf(amount);
        if (pos == 0) {
            etTransferWalletAmount.setText(text);
            tilTransferWalletAmount.setError(null);
            if (etTransferWalletAmount.getText() != null) {
                etTransferWalletAmount.setSelection(etTransferWalletAmount.getText().length());
            }
        } else if (pos == 1) {
            etTransferBankAmount.setText(text);
            tilTransferBankAmount.setError(null);
            if (etTransferBankAmount.getText() != null) {
                etTransferBankAmount.setSelection(etTransferBankAmount.getText().length());
            }
        } else {
            etQrAmount.setText(text);
            tilQrAmount.setError(null);
            if (etQrAmount.getText() != null) {
                etQrAmount.setSelection(etQrAmount.getText().length());
            }
        }
    }

    private void setupKeyboardAdaptiveBehavior(View rootView) {
        View.OnFocusChangeListener amountFocusListener = (v, hasFocus) -> updateKeyboardAdaptiveDock();
        etTransferWalletAmount.setOnFocusChangeListener(amountFocusListener);
        etTransferBankAmount.setOnFocusChangeListener(amountFocusListener);
        etQrAmount.setOnFocusChangeListener(amountFocusListener);

        View.OnFocusChangeListener otherFocusListener = (v, hasFocus) -> {
            if (hasFocus) {
                updateKeyboardAdaptiveDock();
            }
        };
        etTransferWalletNote.setOnFocusChangeListener(otherFocusListener);
        etTransferBankNote.setOnFocusChangeListener(otherFocusListener);
        etQrNote.setOnFocusChangeListener(otherFocusListener);
        etTransferBankName.setOnFocusChangeListener(otherFocusListener);
        etTransferBankAccNum.setOnFocusChangeListener(otherFocusListener);
        etTransferBankAccHolder.setOnFocusChangeListener(otherFocusListener);

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
        boolean isAmountFocused = (etTransferWalletAmount != null && etTransferWalletAmount.hasFocus())
                || (etTransferBankAmount != null && etTransferBankAmount.hasFocus())
                || (etQrAmount != null && etQrAmount.hasFocus());
        if (layoutQuickAmountBarTransfer != null) {
            layoutQuickAmountBarTransfer.setVisibility(isImeOpen && isAmountFocused ? View.VISIBLE : View.GONE);
        }
    }

    private void setupQrTransferSection() {
        btnScanQrTransfer.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Hướng camera về mã QR để thanh toán");
            
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(false);
            options.setOrientationLocked(false);
            if (cameraScanLauncher != null) {
                cameraScanLauncher.launch(options);
            }
        });

        btnPickQrImageTransfer.setOnClickListener(v -> {
            if (pickImageLauncher != null) {
                pickImageLauncher.launch("image/*");
            }
        });
    }

    private void parseQrFromUri(Uri imageUri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, opts);
            int maxDim = Math.max(opts.outWidth, opts.outHeight);
            int inSampleSize = 1;
            while (maxDim / inSampleSize > 1200) {
                inSampleSize *= 2;
            }
            opts.inSampleSize = inSampleSize;
            opts.inJustDecodeBounds = false;

            Bitmap bitmap;
            try (InputStream stream2 = requireContext().getContentResolver().openInputStream(imageUri)) {
                bitmap = BitmapFactory.decodeStream(stream2, null, opts);
            }
            if (bitmap == null) {
                Toast.makeText(requireContext(), "Không thể mở ảnh mã QR", Toast.LENGTH_SHORT).show();
                return;
            }

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            RGBLuminanceSource source = new RGBLuminanceSource(w, h, pixels);
            Result result = null;
            try {
                result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)));
            } catch (Exception ignored) {
                try {
                    result = new MultiFormatReader().decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
                } catch (Exception ignored2) {}
            }

            if (result != null && result.getText() != null) {
                ScannedQrPayment parsed = ScannedQrPayment.parse(result.getText());
                if (parsed != null && parsed.accountNumber != null) {
                    setScannedQrData(parsed);
                } else {
                    Toast.makeText(requireContext(), "Không tìm thấy thông tin chuyển khoản VietQR trong ảnh", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(requireContext(), "Không quét được mã QR từ ảnh này", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Không quét được mã QR từ ảnh này", Toast.LENGTH_SHORT).show();
        }
    }

    public void setScannedQrData(ScannedQrPayment parsed) {
        this.pendingQr = parsed;
        if (tabLayoutRoomTransfer != null && tabLayoutRoomTransfer.getTabCount() > 2) {
            TabLayout.Tab qrTab = tabLayoutRoomTransfer.getTabAt(2);
            if (qrTab != null) qrTab.select();
        }
        if (layoutQrEmptyPrompt != null) {
            layoutQrEmptyPrompt.setVisibility(View.GONE);
        }
        if (layoutQrPaymentForm != null) {
            layoutQrPaymentForm.setVisibility(View.VISIBLE);
        }
        if (cardQrResult != null && parsed != null) {
            cardQrResult.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            sb.append("Ngân hàng: ").append(formatBankLabel(parsed.bankBin)).append("\n");
            sb.append("Số TK: ").append(parsed.accountNumber).append("\n");
            sb.append("Người nhận: ").append(parsed.recipientName != null && !parsed.recipientName.trim().isEmpty() ? parsed.recipientName : "Tài khoản VietQR");
            String info = sb.toString();
            tvQrResultDetails.setText(info);
            if (parsed.amount != null && parsed.amount > 0) {
                etQrAmount.setText(String.valueOf(parsed.amount.longValue()));
            }
            if (parsed.transferContent != null && !parsed.transferContent.trim().isEmpty()) {
                etQrNote.setText(parsed.transferContent);
            }
        }
    }

    private String formatBankLabel(String bankBin) {
        if (bankBin == null) return "VietQR";
        switch (bankBin.trim()) {
            case "970422": return "MBBank (970422)";
            case "970436": return "Vietcombank (970436)";
            case "970415": return "VietinBank (970415)";
            case "970418": return "BIDV (970418)";
            case "970407": return "Techcombank (970407)";
            case "970432": return "VPBank (970432)";
            case "970423": return "TPBank (970423)";
            case "970416": return "ACB (970416)";
            case "970405": return "Agribank (970405)";
            case "970403": return "Sacombank (970403)";
            case "970448": return "OCB (970448)";
            case "970441": return "VIB (970441)";
            case "970443": return "SHB (970443)";
            case "970426": return "MSB (970426)";
            case "970440": return "SeABank (970440)";
            case "970437": return "HDBank (970437)";
            case "970428": return "Nam A Bank (970428)";
            case "970449": return "LPBank (970449)";
            case "970431": return "Eximbank (970431)";
            case "970454": return "BVBank (970454)";
            case "970433": return "VietBank (970433)";
            case "970419": return "NCB (970419)";
            case "970429": return "SCB (970429)";
            case "970438": return "BaoViet Bank (970438)";
            case "970425": return "ABBank (970425)";
            case "970452": return "Kienlongbank (970452)";
            case "970427": return "VietABank (970427)";
            case "970424": return "Shinhan Bank (970424)";
            case "970400": return "Saigonbank (970400)";
            case "970446": return "COOPBANK (970446)";
            case "970414": return "OceanBank (970414)";
            case "970408": return "GPBank (970408)";
            case "970430": return "PG Bank (970430)";
            default: return "Ngân hàng (BIN " + bankBin.trim() + ")";
        }
    }

    private boolean isCurrentUserOwner() {
        String myId = RoomRepository.getInstance().getCurrentUserId();
        if (myId == null || myId.isEmpty()) {
            myId = UserSessionManager.getInstance(requireContext()).getUserId();
        }
        Room room = RoomRepository.getInstance().getRoomSync(roomId);
        if (room != null && room.getCreatedBy() != null) {
            return room.getCreatedBy().equalsIgnoreCase(myId);
        }
        return false;
    }

    private void showVerificationDialog(String type, double amount, String source, String destName, String destAccount, String destBank, String note, Runnable onConfirm) {
        layoutRoomTransferForm.setVisibility(View.GONE);
        if (layoutBottomTransferDock != null) {
            layoutBottomTransferDock.setVisibility(View.GONE);
        }
        layoutConfirmStep.setVisibility(View.VISIBLE);
        if (tvRoomTransferScreenTitle != null) {
            tvRoomTransferScreenTitle.setText("Xác nhận giao dịch");
        }

        tvConfirmStepAmount.setText(df.format(amount) + " ₫");
        if (tvConfirmStepAmountWords != null) {
            tvConfirmStepAmountWords.setText(VietnameseCurrencyWords.toWordsWithParentheses((long) amount));
        }
        tvConfirmStepType.setText(type);
        tvConfirmStepSource.setText(source);

        if (destAccount != null && !destAccount.isEmpty()) {
            tvConfirmStepDest.setText(destName != null ? destName.toUpperCase(Locale.getDefault()) : "");
            if (tvConfirmStepDestAccount != null) {
                tvConfirmStepDestAccount.setText(destAccount);
                tvConfirmStepDestAccount.setVisibility(View.VISIBLE);
            }
            if (tvConfirmStepDestBank != null) {
                tvConfirmStepDestBank.setText(destBank != null ? destBank : "");
                tvConfirmStepDestBank.setVisibility(View.VISIBLE);
            }
        } else {
            tvConfirmStepDest.setText(destName != null ? destName : "");
            if (tvConfirmStepDestAccount != null) {
                tvConfirmStepDestAccount.setVisibility(View.GONE);
            }
            if (tvConfirmStepDestBank != null) {
                tvConfirmStepDestBank.setVisibility(View.GONE);
            }
        }

        tvConfirmStepNote.setText(note);

        btnConfirmStepExecute.setOnClickListener(v -> {
            layoutConfirmStep.setVisibility(View.GONE);
            if (onConfirm != null) onConfirm.run();
        });
    }

    private void showSuccess(String title, String amountFormatted, String source, String dest, String note, String rawTxId) {
        layoutProcessingNotice.setVisibility(View.GONE);
        if (layoutBottomTransferDock != null) {
            layoutBottomTransferDock.setVisibility(View.GONE);
        }
        layoutSuccessNotice.setVisibility(View.VISIBLE);
        if (tvRoomTransferScreenTitle != null) {
            tvRoomTransferScreenTitle.setText("Kết quả giao dịch");
        }

        tvSuccessTitle.setText(title);
        if (tvSuccessAmount != null) {
            if (title != null && title.contains("yêu cầu")) {
                tvSuccessAmount.setVisibility(View.GONE);
            } else {
                tvSuccessAmount.setVisibility(View.VISIBLE);
                tvSuccessAmount.setText(amountFormatted);
                tvSuccessAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            }
        }
        tvSuccessSource.setText(source);
        tvSuccessDest.setText(dest);
        if (tvSuccessNote != null) tvSuccessNote.setText(note);

        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault());
        if (tvSuccessTime != null) tvSuccessTime.setText(timeFormat.format(new java.util.Date()));

        String txId = "#SF-" + String.format(Locale.US, "%08X", (int) (System.currentTimeMillis() & 0xFFFFFFFFL));
        if (rawTxId != null && rawTxId.length() >= 8) {
            txId = "#SF-" + rawTxId.substring(0, 8).toUpperCase(Locale.US);
        }
        if (tvSuccessTxId != null) tvSuccessTxId.setText(txId);

        final String finalTxId = txId;
        if (btnSuccessCopyTxId != null) {
            btnSuccessCopyTxId.setOnClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("TxID", finalTxId);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                CopyToast.show(requireContext(), requireView());
            });
        }

        btnSuccessDone.setOnClickListener(v -> {
            if (listener != null) listener.onTransferComplete();
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void validateAndConfirmWalletTransfer() {
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
        if (note.isEmpty()) note = "Rút tiền về Ví Koin";

        final double finalAmt1 = amount; final String finalNote1 = note; showVerificationDialog("Rút quỹ về Ví Koin", amount, roomName, "Ví cá nhân Koin", null, null, note, () -> executeWalletTransfer(finalAmt1, finalNote1));
    }

    private void executeWalletTransfer(double amount, String note) {
        layoutRoomTransferForm.setVisibility(View.GONE);
        if (layoutBottomTransferDock != null) {
            layoutBottomTransferDock.setVisibility(View.GONE);
        }
        layoutProcessingNotice.setVisibility(View.VISIBLE);

        boolean isOwner = isCurrentUserOwner();
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();

        TransactionRepository remote = new TransactionRepository();
        remote.setAccessToken(session.getAccessToken());
        String category = selectedCategory != null ? selectedCategory : (note.isEmpty() || note.equals("Rút tiền về Ví Koin") ? "Khác" : note);
        remote.submitWithdrawal(userId, userName != null ? userName : "Member", roomId, amount, category, note, isOwner,
                new TransactionRepository.DataCallback<Transaction>() {
                    @Override
                    public void onSuccess(Transaction created) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            if (created != null) {
                                RoomRepository.getInstance().onRemoteTransactionReceived(created);
                            }
                            if (isOwner) {
                                RoomRepository.getInstance().loadUserProfile();
                            }
                            RoomRepository.getInstance().refreshRoomTransactions(roomId, null);

                            showSuccess(isOwner ? "Chuyển tiền thành công!" : "Đã gửi yêu cầu chuyển tiền!",
                                    "-" + df.format(amount) + " ₫", roomName, "Ví cá nhân Koin", note,
                                    created != null ? created.getId() : null);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            layoutProcessingNotice.setVisibility(View.GONE);
                            layoutRoomTransferForm.setVisibility(View.VISIBLE);
                            if (layoutBottomTransferDock != null) {
                                layoutBottomTransferDock.setVisibility(View.VISIBLE);
                            }
                            String friendly = ErrorMessageHelper.getFriendlyMessage(errorMessage);
                            tilTransferWalletAmount.setError(friendly);
                            Toast.makeText(requireContext(), friendly, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void validateAndConfirmBankTransfer() {
        String bankName = etTransferBankName.getText() != null ? etTransferBankName.getText().toString().trim() : "";
        String accNum = etTransferBankAccNum.getText() != null ? etTransferBankAccNum.getText().toString().trim() : "";
        String accHolder = etTransferBankAccHolder.getText() != null ? etTransferBankAccHolder.getText().toString().trim() : "";
        String amountStr = etTransferBankAmount.getText() != null ? etTransferBankAmount.getText().toString().trim() : "";
        String note = etTransferBankNote.getText() != null ? etTransferBankNote.getText().toString().trim() : "";

        if (bankName.isEmpty()) { tilTransferBankName.setError("Vui lòng nhập tên ngân hàng"); return; }
        tilTransferBankName.setError(null);
        if (accNum.isEmpty()) { tilTransferBankAccNum.setError("Vui lòng nhập số tài khoản"); return; }
        tilTransferBankAccNum.setError(null);
        if (accHolder.isEmpty()) { tilTransferBankAccHolder.setError("Vui lòng nhập tên chủ tài khoản"); return; }
        tilTransferBankAccHolder.setError(null);
        if (amountStr.isEmpty()) { tilTransferBankAmount.setError("Vui lòng nhập số tiền"); return; }
        tilTransferBankAmount.setError(null);

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            tilTransferBankAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) { tilTransferBankAmount.setError("Số tiền phải lớn hơn 0"); return; }
        if (amount > roomBalance) {
            tilTransferBankAmount.setError("Số dư quỹ không đủ (Còn " + df.format(roomBalance) + " ₫)");
            return;
        }

        if (note.isEmpty()) note = "Chuyển tiền từ quỹ " + roomName;

        String dest = bankName + " - " + accNum + " (" + accHolder + ")";
        final double finalAmt2 = amount; final String finalNote2 = note; final String finalBank2 = bankName; final String finalAcc2 = accNum; final String finalHolder2 = accHolder; showVerificationDialog("Chuyển đến Ngân hàng", amount, roomName, accHolder, accNum, bankName, note, () -> executeBankTransfer(finalAmt2, finalBank2, finalAcc2, finalHolder2, finalNote2));
    }

    private void executeBankTransfer(double amount, String bankName, String accNum, String accHolder, String note) {
        layoutRoomTransferForm.setVisibility(View.GONE);
        if (layoutBottomTransferDock != null) {
            layoutBottomTransferDock.setVisibility(View.GONE);
        }
        layoutProcessingNotice.setVisibility(View.VISIBLE);

        boolean isOwner = isCurrentUserOwner();
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();

        TransactionRepository remote = new TransactionRepository();
        remote.setAccessToken(session.getAccessToken());
        String category = selectedCategory != null ? selectedCategory : ("Chuyển đến " + bankName);
        String fullNote = note + " [STK: " + accNum + " - " + accHolder + " - " + bankName + "]";

        remote.submitWithdrawal(userId, userName != null ? userName : "Member", roomId, amount, category, fullNote, isOwner,
                new TransactionRepository.DataCallback<Transaction>() {
                    @Override
                    public void onSuccess(Transaction created) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            if (created != null) {
                                RoomRepository.getInstance().onRemoteTransactionReceived(created);
                            }
                            RoomRepository.getInstance().refreshRoomTransactions(roomId, null);

                            showSuccess(isOwner ? "Chuyển khoản thành công!" : "Đã gửi yêu cầu chuyển khoản!",
                                    "-" + df.format(amount) + " ₫", roomName, bankName + " (" + accNum + ")", note,
                                    created != null ? created.getId() : null);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            layoutProcessingNotice.setVisibility(View.GONE);
                            layoutRoomTransferForm.setVisibility(View.VISIBLE);
                            if (layoutBottomTransferDock != null) {
                                layoutBottomTransferDock.setVisibility(View.VISIBLE);
                            }
                            String friendly = ErrorMessageHelper.getFriendlyMessage(errorMessage);
                            tilTransferBankAmount.setError(friendly);
                            Toast.makeText(requireContext(), friendly, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void validateAndConfirmQrTransfer() {
        if (pendingQr == null) {
            Toast.makeText(requireContext(), "Vui lòng quét hoặc chọn ảnh mã QR thanh toán trước", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etQrAmount.getText() != null ? etQrAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilQrAmount.setError("Vui lòng nhập số tiền thanh toán");
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

        if (amount <= 0) { tilQrAmount.setError("Số tiền phải lớn hơn 0"); return; }
        if (amount > roomBalance) {
            tilQrAmount.setError("Số dư quỹ không đủ (Còn " + df.format(roomBalance) + " ₫)");
            return;
        }

        String note = etQrNote.getText() != null ? etQrNote.getText().toString().trim() : "";
        if (note.isEmpty()) note = "Thanh toán QR từ quỹ " + roomName;

        String dest = formatBankLabel(pendingQr.bankBin) + " - " + pendingQr.accountNumber;
        final double finalAmt3 = amount; final String finalNote3 = note; final String finalBank3 = formatBankLabel(pendingQr.bankBin); final String finalAcc3 = pendingQr.accountNumber; final String finalHolder3 = pendingQr.recipientName != null ? pendingQr.recipientName : "VietQR"; showVerificationDialog("Thanh toán quét mã QR", amount, roomName, finalHolder3, finalAcc3, finalBank3, note, () -> executeBankTransfer(finalAmt3, finalBank3, finalAcc3, finalHolder3, finalNote3));
    }
}
