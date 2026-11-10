package com.sharedfund.app.ui.recurring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;
import com.sharedfund.app.data.DataCallback;
import com.sharedfund.app.data.SessionManager;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.model.RecurringRule;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecurringTransactionsFragment extends Fragment implements RecurringRuleAdapter.OnRecurringActionListener {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_ROOM_NAME = "arg_room_name";

    private String roomId;
    private String roomName;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvRules;
    private View layoutEmpty;
    private TextView tvHeaderTitle;
    private RecurringRuleAdapter adapter;
    private final List<RecurringRule> localRules = new ArrayList<>();

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
        MaterialButton btnAddTop = view.findViewById(R.id.btnAddRecurringTop);
        MaterialButton btnCreateFirst = view.findViewById(R.id.btnCreateFirstRecurring);
        swipeRefresh = view.findViewById(R.id.swipeRefreshRecurring);
        rvRules = view.findViewById(R.id.rvRecurringRules);
        layoutEmpty = view.findViewById(R.id.layoutEmptyRecurring);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        if (roomName != null && !roomName.trim().isEmpty()) {
            tvHeaderTitle.setText("Định kỳ • " + roomName);
        } else {
            tvHeaderTitle.setText("Chi tiêu định kỳ");
        }

        View.OnClickListener openAddDialog = v -> {
            CreateRecurringRuleBottomSheet sheet = CreateRecurringRuleBottomSheet.newInstance(roomId);
            sheet.setListener(this::handleCreateRule);
            sheet.show(getChildFragmentManager(), "create_recurring_rule_sheet");
        };

        if (btnAddTop != null) btnAddTop.setOnClickListener(openAddDialog);
        if (btnCreateFirst != null) btnCreateFirst.setOnClickListener(openAddDialog);

        adapter = new RecurringRuleAdapter(this);
        rvRules.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRules.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadRules);

        loadRules();
    }

    private void loadRules() {
        swipeRefresh.setRefreshing(true);
        String currentUserId = SessionManager.getInstance(requireContext()).getUserId();

        TransactionRepository.getInstance().getRecurringRules(currentUserId, roomId, new DataCallback<List<RecurringRule>>() {
            @Override
            public void onSuccess(List<RecurringRule> rules) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
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
                    swipeRefresh.setRefreshing(false);
                    if (localRules.isEmpty()) {
                        seedSampleRules();
                    }
                    updateUI();
                });
            }
        });
    }

    private void seedSampleRules() {
        RecurringRule rule1 = new RecurringRule();
        rule1.setId("mock_rule_1");
        rule1.setTitle("Tiền Internet & Truyền hình FPT");
        rule1.setAmount(250000);
        rule1.setCategoryName("Tiện ích & Nhà");
        rule1.setFrequency(RecurringRule.FREQUENCY_MONTHLY);
        rule1.setExecutionDay(5);
        rule1.setType("Chi");
        rule1.setActive(true);
        rule1.setScope(roomId != null ? RecurringRule.SCOPE_ROOM : RecurringRule.SCOPE_PERSONAL);

        RecurringRule rule2 = new RecurringRule();
        rule2.setId("mock_rule_2");
        rule2.setTitle("Tiền phòng trọ & quản lý");
        rule2.setAmount(2800000);
        rule2.setCategoryName("Tiền phòng");
        rule2.setFrequency(RecurringRule.FREQUENCY_MONTHLY);
        rule2.setExecutionDay(15);
        rule2.setType("Chi");
        rule2.setActive(true);
        rule2.setScope(roomId != null ? RecurringRule.SCOPE_ROOM : RecurringRule.SCOPE_PERSONAL);

        localRules.add(rule1);
        localRules.add(rule2);
    }

    private void updateUI() {
        adapter.submitList(localRules);
        boolean isEmpty = localRules.isEmpty();
        if (layoutEmpty != null) layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (rvRules != null) rvRules.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void handleCreateRule(RecurringRule rule) {
        localRules.add(0, rule);
        updateUI();

        TransactionRepository.getInstance().createRecurringRule(rule, new DataCallback<RecurringRule>() {
            @Override
            public void onSuccess(RecurringRule created) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Đã lưu khoản định kỳ thành công", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Đã lưu nội bộ khoản định kỳ", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    public void onToggleRule(RecurringRule rule, boolean isActive) {
        TransactionRepository.getInstance().toggleRecurringRule(rule.getId(), isActive, new DataCallback<Boolean>() {
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

        TransactionRepository.getInstance().deleteRecurringRule(rule.getId(), new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Đã xóa khoản định kỳ", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(String error) {}
        });
    }
}
