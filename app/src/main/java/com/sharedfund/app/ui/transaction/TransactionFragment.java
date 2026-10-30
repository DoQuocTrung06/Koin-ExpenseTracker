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
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        viewModel.getActionMessage().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String msg) {
                if (msg != null && !msg.isEmpty()) {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
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

    private void updateFilterUI(TransactionViewModel.FilterType filter) {
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int transparentColor = Color.TRANSPARENT;
        int activeTextColor = Color.WHITE;
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        btnFilterAll.setBackgroundTintList(ColorStateList.valueOf(filter == TransactionViewModel.FilterType.ALL ? primaryColor : transparentColor));
        btnFilterAll.setTextColor(filter == TransactionViewModel.FilterType.ALL ? activeTextColor : inactiveTextColor);

        btnFilterIncome.setBackgroundTintList(ColorStateList.valueOf(filter == TransactionViewModel.FilterType.INCOME ? primaryColor : transparentColor));
        btnFilterIncome.setTextColor(filter == TransactionViewModel.FilterType.INCOME ? activeTextColor : inactiveTextColor);

        btnFilterExpense.setBackgroundTintList(ColorStateList.valueOf(filter == TransactionViewModel.FilterType.EXPENSE ? primaryColor : transparentColor));
        btnFilterExpense.setTextColor(filter == TransactionViewModel.FilterType.EXPENSE ? activeTextColor : inactiveTextColor);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
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
