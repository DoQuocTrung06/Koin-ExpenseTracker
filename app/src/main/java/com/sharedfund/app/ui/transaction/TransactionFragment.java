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
import com.sharedfund.app.ui.widget.AppToaster;
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
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Transaction;

import java.util.List;

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
    private ProgressBar pbTxLoading;

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
        updateFilterUI(viewModel.getCurrentFilter());
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
        pbTxLoading = view.findViewById(R.id.pbTxLoading);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        viewModel.setAccessToken(UserSessionManager.getInstance(requireContext()).getAccessToken());
        viewModel.setRoomId(roomId);

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

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                if (pbTxLoading != null) {
                    pbTxLoading.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
                }
            }
        });

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
                        AppToaster.error(requireActivity(), err);
                    }
                }
            }
        });

        viewModel.getActionMessage().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String msg) {
                if (msg != null && !msg.isEmpty()) {
                    if (msg.toLowerCase().contains("thất bại") || msg.toLowerCase().contains("lỗi")) {
                        AppToaster.error(requireActivity(), msg);
                    } else {
                        AppToaster.success(requireActivity(), msg);
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

    private void updateFilterUI(TransactionViewModel.FilterType filter) {
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int transparentColor = Color.TRANSPARENT;
        int activeTextColor = Color.WHITE;
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        setFilterButtonState(btnFilterAll, filter == TransactionViewModel.FilterType.ALL,
                primaryColor, transparentColor, activeTextColor, inactiveTextColor);
        setFilterButtonState(btnFilterIncome, filter == TransactionViewModel.FilterType.INCOME,
                primaryColor, transparentColor, activeTextColor, inactiveTextColor);
        setFilterButtonState(btnFilterExpense, filter == TransactionViewModel.FilterType.EXPENSE,
                primaryColor, transparentColor, activeTextColor, inactiveTextColor);
    }

    private void setFilterButtonState(MaterialButton button, boolean selected, int primaryColor,
                                      int transparentColor, int activeTextColor, int inactiveTextColor) {
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? primaryColor : transparentColor));
        button.setTextColor(selected ? activeTextColor : inactiveTextColor);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        if (transaction == null) return;
        boolean isAdmin = RoomRepository.getInstance().isCurrentUserAdmin(roomId);
        TransactionDetailBottomSheet sheet = TransactionDetailBottomSheet.newInstance(transaction, isAdmin);
        sheet.setActionListener(new TransactionDetailBottomSheet.OnTransactionActionListener() {
            @Override
            public void onApprove(Transaction tx) {
                String adminId = RoomRepository.getInstance().getCurrentUserId();
                viewModel.approveExpense(adminId, tx.getId());
            }

            @Override
            public void onReject(Transaction tx, String reason) {
                String adminId = RoomRepository.getInstance().getCurrentUserId();
                viewModel.rejectExpense(adminId, tx.getId(), reason);
            }
        });
        sheet.show(getChildFragmentManager(), "tx_detail");
    }
}
