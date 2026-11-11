package com.sharedfund.app.ui.recurring;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.data.NotificationManager;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.AppNotification;
import com.sharedfund.app.model.RecurringRule;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.widget.AppToaster;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RecurringTransactionsFragment extends Fragment implements RecurringRuleAdapter.OnRecurringActionListener {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_ROOM_NAME = "arg_room_name";

    private String roomId;
    private String roomName;

    private RecyclerView rvRules;
    private View layoutEmpty;
    private TextView tvHeaderTitle;
    private TextView tvMonthlyTotal;
    private TextView tvCountBadge;
    private MaterialButton btnAddTop;
    private MaterialButton btnCreateFirst;
    private RecurringRuleAdapter adapter;
    private final List<RecurringRule> localRules = new ArrayList<>();
    private final TransactionRepository transactionRepository = new TransactionRepository();

    public static RecurringTransactionsFragment newInstance(String roomId, String roomName) {
        RecurringTransactionsFragment fragment = new RecurringTransactionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putString(ARG_ROOM_NAME, roomName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
            roomName = getArguments().getString(ARG_ROOM_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recurring_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btnRecurringBack);
        tvHeaderTitle = view.findViewById(R.id.tvRecurringHeaderTitle);
        tvMonthlyTotal = view.findViewById(R.id.tvRecurringMonthlyTotal);
        tvCountBadge = view.findViewById(R.id.tvRecurringCountBadge);
        btnAddTop = view.findViewById(R.id.btnAddRecurringTop);
        btnCreateFirst = view.findViewById(R.id.btnCreateFirstRecurring);
        rvRules = view.findViewById(R.id.rvRecurringRules);
        layoutEmpty = view.findViewById(R.id.layoutEmptyRecurring);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        tvHeaderTitle.setText("Chi tiêu định kỳ");

        boolean isManager = isCurrentUserFundManager();

        View.OnClickListener openAddDialog = v -> {
            if (!isCurrentUserFundManager()) {
                if (isAdded()) {
                    AppToaster.info(requireActivity(), "Chỉ Chủ quỹ và Phó quỹ mới có quyền tạo khoản chi định kỳ");
                }
                return;
            }
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCreateRecurringRule(roomId, roomName, null);
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, CreateRecurringRuleFragment.newInstance(roomId, roomName, null))
                        .addToBackStack(null)
                        .commit();
            }
        };

        if (btnAddTop != null) {
            btnAddTop.setVisibility(isManager ? View.VISIBLE : View.GONE);
            btnAddTop.setOnClickListener(openAddDialog);
        }
        if (btnCreateFirst != null) {
            btnCreateFirst.setVisibility(isManager ? View.VISIBLE : View.GONE);
            btnCreateFirst.setOnClickListener(openAddDialog);
        }

        adapter = new RecurringRuleAdapter(this);
        adapter.setManager(isManager);
        adapter.setRoomScope(roomId != null && !roomId.trim().isEmpty());
        rvRules.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRules.setAdapter(adapter);

        RoomRepository.getInstance().getRecurringRulesChangedLiveData().observe(getViewLifecycleOwner(), changed -> {
            if (Boolean.TRUE.equals(changed)) {
                loadRules();
            }
        });

        loadRules();
    }

    private boolean isCurrentUserFundManager() {
        if (roomId == null || roomId.trim().isEmpty()) {
            return true;
        }
        Room room = RoomRepository.getInstance().getRoomSync(roomId);
        if (room == null) return false;
        String role = room.getCurrentUserRole();
        if ("OWNER".equalsIgnoreCase(role) || "TREASURER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return true;
        }
        String myId = UserSessionManager.getInstance(requireContext()).getUserId();
        if (myId != null && !myId.isEmpty()) {
            if (myId.equalsIgnoreCase(room.getCreatedBy())) return true;
            if (room.getTreasurerIds() != null && room.getTreasurerIds().contains(myId)) return true;
        }
        return RoomRepository.getInstance().isCurrentUserAdmin(roomId);
    }

    private void loadRules() {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String currentUserId = session.getUserId();
        if (session.getAccessToken() != null && !session.getAccessToken().trim().isEmpty()) {
            transactionRepository.setAccessToken(session.getAccessToken());
        }

        transactionRepository.getRecurringRules(currentUserId, roomId, new TransactionRepository.DataCallback<List<RecurringRule>>() {
            @Override
            public void onSuccess(List<RecurringRule> rules) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    localRules.clear();
                    if (rules != null && !rules.isEmpty()) {
                        localRules.addAll(rules);
                    }
                    updateUI();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    updateUI();
                });
            }
        });
    }

    private void updateUI() {
        boolean isManager = isCurrentUserFundManager();
        if (btnAddTop != null) {
            btnAddTop.setVisibility(isManager ? View.VISIBLE : View.GONE);
        }
        if (btnCreateFirst != null) {
            btnCreateFirst.setVisibility(isManager ? View.VISIBLE : View.GONE);
        }
        if (adapter != null) {
            adapter.setManager(isManager);
        }

        adapter.submitList(localRules);
        boolean isEmpty = localRules.isEmpty();
        if (layoutEmpty != null) layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (rvRules != null) rvRules.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        long total = 0;
        int activeCount = 0;
        for (RecurringRule rule : localRules) {
            if (rule.isActive()) {
                total += (long) Math.abs(rule.getAmount());
                activeCount++;
            }
        }

        if (tvMonthlyTotal != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
            symbols.setGroupingSeparator('.');
            DecimalFormat df = new DecimalFormat("#,###", symbols);
            tvMonthlyTotal.setText(df.format(total) + " đ");
        }

        if (tvCountBadge != null) {
            tvCountBadge.setText(localRules.size() + " khoản");
        }
    }

    private void handleCreateRule(RecurringRule rule) {
        localRules.add(0, rule);
        updateUI();

        transactionRepository.createRecurringRule(rule, new TransactionRepository.DataCallback<RecurringRule>() {
            @Override
            public void onSuccess(RecurringRule created) {
                if (created != null && created.getId() != null) {
                    rule.setId(created.getId());
                }
            }

            @Override
            public void onError(String error) {}
        });
    }

    @Override
    public void onToggleRule(RecurringRule rule, boolean isActive) {
        if (!isCurrentUserFundManager()) {
            if (isAdded()) {
                AppToaster.info(requireActivity(), "Chỉ Chủ quỹ và Phó quỹ mới có quyền bật/tắt khoản định kỳ");
            }
            updateUI();
            return;
        }
        updateUI();
        transactionRepository.toggleRecurringRule(rule.getId(), isActive, new TransactionRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {}

            @Override
            public void onError(String error) {}
        });
    }

    @Override
    public void onDeleteRule(RecurringRule rule) {
        if (!isCurrentUserFundManager()) {
            if (isAdded()) {
                AppToaster.info(requireActivity(), "Chỉ Chủ quỹ và Phó quỹ mới có quyền xóa khoản định kỳ");
            }
            return;
        }
        localRules.remove(rule);
        updateUI();

        transactionRepository.deleteRecurringRule(rule.getId(), new TransactionRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> AppToaster.success(requireActivity(), "Đã xóa khoản định kỳ"));
                }
            }

            @Override
            public void onError(String error) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRules();
    }

    @Override
    public void onEditRule(RecurringRule rule) {
        if (!isCurrentUserFundManager()) {
            if (isAdded()) {
                AppToaster.info(requireActivity(), "Chỉ Chủ quỹ và Phó quỹ mới có quyền chỉnh sửa khoản chi định kỳ");
            }
            return;
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openCreateRecurringRule(roomId, roomName, rule);
        } else {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, CreateRecurringRuleFragment.newInstance(roomId, roomName, rule))
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onPayRule(RecurringRule rule) {
        if (rule == null) return;
        if (!isCurrentUserFundManager()) {
            if (isAdded()) {
                AppToaster.info(requireActivity(), "Chỉ Chủ quỹ và Phó quỹ mới có quyền xác nhận chi");
            }
            return;
        }
        Context context = requireContext();
        UserSessionManager session = UserSessionManager.getInstance(context);

        String targetRoomId = (rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) ? rule.getRoomId() : roomId;
        Room room = targetRoomId != null ? RoomRepository.getInstance().getRoomSync(targetRoomId) : null;
        double roomBalance = (room != null) ? room.getBalance() : 0.0;
        double ruleAmount = Math.abs(rule.getAmount());

        if (roomBalance < ruleAmount) {
            showInsufficientBalanceDialog(rule, roomBalance, ruleAmount);
            return;
        }

        double newBalance = roomBalance - ruleAmount;
        if (room != null) {
            room.setBalance(newBalance);
        }

        Transaction tx = new Transaction();
        String txId = UUID.randomUUID().toString();
        tx.setId(txId);
        tx.setRoomId(targetRoomId);
        tx.setUserId(session.getUserId());
        tx.setUserName(session.getFullName());
        tx.setAmount(ruleAmount);
        tx.setType(Transaction.TYPE_EXPENSE);
        tx.setCategory(rule.getCategoryName() != null ? rule.getCategoryName() : "Định kỳ");
        tx.setNote(rule.getTitle());
        tx.setStatus("APPROVED");
        tx.setBalanceAfter(newBalance);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(new java.util.Date());
        tx.setCreatedAt(formattedTime);

        java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        rule.setLastRunDate(isoFormat.format(new java.util.Date()));

        updateUI();

        showPaymentSuccessDialog(rule, tx, newBalance, formattedTime);

        transactionRepository.createTransaction(tx, new TransactionRepository.DataCallback<Transaction>() {
            @Override
            public void onSuccess(Transaction createdTx) {
                if (targetRoomId != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        RoomRepository.getInstance().refreshRoomTransactions(targetRoomId, null);
                    });
                }
            }

            @Override
            public void onError(String error) {}
        });

        transactionRepository.updateRecurringRule(rule, new TransactionRepository.DataCallback<RecurringRule>() {
            @Override
            public void onSuccess(RecurringRule updated) {}

            @Override
            public void onError(String error) {}
        });
    }

    private void showPaymentSuccessDialog(RecurringRule rule, Transaction tx, double newBalance, String formattedTime) {
        if (!isAdded()) return;
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recurring_payment_success);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);

        TextView tvAmount = dialog.findViewById(R.id.tvPaymentSuccessAmount);
        TextView tvTxId = dialog.findViewById(R.id.tvPaymentSuccessTxId);
        TextView tvRuleName = dialog.findViewById(R.id.tvPaymentSuccessRuleName);
        TextView tvRoomName = dialog.findViewById(R.id.tvPaymentSuccessRoomName);
        TextView tvUserName = dialog.findViewById(R.id.tvPaymentSuccessUserName);
        TextView tvTime = dialog.findViewById(R.id.tvPaymentSuccessTime);
        TextView tvBalanceAfter = dialog.findViewById(R.id.tvPaymentSuccessBalanceAfter);
        View btnCopy = dialog.findViewById(R.id.btnPaymentSuccessCopy);
        View btnDone = dialog.findViewById(R.id.btnPaymentSuccessDone);

        String displayAmount = "- " + df.format(Math.abs(rule.getAmount())) + " đ";
        if (tvAmount != null) tvAmount.setText(displayAmount);

        String shortTxId = "#RC-" + (tx.getId().length() >= 8 ? tx.getId().substring(0, 8).toUpperCase() : tx.getId().toUpperCase());
        if (tvTxId != null) tvTxId.setText(shortTxId);

        if (tvRuleName != null) tvRuleName.setText(rule.getTitle());

        String displayRoomName = (roomName != null && !roomName.trim().isEmpty()) ? roomName : (rule.getRoomName() != null ? rule.getRoomName() : "Quỹ nhóm");
        if (tvRoomName != null) tvRoomName.setText(displayRoomName);

        if (tvUserName != null) tvUserName.setText(tx.getUserName());
        if (tvTime != null) tvTime.setText(formattedTime);
        if (tvBalanceAfter != null) tvBalanceAfter.setText(df.format(newBalance) + " đ");

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Transaction ID", shortTxId);
                    clipboard.setPrimaryClip(clip);
                    AppToaster.success(requireActivity(), "Đã sao chép mã giao dịch");
                }
            });
        }

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showInsufficientBalanceDialog(RecurringRule rule, double currentBalance, double ruleAmount) {
        if (!isAdded()) return;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);

        double missing = ruleAmount - currentBalance;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_koin_notice);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvNoticeDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvNoticeDialogMessage);
        View btnClose = dialog.findViewById(R.id.btnNoticeDialogClose);

        if (tvTitle != null) tvTitle.setText("Số dư quỹ không đủ");
        if (tvMsg != null) {
            tvMsg.setText("Số dư hiện tại của quỹ là " + df.format(currentBalance) + " đ, không đủ để chi " + df.format(ruleAmount) + " đ cho khoản [" + rule.getTitle() + "].\n\nQuỹ còn thiếu: " + df.format(missing) + " đ. Vui lòng yêu cầu thành viên nạp thêm vào quỹ trước khi thanh toán.");
        }
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    @Override
    public void onNudgeRule(RecurringRule rule) {
        if (rule == null || !isAdded()) return;
        Context context = requireContext();
        android.content.SharedPreferences prefs = context.getSharedPreferences("recurring_nudge_prefs", Context.MODE_PRIVATE);
        long lastNudge = prefs.getLong("nudge_cooldown_" + rule.getId(), 0);
        long now = System.currentTimeMillis();
        long diff = now - lastNudge;
        long cooldownMs = 60 * 60 * 1000L;
        if (diff < cooldownMs) {
            long remainingMinutes = (cooldownMs - diff + 59999) / 60000;
            AppToaster.info(requireActivity(), "Bạn vừa gửi nhắc nhở rồi. Vui lòng chờ " + remainingMinutes + " phút nữa nhé!");
            return;
        }

        prefs.edit().putLong("nudge_cooldown_" + rule.getId(), now).apply();
        AppToaster.success(requireActivity(), "Đã gửi thông báo nhắc đóng " + rule.getTitle());

        AppNotification notif = new AppNotification(
                UUID.randomUUID().toString(),
                AppNotification.TYPE_RECURRING_DUE,
                rule.getRoomName() != null ? rule.getRoomName() : "Lịch định kỳ",
                "Nhắc thanh toán: " + rule.getTitle(),
                "Thành viên " + UserSessionManager.getInstance(context).getFullName() + " nhắc đóng khoản định kỳ " + rule.getTitle(),
                "Vừa xong",
                false,
                rule.getRoomId(),
                rule.getRoomName()
        );
        String targetRoom = (rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) ? rule.getRoomId() : roomId;
        Room r = targetRoom != null ? RoomRepository.getInstance().getRoomSync(targetRoom) : null;
        String ownerId = (r != null) ? r.getCreatedBy() : null;
        if (ownerId != null && !ownerId.trim().isEmpty()) {
            NotificationManager.getInstance(context).sendNotificationToUser(notif, ownerId);
        } else {
            NotificationManager.getInstance(context).addNotification(notif);
        }
        updateUI();
    }
}
