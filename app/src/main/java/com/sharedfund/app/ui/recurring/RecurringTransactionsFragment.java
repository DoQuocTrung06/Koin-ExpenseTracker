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
    private static final java.util.Map<String, Long> nudgeCooldownMap = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<String> overdueNotifiedSet = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

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

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String formattedTime = sdf.format(new java.util.Date());

        java.text.SimpleDateFormat localDateFmt = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String localToday = localDateFmt.format(new java.util.Date());
        rule.setLastRunDate(localToday);
        rule.setNextRunDate(rule.computeNextRunDateIso());

        String category = resolveStandardCategory(rule);
        String note = rule.getTitle();

        if (layoutMainContent != null) layoutMainContent.setVisibility(View.GONE);
        if (layoutProcessing != null) layoutProcessing.setVisibility(View.VISIBLE);
        if (layoutSuccess != null) layoutSuccess.setVisibility(View.GONE);

        transactionRepository.submitWithdrawal(
                session.getUserId(),
                session.getFullName(),
                targetRoomId,
                ruleAmount,
                category,
                note,
                true,
                new TransactionRepository.DataCallback<Transaction>() {
                    @Override
                    public void onSuccess(Transaction createdTx) {
                        double updatedBalance = createdTx.getBalanceAfter() > 0 ? createdTx.getBalanceAfter() : (roomBalance - ruleAmount);
                        if (room != null) {
                            room.setBalance(updatedBalance);
                        }

                        RoomRepository.getInstance().addTransaction(createdTx);
                        RoomRepository.getInstance().notifyDataChanged();
                        if (targetRoomId != null) {
                            RoomRepository.getInstance().refreshRoomTransactions(targetRoomId, null);
                        }

                        transactionRepository.updateRecurringRule(rule, new TransactionRepository.DataCallback<RecurringRule>() {
                            @Override
                            public void onSuccess(RecurringRule updated) {
                                RoomRepository.getInstance().notifyRecurringRulesChanged();
                            }

                            @Override
                            public void onError(String error) {}
                        });

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                updateUI();
                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    if (!isAdded()) return;
                                    if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
                                    if (layoutSuccess != null) layoutSuccess.setVisibility(View.VISIBLE);

                                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
                                    symbols.setGroupingSeparator('.');
                                    DecimalFormat df = new DecimalFormat("#,###", symbols);

                                    if (tvSuccessAmount != null) tvSuccessAmount.setText("- " + df.format(ruleAmount) + " đ");
                                    String rawTxId = createdTx.getId() != null ? createdTx.getId() : UUID.randomUUID().toString();
                                    String shortTxId = "#SF-" + (rawTxId.length() >= 8 ? rawTxId.substring(0, 8).toUpperCase(Locale.ROOT) : rawTxId.toUpperCase(Locale.ROOT));
                                    if (tvSuccessTxId != null) tvSuccessTxId.setText(shortTxId);
                                    if (tvSuccessRuleName != null) tvSuccessRuleName.setText(rule.getTitle());
                                    String displayRoom = (roomName != null && !roomName.trim().isEmpty()) ? roomName : (rule.getRoomName() != null ? rule.getRoomName() : "Quỹ nhóm");
                                    if (tvSuccessRoomName != null) tvSuccessRoomName.setText(displayRoom);
                                    if (tvSuccessUserName != null) tvSuccessUserName.setText(session.getFullName());
                                    if (tvSuccessTime != null) tvSuccessTime.setText(formattedTime);
                                    if (tvSuccessBalance != null) tvSuccessBalance.setText(df.format(updatedBalance) + " đ");

                                    if (btnSuccessCopyTxId != null) {
                                        btnSuccessCopyTxId.setOnClickListener(v -> com.sharedfund.app.util.CopyToast.show(requireContext(), shortTxId));
                                    }
                                    if (btnSuccessDone != null) {
                                        btnSuccessDone.setOnClickListener(v -> {
                                            if (layoutSuccess != null) layoutSuccess.setVisibility(View.GONE);
                                            if (layoutMainContent != null) layoutMainContent.setVisibility(View.VISIBLE);
                                            loadRules();
                                        });
                                    }
                                    if (btnSuccessBack != null) {
                                        btnSuccessBack.setOnClickListener(v -> {
                                            if (layoutSuccess != null) layoutSuccess.setVisibility(View.GONE);
                                            if (layoutMainContent != null) layoutMainContent.setVisibility(View.VISIBLE);
                                            loadRules();
                                        });
                                    }
                                }, 600);
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
                                if (layoutMainContent != null) layoutMainContent.setVisibility(View.VISIBLE);
                                AppToaster.error(requireActivity(), "Không thể hoàn tất xuất quỹ: " + error);
                            });
                        }
                    }
                }
        );
    }

    private String resolveStandardCategory(RecurringRule rule) {
        if (rule == null) return "Khác";
        String cat = rule.getCategoryName();
        if (cat != null && !cat.trim().isEmpty() && !cat.equalsIgnoreCase("Định kỳ") && !cat.equalsIgnoreCase("Dinh ky")) {
            return cat;
        }
        String title = rule.getTitle();
        if (title != null) {
            String lower = title.toLowerCase(Locale.ROOT);
            if (lower.contains("điện") || lower.contains("nước") || lower.contains("dien nuoc") || lower.contains("mạng") || lower.contains("internet") || lower.contains("wifi") || lower.contains("fpt") || lower.contains("viettel")) {
                return "Điện nước";
            }
            if (lower.contains("phòng") || lower.contains("trọ") || lower.contains("nhà") || lower.contains("thuê") || lower.contains("phong tro")) {
                return "Tiền phòng";
            }
            if (lower.contains("ăn") || lower.contains("uống") || lower.contains("cơm") || lower.contains("chợ") || lower.contains("thực phẩm") || lower.contains("food") || lower.contains("an uong")) {
                return "Ăn uống";
            }
            if (lower.contains("mua") || lower.contains("sắm") || lower.contains("đồ dùng") || lower.contains("siêu thị") || lower.contains("mua sam")) {
                return "Mua sắm";
            }
            if (lower.contains("xe") || lower.contains("xăng") || lower.contains("gửi xe") || lower.contains("đi lại") || lower.contains("di chuyen") || lower.contains("grab")) {
                return "Di chuyển";
            }
            if (lower.contains("liên hoan") || lower.contains("tiệc") || lower.contains("sinh nhật") || lower.contains("party") || lower.contains("bóng") || lower.contains("thể thao") || lower.contains("bong da")) {
                return "Liên hoan";
            }
        }
        return "Khác";
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
        Long lastNudge = nudgeCooldownMap.get(rule.getId());
        long lastNudgeTime = (lastNudge != null) ? lastNudge : 0L;
        long now = System.currentTimeMillis();
        long diff = now - lastNudgeTime;
        long cooldownMs = 60 * 60 * 1000L;
        if (diff < cooldownMs) {
            long remainingMinutes = (cooldownMs - diff + 59999) / 60000;
            AppToaster.info(requireActivity(), "Bạn vừa gửi nhắc nhở rồi. Vui lòng chờ " + remainingMinutes + " phút!");
            return;
        }

        nudgeCooldownMap.put(rule.getId(), now);
        AppToaster.success(requireActivity(), "Đã gửi thông báo nhắc đóng " + rule.getTitle());

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);

        String targetRoom = (rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) ? rule.getRoomId() : roomId;
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
                        boolean isOwnerOrTreasurer = "OWNER".equalsIgnoreCase(m.getRole())
                                || (ownerId != null && ownerId.equals(uId))
                                || "TREASURER".equalsIgnoreCase(m.getRole())
                                || (r != null && r.getTreasurerIds() != null && r.getTreasurerIds().contains(uId));

                        if (isOwnerOrTreasurer) {
                            AppNotification tNotif = new AppNotification(
                                    UUID.randomUUID().toString(),
                                    AppNotification.TYPE_RECURRING_DUE,
                                    fundName,
                                    "Nhắc thanh toán: " + rule.getTitle(),
                                    "Thành viên " + UserSessionManager.getInstance(context).getFullName() + " nhắc xuất quỹ đóng khoản " + rule.getTitle() + " (" + df.format(rule.getAmount()) + " đ).",
                                    "Vừa xong",
                                    false,
                                    targetRoom,
                                    fundName
                            );
                            NotificationManager.getInstance(context).sendNotificationToUser(tNotif, uId);
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    if (ownerId != null && !ownerId.trim().isEmpty()) {
                        AppNotification notif = new AppNotification(
                                UUID.randomUUID().toString(),
                                AppNotification.TYPE_RECURRING_DUE,
                                fundName,
                                "Nhắc thanh toán: " + rule.getTitle(),
                                "Thành viên " + UserSessionManager.getInstance(context).getFullName() + " nhắc xuất quỹ đóng khoản " + rule.getTitle() + " (" + df.format(rule.getAmount()) + " đ).",
                                "Vừa xong",
                                false,
                                targetRoom,
                                fundName
                        );
                        NotificationManager.getInstance(context).sendNotificationToUser(notif, ownerId);
                    }
                }
            });
        }
        updateUI();
    }

    private void checkAndDispatchOverdueNotifications(List<RecurringRule> rules) {
        if (rules == null || rules.isEmpty() || getContext() == null) return;
        Context context = requireContext();
        java.text.SimpleDateFormat monthFormat = new java.text.SimpleDateFormat("yyyy_MM", Locale.US);
        String currentMonth = monthFormat.format(new java.util.Date());

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);

        for (RecurringRule rule : rules) {
            if (rule != null && rule.isActive() && !rule.isPaidInCurrentMonth() && rule.getDaysDiffCurrentMonth() >= 0) {
                String key = "overdue_v2_notified_" + rule.getId() + "_" + currentMonth;
                if (!overdueNotifiedSet.contains(key)) {
                    overdueNotifiedSet.add(key);

                    String targetRoom = (rule.getRoomId() != null && !rule.getRoomId().trim().isEmpty()) ? rule.getRoomId() : roomId;
                    if (targetRoom == null || targetRoom.trim().isEmpty()) continue;

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
                                        || (ownerId != null && ownerId.equals(uId))
                                        || "TREASURER".equalsIgnoreCase(m.getRole())
                                        || (r != null && r.getTreasurerIds() != null && r.getTreasurerIds().contains(uId));

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
                                    NotificationManager.getInstance(context).sendNotificationToUser(ownerNotif, uId);
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
                                    NotificationManager.getInstance(context).sendNotificationToUser(memberNotif, uId);
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
                                NotificationManager.getInstance(context).sendNotificationToUser(ownerNotif, ownerId);
                            }
                        }
                    });
                }
            }
        }
    }
}
