package com.sharedfund.app.ui.room;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.transaction.TransactionAdapter;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Màn hình Chi tiết quỹ (RoomDetailFragment)
 * Quản lý số dư quỹ, danh sách giao dịch gần nhất, thành viên và các nút chuyển hướng nhanh.
 * Hỗ trợ tự động cập nhật khi dữ liệu thay đổi, tự co giãn số tiền dài, phản hồi nút bấm đầy đủ.
 */
public class RoomDetailFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private static final String ARG_ROOM_ID = "arg_room_id";

    private String roomId;
    private RoomViewModel viewModel;
    private TransactionAdapter transactionAdapter;

    private ImageButton btnBack;
    private TextView tvHeaderRoomName;
    private TextView tvDetailJoinCode;
    private View btnDetailCopyCode;
    private TextView tvDetailTitle;
    private TextView tvDetailBalance;
    private TextView tvDetailTotalIncome;
    private TextView tvDetailTotalExpense;
    private TextView tvDetailMemberCount;
    private TextView tvDetailMembersList;
    private View btnNavTransactions;
    private View btnNavAddTransaction;
    private View btnNavStatistics;
    private TextView tvSeeAllTransactions;
    private LinearLayout layoutEmptyRecentTx;
    private MaterialButton btnEmptyAddTx;
    private MaterialCardView cardRecentTransactions;
    private RecyclerView rvRecentTransactions;
    private ProgressBar pbDetailLoading;

    public static RoomDetailFragment newInstance(String roomId) {
        RoomDetailFragment fragment = new RoomDetailFragment();
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
        return inflater.inflate(R.layout.fragment_room_detail, container, false);
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
            viewModel.refresh();
        }
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        tvHeaderRoomName = view.findViewById(R.id.tvHeaderRoomName);
        tvDetailJoinCode = view.findViewById(R.id.tvDetailJoinCode);
        btnDetailCopyCode = view.findViewById(R.id.btnDetailCopyCode);
        tvDetailTitle = view.findViewById(R.id.tvDetailTitle);
        tvDetailBalance = view.findViewById(R.id.tvDetailBalance);
        tvDetailTotalIncome = view.findViewById(R.id.tvDetailTotalIncome);
        tvDetailTotalExpense = view.findViewById(R.id.tvDetailTotalExpense);
        tvDetailMemberCount = view.findViewById(R.id.tvDetailMemberCount);
        tvDetailMembersList = view.findViewById(R.id.tvDetailMembersList);
        btnNavTransactions = view.findViewById(R.id.btnNavTransactions);
        btnNavAddTransaction = view.findViewById(R.id.btnNavAddTransaction);
        btnNavStatistics = view.findViewById(R.id.btnNavStatistics);
        tvSeeAllTransactions = view.findViewById(R.id.tvSeeAllTransactions);
        layoutEmptyRecentTx = view.findViewById(R.id.layoutEmptyRecentTx);
        btnEmptyAddTx = view.findViewById(R.id.btnEmptyAddTx);
        cardRecentTransactions = view.findViewById(R.id.cardRecentTransactions);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);
        pbDetailLoading = view.findViewById(R.id.pbDetailLoading);
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(this);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTransactions.setAdapter(transactionAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        viewModel.setRoomId(roomId);

        viewModel.getRoom().observe(getViewLifecycleOwner(), new Observer<Room>() {
            @Override
            public void onChanged(Room room) {
                if (room != null) {
                    tvHeaderRoomName.setText(room.getName());
                    tvDetailTitle.setText(room.getName());
                    tvDetailBalance.setText(room.getFormattedBalance());
                    tvDetailJoinCode.setText(room.getJoinCode());
                    tvDetailMemberCount.setText(room.getMemberCount() + " thành viên");

                    if (room.getMembers() != null && !room.getMembers().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < room.getMembers().size(); i++) {
                            sb.append(room.getMembers().get(i));
                            if (i < room.getMembers().size() - 1) {
                                sb.append(", ");
                            }
                        }
                        tvDetailMembersList.setText(sb.toString());
                    }
                }
            }
        });

        final DecimalFormat df = new DecimalFormat("#,###");

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double income) {
                if (income != null) {
                    tvDetailTotalIncome.setText("+" + df.format(income).replace(',', '.') + " ₫");
                }
            }
        });

        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double expense) {
                if (expense != null) {
                    tvDetailTotalExpense.setText("-" + df.format(expense).replace(',', '.') + " ₫");
                }
            }
        });

        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                transactionAdapter.setTransactions(transactions);
                if (transactions == null || transactions.isEmpty()) {
                    if (layoutEmptyRecentTx != null) layoutEmptyRecentTx.setVisibility(View.VISIBLE);
                    if (cardRecentTransactions != null) cardRecentTransactions.setVisibility(View.GONE);
                } else {
                    if (layoutEmptyRecentTx != null) layoutEmptyRecentTx.setVisibility(View.GONE);
                    if (cardRecentTransactions != null) cardRecentTransactions.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                if (pbDetailLoading != null) {
                    pbDetailLoading.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });

        btnDetailCopyCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyJoinCode();
            }
        });

        btnNavTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openTransactions(roomId);
                }
            }
        });

        btnNavAddTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openAddTransaction(roomId);
                }
            }
        });

        if (btnEmptyAddTx != null) {
            btnEmptyAddTx.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openAddTransaction(roomId);
                    }
                }
            });
        }

        btnNavStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openStatistics(roomId);
                }
            }
        });

        tvSeeAllTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openTransactions(roomId);
                }
            }
        });
    }

    private void copyJoinCode() {
        Room r = viewModel.getRoom().getValue();
        if (r != null) {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Join Code", r.getJoinCode());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), getString(R.string.copied_code) + " (" + r.getJoinCode() + ")", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        Toast.makeText(requireContext(), transaction.getNote() + ": " + transaction.getFormattedAmount(), Toast.LENGTH_SHORT).show();
    }
}
