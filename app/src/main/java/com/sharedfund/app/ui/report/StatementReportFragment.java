package com.sharedfund.app.ui.report;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.transaction.TransactionDetailBottomSheet;
import com.sharedfund.app.ui.widget.AppToaster;
import com.sharedfund.app.util.ReportExportHelper;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatementReportFragment extends Fragment {

    private static final String ARG_TARGET_TITLE = "arg_target_title";
    private static final String ARG_TARGET_SUBTITLE = "arg_target_subtitle";
    private static final String ARG_DATE_RANGE = "arg_date_range";
    private static final String ARG_REPORTER_NAME = "arg_reporter_name";
    private static final String ARG_REPORTER_ROLE = "arg_reporter_role";
    private static final String ARG_TRANSACTIONS = "arg_transactions";

    private String targetTitle;
    private String targetSubtitle;
    private String dateRange;
    private String reporterName;
    private String reporterRole;
    private ArrayList<Transaction> transactions = new ArrayList<>();

    public static StatementReportFragment newInstance(
            String targetTitle,
            String targetSubtitle,
            String dateRange,
            String reporterName,
            String reporterRole,
            ArrayList<Transaction> transactions
    ) {
        StatementReportFragment fragment = new StatementReportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TARGET_TITLE, targetTitle);
        args.putString(ARG_TARGET_SUBTITLE, targetSubtitle);
        args.putString(ARG_DATE_RANGE, dateRange);
        args.putString(ARG_REPORTER_NAME, reporterName);
        args.putString(ARG_REPORTER_ROLE, reporterRole);
        args.putSerializable(ARG_TRANSACTIONS, transactions);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetTitle = getArguments().getString(ARG_TARGET_TITLE, "Báo cáo Koin");
            targetSubtitle = getArguments().getString(ARG_TARGET_SUBTITLE, "");
            dateRange = getArguments().getString(ARG_DATE_RANGE, "Toàn bộ");
            reporterName = getArguments().getString(ARG_REPORTER_NAME, "");
            reporterRole = getArguments().getString(ARG_REPORTER_ROLE, "Thành viên");
            Object serializableObj = getArguments().getSerializable(ARG_TRANSACTIONS);
            if (serializableObj instanceof ArrayList<?>) {
                transactions = (ArrayList<Transaction>) serializableObj;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statement_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnReportBack = view.findViewById(R.id.btnReportBack);
        ImageButton btnReportQuickShare = view.findViewById(R.id.btnReportQuickShare);
        TextView tvReportTargetName = view.findViewById(R.id.tvReportTargetName);
        TextView tvReportTargetSubtitle = view.findViewById(R.id.tvReportTargetSubtitle);
        TextView tvReportDateRange = view.findViewById(R.id.tvReportDateRange);
        TextView tvReportCreatorName = view.findViewById(R.id.tvReportCreatorName);
        TextView tvReportCreatorRoleBadge = view.findViewById(R.id.tvReportCreatorRoleBadge);
        TextView tvReportTxCount = view.findViewById(R.id.tvReportTxCount);
        TextView tvReportTotalIncome = view.findViewById(R.id.tvReportTotalIncome);
        TextView tvReportTotalExpense = view.findViewById(R.id.tvReportTotalExpense);
        TextView tvReportNetBalance = view.findViewById(R.id.tvReportNetBalance);
        TextView tvReportListTitle = view.findViewById(R.id.tvReportListTitle);
        TextView tvReportEmptyMessage = view.findViewById(R.id.tvReportEmptyMessage);
        RecyclerView rvReportTransactions = view.findViewById(R.id.rvReportTransactions);
        MaterialButton btnConfirmExportReport = view.findViewById(R.id.btnConfirmExportReport);

        if (btnReportBack != null) {
            btnReportBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        if (tvReportTargetName != null) tvReportTargetName.setText(targetTitle);
        if (tvReportTargetSubtitle != null) {
            if (targetSubtitle != null && !targetSubtitle.isEmpty()) {
                tvReportTargetSubtitle.setText(targetSubtitle);
                tvReportTargetSubtitle.setVisibility(View.VISIBLE);
            } else {
                tvReportTargetSubtitle.setVisibility(View.GONE);
            }
        }
        if (tvReportDateRange != null) tvReportDateRange.setText(dateRange);
        if (tvReportCreatorName != null) tvReportCreatorName.setText(reporterName);
        if (tvReportCreatorRoleBadge != null) {
            if (reporterRole != null && (reporterRole.contains("Chủ quỹ") || "OWNER".equalsIgnoreCase(reporterRole))) {
                tvReportCreatorRoleBadge.setText("★ Chủ quỹ");
                tvReportCreatorRoleBadge.setBackgroundResource(R.drawable.bg_role_badge_owner);
                tvReportCreatorRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_owner_text));
            } else if (reporterRole != null && (reporterRole.contains("Phó quỹ") || "TREASURER".equalsIgnoreCase(reporterRole) || "VICE".equalsIgnoreCase(reporterRole))) {
                tvReportCreatorRoleBadge.setText("★ Phó quỹ");
                tvReportCreatorRoleBadge.setBackgroundResource(R.drawable.bg_role_badge_vice);
                tvReportCreatorRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_vice_text));
            } else if (reporterRole != null && reporterRole.contains("Chủ tài khoản")) {
                tvReportCreatorRoleBadge.setText("Chủ tài khoản");
                tvReportCreatorRoleBadge.setBackgroundResource(R.drawable.bg_id_badge);
                tvReportCreatorRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            } else {
                tvReportCreatorRoleBadge.setText(reporterRole != null && !reporterRole.isEmpty() ? reporterRole : "Thành viên");
                tvReportCreatorRoleBadge.setBackgroundResource(R.drawable.bg_role_badge_vice);
                tvReportCreatorRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
        }

        int count = transactions != null ? transactions.size() : 0;
        if (tvReportTxCount != null) tvReportTxCount.setText(count + " giao dịch");
        if (tvReportListTitle != null) tvReportListTitle.setText("DANH SÁCH GIAO DỊCH TRONG BÁO CÁO (" + count + ")");

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);

        double totalIncome = 0;
        double totalExpense = 0;

        if (transactions != null) {
            for (Transaction tx : transactions) {
                if (tx == null) continue;
                if (tx.isIncome()) totalIncome += tx.getAmount();
                else totalExpense += tx.getAmount();
            }
        }

        double netBalance = totalIncome - totalExpense;

        if (tvReportTotalIncome != null) {
            tvReportTotalIncome.setText("+" + df.format(totalIncome) + " ₫");
        }
        if (tvReportTotalExpense != null) {
            tvReportTotalExpense.setText("-" + df.format(totalExpense) + " ₫");
        }
        if (tvReportNetBalance != null) {
            String prefix = netBalance >= 0 ? "+" : "";
            tvReportNetBalance.setText(prefix + df.format(netBalance) + " ₫");
        }

        if (transactions == null || transactions.isEmpty()) {
            if (tvReportEmptyMessage != null) tvReportEmptyMessage.setVisibility(View.VISIBLE);
            if (rvReportTransactions != null) rvReportTransactions.setVisibility(View.GONE);
        } else {
            if (tvReportEmptyMessage != null) tvReportEmptyMessage.setVisibility(View.GONE);
            if (rvReportTransactions != null) {
                rvReportTransactions.setVisibility(View.VISIBLE);
                rvReportTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvReportTransactions.setAdapter(new ReportPreviewAdapter(getParentFragmentManager(), transactions));
            }
        }

        if (btnConfirmExportReport != null) {
            btnConfirmExportReport.setOnClickListener(v -> performDownloadReport());
        }
        if (btnReportQuickShare != null) {
            btnReportQuickShare.setOnClickListener(v -> performShareReport());
        }
    }

    private void performShareReport() {
        if (!isAdded() || getContext() == null) return;
        if (transactions == null || transactions.isEmpty()) {
            if (getActivity() != null) {
                AppToaster.show(requireActivity(), AppToaster.ToastType.INFO, "Không có giao dịch nào để chia sẻ");
            }
            return;
        }

        try {
            List<File> reportFiles = ReportExportHelper.exportReportBundle(
                    requireContext(),
                    targetTitle,
                    targetSubtitle,
                    dateRange,
                    reporterName,
                    reporterRole,
                    transactions
            );
            if (reportFiles != null && !reportFiles.isEmpty()) {
                ReportExportHelper.shareReportBundle(requireContext(), reportFiles);
            } else {
                if (getActivity() != null) {
                    AppToaster.show(requireActivity(), AppToaster.ToastType.ERROR, "Không thể tạo bộ file sao kê");
                }
            }
        } catch (Exception e) {
            if (getActivity() != null) {
                AppToaster.show(requireActivity(), AppToaster.ToastType.ERROR, "Lỗi khi xuất file: " + e.getMessage());
            }
        }
    }

    private void performDownloadReport() {
        if (!isAdded() || getContext() == null) return;
        if (transactions == null || transactions.isEmpty()) {
            if (getActivity() != null) {
                AppToaster.show(requireActivity(), AppToaster.ToastType.INFO, "Không có giao dịch nào để tải về");
            }
            return;
        }

        boolean success = ReportExportHelper.saveReportToDownloads(
                requireContext(),
                targetTitle,
                targetSubtitle,
                dateRange,
                reporterName,
                reporterRole,
                transactions
        );

        if (getActivity() != null) {
            if (success) {
                AppToaster.show(requireActivity(), AppToaster.ToastType.SUCCESS, "Đã lưu bản sao kê vào thư mục Tải về (Downloads)");
            } else {
                AppToaster.show(requireActivity(), AppToaster.ToastType.ERROR, "Không thể lưu file vào thư mục Tải về");
            }
        }
    }

    private static class ReportPreviewAdapter extends RecyclerView.Adapter<ReportPreviewAdapter.ViewHolder> {
        private final FragmentManager fragmentManager;
        private final List<Transaction> items;
        private final DecimalFormat df;

        public ReportPreviewAdapter(FragmentManager fragmentManager, List<Transaction> items) {
            this.fragmentManager = fragmentManager;
            this.items = items != null ? items : new ArrayList<>();
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator('.');
            this.df = new DecimalFormat("#,##0", symbols);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction tx = items.get(position);
            if (tx == null) return;

            String title = tx.getNote();
            if (title == null || title.trim().isEmpty()) {
                title = tx.getCategory();
            }
            if (title == null || title.trim().isEmpty()) {
                title = tx.isIncome() ? "Tiền vào ví" : "Chi tiêu";
            }
            holder.tvTitle.setText(title);

            String time = com.sharedfund.app.ui.transaction.TransactionAdapter.formatDateTime(tx.getCreatedAt());
            String creator = tx.getCreatedBy() != null ? tx.getCreatedBy() : "";
            String sub = time + (creator.isEmpty() ? "" : " • " + creator);
            holder.tvSub.setText(sub);

            boolean hasBill = tx.getBillImageUrl() != null && !tx.getBillImageUrl().trim().isEmpty();
            holder.tvBillBadge.setVisibility(hasBill ? View.VISIBLE : View.GONE);

            double amount = tx.getAmount();
            if (tx.isIncome()) {
                holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_income));
                holder.tvAmount.setText("+" + df.format(amount) + " ₫");
            } else {
                holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
                holder.tvAmount.setText("-" + df.format(amount) + " ₫");
            }

            holder.itemView.setOnClickListener(v -> {
                if (fragmentManager != null) {
                    TransactionDetailBottomSheet.newInstance(tx, false)
                            .show(fragmentManager, "report_transaction_detail");
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvSub;
            TextView tvBillBadge;
            TextView tvAmount;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvItemReportTitle);
                tvSub = itemView.findViewById(R.id.tvItemReportSub);
                tvBillBadge = itemView.findViewById(R.id.tvItemReportBillBadge);
                tvAmount = itemView.findViewById(R.id.tvItemReportAmount);
            }
        }
    }
}