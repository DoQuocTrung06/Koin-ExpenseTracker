package com.sharedfund.app.ui.statistics;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.sharedfund.app.R;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private static final String ARG_ROOM_ID = "arg_room_id";

    private String roomId;
    private StatisticsViewModel viewModel;

    private ImageButton btnStatBack;
    private TextView tvStatRoomName;
    private TextView tvStatNetBalance;
    private TextView tvStatIncome;
    private TextView tvStatExpense;
    private LinearLayout layoutCategoryBars;
    private TextView tvEmptyBreakdown;
    private LinearLayout layoutTopExpenses;
    private ProgressBar pbStatLoading;

    private final DecimalFormat df = new DecimalFormat("#,###");

    private final int[] categoryColors = new int[]{
            Color.parseColor("#0052CC"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#10B981"),
            Color.parseColor("#06B6D4"),
            Color.parseColor("#8B5CF6")
    };

    public static StatisticsFragment newInstance(String roomId) {
        StatisticsFragment fragment = new StatisticsFragment();
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
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewModel();
        setupClickListeners();
    }

    private void initViews(View view) {
        btnStatBack = view.findViewById(R.id.btnStatBack);
        tvStatRoomName = view.findViewById(R.id.tvStatRoomName);
        tvStatNetBalance = view.findViewById(R.id.tvStatNetBalance);
        tvStatIncome = view.findViewById(R.id.tvStatIncome);
        tvStatExpense = view.findViewById(R.id.tvStatExpense);
        layoutCategoryBars = view.findViewById(R.id.layoutCategoryBars);
        tvEmptyBreakdown = view.findViewById(R.id.tvEmptyBreakdown);
        layoutTopExpenses = view.findViewById(R.id.layoutTopExpenses);
        pbStatLoading = view.findViewById(R.id.pbStatLoading);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
        viewModel.setRoomId(roomId);

        viewModel.getRoom().observe(getViewLifecycleOwner(), new Observer<Room>() {
            @Override
            public void onChanged(Room room) {
                if (room != null) {
                    tvStatRoomName.setText("Thống kê: " + room.getName());
                }
            }
        });

        viewModel.getNetBalance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double net) {
                if (net != null) {
                    String prefix = net >= 0 ? "+" : "";
                    tvStatNetBalance.setText(prefix + df.format(net).replace(',', '.') + " ₫");
                    if (net < 0) {
                        tvStatNetBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_expense));
                    } else {
                        tvStatNetBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                    }
                }
            }
        });

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double income) {
                if (income != null) {
                    tvStatIncome.setText("+" + df.format(income).replace(',', '.') + " ₫");
                }
            }
        });

        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double expense) {
                if (expense != null) {
                    tvStatExpense.setText("-" + df.format(expense).replace(',', '.') + " ₫");
                }
            }
        });

        viewModel.getCategoryStats().observe(getViewLifecycleOwner(), new Observer<Map<String, Double>>() {
            @Override
            public void onChanged(Map<String, Double> map) {
                renderCategoryBreakdown(map);
            }
        });

        viewModel.getTopExpenses().observe(getViewLifecycleOwner(), new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> list) {
                renderTopExpenses(list);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                if (pbStatLoading != null) {
                    pbStatLoading.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    private void renderCategoryBreakdown(Map<String, Double> map) {
        layoutCategoryBars.removeAllViews();
        if (map == null || map.isEmpty()) {
            tvEmptyBreakdown.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyBreakdown.setVisibility(View.GONE);

        double totalExpense = 0;
        for (Double amt : map.values()) {
            totalExpense += amt;
        }
        if (totalExpense <= 0) {
            tvEmptyBreakdown.setVisibility(View.VISIBLE);
            return;
        }

        int colorIdx = 0;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            double amt = entry.getValue();
            int percent = (int) Math.round((amt / totalExpense) * 100);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 10, 0, 10);

            LinearLayout topLabel = new LinearLayout(requireContext());
            topLabel.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvName = new TextView(requireContext());
            tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvName.setText(entry.getKey() + " (" + percent + "%)");
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvName.setTextSize(13);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setSingleLine(true);
            tvName.setEllipsize(TextUtils.TruncateAt.END);

            TextView tvAmt = new TextView(requireContext());
            tvAmt.setText(df.format(amt).replace(',', '.') + " ₫");
            tvAmt.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvAmt.setTextSize(13);
            tvAmt.setSingleLine(true);
            tvAmt.setPadding(8, 0, 0, 0);

            topLabel.addView(tvName);
            topLabel.addView(tvAmt);

            ProgressBar pb = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 18);
            pbParams.topMargin = 10;
            pb.setLayoutParams(pbParams);
            pb.setMax(100);
            pb.setProgress(Math.max(5, percent));

            int color = categoryColors[colorIdx % categoryColors.length];
            pb.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);

            row.addView(topLabel);
            row.addView(pb);

            layoutCategoryBars.addView(row);
            colorIdx++;
        }
    }

    private void renderTopExpenses(List<Transaction> list) {
        layoutTopExpenses.removeAllViews();
        if (list == null || list.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Chưa có khoản chi nào.");
            empty.setPadding(24, 24, 24, 24);
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary));
            layoutTopExpenses.addView(empty);
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            Transaction tx = list.get(i);

            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(24, 16, 24, 16);
            item.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvIndex = new TextView(requireContext());
            tvIndex.setText("#" + (i + 1));
            tvIndex.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            tvIndex.setTextSize(14);
            tvIndex.setTypeface(null, Typeface.BOLD);
            tvIndex.setWidth(60);

            LinearLayout info = new LinearLayout(requireContext());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(tx.getNote());
            tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvTitle.setTextSize(14);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setSingleLine(true);
            tvTitle.setEllipsize(TextUtils.TruncateAt.END);

            TextView tvSub = new TextView(requireContext());
            tvSub.setText(tx.getCreatedBy() + " · " + tx.getCategory());
            tvSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvSub.setTextSize(12);
            tvSub.setSingleLine(true);
            tvSub.setEllipsize(TextUtils.TruncateAt.END);

            info.addView(tvTitle);
            info.addView(tvSub);

            TextView tvAmount = new TextView(requireContext());
            tvAmount.setText(tx.getFormattedAmount());
            tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_expense));
            tvAmount.setTextSize(14);
            tvAmount.setTypeface(null, Typeface.BOLD);
            tvAmount.setSingleLine(true);
            tvAmount.setPadding(12, 0, 0, 0);

            item.addView(tvIndex);
            item.addView(info);
            item.addView(tvAmount);

            layoutTopExpenses.addView(item);

            if (i < list.size() - 1) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.border_light));
                layoutTopExpenses.addView(divider);
            }
        }
    }

    private void setupClickListeners() {
        btnStatBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });
    }
}