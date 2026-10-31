package com.sharedfund.app.ui.room;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class FundActionBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_ROOM_BALANCE = "arg_room_balance";

    public interface OnFundActionListener {
        void onActionSuccess();
    }

    private String roomId;
    private double roomBalance;
    private double walletBalance;
    private OnFundActionListener listener;

    private TextView tvFundActionRoomBalance;
    private TextView tvWalletAvailableBalance;
    private TextView tvWithdrawRoleHint;
    private MaterialButton btnQuickDepositWallet;
    private MaterialButton btnDepositRoomViaSePayQr;
    private TabLayout tabLayoutFundAction;
    private LinearLayout layoutContributeTab;
    private LinearLayout layoutWithdrawTab;

    private MaterialButton btnQuickContribute50k;
    private MaterialButton btnQuickContribute100k;
    private MaterialButton btnQuickContribute200k;
    private MaterialButton btnQuickContributeAll;
    private TextInputLayout tilContributeAmount;
    private TextInputEditText etContributeAmount;
    private TextInputLayout tilContributeNote;
    private TextInputEditText etContributeNote;
    private MaterialButton btnConfirmContribute;

    private MaterialButton btnQuickWithdraw50k;
    private MaterialButton btnQuickWithdraw100k;
    private MaterialButton btnQuickWithdraw200k;
    private MaterialButton btnQuickWithdrawAll;
    private TextInputLayout tilWithdrawAmount;
    private TextInputEditText etWithdrawAmount;
    private TextInputLayout tilWithdrawNote;
    private TextInputEditText etWithdrawNote;
    private MaterialButton btnConfirmWithdraw;

    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    private DecimalFormat df;

    public static FundActionBottomSheet newInstance(String roomId, double roomBalance) {
        FundActionBottomSheet sheet = new FundActionBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putDouble(ARG_ROOM_BALANCE, roomBalance);
        sheet.setArguments(args);
        return sheet;
    }

    public static FundActionBottomSheet newInstance(double roomBalance) {
        return newInstance("", roomBalance);
    }

    public void setListener(OnFundActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_fund_action, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabs();
        setupListeners();

        RoomRepository.getInstance().getUserWalletLiveData().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                walletBalance = balance;
                if (tvWalletAvailableBalance != null) {
                    tvWalletAvailableBalance.setText("Số dư Ví Koin khả dụng: " + df.format(walletBalance) + " ₫");
                }
            }
        });
    }

    private void initViews(View view) {
        tvFundActionRoomBalance = view.findViewById(R.id.tvFundActionRoomBalance);
        tvWalletAvailableBalance = view.findViewById(R.id.tvWalletAvailableBalance);
        tvWithdrawRoleHint = view.findViewById(R.id.tvWithdrawRoleHint);
        btnQuickDepositWallet = view.findViewById(R.id.btnQuickDepositWallet);
        btnDepositRoomViaSePayQr = view.findViewById(R.id.btnDepositRoomViaSePayQr);
        tabLayoutFundAction = view.findViewById(R.id.tabLayoutFundAction);
        layoutContributeTab = view.findViewById(R.id.layoutContributeTab);
        layoutWithdrawTab = view.findViewById(R.id.layoutWithdrawTab);

        btnQuickContribute50k = view.findViewById(R.id.btnQuickContribute50k);
        btnQuickContribute100k = view.findViewById(R.id.btnQuickContribute100k);
        btnQuickContribute200k = view.findViewById(R.id.btnQuickContribute200k);
        btnQuickContributeAll = view.findViewById(R.id.btnQuickContributeAll);
        tilContributeAmount = view.findViewById(R.id.tilContributeAmount);
        etContributeAmount = view.findViewById(R.id.etContributeAmount);
        tilContributeNote = view.findViewById(R.id.tilContributeNote);
        etContributeNote = view.findViewById(R.id.etContributeNote);
        btnConfirmContribute = view.findViewById(R.id.btnConfirmContribute);

        btnQuickWithdraw50k = view.findViewById(R.id.btnQuickWithdraw50k);
        btnQuickWithdraw100k = view.findViewById(R.id.btnQuickWithdraw100k);
        btnQuickWithdraw200k = view.findViewById(R.id.btnQuickWithdraw200k);
        btnQuickWithdrawAll = view.findViewById(R.id.btnQuickWithdrawAll);
        tilWithdrawAmount = view.findViewById(R.id.tilWithdrawAmount);
        etWithdrawAmount = view.findViewById(R.id.etWithdrawAmount);
        tilWithdrawNote = view.findViewById(R.id.tilWithdrawNote);
        etWithdrawNote = view.findViewById(R.id.etWithdrawNote);
        btnConfirmWithdraw = view.findViewById(R.id.btnConfirmWithdraw);

        walletBalance = RoomRepository.getInstance().getUserWalletBalance();
        tvFundActionRoomBalance.setText("Số dư quỹ hiện tại: " + df.format(roomBalance) + " ₫");
        tvWalletAvailableBalance.setText("Số dư Ví Koin khả dụng: " + df.format(walletBalance) + " ₫");

        String currentUserName = UserSessionManager.getInstance(requireContext()).getCurrentUserName();
        if (currentUserName != null && !currentUserName.trim().isEmpty()) {
            etContributeNote.setText("Đóng quỹ - " + currentUserName);
        }

        boolean isOwner = isCurrentUserOwner();
        if (tvWithdrawRoleHint != null) {
            tvWithdrawRoleHint.setText(isOwner
                    ? "Trưởng quỹ: Rút tiền từ quỹ chuyển thẳng về ví cá nhân Koin"
                    : "Thành viên: Gửi yêu cầu rút tiền về ví (Chờ Trưởng quỹ phê duyệt)");
        }
        if (btnConfirmWithdraw != null) {
            btnConfirmWithdraw.setText(isOwner
                    ? "Xác nhận rút về Ví Koin"
                    : "Gửi yêu cầu rút về Ví (Chờ duyệt)");
        }
    }

    private boolean isCurrentUserOwner() {
        Room room = RoomRepository.getInstance().getRoomSync(roomId);
        if (room == null) return false;
        String role = room.getCurrentUserRole();
        if (role != null) return "ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role);
        String userId = UserSessionManager.getInstance(requireContext()).getUserId();
        return userId != null && userId.equals(room.getCreatedBy());
    }

    private void setupTabs() {
        tabLayoutFundAction.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                layoutContributeTab.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
                layoutWithdrawTab.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {
        btnQuickContribute50k.setOnClickListener(v -> { etContributeAmount.setText("50000"); tilContributeAmount.setError(null); });
        btnQuickContribute100k.setOnClickListener(v -> { etContributeAmount.setText("100000"); tilContributeAmount.setError(null); });
        btnQuickContribute200k.setOnClickListener(v -> { etContributeAmount.setText("200000"); tilContributeAmount.setError(null); });
        btnQuickContributeAll.setOnClickListener(v -> {
            long maxWhole = (long) Math.floor(walletBalance);
            if (maxWhole > 0) {
                etContributeAmount.setText(String.valueOf(maxWhole));
                tilContributeAmount.setError(null);
            } else {
                Toast.makeText(requireContext(), "Số dư ví hiện tại là 0 ₫. Vui lòng nạp thêm vào ví!", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnQuickDepositWallet != null) {
            btnQuickDepositWallet.setOnClickListener(v -> {
                DepositDialogFragment depositDialog = DepositDialogFragment.newWalletInstance();
                depositDialog.setOnDepositCompleteListener(amount -> {
                    RoomRepository.getInstance().loadUserProfile();
                });
                depositDialog.show(getParentFragmentManager(), "wallet_deposit_from_fund");
            });
        }

        if (btnDepositRoomViaSePayQr != null) {
            btnDepositRoomViaSePayQr.setOnClickListener(v -> {
                DepositDialogFragment roomDepositDialog = DepositDialogFragment.newInstance(roomId);
                roomDepositDialog.setOnDepositCompleteListener(amount -> {
                    if (listener != null) listener.onActionSuccess();
                    dismiss();
                });
                roomDepositDialog.show(getParentFragmentManager(), "room_sepay_deposit");
            });
        }

        btnQuickWithdraw50k.setOnClickListener(v -> { etWithdrawAmount.setText("50000"); tilWithdrawAmount.setError(null); });
        btnQuickWithdraw100k.setOnClickListener(v -> { etWithdrawAmount.setText("100000"); tilWithdrawAmount.setError(null); });
        btnQuickWithdraw200k.setOnClickListener(v -> { etWithdrawAmount.setText("200000"); tilWithdrawAmount.setError(null); });
        btnQuickWithdrawAll.setOnClickListener(v -> {
            long maxWhole = (long) Math.floor(roomBalance);
            if (maxWhole > 0) {
                etWithdrawAmount.setText(String.valueOf(maxWhole));
                tilWithdrawAmount.setError(null);
            }
        });

        btnConfirmContribute.setOnClickListener(v -> processContribute());
        btnConfirmWithdraw.setOnClickListener(v -> processWithdraw());
    }

    private void processContribute() {
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

        if (amount < 1000) {
            tilContributeAmount.setError("Số tiền tối thiểu là 1.000 ₫");
            return;
        }

        if (amount > walletBalance) {
            tilContributeAmount.setError("Số dư Ví Koin không đủ (Còn " + df.format(walletBalance) + " ₫). Hãy bấm '+ Nạp ví nhanh' hoặc góp trực tiếp bằng mã VietQR Ngân hàng!");
            return;
        }

        String note = etContributeNote.getText() != null ? etContributeNote.getText().toString().trim() : "";
        if (note.isEmpty()) note = "Đóng quỹ nhóm";

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();

        if (userId == null || userId.isEmpty() || userId.startsWith("user-") || (roomId != null && roomId.startsWith("room-"))) {
            Transaction tx = new Transaction();
            tx.setRoomId(roomId);
            tx.setUserId(userId);
            tx.setCreatedBy(userName != null ? userName : "Bạn");
            tx.setType(Transaction.TYPE_INCOME);
            tx.setAmount(amount);
            tx.setCategory("Đóng quỹ");
            tx.setNote(note);
            tx.setStatus("APPROVED");
            RoomRepository.getInstance().addTransaction(tx);
            RoomRepository.getInstance().setUserWalletBalance(walletBalance - amount);
            Toast.makeText(requireContext(), "Đã góp " + df.format(amount) + " ₫ vào quỹ từ Ví Koin!", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onActionSuccess();
            dismiss();
            return;
        }

        btnConfirmContribute.setEnabled(false);
        btnConfirmContribute.setText("Đang xử lý...");

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
                    RoomRepository.getInstance().refreshRoomTransactions(roomId, new RoomRepository.DataCallback<List<Transaction>>() {
                        @Override
                        public void onSuccess(List<Transaction> rows) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "Đã góp " + df.format(amount) + " ₫ vào quỹ thành công!", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onActionSuccess();
                                dismiss();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "Đã góp " + df.format(amount) + " ₫ vào quỹ thành công!", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onActionSuccess();
                                dismiss();
                            });
                        }
                    });
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    btnConfirmContribute.setEnabled(true);
                    btnConfirmContribute.setText("Xác nhận góp từ Ví Koin");
                    tilContributeAmount.setError(errorMessage);
                    Toast.makeText(requireContext(), "Góp quỹ thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void processWithdraw() {
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

        boolean isOwner = isCurrentUserOwner();
        if (amount > roomBalance) {
            tilWithdrawAmount.setError("Số dư quỹ không đủ (Còn " + df.format(roomBalance) + " ₫)");
            return;
        }

        String note = etWithdrawNote.getText() != null ? etWithdrawNote.getText().toString().trim() : "";
        if (note.isEmpty()) note = "Rút tiền về ví Koin";

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String userId = session.getUserId();
        String userName = session.getCurrentUserName();

        if (userId == null || userId.isEmpty() || userId.startsWith("user-") || (roomId != null && roomId.startsWith("room-"))) {
            Transaction tx = new Transaction();
            tx.setRoomId(roomId);
            tx.setUserId(userId);
            tx.setCreatedBy(userName != null ? userName : "Bạn");
            tx.setType(Transaction.TYPE_EXPENSE);
            tx.setAmount(amount);
            tx.setCategory("Rút về ví");
            tx.setNote(note);
            tx.setStatus(isOwner ? "APPROVED" : "PENDING");
            RoomRepository.getInstance().addTransaction(tx);
            if (isOwner) {
                RoomRepository.getInstance().setUserWalletBalance(walletBalance + amount);
            }
            Toast.makeText(requireContext(), isOwner
                    ? "Đã rút " + df.format(amount) + " ₫ về Ví Koin thành công!"
                    : "Đã gửi yêu cầu rút quỹ. Vui lòng chờ Trưởng quỹ duyệt!", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onActionSuccess();
            dismiss();
            return;
        }

        btnConfirmWithdraw.setEnabled(false);
        btnConfirmWithdraw.setText("Đang xử lý...");

        TransactionRepository remote = new TransactionRepository();
        remote.setAccessToken(session.getAccessToken());
        remote.submitWithdrawal(userId, userName != null ? userName : "Member", roomId, amount, "Rút về ví", note, isOwner,
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
                            RoomRepository.getInstance().refreshRoomTransactions(roomId, new RoomRepository.DataCallback<List<Transaction>>() {
                                @Override
                                public void onSuccess(List<Transaction> rows) {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() -> {
                                        if (!isAdded()) return;
                                        Toast.makeText(requireContext(), isOwner
                                                ? "Đã rút " + df.format(amount) + " ₫ về Ví Koin thành công!"
                                                : "Đã gửi yêu cầu rút quỹ. Vui lòng chờ Trưởng quỹ phê duyệt!", Toast.LENGTH_SHORT).show();
                                        if (listener != null) listener.onActionSuccess();
                                        dismiss();
                                    });
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() -> {
                                        if (!isAdded()) return;
                                        Toast.makeText(requireContext(), isOwner
                                                ? "Đã rút " + df.format(amount) + " ₫ về Ví Koin thành công!"
                                                : "Đã gửi yêu cầu rút quỹ. Vui lòng chờ Trưởng quỹ phê duyệt!", Toast.LENGTH_SHORT).show();
                                        if (listener != null) listener.onActionSuccess();
                                        dismiss();
                                    });
                                }
                            });
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            btnConfirmWithdraw.setEnabled(true);
                            btnConfirmWithdraw.setText(isOwner
                                    ? "Xác nhận rút về Ví Koin"
                                    : "Gửi yêu cầu rút về Ví (Chờ duyệt)");
                            tilWithdrawAmount.setError(errorMessage);
                            Toast.makeText(requireContext(), "Rút tiền thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }
}
