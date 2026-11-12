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
import com.sharedfund.app.ui.transaction.TransactionAdapter;
import com.sharedfund.app.ui.widget.AppToaster;
import com.sharedfund.app.util.ErrorMessageHelper;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final Map<String, Long> cloudNudgeTimestamps = new HashMap<>();
    private final Set<String> pendingRuleKeys = new HashSet<>();
    private final AtomicBoolean isSubmittingPayment = new AtomicBoolean(false);
    private static final java.util.Set<String> overdueNotifiedSet = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private View layoutMainContent;
    private View layoutProcessing;
    private View layoutSuccess;
    private ImageButton btnProcessingBack;
    private ImageButton btnSuccessBack;
    private TextView tvSuccessTitle;
    private TextView tvSuccessAmount;
    private TextView tvSuccessSubtitle;
    private TextView tvSuccessTxId;
    private View btnSuccessCopyTxId;
    private TextView tvSuccessRuleName;
    private TextView tvSuccessRoomName;
    private TextView tvSuccessUserName;
    private TextView tvSuccessTime;
    private View dividerSuccessBalance;
    private View layoutSuccessBalanceRow;
    private TextView tvSuccessBalance;
    private MaterialButton btnSuccessDone;

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

        layoutMainContent = view.findViewById(R.id.layoutRecurringMainContent);
        layoutProcessing = view.findViewById(R.id.layoutRecurringProcessing);
        layoutSuccess = view.findViewById(R.id.layoutRecurringSuccess);
        btnProcessingBack = view.findViewById(R.id.btnRecurringProcessingBack);
        btnSuccessBack = view.findViewById(R.id.btnRecurringSuccessBack);
        tvSuccessTitle = view.findViewById(R.id.tvRecurringSuccessTitle);
        tvSuccessAmount = view.findViewById(R.id.tvRecurringSuccessAmount);
        tvSuccessSubtitle = view.findViewById(R.id.tvRecurringSuccessSubtitle);
        tvSuccessTxId = view.findViewById(R.id.tvRecurringSuccessTxId);
        btnSuccessCopyTxId = view.findViewById(R.id.btnRecurringSuccessCopyTxId);
        tvSuccessRuleName = view.findViewById(R.id.tvRecurringSuccessRuleName);
        tvSuccessRoomName = view.findViewById(R.id.tvRecurringSuccessRoomName);
        tvSuccessUserName = view.findViewById(R.id.tvRecurringSuccessUserName);
        tvSuccessTime = view.findViewById(R.id.tvRecurringSuccessTime);
        dividerSuccessBalance = view.findViewById(R.id.dividerRecurringSuccessBalance);
        layoutSuccessBalanceRow = view.findViewById(R.id.layoutRecurringSuccessBalanceRow);
        tvSuccessBalance = view.findViewById(R.id.tvRecurringSuccessBalance);
        btnSuccessDone = view.findViewById(R.id.btnRecurringSuccessDone);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutSuccess != null && layoutSuccess.getVisibility() == View.VISIBLE) {
                    layoutSuccess.setVisibility(View.GONE);
                    if (layoutMainContent != null) layoutMainContent.setVisibility(View.VISIBLE);
                    refreshPendingRuleKeys();
                    loadRules();
                    return;
                }
                if (layoutProcessing != null && layoutProcessing.getVisibility() == View.VISIBLE) {
                    return;
                }
                setEnabled(false);
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

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
        adapter.setScopeRoomId(roomId);
        rvRules.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRules.setAdapter(adapter);

        RoomRepository.getInstance().getRecurringRulesChangedLiveData().observe(getViewLifecycleOwner(), changed -> {
            if (Boolean.TRUE.equals(changed)) {
                loadRules();
                refreshPendingRuleKeys();
            }
        });

        RoomRepository.getInstance().getRoomsLiveData().observe(getViewLifecycleOwner(), rooms -> {
            refreshCloudNudgeState();
            updateManagerControls();
        });

        RoomRepository.getInstance().getTransactionsLiveData().observe(getViewLifecycleOwner(), transactions -> {
            Set<String> prevPending = new HashSet<>(pendingRuleKeys);
            refreshPendingRuleKeys();
            if (!prevPending.isEmpty() && !prevPending.equals(pendingRuleKeys)) {
                loadRules();
            }
        });

        refreshCloudNudgeState();
        refreshPendingRuleKeys();
        fetchRoomNudgeTimestamps();
        loadRules();
    }

    private void updateManagerControls() {
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
    }

    private boolean isCurrentUserFundManager() {
        if (roomId == null || roomId.trim().isEmpty()) {
            return true;
        }
        String myId = getContext() != null ? UserSessionManager.getInstance(requireContext()).getUserId() : "";
        RoomRepository repo = RoomRepository.getInstance();
        return repo.isUserRoomOwner(roomId, myId)
                || repo.isUserRoomTreasurer(roomId, myId)
                || repo.isCurrentUserAdmin(roomId);
    }

    private boolean isManagerForRule(RecurringRule rule) {
        if (rule != null && rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) {
            String targetRoomId = rule.getRoomId().trim();
            String myId = getContext() != null ? UserSessionManager.getInstance(requireContext()).getUserId() : "";
            RoomRepository repo = RoomRepository.getInstance();
            return repo.isUserRoomOwner(targetRoomId, myId)
                    || repo.isUserRoomTreasurer(targetRoomId, myId)
                    || repo.isCurrentUserAdmin(targetRoomId);
        }
        return isCurrentUserFundManager();
    }

    private boolean isCurrentUserOwnerForRoom(String targetRoomId) {
        if (targetRoomId == null || targetRoomId.trim().isEmpty()) {
            return true;
        }
        String myId = getContext() != null ? UserSessionManager.getInstance(requireContext()).getUserId() : "";
        if (myId == null || myId.trim().isEmpty()) {
            myId = RoomRepository.getInstance().getCurrentUserId();
        }
        return RoomRepository.getInstance().isUserRoomOwner(targetRoomId.trim(), myId);
    }

    private Map<String, Long> buildMergedNudgeMap() {
        Map<String, Long> merged = new HashMap<>(cloudNudgeTimestamps);
        if (roomId != null && !roomId.trim().isEmpty()) {
            Room room = RoomRepository.getInstance().getRoomSync(roomId.trim());
            if (room != null) {
                mergeRoomNudgeMap(merged, room);
            }
        } else {
            List<Room> rooms = RoomRepository.getInstance().getUserRooms();
            if (rooms != null) {
                for (Room room : rooms) {
                    if (room != null) mergeRoomNudgeMap(merged, room);
                }
            }
        }
        return merged;
    }

    private void mergeRoomNudgeMap(Map<String, Long> target, Room room) {
        Map<String, Long> roomNudges = room.getRecurringNudgeTimestamps();
        if (roomNudges != null) {
            for (Map.Entry<String, Long> e : roomNudges.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                Long cur = target.get(e.getKey());
                if (cur == null || e.getValue() > cur) {
                    target.put(e.getKey(), e.getValue());
                }
            }
        }
    }

    private void refreshCloudNudgeState() {
        if (adapter != null) {
            adapter.setNudgeTimestamps(buildMergedNudgeMap());
        }
    }

    private void fetchRoomNudgeTimestamps() {
        if (!isAdded() || getContext() == null) return;
        Context appCtx = requireContext().getApplicationContext();
        Set<String> targetRooms = new HashSet<>();
        if (roomId != null && !roomId.trim().isEmpty()) {
            targetRooms.add(roomId.trim());
        } else {
            for (RecurringRule r : localRules) {
                if (r != null && r.getRoomId() != null && !r.getRoomId().trim().isEmpty()) {
                    targetRooms.add(r.getRoomId().trim());
                }
            }
        }
        for (String rId : targetRooms) {
            NotificationManager.getInstance(appCtx).fetchRoomRecurringNudgeTimestamps(rId, map -> {
                if (!isAdded() || map == null) return;
                for (Map.Entry<String, Long> entry : map.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) continue;
                    String scopedKey = rId + "|" + entry.getKey();
                    Long existing = cloudNudgeTimestamps.get(scopedKey);
                    if (existing == null || entry.getValue() > existing) {
                        cloudNudgeTimestamps.put(scopedKey, entry.getValue());
                    }
                }
                refreshCloudNudgeState();
            });
        }
    }

    private void refreshPendingRuleKeys() {
        pendingRuleKeys.clear();
        List<Transaction> sourceTxs;
        if (roomId != null && !roomId.trim().isEmpty()) {
            sourceTxs = RoomRepository.getInstance().getTransactionsForRoom(roomId.trim());
        } else {
            List<Transaction> all = RoomRepository.getInstance().getTransactionsLiveData().getValue();
            sourceTxs = all != null ? all : new ArrayList<>();
        }
        if (sourceTxs != null) {
            for (Transaction tx : sourceTxs) {
                if (tx == null || !tx.isPending() || !tx.isExpense()) continue;
                if (tx.getRecurringRuleId() != null && !tx.getRecurringRuleId().trim().isEmpty()) {
                    pendingRuleKeys.add(tx.getRecurringRuleId().trim());
                }
                if (tx.isRecurring()) {
                    String rId = tx.getRoomId() != null ? tx.getRoomId().trim() : "";
                    String noteKey = tx.getNote() != null ? tx.getNote().trim().toLowerCase(Locale.ROOT) : "";
                    long amtKey = (long) Math.abs(tx.getAmount());
                    if (!noteKey.isEmpty()) {
                        pendingRuleKeys.add(rId + "|" + noteKey + "|" + amtKey);
                    }
                }
            }
        }
        if (adapter != null) {
            adapter.setPendingRuleKeys(pendingRuleKeys);
        }
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
                    refreshPendingRuleKeys();
                    refreshCloudNudgeState();
                    fetchRoomNudgeTimestamps();
                    checkAndDispatchOverdueNotifications(rules);
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
        if (!isManagerForRule(rule)) {
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
        if (!isManagerForRule(rule)) {
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
        refreshCloudNudgeState();
        refreshPendingRuleKeys();
        fetchRoomNudgeTimestamps();
        loadRules();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isSubmittingPayment.set(false);
        uiHandler.removeCallbacksAndMessages(null);
        if (adapter != null) {
            adapter.stopTimers();
        }
    }

    @Override
    public void onEditRule(RecurringRule rule) {
        if (!isManagerForRule(rule)) {
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
        if (!isManagerForRule(rule)) {
            if (isAdded()) {
                AppToaster.info(requireActivity(), "Chỉ Chủ quỹ và Phó quỹ mới có quyền xác nhận chi");
            }
            return;
        }
        Context context = requireContext();
        final Context appCtx = context.getApplicationContext();
        UserSessionManager session = UserSessionManager.getInstance(context);
        final String requesterId = session.getUserId();
        final String requesterName = session.getFullName();

        final String targetRoomId = (rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) ? rule.getRoomId() : roomId;
        final Room room = targetRoomId != null ? RoomRepository.getInstance().getRoomSync(targetRoomId) : null;
        final double roomBalance = (room != null) ? room.getBalance() : 0.0;
        final double ruleAmount = Math.abs(rule.getAmount());

        if (roomBalance < ruleAmount) {
            showInsufficientBalanceDialog(rule, roomBalance, ruleAmount);
            return;
        }

        if (!isSubmittingPayment.compareAndSet(false, true)) {
            return;
        }

        final boolean isOwner = isCurrentUserOwnerForRoom(targetRoomId);
        final boolean isTreasurer = RoomRepository.getInstance().isUserRoomTreasurer(targetRoomId, requesterId);
        final boolean canDirectApprove = isOwner || (isTreasurer && ruleAmount <= 5000000.0);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        final String formattedTime = sdf.format(new java.util.Date());

        String category = resolveStandardCategory(rule);
        String note = "Rút quỹ về Ví Koin - Thanh toán định kỳ: " + rule.getTitle();

        if (layoutMainContent != null) layoutMainContent.setVisibility(View.GONE);
        if (layoutProcessing != null) layoutProcessing.setVisibility(View.VISIBLE);
        if (layoutSuccess != null) layoutSuccess.setVisibility(View.GONE);

        transactionRepository.submitRecurringWithdrawal(
                requesterId,
                requesterName,
                targetRoomId,
                ruleAmount,
                category,
                note,
                canDirectApprove,
                rule.getId(),
                rule.getFrequency(),
                new TransactionRepository.DataCallback<Transaction>() {
                    @Override
                    public void onSuccess(Transaction createdTx) {
                        isSubmittingPayment.set(false);
                        final double updatedBalance;
                        if (canDirectApprove) {
                            java.text.SimpleDateFormat localDateFmt = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            rule.setLastRunDate(localDateFmt.format(new java.util.Date()));
                            rule.setNextRunDate(rule.computeNextRunDateIso());
                            updatedBalance = createdTx.getBalanceAfter() > 0 ? createdTx.getBalanceAfter() : (roomBalance - ruleAmount);
                            if (room != null) {
                                room.setBalance(updatedBalance);
                            }
                        } else {
                            updatedBalance = roomBalance;
                            createdTx.setStatus(Transaction.STATUS_PENDING);
                            createdTx.setRecurring(true);
                            if (rule.getId() != null) {
                                createdTx.setRecurringRuleId(rule.getId());
                            }
                        }

                        RoomRepository.getInstance().addTransaction(createdTx);
                        RoomRepository.getInstance().notifyDataChanged();
                        if (targetRoomId != null) {
                            RoomRepository.getInstance().refreshRoomTransactions(targetRoomId, null);
                        }
                        if (canDirectApprove) {
                            RoomRepository.getInstance().loadUserProfile();
                        }

                        if (canDirectApprove) {
                            transactionRepository.updateRecurringRule(rule, new TransactionRepository.DataCallback<RecurringRule>() {
                                @Override
                                public void onSuccess(RecurringRule updated) {
                                    RoomRepository.getInstance().notifyRecurringRulesChanged();
                                }

                                @Override
                                public void onError(String error) {}
                            });
                        } else if (targetRoomId != null && !targetRoomId.trim().isEmpty()) {
                            RoomRepository.getInstance().dispatchExpenseApprovalNotifications(
                                    appCtx,
                                    targetRoomId,
                                    requesterId,
                                    requesterName,
                                    ruleAmount,
                                    note
                            );
                        }

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                refreshPendingRuleKeys();
                                updateUI();
                                uiHandler.postDelayed(() -> {
                                    if (!isAdded()) return;
                                    if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
                                    if (layoutSuccess != null) layoutSuccess.setVisibility(View.VISIBLE);

                                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
                                    symbols.setGroupingSeparator('.');
                                    DecimalFormat df = new DecimalFormat("#,###", symbols);

                                    if (tvSuccessTitle != null) {
                                        tvSuccessTitle.setText(canDirectApprove ? "Xuất quỹ thành công!" : "Yêu cầu xuất quỹ đã gửi!");
                                    }
                                    if (tvSuccessSubtitle != null) {
                                        tvSuccessSubtitle.setText(canDirectApprove ? ("Đã rút về Ví Koin của " + requesterName + " để thanh toán định kỳ") : "Yêu cầu đã được gửi tới Chủ quỹ để phê duyệt");
                                    }
                                    if (tvSuccessAmount != null) tvSuccessAmount.setText("- " + df.format(ruleAmount) + " đ");
                                    String rawTxId = createdTx.getId() != null ? createdTx.getId() : UUID.randomUUID().toString();
                                    String shortTxId = "#SF-" + (rawTxId.length() >= 8 ? rawTxId.substring(0, 8).toUpperCase(Locale.ROOT) : rawTxId.toUpperCase(Locale.ROOT));
                                    if (tvSuccessTxId != null) tvSuccessTxId.setText(shortTxId);
                                    if (tvSuccessRuleName != null) tvSuccessRuleName.setText("Thanh toán định kỳ - " + rule.getTitle());
                                    String displayRoom = (roomName != null && !roomName.trim().isEmpty())
                                            ? roomName
                                            : (room != null && room.getName() != null ? room.getName() : (rule.getRoomName() != null ? rule.getRoomName() : "Quỹ nhóm"));
                                    if (tvSuccessRoomName != null) tvSuccessRoomName.setText(displayRoom);
                                    if (tvSuccessUserName != null) tvSuccessUserName.setText("Ví cá nhân Koin (" + requesterName + ")");
                                    if (tvSuccessTime != null) tvSuccessTime.setText(formattedTime);
                                    if (dividerSuccessBalance != null) {
                                        dividerSuccessBalance.setVisibility(canDirectApprove ? View.VISIBLE : View.GONE);
                                    }
                                    if (layoutSuccessBalanceRow != null) {
                                        layoutSuccessBalanceRow.setVisibility(canDirectApprove ? View.VISIBLE : View.GONE);
                                    }
                                    if (tvSuccessBalance != null) tvSuccessBalance.setText(df.format(updatedBalance) + " đ");

                                    if (btnSuccessCopyTxId != null) {
                                        btnSuccessCopyTxId.setOnClickListener(v -> {
                                            android.content.ClipboardManager cm = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                            if (cm != null) {
                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("Transaction ID", shortTxId));
                                            }
                                            com.sharedfund.app.util.CopyToast.show(requireContext(), v, "Đã sao chép mã giao dịch");
                                        });
                                    }
                                    View.OnClickListener closeSuccessListener = v -> {
                                        if (layoutSuccess != null) layoutSuccess.setVisibility(View.GONE);
                                        if (layoutMainContent != null) layoutMainContent.setVisibility(View.VISIBLE);
                                        refreshPendingRuleKeys();
                                        loadRules();
                                    };
                                    if (btnSuccessDone != null) btnSuccessDone.setOnClickListener(closeSuccessListener);
                                    if (btnSuccessBack != null) btnSuccessBack.setOnClickListener(closeSuccessListener);
                                }, 600);
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        isSubmittingPayment.set(false);
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
                                if (layoutMainContent != null) layoutMainContent.setVisibility(View.VISIBLE);
                                AppToaster.error(requireActivity(), "Không thể hoàn tất xuất quỹ: " + ErrorMessageHelper.getFriendlyMessage(error));
                            });
                        }
                    }
                }
        );
    }

    private String resolveStandardCategory(RecurringRule rule) {
        if (rule == null) return "Khác";
        return TransactionAdapter.resolveDisplayCategory(rule.getCategoryName(), rule.getTitle());
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

    private long getLatestNudgeTime(RecurringRule rule, String targetRoom) {
        Map<String, Long> merged = buildMergedNudgeMap();
        long latest = 0L;
        if (rule.getId() != null && merged.containsKey(rule.getId().trim())) {
            Long v = merged.get(rule.getId().trim());
            if (v != null && v > latest) latest = v;
        }
        String titleKey = rule.getTitle() != null ? rule.getTitle().trim().toLowerCase(Locale.ROOT) : "";
        if (!titleKey.isEmpty()) {
            if (targetRoom != null && !targetRoom.trim().isEmpty()) {
                Long v = merged.get(targetRoom.trim() + "|" + titleKey);
                if (v != null && v > latest) latest = v;
            }
            Long vPlain = merged.get(titleKey);
            if (vPlain != null && vPlain > latest) latest = vPlain;
        }
        return latest;
    }

    @Override
    public void onNudgeRule(RecurringRule rule) {
        if (rule == null || !isAdded()) return;
        Context context = requireContext();
        final Context appCtx = context.getApplicationContext();
        String targetRoom = (rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) ? rule.getRoomId() : roomId;

        long lastNudgeTime = getLatestNudgeTime(rule, targetRoom);
        long now = System.currentTimeMillis();
        long diff = now - lastNudgeTime;
        long cooldownMs = 60 * 60 * 1000L;
        if (lastNudgeTime > 0L && diff < cooldownMs && (lastNudgeTime - now) < 300000L) {
            long remainingMinutes = (cooldownMs - Math.max(0L, diff) + 59999) / 60000;
            AppToaster.info(requireActivity(), "Bạn vừa gửi nhắc nhở rồi. Vui lòng chờ " + remainingMinutes + " phút!");
            refreshCloudNudgeState();
            return;
        }

        if (rule.getId() != null && !rule.getId().trim().isEmpty()) {
            cloudNudgeTimestamps.put(rule.getId().trim(), now);
        }
        String titleKey = rule.getTitle() != null ? rule.getTitle().trim().toLowerCase(Locale.ROOT) : "";
        if (!titleKey.isEmpty()) {
            if (targetRoom != null && !targetRoom.trim().isEmpty()) {
                cloudNudgeTimestamps.put(targetRoom.trim() + "|" + titleKey, now);
            } else {
                cloudNudgeTimestamps.put(titleKey, now);
            }
        }
        refreshCloudNudgeState();

        if (targetRoom != null && !targetRoom.trim().isEmpty() && rule.getId() != null) {
            RoomRepository.getInstance().recordRecurringNudge(targetRoom, rule.getId(), now);
        }

        AppToaster.success(requireActivity(), "Đã gửi thông báo nhắc đóng " + rule.getTitle());

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);

        UserSessionManager session = UserSessionManager.getInstance(context);
        final String myId = session.getUserId();
        final String senderName = session.getFullName();
        Room r = targetRoom != null ? RoomRepository.getInstance().getRoomSync(targetRoom) : null;
        String ownerId = (r != null) ? r.getCreatedBy() : null;
        String fundName = (r != null && r.getName() != null) ? r.getName() : (rule.getRoomName() != null ? rule.getRoomName() : "Quỹ phòng");

        if (targetRoom != null && !targetRoom.trim().isEmpty()) {
            RoomRepository.getInstance().loadRoomMembers(targetRoom, new RoomRepository.DataCallback<List<com.sharedfund.app.model.RoomMember>>() {
                @Override
                public void onSuccess(List<com.sharedfund.app.model.RoomMember> members) {
                    if (members == null) return;
                    for (com.sharedfund.app.model.RoomMember m : members) {
                        if (m == null || m.getUserId() == null || m.getUserId().trim().isEmpty()) continue;
                        String uId = m.getUserId();
                        if (myId != null && myId.equalsIgnoreCase(uId)) continue;
                        boolean isOwnerOrTreasurer = "OWNER".equalsIgnoreCase(m.getRole())
                                || (ownerId != null && ownerId.equalsIgnoreCase(uId))
                                || RoomRepository.getInstance().isUserRoomOwner(targetRoom, uId)
                                || RoomRepository.getInstance().isUserRoomTreasurer(targetRoom, uId);

                        if (isOwnerOrTreasurer) {
                            AppNotification tNotif = new AppNotification(
                                    UUID.randomUUID().toString(),
                                    AppNotification.TYPE_RECURRING_DUE,
                                    fundName,
                                    "Nhắc thanh toán: " + rule.getTitle(),
                                    "Thành viên " + senderName + " nhắc xuất quỹ đóng khoản " + rule.getTitle() + " (" + df.format(rule.getAmount()) + " đ).",
                                    "Vừa xong",
                                    false,
                                    targetRoom,
                                    fundName
                            );
                            NotificationManager.getInstance(appCtx).sendNotificationToUser(tNotif, uId);
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    if (ownerId != null && !ownerId.trim().isEmpty() && (myId == null || !myId.equalsIgnoreCase(ownerId))) {
                        AppNotification notif = new AppNotification(
                                UUID.randomUUID().toString(),
                                AppNotification.TYPE_RECURRING_DUE,
                                fundName,
                                "Nhắc thanh toán: " + rule.getTitle(),
                                "Thành viên " + senderName + " nhắc xuất quỹ đóng khoản " + rule.getTitle() + " (" + df.format(rule.getAmount()) + " đ).",
                                "Vừa xong",
                                false,
                                targetRoom,
                                fundName
                        );
                        NotificationManager.getInstance(appCtx).sendNotificationToUser(notif, ownerId);
                    }
                }
            });
        }
    }

    private String buildOverdueCycleKey(RecurringRule rule) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String freq = rule.getFrequency();
        if (RecurringRule.FREQUENCY_DAILY.equalsIgnoreCase(freq)) {
            return String.format(Locale.US, "%04d_%02d_%02d",
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH));
        }
        if (RecurringRule.FREQUENCY_WEEKLY.equalsIgnoreCase(freq)) {
            return String.format(Locale.US, "%04d_W%02d",
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.WEEK_OF_YEAR));
        }
        return String.format(Locale.US, "%04d_%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1);
    }

    private void checkAndDispatchOverdueNotifications(List<RecurringRule> rules) {
        if (rules == null || rules.isEmpty() || getContext() == null) return;
        final Context appCtx = requireContext().getApplicationContext();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);

        for (RecurringRule rule : rules) {
            if (rule != null && rule.isActive() && !rule.isPaidInCurrentCycle() && rule.getDaysDiffCurrentCycle() >= 0) {
                String targetRoom = (rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) ? rule.getRoomId() : roomId;
                if (targetRoom == null || targetRoom.trim().isEmpty()) continue;

                String cycleKey = buildOverdueCycleKey(rule);
                String key = "overdue_v3_notified_" + rule.getId() + "_" + cycleKey;
                if (!overdueNotifiedSet.contains(key)) {
                    overdueNotifiedSet.add(key);
                    if (NotificationManager.getInstance(appCtx).hasRecentOverdueNotification(targetRoom, rule.getTitle(), 24 * 60 * 60 * 1000L)) {
                        continue;
                    }

                    Room r = RoomRepository.getInstance().getRoomSync(targetRoom);
                    String ownerId = (r != null) ? r.getCreatedBy() : null;
                    String fundName = (r != null && r.getName() != null) ? r.getName() : (rule.getRoomName() != null ? rule.getRoomName() : "Quỹ phòng");

                    RoomRepository.getInstance().loadRoomMembers(targetRoom, new RoomRepository.DataCallback<List<com.sharedfund.app.model.RoomMember>>() {
                        @Override
                        public void onSuccess(List<com.sharedfund.app.model.RoomMember> members) {
                            if (members == null) return;
                            for (com.sharedfund.app.model.RoomMember m : members) {
                                if (m == null || m.getUserId() == null || m.getUserId().trim().isEmpty()) continue;
                                String uId = m.getUserId();
                                boolean isOwnerOrTreasurer = "OWNER".equalsIgnoreCase(m.getRole())
                                        || (ownerId != null && ownerId.equalsIgnoreCase(uId))
                                        || RoomRepository.getInstance().isUserRoomOwner(targetRoom, uId)
                                        || RoomRepository.getInstance().isUserRoomTreasurer(targetRoom, uId);

                                if (isOwnerOrTreasurer) {
                                    AppNotification ownerNotif = new AppNotification(
                                            UUID.randomUUID().toString(),
                                            AppNotification.TYPE_RECURRING_DUE,
                                            fundName,
                                            "[Quá hạn] Cần xuất quỹ thanh toán " + rule.getTitle(),
                                            "Khoản chi định kỳ " + df.format(rule.getAmount()) + " đ cho quỹ " + fundName + " đã quá hạn. Vui lòng kiểm tra và duyệt chi.",
                                            "Vừa xong",
                                            false,
                                            targetRoom,
                                            fundName
                                    );
                                    NotificationManager.getInstance(appCtx).sendNotificationToUser(ownerNotif, uId);
                                } else {
                                    AppNotification memberNotif = new AppNotification(
                                            UUID.randomUUID().toString(),
                                            AppNotification.TYPE_RECURRING_DUE,
                                            fundName,
                                            "[Quỹ phòng] Khoản chi " + rule.getTitle() + " chưa thanh toán",
                                            "Khoản chi định kỳ " + df.format(rule.getAmount()) + " đ cho quỹ " + fundName + " đã quá hạn. Bạn có thể bấm để nhắc Chủ quỹ thanh toán sớm nhé.",
                                            "Vừa xong",
                                            false,
                                            targetRoom,
                                            fundName
                                    );
                                    NotificationManager.getInstance(appCtx).sendNotificationToUser(memberNotif, uId);
                                }
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (ownerId != null && !ownerId.trim().isEmpty()) {
                                AppNotification ownerNotif = new AppNotification(
                                        UUID.randomUUID().toString(),
                                        AppNotification.TYPE_RECURRING_DUE,
                                        fundName,
                                        "[Quá hạn] Cần xuất quỹ thanh toán " + rule.getTitle(),
                                        "Khoản chi định kỳ " + df.format(rule.getAmount()) + " đ cho quỹ " + fundName + " đã quá hạn. Vui lòng kiểm tra và duyệt chi.",
                                        "Vừa xong",
                                        false,
                                        targetRoom,
                                        fundName
                                );
                                NotificationManager.getInstance(appCtx).sendNotificationToUser(ownerNotif, ownerId);
                            }
                        }
                    });
                }
            }
        }
    }
}
