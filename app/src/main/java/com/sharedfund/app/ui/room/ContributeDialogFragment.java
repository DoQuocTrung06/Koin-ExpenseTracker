package com.sharedfund.app.ui.room;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;

import java.text.DecimalFormat;

/**
 * BottomSheet đóng tiền vào quỹ từ số dư ví tài khoản cá nhân.
 * Kiểm tra số dư ví hiện có, trừ tiền trong ví và chuyển vào quỹ nhóm.
 */
public class ContributeDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "room_id";

    private String roomId;
    private double currentWalletBalance = 0;

    private TextView tvContributeWalletBalance;
    private MaterialButton btnQuickDepositWallet;
    private TextInputLayout tilContributeAmount;
    private TextInputEditText etContributeAmount;
    private MaterialButton btnQuickContribute50k;
    private MaterialButton btnQuickContribute100k;
    private MaterialButton btnQuickContribute200k;
    private MaterialButton btnQuickContributeAll;
    private TextInputLayout tilContributeNote;
    private TextInputEditText etContributeNote;
    private MaterialButton btnCancelContribute;
    private MaterialButton btnConfirmContribute;

    public interface OnContributeCompleteListener {
        void onContributeComplete(double amount);
    }

    private OnContributeCompleteListener listener;

    public static ContributeDialogFragment newInstance(String roomId) {
        ContributeDialogFragment fragment = new ContributeDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnContributeCompleteListener(OnContributeCompleteListener listener) {
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
        return inflater.inflate(R.layout.dialog_contribute, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        updateWalletDisplay();
        setupQuickButtons();
        setupListeners();
    }

    private void bindViews(View view) {
        tvContributeWalletBalance = view.findViewById(R.id.tvContributeWalletBalance);
        btnQuickDepositWallet = view.findViewById(R.id.btnQuickDepositWallet);
        tilContributeAmount = view.findViewById(R.id.tilContributeAmount);
        etContributeAmount = view.findViewById(R.id.etContributeAmount);
        btnQuickContribute50k = view.findViewById(R.id.btnQuickContribute50k);
        btnQuickContribute100k = view.findViewById(R.id.btnQuickContribute100k);
        btnQuickContribute200k = view.findViewById(R.id.btnQuickContribute200k);
        btnQuickContributeAll = view.findViewById(R.id.btnQuickContributeAll);
        tilContributeNote = view.findViewById(R.id.tilContributeNote);
        etContributeNote = view.findViewById(R.id.etContributeNote);
        btnCancelContribute = view.findViewById(R.id.btnCancelContribute);
        btnConfirmContribute = view.findViewById(R.id.btnConfirmContribute);

        String currentUserName = UserSessionManager.getInstance(requireContext()).getCurrentUserName();
        etContributeNote.setText("Đóng quỹ - " + currentUserName);
    }

    private void updateWalletDisplay() {
        currentWalletBalance = RoomRepository.getInstance().getUserWalletBalance();
        DecimalFormat df = new DecimalFormat("#,###");
        String formatted = df.format(currentWalletBalance).replace(',', '.') + " ₫";
        tvContributeWalletBalance.setText(formatted);
    }

    private void setupQuickButtons() {
        btnQuickContribute50k.setOnClickListener(v -> setAmount(50000));
        btnQuickContribute100k.setOnClickListener(v -> setAmount(100000));
        btnQuickContribute200k.setOnClickListener(v -> setAmount(200000));
        btnQuickContributeAll.setOnClickListener(v -> {
            if (currentWalletBalance > 0) {
                setAmount((long) currentWalletBalance);
            } else {
                Toast.makeText(requireContext(), "Số dư ví hiện tại là 0 ₫", Toast.LENGTH_SHORT).show();
            }
        });

        btnQuickDepositWallet.setOnClickListener(v -> {
            DepositDialogFragment depositDialog = DepositDialogFragment.newWalletInstance();
            depositDialog.setOnDepositCompleteListener(amount -> {
                updateWalletDisplay();
            });
            depositDialog.show(getParentFragmentManager(), "wallet_deposit");
        });
    }

    private void setAmount(long amount) {
        etContributeAmount.setText(String.valueOf(amount));
        tilContributeAmount.setError(null);
    }

    private void setupListeners() {
        etContributeAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilContributeAmount.getError() != null) tilContributeAmount.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCancelContribute.setOnClickListener(v -> dismiss());
        btnConfirmContribute.setOnClickListener(v -> confirmContribution());
    }

    private void confirmContribution() {
        String amountStr = etContributeAmount.getText() != null ? etContributeAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilContributeAmount.setError("Vui lòng nhập số tiền muốn đóng");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            tilContributeAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount < 1000) {
            tilContributeAmount.setError("Số tiền tối thiểu là 1.000 ₫");
            return;
        }

        currentWalletBalance = RoomRepository.getInstance().getUserWalletBalance();
        DecimalFormat df = new DecimalFormat("#,###");
        if (amount > currentWalletBalance) {
            String formattedWallet = df.format(currentWalletBalance).replace(',', '.') + " ₫";
            tilContributeAmount.setError("Số dư ví không đủ (Hiện có: " + formattedWallet + "). Vui lòng nạp thêm!");
            return;
        }

        String note = etContributeNote.getText() != null ? etContributeNote.getText().toString().trim() : "";

        boolean success = RoomRepository.getInstance().contributeToRoom(roomId, amount, note);
        if (success) {
            String formattedAmount = df.format(amount).replace(',', '.') + " ₫";
            Toast.makeText(requireContext(), "Đóng quỹ thành công: " + formattedAmount, Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onContributeComplete(amount);
            }
            dismiss();
        } else {
            Toast.makeText(requireContext(), "Giao dịch thất bại! Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
        }
    }
}
