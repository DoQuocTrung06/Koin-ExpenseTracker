package com.sharedfund.app.ui.room;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.widget.AppToaster;
import com.sharedfund.app.util.ErrorMessageHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExpenseApprovalFragment extends Fragment {

    private static final String ARG_ROOM_ID = "arg_room_id";

    private String roomId;
    private ImageButton btnApprovalBack;
    private TextView tvApprovalTitle;
    private TextView badgeApprovalCount;
    private RecyclerView rvPendingApprovals;
    private LinearLayout layoutEmptyApproval;
    private PendingApprovalAdapter adapter;
    private TransactionRepository transactionRepository;
    private boolean isViewerOwner = false;
    private final Set<String> privilegedRequesterIds = new HashSet<>();

    public static ExpenseApprovalFragment newInstance(String roomId) {
        ExpenseApprovalFragment fragment = new ExpenseApprovalFragment();
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
        transactionRepository = new TransactionRepository();
        String token = UserSessionManager.getInstance(requireContext()).getAccessToken();
        transactionRepository.setAccessToken(token);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense_approval, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnApprovalBack = view.findViewById(R.id.btnApprovalBack);
        tvApprovalTitle = view.findViewById(R.id.tvApprovalTitle);
        badgeApprovalCount = view.findViewById(R.id.badgeApprovalCount);
        rvPendingApprovals = view.findViewById(R.id.rvPendingApprovals);
        layoutEmptyApproval = view.findViewById(R.id.layoutEmptyApproval);

        btnApprovalBack.setOnClickListener(v -> {
            if (isAdded()) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        rvPendingApprovals.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PendingApprovalAdapter(currentUserId, new PendingApprovalAdapter.OnApprovalActionListener() {
            @Override
            public void onApprove(Transaction tx, int position) {
                executeApprove(tx, position);
            }

            @Override
            public void onReject(Transaction tx, int position) {
                showRejectDialog(tx, position);
            }
        });
        rvPendingApprovals.setAdapter(adapter);

        RoomRepository.getInstance().getTransactionsLiveData().observe(getViewLifecycleOwner(), list -> {
            if (isAdded()) {
                refreshPendingFromCache();
            }
        });
        RoomRepository.getInstance().getRoomsLiveData().observe(getViewLifecycleOwner(), rooms -> {
            if (isAdded()) {
                refreshApprovalPermissions();
            }
        });

        refreshApprovalPermissions();
        loadPendingTransactions();
    }

    private void refreshPendingFromCache() {
        if (roomId == null) return;
        List<Transaction> all = RoomRepository.getInstance().getTransactionsForRoom(roomId);
        List<Transaction> pending = new ArrayList<>();
        if (all != null) {
            for (Transaction tx : all) {
                if (tx.isPending()) {
                    pending.add(tx);
                }
            }
        }
        updateUI(pending);
    }

    private void refreshApprovalPermissions() {
        if (roomId == null || !isAdded()) return;
        String myId = UserSessionManager.getInstance(requireContext()).getUserId();
        if (myId == null || myId.trim().isEmpty()) {
            myId = RoomRepository.getInstance().getCurrentUserId();
        }
        isViewerOwner = RoomRepository.getInstance().isUserRoomOwner(roomId, myId);
        privilegedRequesterIds.clear();
        Room r = RoomRepository.getInstance().getRoomSync(roomId);
        if (r != null) {
            if (r.getCreatedBy() != null && !r.getCreatedBy().trim().isEmpty()) {
                privilegedRequesterIds.add(r.getCreatedBy().trim());
            }
            if (r.getTreasurerIds() != null) {
                privilegedRequesterIds.addAll(r.getTreasurerIds());
            }
        }
        if (adapter != null) {
            adapter.setApprovalContext(isViewerOwner, privilegedRequesterIds);
        }
        RoomRepository.getInstance().loadRoomMembers(roomId, new RoomRepository.DataCallback<List<RoomMember>>() {
            @Override
            public void onSuccess(List<RoomMember> members) {
                if (!isAdded()) return;
                Set<String> updatedSet = new HashSet<>(privilegedRequesterIds);
                if (members != null) {
                    for (RoomMember m : members) {
                        if (m == null || m.getUserId() == null || m.getUserId().trim().isEmpty()) continue;
                        if ("OWNER".equalsIgnoreCase(m.getRole()) || "TREASURER".equalsIgnoreCase(m.getRole()) || "ADMIN".equalsIgnoreCase(m.getRole())) {
                            updatedSet.add(m.getUserId().trim());
                        }
                    }
                }
                requireActivity().runOnUiThread(() -> {
                    privilegedRequesterIds.clear();
                    privilegedRequesterIds.addAll(updatedSet);
                    if (adapter != null) {
                        adapter.setApprovalContext(isViewerOwner, privilegedRequesterIds);
                    }
                });
            }

            @Override
            public void onError(String message) {}
        });
    }

    private boolean isRequesterPrivileged(String userId) {
        if (userId == null || userId.trim().isEmpty()) return false;
        if (RoomRepository.getInstance().isUserRoomOwner(roomId, userId)
                || RoomRepository.getInstance().isUserRoomTreasurer(roomId, userId)) {
            return true;
        }
        for (String id : privilegedRequesterIds) {
            if (id != null && id.equalsIgnoreCase(userId.trim())) return true;
        }
        return false;
    }

    private void loadPendingTransactions() {
        if (roomId == null) return;
        List<Transaction> all = RoomRepository.getInstance().getTransactionsForRoom(roomId);
        List<Transaction> pending = new ArrayList<>();
        if (all != null) {
            for (Transaction tx : all) {
                if (tx.isPending()) {
                    pending.add(tx);
                }
            }
        }
        updateUI(pending);

        RoomRepository.getInstance().refreshRoomTransactions(roomId, new RoomRepository.DataCallback<List<Transaction>>() {
            @Override
            public void onSuccess(List<Transaction> list) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    List<Transaction> freshPending = new ArrayList<>();
                    if (list != null) {
                        for (Transaction tx : list) {
                            if (tx.isPending()) {
                                freshPending.add(tx);
                            }
                        }
                    }
                    updateUI(freshPending);
                });
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void updateUI(List<Transaction> list) {
        if (adapter != null) {
            adapter.setItems(list);
        }
        int count = list != null ? list.size() : 0;
        updateBadgeCount(count);
        if (count == 0) {
            if (layoutEmptyApproval != null) layoutEmptyApproval.setVisibility(View.VISIBLE);
            if (rvPendingApprovals != null) rvPendingApprovals.setVisibility(View.GONE);
        } else {
            if (layoutEmptyApproval != null) layoutEmptyApproval.setVisibility(View.GONE);
            if (rvPendingApprovals != null) rvPendingApprovals.setVisibility(View.VISIBLE);
        }
    }

    private void updateBadgeCount(int count) {
        if (badgeApprovalCount != null) {
            if (count > 0) {
                badgeApprovalCount.setText(count + " yêu cầu");
                badgeApprovalCount.setVisibility(View.VISIBLE);
            } else {
                badgeApprovalCount.setVisibility(View.GONE);
            }
        }
    }

    private void executeApprove(Transaction tx, int position) {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String adminId = session.getUserId();
        if (adminId == null || adminId.isEmpty()) {
            adminId = RoomRepository.getInstance().getCurrentUserId();
        }
        if (adminId != null && tx.getUserId() != null && adminId.equalsIgnoreCase(tx.getUserId())) {
            AppToaster.error(requireActivity(), "Không thể tự phê duyệt khoản chi của chính mình!");
            return;
        }
        boolean isOwnerViewer = RoomRepository.getInstance().isUserRoomOwner(roomId, adminId);
        if (!isOwnerViewer && isRequesterPrivileged(tx.getUserId())) {
            AppToaster.error(requireActivity(), "Yêu cầu của Phó quỹ cần Chủ quỹ phê duyệt!");
            return;
        }

        adapter.removeItem(position);
        int remaining = adapter.getItemCount();
        updateBadgeCount(remaining);
        if (remaining == 0) {
            layoutEmptyApproval.setVisibility(View.VISIBLE);
            rvPendingApprovals.setVisibility(View.GONE);
        }

        final android.content.Context appCtx = requireContext().getApplicationContext();
        final String approverId = adminId;
        final String roleLabel = isOwnerViewer ? "Chủ quỹ" : "Phó quỹ";
        RoomRepository.getInstance().configureAccessToken(session.getAccessToken());
        transactionRepository.setAccessToken(session.getAccessToken());
        transactionRepository.approveExpense(adminId, tx.getId(), new TransactionRepository.DataCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject data) {
                RoomRepository.getInstance().markRecurringRulePaidIfMatched(roomId, tx);
                RoomRepository.getInstance().dispatchExpenseResultNotification(appCtx, roomId, tx, true, null, approverId, roleLabel);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        RoomRepository.getInstance().loadUserProfile();
                        RoomRepository.getInstance().refreshRoomTransactions(roomId, null);
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        AppToaster.error(requireActivity(), "Phê duyệt thất bại: " + ErrorMessageHelper.getFriendlyMessage(errorMessage));
                        loadPendingTransactions();
                    });
                }
            }
        });
    }

    private void showRejectDialog(Transaction tx, int position) {
        if (getContext() == null) return;
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).create();
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_koin_reject_expense, null);
        dialog.setView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View layoutInputBox = view.findViewById(R.id.layoutRejectInputBox);
        TextView tvError = view.findViewById(R.id.tvRejectReasonError);
        EditText etReason = view.findViewById(R.id.etRejectReason);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelReject);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmReject);

        if (etReason != null) {
            etReason.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tvError != null) tvError.setVisibility(View.GONE);
                    if (layoutInputBox != null) layoutInputBox.setBackgroundResource(R.drawable.bg_input_box);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String reason = etReason != null && etReason.getText() != null ? etReason.getText().toString().trim() : "";
                if (reason.isEmpty()) {
                    if (tvError != null) tvError.setVisibility(View.VISIBLE);
                    if (layoutInputBox != null) layoutInputBox.setBackgroundResource(R.drawable.bg_input_box_error);
                    if (etReason != null) etReason.requestFocus();
                    return;
                }
                dialog.dismiss();
                executeReject(tx, position, reason);
            });
        }

        dialog.show();
    }

    private void executeReject(Transaction tx, int position, String reason) {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String adminId = session.getUserId();
        if (adminId == null || adminId.isEmpty()) {
            adminId = RoomRepository.getInstance().getCurrentUserId();
        }
        if (adminId != null && tx.getUserId() != null && adminId.equalsIgnoreCase(tx.getUserId())) {
            AppToaster.error(requireActivity(), "Không thể tự từ chối khoản chi của chính mình!");
            return;
        }
        boolean isOwnerViewer = RoomRepository.getInstance().isUserRoomOwner(roomId, adminId);
        if (!isOwnerViewer && isRequesterPrivileged(tx.getUserId())) {
            AppToaster.error(requireActivity(), "Yêu cầu của Phó quỹ cần Chủ quỹ xử lý!");
            return;
        }

        adapter.removeItem(position);
        int remaining = adapter.getItemCount();
        updateBadgeCount(remaining);
        if (remaining == 0) {
            layoutEmptyApproval.setVisibility(View.VISIBLE);
            rvPendingApprovals.setVisibility(View.GONE);
        }

        final android.content.Context appCtx = requireContext().getApplicationContext();
        final String approverId = adminId;
        final String roleLabel = isOwnerViewer ? "Chủ quỹ" : "Phó quỹ";
        RoomRepository.getInstance().configureAccessToken(session.getAccessToken());
        transactionRepository.setAccessToken(session.getAccessToken());
        transactionRepository.rejectExpense(adminId, tx.getId(), reason, new TransactionRepository.DataCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject data) {
                RoomRepository.getInstance().dispatchExpenseResultNotification(appCtx, roomId, tx, false, reason, approverId, roleLabel);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        RoomRepository.getInstance().refreshRoomTransactions(roomId, null);
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        AppToaster.error(requireActivity(), "Từ chối thất bại: " + ErrorMessageHelper.getFriendlyMessage(errorMessage));
                        loadPendingTransactions();
                    });
                }
            }
        });
    }
}
