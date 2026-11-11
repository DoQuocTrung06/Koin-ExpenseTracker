package com.sharedfund.app.ui.recurring;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
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
            CreateRecurringRuleBottomSheet sheet = CreateRecurringRuleBottomSheet.newInstance(roomId, roomName);
            sheet.setListener(this::handleCreateRule);
            sheet.show(getChildFragmentManager(), "create_recurring_rule_sheet");
        };

        if (btnAddTop != null) {
            if (isManager) {
                btnAddTop.setVisibility(View.VISIBLE);
                btnAddTop.setOnClickListener(openAddDialog);
            } else {
                btnAddTop.setVisibility(View.GONE);
            }
        }
        if (btnCreateFirst != null) {
            if (isManager) {
                btnCreateFirst.setVisibility(View.VISIBLE);
                btnCreateFirst.setOnClickListener(openAddDialog);
            } else {
                btnCreateFirst.setVisibility(View.GONE);
            }
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
        if (room == null) return true;
        String role = room.getCurrentUserRole();
        if ("OWNER".equalsIgnoreCase(role) || "TREASURER".equalsIgnoreCase(role)) {
            return true;
        }
        String myId = UserSessionManager.getInstance(requireContext()).getUserId();
        return room.getCreatedBy() != null && room.getCreatedBy().equalsIgnoreCase(myId);
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
                    } else if (localRules.isEmpty()) {
                        seedSampleRules();
                    }
                    updateUI();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (localRules.isEmpty()) {
                        seedSampleRules();
                    }
                    updateUI();
                });
            }
        });
    }

    private void seedSampleRules() {
        String currentFund = (roomName != null && !roomName.trim().isEmpty()) ? roomName : "Quỹ nhóm";
        RecurringRule rule1 = new RecurringRule();
        rule1.setId("mock_rule_1");
        rule1.setTitle("Tiền Internet & Truyền hình FPT");
        rule1.setAmount(250000);
        rule1.setCategoryName("Tiện ích & Nhà");
        rule1.setFrequency(RecurringRule.FREQUENCY_MONTHLY);
        rule1.setExecutionDay(25);
        rule1.setType("Chi");
        rule1.setActive(true);
        rule1.setRoomId(roomId);
        rule1.setRoomName(currentFund);
        rule1.setScope(roomId != null ? RecurringRule.SCOPE_ROOM : RecurringRule.SCOPE_PERSONAL);

        RecurringRule rule2 = new RecurringRule();
        rule2.setId("mock_rule_2");
        rule2.setTitle("Tiền phòng trọ & quản lý");
        rule2.setAmount(2800000);
        rule2.setCategoryName("Tiền phòng");
        rule2.setFrequency(RecurringRule.FREQUENCY_MONTHLY);
        rule2.setExecutionDay(10);
        rule2.setType("Chi");
        rule2.setActive(true);
        rule2.setRoomId(roomId);
        rule2.setRoomName(currentFund);
        rule2.setScope(roomId != null ? RecurringRule.SCOPE_ROOM : RecurringRule.SCOPE_PERSONAL);

        localRules.add(rule1);
        localRules.add(rule2);
    }

    private void updateUI() {
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
        if (isAdded()) {
            AppToaster.success(requireActivity(), "Đã lưu khoản định kỳ thành công");
        }

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
    public void onEditRule(RecurringRule rule) {
        if (!isCurrentUserFundManager()) {
            return;
        }
        CreateRecurringRuleBottomSheet sheet = CreateRecurringRuleBottomSheet.newInstance(roomId, roomName, rule);
        sheet.setListener(updatedRule -> {
            int index = -1;
            for (int i = 0; i < localRules.size(); i++) {
                if (localRules.get(i).getId() != null && localRules.get(i).getId().equals(updatedRule.getId())) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                localRules.set(index, updatedRule);
            }
            updateUI();
            if (isAdded()) {
                AppToaster.success(requireActivity(), "Đã cập nhật khoản định kỳ");
            }
            transactionRepository.updateRecurringRule(updatedRule, new TransactionRepository.DataCallback<RecurringRule>() {
                @Override
                public void onSuccess(RecurringRule result) {}

                @Override
                public void onError(String error) {}
            });
        });
        sheet.show(getChildFragmentManager(), "edit_recurring_rule_sheet");
    }

    @Override
    public void onPayRule(RecurringRule rule) {
        if (rule == null) return;
        Context context = requireContext();
        UserSessionManager session = UserSessionManager.getInstance(context);

        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setRoomId(rule.getRoomId());
        tx.setUserId(session.getUserId());
        tx.setUserName(session.getFullName());
        tx.setAmount(Math.abs(rule.getAmount()));
        tx.setType(Transaction.TYPE_EXPENSE);
        tx.setCategory(rule.getCategoryName() != null ? rule.getCategoryName() : "Định kỳ");
        tx.setNote(rule.getTitle());
        tx.setStatus("APPROVED");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tx.setCreatedAt(sdf.format(new java.util.Date()));

        java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        rule.setLastRunDate(isoFormat.format(new java.util.Date()));

        updateUI();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        String amountStr = df.format(Math.abs(rule.getAmount())) + " đ";

        if (isAdded()) {
            AppToaster.success(requireActivity(), "Đã xuất quỹ " + amountStr + " cho " + rule.getTitle());
        }

        transactionRepository.createTransaction(tx, new TransactionRepository.DataCallback<Transaction>() {
            @Override
            public void onSuccess(Transaction createdTx) {
                if (roomId != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        RoomRepository.getInstance().refreshRoomTransactions(roomId, null);
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

    @Override
    public void onNudgeRule(RecurringRule rule) {
        if (rule == null || !isAdded()) return;
        AppToaster.success(requireActivity(), "Đã gửi thông báo nhắc đóng " + rule.getTitle());

        AppNotification notif = new AppNotification(
                UUID.randomUUID().toString(),
                AppNotification.TYPE_RECURRING_DUE,
                rule.getRoomName() != null ? rule.getRoomName() : "Lịch định kỳ",
                "Nhắc thanh toán: " + rule.getTitle(),
                "Thành viên " + UserSessionManager.getInstance(requireContext()).getFullName() + " nhắc đóng khoản định kỳ " + rule.getTitle(),
                "Vừa xong",
                false,
                rule.getRoomId(),
                rule.getRoomName()
        );
        NotificationManager.getInstance(requireContext()).addNotification(notif);
    }
}
