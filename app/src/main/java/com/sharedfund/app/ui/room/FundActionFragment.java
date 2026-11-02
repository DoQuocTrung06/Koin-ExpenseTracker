package com.sharedfund.app.ui.room;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.sharedfund.app.R;
import com.sharedfund.app.ui.widget.AppToaster;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.ui.widget.CategoryChipHelper;
import com.sharedfund.app.util.VietnameseCurrencyWords;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FundActionFragment extends Fragment {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_ROOM_BALANCE = "arg_room_balance";

    private String roomId;
    private double roomBalance;
    private double walletBalance = 0.0;
    private String roomName = "Quỹ nhóm";
    private boolean isContributeTab = true;

    private View layoutFundActionForm;
    private View layoutConfirmStep;
    private View layoutProcessingNotice;
    private View layoutSuccessNotice;

    private TextView tvFundActionRoomBalance;
    private TextView tvFundActionWalletBalance;
    private TextView tvWithdrawRoleHint;

    private View btnTabContribute;
    private View btnTabWithdraw;
    private ImageView ivTabContribute;
    private ImageView ivTabWithdraw;
    private TextView tvTabContribute;
    private TextView tvTabWithdraw;
    private LinearLayout layoutContributeSection;
    private LinearLayout layoutWithdrawSection;

    private TextInputLayout tilContributeAmount;
    private TextInputEditText etContributeAmount;
    private TextInputLayout tilContributeNote;
    private TextInputEditText etContributeNote;

    private TextInputLayout tilWithdrawAmount;
    private TextInputEditText etWithdrawAmount;
    private TextInputLayout tilWithdrawNote;
    private TextInputEditText etWithdrawNote;
    private LinearLayout layoutCategoryChipsWithdraw;
    private String selectedWithdrawCategory = null;

    private LinearLayout layoutBottomActionDock;
    private LinearLayout layoutQuickAmountBar;
    private MaterialButton btnQuick50k;
    private MaterialButton btnQuick100k;
    private MaterialButton btnQuick500k;
    private MaterialButton btnQuickAll;
    private MaterialButton btnActionSubmit;

    private TextView tvFundActionScreenTitle;
    private TextView tvConfirmStepAmount;
    private TextView tvConfirmStepAmountWords;
    private TextView tvConfirmStepType;
    private TextView tvConfirmStepSource;
    private TextView tvConfirmStepDest;
    private TextView tvConfirmStepNote;
    private MaterialButton btnConfirmStepBackForm;
    private MaterialButton btnConfirmStepExecute;

    private TextView tvSuccessTitle;
    private TextView tvSuccessAmount;
    private TextView tvSuccessTxId;
    private ImageView btnSuccessCopyTxId;
    private TextView tvSuccessTime;
    private TextView tvSuccessType;
    private TextView tvSuccessSource;
    private TextView tvSuccessDest;
    private TextView tvSuccessNote;
    private MaterialButton btnSuccessDone;
    private boolean isImeOpen = false;

    private final DecimalFormat df;

    public FundActionFragment() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        df = new DecimalFormat("#,##0", symbols);
    }

    public static FundActionFragment newInstance(String roomId, double roomBalance) {
        FundActionFragment fragment = new FundActionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putDouble(ARG_ROOM_BALANCE, roomBalance);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID, "");
            roomBalance = getArguments().getDouble(ARG_ROOM_BALANCE, 0.0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fund_action, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabs();
        setupQuickAmounts();
        setupCategoryChips();
        setupKeyboardAdaptiveBehavior(view);
        loadBalances();

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
            layoutFundActionForm.setVisibility(View.VISIBLE);
            layoutBottomActionDock.setVisibility(View.VISIBLE);
            if (tvFundActionScreenTitle != null) {
                tvFundActionScreenTitle.setText("Góp / Rút quỹ");
            }
        } else {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void initViews(View view) {
        tvFundActionScreenTitle = view.findViewById(R.id.tvFundActionScreenTitle);
        ImageButton btnBack = view.findViewById(R.id.btnFundActionBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        layoutFundActionForm = view.findViewById(R.id.layoutFundActionForm);
        layoutConfirmStep = view.findViewById(R.id.layoutConfirmStep);
        layoutProcessingNotice = view.findViewById(R.id.layoutProcessingNotice);
        layoutSuccessNotice = view.findViewById(R.id.layoutSuccessNotice);

        tvFundActionRoomBalance = view.findViewById(R.id.tvFundActionRoomBalance);
        tvFundActionWalletBalance = view.findViewById(R.id.tvFundActionWalletBalance);
        tvWithdrawRoleHint = view.findViewById(R.id.tvWithdrawRoleHint);

        btnTabContribute = view.findViewById(R.id.btnTabContribute);
        btnTabWithdraw = view.findViewById(R.id.btnTabWithdraw);
        ivTabContribute = view.findViewById(R.id.ivTabContribute);
        ivTabWithdraw = view.findViewById(R.id.ivTabWithdraw);
        tvTabContribute = view.findViewById(R.id.tvTabContribute);
        tvTabWithdraw = view.findViewById(R.id.tvTabWithdraw);
        layoutContributeSection = view.findViewById(R.id.layoutContributeSection);
        layoutWithdrawSection = view.findViewById(R.id.layoutWithdrawSection);

        tilContributeAmount = view.findViewById(R.id.tilContributeAmount);
        etContributeAmount = view.findViewById(R.id.etContributeAmount);
        tilContributeNote = view.findViewById(R.id.tilContributeNote);
        etContributeNote = view.findViewById(R.id.etContributeNote);

        tilWithdrawAmount = view.findViewById(R.id.tilWithdrawAmount);
        etWithdrawAmount = view.findViewById(R.id.etWithdrawAmount);
        tilWithdrawNote = view.findViewById(R.id.tilWithdrawNote);
        etWithdrawNote = view.findViewById(R.id.etWithdrawNote);
        layoutCategoryChipsWithdraw = view.findViewById(R.id.layoutCategoryChipsWithdraw);

        etWithdrawNote.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && selectedWithdrawCategory != null) {
                selectedWithdrawCategory = null;
                setupCategoryChips();
            }
        });
        etWithdrawNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0 && selectedWithdrawCategory != null) {
                    selectedWithdrawCategory = null;
                    setupCategoryChips();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        layoutBottomActionDock = view.findViewById(R.id.layoutBottomActionDock);
        layoutQuickAmountBar = view.findViewById(R.id.layoutQuickAmountBar);
        btnQuick50k = view.findViewById(R.id.btnQuick50k);
        btnQuick100k = view.findViewById(R.id.btnQuick100k);
        btnQuick500k = view.findViewById(R.id.btnQuick500k);
        btnQuickAll = view.findViewById(R.id.btnQuickAll);
        btnActionSubmit = view.findViewById(R.id.btnActionSubmit);

        tvConfirmStepAmount = view.findViewById(R.id.tvConfirmStepAmount);
        tvConfirmStepAmountWords = view.findViewById(R.id.tvConfirmStepAmountWords);
        tvConfirmStepType = view.findViewById(R.id.tvConfirmStepType);
        tvConfirmStepSource = view.findViewById(R.id.tvConfirmStepSource);
        tvConfirmStepDest = view.findViewById(R.id.tvConfirmStepDest);
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
        tvSuccessType = view.findViewById(R.id.tvSuccessType);
        tvSuccessSource = view.findViewById(R.id.tvSuccessSource);
        tvSuccessDest = view.findViewById(R.id.tvSuccessDest);
        tvSuccessNote = view.findViewById(R.id.tvSuccessNote);
        btnSuccessDone = view.findViewById(R.id.btnSuccessDone);

        btnActionSubmit.setOnClickListener(v -> {
            if (isContributeTab) {
                validateAndProceedContribute();
            } else {
                validateAndProceedWithdraw();
            }
        });
    }

    private void setupTabs() {
        btnTabContribute.setOnClickListener(v -> selectTab(true));
        btnTabWithdraw.setOnClickListener(v -> selectTab(false));
        selectTab(true);
    }

    private void selectTab(boolean isContribute) {
        this.isContributeTab = isContribute;
        int secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int whiteColor = ContextCompat.getColor(requireContext(), R.color.white);

        btnTabContribute.setBackgroundResource(isContribute ? R.drawable.bg_wallet_tab_active : R.drawable.bg_wallet_tab_inactive);
        tvTabContribute.setTextColor(isContribute ? whiteColor : secondaryColor);
        ivTabContribute.setImageTintList(ColorStateList.valueOf(isContribute ? whiteColor : secondaryColor));

        btnTabWithdraw.setBackgroundResource(!isContribute ? R.drawable.bg_wallet_tab_active : R.drawable.bg_wallet_tab_inactive);
        tvTabWithdraw.setTextColor(!isContribute ? whiteColor : secondaryColor);
        ivTabWithdraw.setImageTintList(ColorStateList.valueOf(!isContribute ? whiteColor : secondaryColor));

        layoutContributeSection.setVisibility(isContribute ? View.VISIBLE : View.GONE);
        layoutWithdrawSection.setVisibility(isContribute ? View.GONE : View.VISIBLE);

        if (isContribute) {
            btnActionSubmit.setText("Tiếp tục góp từ Ví Koin");
            btnQuickAll.setText("Tất cả ví");
        } else {
            btnActionSubmit.setText("Tiếp tục rút về Ví Koin");
            btnQuickAll.setText("Rút hết quỹ");
        }
    }

    private void setupQuickAmounts() {
        btnQuick50k.setOnClickListener(v -> applyQuickAmount(50000));
        btnQuick100k.setOnClickListener(v -> applyQuickAmount(100000));
        btnQuick500k.setOnClickListener(v -> applyQuickAmount(500000));
        btnQuickAll.setOnClickListener(v -> {
            if (isContributeTab) {
                long max = (long) Math.floor(walletBalance);
                if (max > 0) applyQuickAmount(max);
            } else {
                long max = (long) Math.floor(roomBalance);
                if (max > 0) applyQuickAmount(max);
            }
        });
    }

    private void applyQuickAmount(long amount) {
        if (isContributeTab) {
            etContributeAmount.setText(String.valueOf(amount));
            tilContributeAmount.setError(null);
            etContributeAmount.setSelection(etContributeAmount.getText().length());
        } else {
            etWithdrawAmount.setText(String.valueOf(amount));
            tilWithdrawAmount.setError(null);
            etWithdrawAmount.setSelection(etWithdrawAmount.getText().length());
        }
    }

    private void setupKeyboardAdaptiveBehavior(View rootView) {
        View.OnFocusChangeListener amountFocusListener = (v, hasFocus) -> updateKeyboardAdaptiveDock();
        etContributeAmount.setOnFocusChangeListener(amountFocusListener);
        etWithdrawAmount.setOnFocusChangeListener(amountFocusListener);

        View.OnFocusChangeListener otherFocusListener = (v, hasFocus) -> {
            if (hasFocus) {
                updateKeyboardAdaptiveDock();
            }
        };
        etContributeNote.setOnFocusChangeListener(otherFocusListener);
        etWithdrawNote.setOnFocusChangeListener(otherFocusListener);

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
        boolean isAmountFocused = (etContributeAmount != null && etContributeAmount.hasFocus())
                || (etWithdrawAmount != null && etWithdrawAmount.hasFocus());
        if (layoutQuickAmountBar != null) {
            layoutQuickAmountBar.setVisibility(isImeOpen && isAmountFocused ? View.VISIBLE : View.GONE);
        }
    }

    private void setupCategoryChips() {
        boolean isOwner = isCurrentUserOwner();
        if (tvWithdrawRoleHint != null) {
            tvWithdrawRoleHint.setText(isOwner
                    ? "Bạn là Trưởng quỹ: Tiền rút sẽ được cộng trực tiếp vào Ví Koin cá nhân của bạn."
                    : "Bạn là Thành viên: Yêu cầu rút quỹ sẽ được gửi đến Trưởng quỹ phê duyệt.");
        }

        List<CategoryChipHelper.CategoryItem> categories = CategoryChipHelper.getDefaultExpenseCategories();
        CategoryChipHelper.populateChips(requireContext(), layoutCategoryChipsWithdraw, categories, selectedWithdrawCategory, category -> {
            selectedWithdrawCategory = category;
            setupCategoryChips();
        });
    }

    private void loadBalances() {
        Room r = RoomRepository.getInstance().getRoomSync(roomId);
        if (r != null) {
            roomBalance = r.getBalance();
            if (r.getName() != null && !r.getName().trim().isEmpty()) {
                roomName = r.getName();
            }
        }
        tvFundActionRoomBalance.setText(df.format(roomBalance) + " ₫");

        walletBalance = RoomRepository.getInstance().getUserWalletBalance();
        tvFundActionWalletBalance.setText("Số dư Ví cá nhân: " + df.format(walletBalance) + " ₫");
    }

    private boolean isCurrentUserOwner() {
        String myId = UserSessionManager.getInstance(requireContext()).getUserId();
        if (myId == null || myId.isEmpty()) {
            myId = RoomRepository.getInstance().getCurrentUserId();
        }
        Room room = RoomRepository.getInstance().getRoomSync(roomId);
        if (room != null && room.getCreatedBy() != null) {
            return room.getCreatedBy().equalsIgnoreCase(myId);
        }
        return true;
    }

    private void validateAndProceedContribute() {
        String amountStr = etContributeAmount.getText() != null ? etContributeAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilContributeAmount.setError("Vui lòng nhập số tiền muốn góp");
            return;
        }
        tilContributeAmount.setError(null);

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            tilContributeAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) {
            tilContributeAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        if (amount > walletBalance) {
            tilContributeAmount.setError("Số dư Ví Koin không đủ (Còn " + df.format(walletBalance) + " ₫)");
            return;
        }
        tilContributeAmount.setError(null);

        String note = etContributeNote.getText() != null ? etContributeNote.getText().toString().trim() : "";
        if (note.isEmpty()) note = "Góp quỹ từ Ví Koin";

        showFullVerificationScreen(true, amount, note);
    }

    private void validateAndProceedWithdraw() {
        String amountStr = etWithdrawAmount.getText() != null ? etWithdrawAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilWithdrawAmount.setError("Vui lòng nhập số tiền muốn rút");
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
            tilWithdrawAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        if (amount > roomBalance) {
            tilWithdrawAmount.setError("Số dư quỹ không đủ (Còn " + df.format(roomBalance) + " ₫)");
            return;
        }
        tilWithdrawAmount.setError(null);

        String note = etWithdrawNote.getText() != null ? etWithdrawNote.getText().toString().trim() : "";
        String category = selectedWithdrawCategory;
        if (category == null || category.trim().isEmpty()) {
            if (!note.isEmpty()) {
                category = note;
            } else {
                category = "Khác";
            }
        }
        selectedWithdrawCategory = category;
        if (note.isEmpty()) {
            note = category.equals("Khác") ? "Rút quỹ về Ví Koin" : category;
        }

        showFullVerificationScreen(false, amount, note);
    }

    private void showFullVerificationScreen(boolean isContribute, double amount, String note) {
        layoutFundActionForm.setVisibility(View.GONE);
        layoutBottomActionDock.setVisibility(View.GONE);
        layoutConfirmStep.setVisibility(View.VISIBLE);
        if (tvFundActionScreenTitle != null) {
            tvFundActionScreenTitle.setText("Xác nhận giao dịch");
        }

        tvConfirmStepAmount.setText(df.format(amount) + " ₫");
        if (tvConfirmStepAmountWords != null) {
            tvConfirmStepAmountWords.setText(VietnameseCurrencyWords.toWordsWithParentheses((long) amount));
        }
        tvConfirmStepType.setText(isContribute ? "Đóng góp quỹ nhóm" : "Rút quỹ về Ví Koin");
        tvConfirmStepSource.setText(isContribute ? "Ví cá nhân Koin" : roomName);
        tvConfirmStepDest.setText(isContribute ? roomName : "Ví cá nhân Koin");
        tvConfirmStepNote.setText(note);

        btnConfirmStepExecute.setOnClickListener(v -> {
            layoutConfirmStep.setVisibility(View.GONE);
            if (isContribute) {
                executeContribute(amount, note);
            } else {
                executeWithdraw(amount, note);
            }
        });
    }

    private void executeContribute(double amount, String note) {
        layoutProcessingNotice.setVisibility(View.VISIBLE);

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        if (userId == null || userId.isEmpty()) userId = RoomRepository.getInstance().getCurrentUserId();

        TransactionRepository remote = new TransactionRepository();
        remote.setAccessToken(session.getAccessToken());
        remote.transferWalletToRoom(userId, roomId, amount, note, new TransactionRepository.DataCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (result != null && result.has("new_wallet_balance") && !result.get("new_wallet_balance").isJsonNull()) {
                        RoomRepository.getInstance().setUserWalletBalance(result.get("new_wallet_balance").getAsDouble());
                    } else {
                        RoomRepository.getInstance().loadUserProfile();
                    }
                    RoomRepository.getInstance().refreshRoomTransactions(roomId, null);

                    String txId = "#SF-" + String.format(Locale.US, "%08X", (int) (System.currentTimeMillis() & 0xFFFFFFFFL));
                    if (result != null && result.has("transaction_id") && !result.get("transaction_id").isJsonNull()) {
                        String rawId = result.get("transaction_id").getAsString();
                        if (rawId != null && rawId.length() >= 8) {
                            txId = "#SF-" + rawId.substring(0, 8).toUpperCase(Locale.US);
                        }
                    }

                    showSuccessNotice("Góp quỹ thành công!", "+" + df.format(amount) + " ₫",
                            txId, "Đóng góp quỹ nhóm", "Ví cá nhân Koin", roomName, note);
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    layoutProcessingNotice.setVisibility(View.GONE);
                    layoutFundActionForm.setVisibility(View.VISIBLE);
                    layoutBottomActionDock.setVisibility(View.VISIBLE);
                    String friendly = AppToaster.sanitizeErrorMessage(errorMessage);
                    tilContributeAmount.setError(friendly);
                    AppToaster.error(requireActivity(), "Góp quỹ thất bại", friendly);
                });
            }
        });
    }

    private void executeWithdraw(double amount, String note) {
        layoutProcessingNotice.setVisibility(View.VISIBLE);

        boolean isOwner = isCurrentUserOwner();
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();
        if (userId == null || userId.isEmpty()) userId = RoomRepository.getInstance().getCurrentUserId();

        TransactionRepository remote = new TransactionRepository();
        remote.setAccessToken(session.getAccessToken());
        remote.submitWithdrawal(userId, userName != null ? userName : "Member", roomId, amount, selectedWithdrawCategory, note, isOwner,
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

                            String txId = "#SF-" + String.format(Locale.US, "%08X", (int) (System.currentTimeMillis() & 0xFFFFFFFFL));
                            if (created != null && created.getId() != null && created.getId().length() >= 8) {
                                txId = "#SF-" + created.getId().substring(0, 8).toUpperCase(Locale.US);
                            }

                            showSuccessNotice(isOwner ? "Rút quỹ thành công!" : "Yêu cầu rút quỹ đã gửi!",
                                    "-" + df.format(amount) + " ₫", txId, "Rút quỹ về Ví Koin", roomName, "Ví cá nhân Koin", note);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            layoutProcessingNotice.setVisibility(View.GONE);
                            layoutFundActionForm.setVisibility(View.VISIBLE);
                            layoutBottomActionDock.setVisibility(View.VISIBLE);
                            tilWithdrawAmount.setError(errorMessage);
                            Toast.makeText(requireContext(), "Rút quỹ thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void showSuccessNotice(String title, String amountFormatted, String txId, String type, String source, String dest, String note) {
        layoutProcessingNotice.setVisibility(View.GONE);
        layoutSuccessNotice.setVisibility(View.VISIBLE);
        if (tvFundActionScreenTitle != null) {
            tvFundActionScreenTitle.setText("Kết quả giao dịch");
        }

        tvSuccessTitle.setText(title);
        tvSuccessAmount.setText(amountFormatted);
        tvSuccessAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvSuccessTxId.setText(txId);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault());
        tvSuccessTime.setText(timeFormat.format(new Date()));

        tvSuccessType.setText(type);
        tvSuccessSource.setText(source);
        tvSuccessDest.setText(dest);
        tvSuccessNote.setText(note);

        final String finalTxId = txId;
        btnSuccessCopyTxId.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("TxID", finalTxId);
            if (clipboard != null) clipboard.setPrimaryClip(clip);
            AppToaster.info(requireActivity(), "Đã sao chép mã: " + finalTxId);
        });

        btnSuccessDone.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    }
}
