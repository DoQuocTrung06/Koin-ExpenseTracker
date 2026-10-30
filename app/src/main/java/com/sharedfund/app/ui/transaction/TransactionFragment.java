package com.sharedfund.app.ui.transaction;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.text.InputType;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Transaction;

import java.util.List;

/**
 * Màn hình Lịch sử giao dịch (TransactionFragment)
 * Quản lý danh sách giao dịch, bộ lọc Thu/Chi/Tất cả, trạng thái tải, lỗi và danh sách trống.
 * Phản hồi cảm ứng mượt mà (ripple effect), cuộn danh sách độc lập, hỗ trợ cập nhật dữ liệu tức thì.
 */
public class TransactionFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private static final String ARG_ROOM_ID = "arg_room_id";

    private String roomId;
    private TransactionViewModel viewModel;
    private TransactionAdapter adapter;

    private ImageButton btnTxBack;
    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterIncome;
    private MaterialButton btnFilterExpense;
    private RecyclerView rvTransactions;
    private LinearLayout layoutTxEmptyState;
    private LinearLayout layoutTxError;
    private TextView tvTxErrorMessage;
    private MaterialButton btnTxRetry;
    private MaterialButton btnEmptyAddTransaction;
    private ProgressBar pbTxLoading;
    private ExtendedFloatingActionButton fabAddTransaction;

    public static TransactionFragment newInstance(String roomId) {
        TransactionFragment fragment = new TransactionFragment();
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
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.applyFilter(viewModel.getCurrentFilter());
        }
    }

    private void initViews(View view) {
        btnTxBack = view.findViewById(R.id.btnTxBack);
        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnFilterIncome = view.findViewById(R.id.btnFilterIncome);
        btnFilterExpense = view.findViewById(R.id.btnFilterExpense);
        rvTransactions = view.findViewById(R.id.rvTransactions);
        layoutTxEmptyState = view.findViewById(R.id.layoutTxEmptyState);
        layoutTxError = view.findViewById(R.id.layoutTxError);
        tvTxErrorMessage = view.findViewById(R.id.tvTxErrorMessage);
        btnTxRetry = view.findViewById(R.id.btnTxRetry);
        btnEmptyAddTransaction = view.findViewById(R.id.btnEmptyAddTransaction);
        pbTxLoading = view.findViewById(R.id.pbTxLoading);
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        viewModel.setAccessToken(com.sharedfund.app.data.UserSessionManager.getInstance(requireContext()).getAccessToken());
        viewModel.setRoomId(roomId);

        // Quan sát danh sách giao dịch đã lọc
        viewModel.getTransactions().observe(getViewLifecycleOwner(), new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                adapter.setTransactions(transactions);
                if (layoutTxError != null) {
                    layoutTxError.setVisibility(View.GONE);
                }

                if (transactions == null || transactions.isEmpty()) {
                    layoutTxEmptyState.setVisibility(View.VISIBLE);
                    rvTransactions.setVisibility(View.GONE);
                } else {
                    layoutTxEmptyState.setVisibility(View.GONE);
                    rvTransactions.setVisibility(View.VISIBLE);
                }
            }
        });

        // Quan sát trạng thái tải
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                if (pbTxLoading != null) {
                    pbTxLoading.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
                }
            }
        });

        // Quan sát thông báo lỗi
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String err) {
                if (err != null && !err.isEmpty()) {
                    if (layoutTxError != null && (adapter.getItemCount() == 0)) {
                        tvTxErrorMessage.setText(err);
                        layoutTxError.setVisibility(View.VISIBLE);
                        layoutTxEmptyState.setVisibility(View.GONE);
                        rvTransactions.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        btnTxBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });

        btnFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFilterUI(TransactionViewModel.FilterType.ALL);
                viewModel.setFilter(TransactionViewModel.FilterType.ALL);
            }
        });

        btnFilterIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFilterUI(TransactionViewModel.FilterType.INCOME);
                viewModel.setFilter(TransactionViewModel.FilterType.INCOME);
            }
        });

        btnFilterExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFilterUI(TransactionViewModel.FilterType.EXPENSE);
                viewModel.setFilter(TransactionViewModel.FilterType.EXPENSE);
            }
        });

        fabAddTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddTransactionScreen();
            }
        });

        if (btnEmptyAddTransaction != null) {
            btnEmptyAddTransaction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openAddTransactionScreen();
                }
            });
        }

        if (btnTxRetry != null) {
            btnTxRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (layoutTxError != null) layoutTxError.setVisibility(View.GONE);
                    viewModel.applyFilter(viewModel.getCurrentFilter());
                }
            });
        }
    }

    private void openAddTransactionScreen() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openAddTransaction(roomId);
        }
    }

    /**
     * Cập nhật màu sắc nút bộ lọc giữ nguyên hiệu ứng chạm (ripple) và viền bo tròn của MaterialButton
     */
    private void updateFilterUI(TransactionViewModel.FilterType filter) {
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int transparentColor = Color.TRANSPARENT;
        int activeTextColor = Color.WHITE;
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        // Nút Tất cả
        btnFilterAll.setBackgroundTintList(ColorStateList.valueOf(filter == TransactionViewModel.FilterType.ALL ? primaryColor : transparentColor));
        btnFilterAll.setTextColor(filter == TransactionViewModel.FilterType.ALL ? activeTextColor : inactiveTextColor);

        // Nút Khoản thu
        btnFilterIncome.setBackgroundTintList(ColorStateList.valueOf(filter == TransactionViewModel.FilterType.INCOME ? primaryColor : transparentColor));
        btnFilterIncome.setTextColor(filter == TransactionViewModel.FilterType.INCOME ? activeTextColor : inactiveTextColor);

        // Nút Khoản chi
        btnFilterExpense.setBackgroundTintList(ColorStateList.valueOf(filter == TransactionViewModel.FilterType.EXPENSE ? primaryColor : transparentColor));
        btnFilterExpense.setTextColor(filter == TransactionViewModel.FilterType.EXPENSE ? activeTextColor : inactiveTextColor);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        if (transaction == null) return;
        if ("PENDING".equalsIgnoreCase(transaction.getStatus()) && isCurrentUserOwner()) {
            showWithdrawalReviewDialog(transaction);
            return;
        }
        String message = transaction.getNote() + " (" + transaction.getFormattedAmount() + ")";
        if (transaction.getStatus() != null) message += "\nTrạng thái: " + transaction.getStatus();
        if (transaction.getRejectionReason() != null && !transaction.getRejectionReason().trim().isEmpty())
            message += "\nLý do từ chối: " + transaction.getRejectionReason();
        new AlertDialog.Builder(requireContext()).setMessage(message).setPositiveButton("Đóng", null).show();
    }

    private boolean isCurrentUserOwner() {
        com.sharedfund.app.model.Room room = RoomRepository.getInstance().getRoomSync(roomId);
        if (room == null) return false;
        String role = room.getCurrentUserRole();
        if (role != null) return "ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role);
        String userId = UserSessionManager.getInstance(requireContext()).getUserId();
        return userId != null && userId.equals(room.getCreatedBy());
    }

    private void showWithdrawalReviewDialog(Transaction transaction) {
        EditText reasonInput = new EditText(requireContext());
        reasonInput.setHint("Nhập lý do nếu từ chối");
        reasonInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        reasonInput.setPadding(padding, padding, padding, padding);
        new AlertDialog.Builder(requireContext())
                .setTitle("Duyệt yêu cầu rút quỹ")
                .setMessage(transaction.getCreatedBy() + " yêu cầu rút " + transaction.getFormattedAmount()
                        + "\n" + transaction.getNote())
                .setView(reasonInput)
                .setNegativeButton("Từ chối", (dialog, which) -> {
                    String reason = reasonInput.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(requireContext(), "Cần nhập lý do từ chối.", Toast.LENGTH_SHORT).show();
                        showWithdrawalReviewDialog(transaction);
                    } else reviewWithdrawal(transaction, false, reason);
                })
                .setPositiveButton("Duyệt", (dialog, which) -> reviewWithdrawal(transaction, true, null))
                .setNeutralButton("Để sau", null)
                .show();
    }

    private void reviewWithdrawal(Transaction transaction, boolean approve, String reason) {
        String token = UserSessionManager.getInstance(requireContext()).getAccessToken();
        TransactionRepository repository = new TransactionRepository();
        repository.setAccessToken(token);
        String adminUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        repository.reviewWithdrawal(adminUserId, transaction.getId(), approve, reason,
                new TransactionRepository.DataCallback<com.google.gson.JsonObject>() {
                    @Override public void onSuccess(com.google.gson.JsonObject result) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            transaction.setStatus(approve ? "APPROVED" : "REJECTED");
                            transaction.setApprovedBy(adminUserId);
                            transaction.setApprovedAt(java.time.Instant.now().toString());
                            if (approve && result.has("new_room_balance")
                                    && !result.get("new_room_balance").isJsonNull())
                                transaction.setBalanceAfter(result.get("new_room_balance").getAsDouble());
                            if (!approve) transaction.setRejectionReason(reason);
                            RoomRepository.getInstance().onRemoteTransactionReceived(transaction);
                            Toast.makeText(requireContext(), approve
                                    ? "Đã duyệt. Tiền đã chuyển vào ví thành viên."
                                    : "Đã từ chối yêu cầu rút quỹ.", Toast.LENGTH_LONG).show();
                        });
                    }
                    @Override public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                                "Không thể xử lý yêu cầu: " + message, Toast.LENGTH_LONG).show());
                    }
                });
    }
}
