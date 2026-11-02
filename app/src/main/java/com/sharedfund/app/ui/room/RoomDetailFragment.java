package com.sharedfund.app.ui.room;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.sharedfund.app.model.ScannedQrPayment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.adapter.MemberAdapter;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.transaction.TransactionAdapter;
import com.sharedfund.app.ui.transaction.TransactionDetailBottomSheet;
import com.sharedfund.app.ui.widget.AppToaster;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomDetailFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private static final String ARG_ROOM_ID = "arg_room_id";

    private String roomId;
    private RoomViewModel viewModel;

    private TransactionAdapter recentTransactionAdapter;
    private TransactionAdapter historyTransactionAdapter;
    private MemberAdapter memberAdapter;

    private View layoutRoomDetailHeader;
    private ImageButton btnBack;
    private TextView tvHeaderRoomName;
    private ImageButton btnHeaderQuickEdit;
    private View btnDetailCopyCode;
    private TextView tvDetailJoinCode;

    private View cardFloatingBottomNav;
    private BottomNavigationView bottomNavRoomDetail;
    private NestedScrollView layoutTabHome;
    private NestedScrollView layoutTabHistory;
    private NestedScrollView layoutTabMembers;
    private NestedScrollView layoutTabSettings;
    private ProgressBar pbDetailLoading;

    private ImageButton btnChangeRoomBackground;
    private ImageView ivRoomCover;
    private TextView tvDetailBalance;
    private LinearLayout layoutHomeTarget;
    private TextView tvHomeTargetText;
    private ProgressBar pbHomeTarget;
    private MaterialButton btnRoomTransfer;
    private MaterialButton btnUnifiedTransfer;
    private MaterialButton btnRoomUtilities;
    private TextView tvSeeAllTransactions;
    private LinearLayout layoutEmptyRecentTx;
    private View cardRecentTransactions;
    private RecyclerView rvRecentTransactions;


    private final ActivityResultLauncher<ScanOptions> qrScanner = registerForActivityResult(
            new ScanContract(), result -> {
                if (result.getContents() != null) {
                    ScannedQrPayment parsed = ScannedQrPayment.parse(result.getContents());
                    if (parsed == null || parsed.accountNumber == null || parsed.accountNumber.trim().isEmpty()) {
                        AppToaster.error(requireActivity(), "Mã QR không chứa thông tin VietQR hợp lệ.");
                        return;
                    }
                    showTransferSheetWithQr(parsed);
                }
            });

    private TextView tvHistoryTotalIncome;
    private TextView tvHistoryTotalExpense;
    private TextView tvHistoryNetBalance;
    private com.sharedfund.app.ui.widget.DonutChartView donutHistoryChartView;
    private LinearLayout layoutHistoryCategoryBars;
    private boolean isCategoryListExpanded = false;
    private TextView tvEmptyCategoryBars;
    private MaterialButton btnHistoryFilterAll;
    private MaterialButton btnHistoryFilterIncome;
    private MaterialButton btnHistoryFilterExpense;
    private LinearLayout layoutEmptyHistory;
    private RecyclerView rvHistoryTransactions;

    private TextView tvMembersTabCount;
    private TextView tvMembersTabTotalContributed;
    private MaterialButton btnInviteMember;
    private RecyclerView rvMembersList;

    private TextView tvSettingsRoomName;
    private TextView tvSettingsJoinCode;
    private TextView tvSettingsStatus;
    private TextView tvSettingsTarget;
    private TextView tvSettingsDescription;
    private MaterialCardView cardSettingsOwnerActions;
    private MaterialButton btnSettingsEditRoom;
    private MaterialButton btnSettingsDeleteRoom;
    private MaterialCardView cardSettingsMemberActions;
    private MaterialButton btnSettingsLeaveRoom;

    private final DecimalFormat df = new DecimalFormat("#,###");
    private final int[] categoryColors = new int[]{
            Color.parseColor("#0052CC"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#06B6D4"),
            Color.parseColor("#8B5CF6")
    };

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
        setupAdapters();
        setupBottomNav();
        setupViewModel();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().getWindow() != null) {
            boolean isDark = false;
            if (getActivity() instanceof com.sharedfund.app.MainActivity) {
                isDark = ((com.sharedfund.app.MainActivity) getActivity()).isDarkTheme();
            }
            androidx.core.view.WindowInsetsControllerCompat controller =
                    androidx.core.view.WindowCompat.getInsetsController(
                            getActivity().getWindow(), getActivity().getWindow().getDecorView());
            if (controller != null) {
                controller.setAppearanceLightStatusBars(!isDark);
            }
        }
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void initViews(View view) {
        layoutRoomDetailHeader = view.findViewById(R.id.layoutRoomDetailHeader);
        if (layoutRoomDetailHeader != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(layoutRoomDetailHeader, (v, insets) -> {
                androidx.core.graphics.Insets statusBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                int extraTop = Math.round(18 * getResources().getDisplayMetrics().density);
                int padBottom = Math.round(14 * getResources().getDisplayMetrics().density);
                v.setPadding(v.getPaddingLeft(), statusBars.top + extraTop, v.getPaddingRight(), padBottom);
                return insets;
            });
            androidx.core.view.ViewCompat.requestApplyInsets(layoutRoomDetailHeader);
        }
        btnBack = view.findViewById(R.id.btnBack);
        tvHeaderRoomName = view.findViewById(R.id.tvHeaderRoomName);
        btnHeaderQuickEdit = view.findViewById(R.id.btnHeaderQuickEdit);
        btnDetailCopyCode = view.findViewById(R.id.btnDetailCopyCode);
        tvDetailJoinCode = view.findViewById(R.id.tvDetailJoinCode);

        cardFloatingBottomNav = view.findViewById(R.id.cardFloatingBottomNav);
        bottomNavRoomDetail = view.findViewById(R.id.bottomNavRoomDetail);
        layoutTabHome = view.findViewById(R.id.layoutTabHome);
        layoutTabHistory = view.findViewById(R.id.layoutTabHistory);
        layoutTabMembers = view.findViewById(R.id.layoutTabMembers);
        layoutTabSettings = view.findViewById(R.id.layoutTabSettings);
        pbDetailLoading = view.findViewById(R.id.pbDetailLoading);

        btnChangeRoomBackground = view.findViewById(R.id.btnChangeRoomBackground);
        if (btnHeaderQuickEdit != null) {
            btnHeaderQuickEdit.setColorFilter(Color.WHITE);
        }
        if (btnChangeRoomBackground != null) {
            btnChangeRoomBackground.setColorFilter(Color.WHITE);
        }
        ivRoomCover = view.findViewById(R.id.ivRoomCover);
        tvDetailBalance = view.findViewById(R.id.tvDetailBalance);
        layoutHomeTarget = view.findViewById(R.id.layoutHomeTarget);
        tvHomeTargetText = view.findViewById(R.id.tvHomeTargetText);
        pbHomeTarget = view.findViewById(R.id.pbHomeTarget);
        btnRoomTransfer = view.findViewById(R.id.btnRoomTransfer);
        btnUnifiedTransfer = view.findViewById(R.id.btnUnifiedTransfer);
        btnRoomUtilities = view.findViewById(R.id.btnRoomUtilities);
        tvSeeAllTransactions = view.findViewById(R.id.tvSeeAllTransactions);
        layoutEmptyRecentTx = view.findViewById(R.id.layoutEmptyRecentTx);
        cardRecentTransactions = view.findViewById(R.id.cardRecentTransactions);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);

        donutHistoryChartView = view.findViewById(R.id.donutHistoryChartView);
        tvHistoryTotalIncome = view.findViewById(R.id.tvHistoryTotalIncome);
        tvHistoryTotalExpense = view.findViewById(R.id.tvHistoryTotalExpense);
        tvHistoryNetBalance = view.findViewById(R.id.tvHistoryNetBalance);
        layoutHistoryCategoryBars = view.findViewById(R.id.layoutHistoryCategoryBars);
        tvEmptyCategoryBars = view.findViewById(R.id.tvEmptyCategoryBars);
        btnHistoryFilterAll = view.findViewById(R.id.btnHistoryFilterAll);
        btnHistoryFilterIncome = view.findViewById(R.id.btnHistoryFilterIncome);
        btnHistoryFilterExpense = view.findViewById(R.id.btnHistoryFilterExpense);
        layoutEmptyHistory = view.findViewById(R.id.layoutEmptyHistory);
        rvHistoryTransactions = view.findViewById(R.id.rvHistoryTransactions);

        tvMembersTabCount = view.findViewById(R.id.tvMembersTabCount);
        tvMembersTabTotalContributed = view.findViewById(R.id.tvMembersTabTotalContributed);
        btnInviteMember = view.findViewById(R.id.btnInviteMember);
        rvMembersList = view.findViewById(R.id.rvMembersList);

        tvSettingsRoomName = view.findViewById(R.id.tvSettingsRoomName);
        tvSettingsJoinCode = view.findViewById(R.id.tvSettingsJoinCode);
        tvSettingsStatus = view.findViewById(R.id.tvSettingsStatus);
        tvSettingsTarget = view.findViewById(R.id.tvSettingsTarget);
        tvSettingsDescription = view.findViewById(R.id.tvSettingsDescription);
        cardSettingsOwnerActions = view.findViewById(R.id.cardSettingsOwnerActions);
        btnSettingsEditRoom = view.findViewById(R.id.btnSettingsEditRoom);
        btnSettingsDeleteRoom = view.findViewById(R.id.btnSettingsDeleteRoom);
        cardSettingsMemberActions = view.findViewById(R.id.cardSettingsMemberActions);
        btnSettingsLeaveRoom = view.findViewById(R.id.btnSettingsLeaveRoom);
    }

    private void setupAdapters() {
        recentTransactionAdapter = new TransactionAdapter(this);
        recentTransactionAdapter.setDisplayMode(TransactionAdapter.MODE_RECENT_CARDS);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTransactions.setAdapter(recentTransactionAdapter);

        historyTransactionAdapter = new TransactionAdapter(this);
        historyTransactionAdapter.setDisplayMode(TransactionAdapter.MODE_MOMO_HISTORY);
        rvHistoryTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistoryTransactions.setAdapter(historyTransactionAdapter);

        memberAdapter = new MemberAdapter();
        rvMembersList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMembersList.setAdapter(memberAdapter);
    }

    private void setupBottomNav() {
        if (cardFloatingBottomNav != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(cardFloatingBottomNav, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                if (mlp != null) {
                    mlp.bottomMargin = Math.round(12 * getResources().getDisplayMetrics().density) + systemBars.bottom;
                    v.setLayoutParams(mlp);
                    int extraPad = Math.round(84 * getResources().getDisplayMetrics().density) + systemBars.bottom;
                    if (layoutTabHome != null) layoutTabHome.setPadding(0, 0, 0, extraPad);
                    if (layoutTabHistory != null) layoutTabHistory.setPadding(0, 0, 0, extraPad);
                    if (layoutTabMembers != null) layoutTabMembers.setPadding(0, 0, 0, extraPad);
                    if (layoutTabSettings != null) layoutTabSettings.setPadding(0, 0, 0, extraPad);
                }
                return insets;
            });
        }
        bottomNavRoomDetail.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_room_home) {
                switchTab(0);
                return true;
            } else if (id == R.id.nav_room_history) {
                switchTab(1);
                return true;
            } else if (id == R.id.nav_room_members) {
                switchTab(2);
                return true;
            } else if (id == R.id.nav_room_settings) {
                switchTab(3);
                return true;
            }
            return false;
        });
    }

    private void switchTab(int tabIndex) {
        layoutTabHome.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        layoutTabHistory.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        layoutTabMembers.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        layoutTabSettings.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        viewModel.setRoomId(roomId);

        viewModel.getRoom().observe(getViewLifecycleOwner(), new Observer<Room>() {
            @Override
            public void onChanged(Room room) {
                if (room != null) {
                    renderRoomData(room);
                }
            }
        });

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double income) {
                if (income != null) {
                    String formatted = "+" + df.format(income).replace(',', '.') + " ₫";
                    tvHistoryTotalIncome.setText(formatted);
                    updateNetBalance();
                }
            }
        });

        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double expense) {
                if (expense != null) {
                    String formatted = "-" + df.format(expense).replace(',', '.') + " ₫";
                    tvHistoryTotalExpense.setText(formatted);
                    updateNetBalance();
                }
            }
        });

        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                recentTransactionAdapter.setTransactions(transactions);
                if (cardRecentTransactions != null) {
                    cardRecentTransactions.setVisibility(View.VISIBLE);
                }
                if (transactions == null || transactions.isEmpty()) {
                    if (layoutEmptyRecentTx != null) layoutEmptyRecentTx.setVisibility(View.VISIBLE);
                    if (rvRecentTransactions != null) rvRecentTransactions.setVisibility(View.GONE);
                    if (tvSeeAllTransactions != null) tvSeeAllTransactions.setVisibility(View.GONE);
                } else {
                    if (layoutEmptyRecentTx != null) layoutEmptyRecentTx.setVisibility(View.GONE);
                    if (rvRecentTransactions != null) rvRecentTransactions.setVisibility(View.VISIBLE);
                    if (tvSeeAllTransactions != null) tvSeeAllTransactions.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                historyTransactionAdapter.setTransactions(transactions);
                if (transactions == null || transactions.isEmpty()) {
                    layoutEmptyHistory.setVisibility(View.VISIBLE);
                    rvHistoryTransactions.setVisibility(View.GONE);
                } else {
                    layoutEmptyHistory.setVisibility(View.GONE);
                    rvHistoryTransactions.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getCategoryStats().observe(getViewLifecycleOwner(), new Observer<Map<String, Double>>() {
            @Override
            public void onChanged(Map<String, Double> stats) {
                renderCategoryBreakdown(stats);
            }
        });

        viewModel.getMembers().observe(getViewLifecycleOwner(), new Observer<List<RoomMember>>() {
            @Override
            public void onChanged(List<RoomMember> members) {
                renderMembers(members);
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

    private void renderRoomData(Room room) {
        tvHeaderRoomName.setText(room.getName());
        tvDetailBalance.setText(room.getFormattedBalance());
        tvDetailJoinCode.setText("Mã: " + (room.getJoinCode() != null ? room.getJoinCode() : "—"));
        loadRoomBackground();

        if (room.getTargetAmount() > 0) {
            layoutHomeTarget.setVisibility(View.VISIBLE);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator('.');
            DecimalFormat customDf = new DecimalFormat("#,##0", symbols);
            double percent = (room.getBalance() / room.getTargetAmount()) * 100;
            int progress = (int) Math.min(100, Math.max(0, percent));
            tvHomeTargetText.setText("Mục tiêu: " + customDf.format(room.getTargetAmount()) + " ₫ (" + progress + "%)");
            pbHomeTarget.setProgress(progress);
        } else {
            layoutHomeTarget.setVisibility(View.GONE);
        }

        tvSettingsRoomName.setText(room.getName());
        tvSettingsJoinCode.setText(room.getJoinCode());
        tvSettingsStatus.setText(room.isArchived() ? "Đã lưu trữ" : "Đang hoạt động");
        tvSettingsStatus.setTextColor(ContextCompat.getColor(requireContext(),
                room.isArchived() ? R.color.text_tertiary : R.color.accent_income));

        if (room.getTargetAmount() > 0) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator('.');
            DecimalFormat customDf = new DecimalFormat("#,##0", symbols);
            tvSettingsTarget.setText(customDf.format(room.getTargetAmount()) + " ₫");
        } else {
            tvSettingsTarget.setText("Chưa thiết lập");
        }

        String desc = room.getDescription();
        tvSettingsDescription.setText(desc != null && !desc.trim().isEmpty() ? desc : "Chưa có mô tả cho quỹ.");

        applyRoleBasedUi(room);
    }

    private void updateNetBalance() {
        Double income = viewModel.getTotalIncome().getValue();
        Double expense = viewModel.getTotalExpense().getValue();
        double inc = income != null ? income : 0.0;
        double exp = expense != null ? expense : 0.0;
        double net = inc - exp;
        String prefix = net >= 0 ? "+" : "";
        if (tvHistoryNetBalance != null) {
            tvHistoryNetBalance.setText(prefix + df.format(net).replace(',', '.') + " ₫");
        }
        if (tvHistoryTotalIncome != null) {
            tvHistoryTotalIncome.setText("Tổng thu: +" + df.format(inc).replace(',', '.') + " ₫");
        }
        if (tvHistoryTotalExpense != null) {
            tvHistoryTotalExpense.setText("-" + df.format(exp).replace(',', '.') + " ₫");
        }
    }

    private static final int[] FINTECH_PALETTE = new int[] {
        Color.parseColor("#EC4899"),
        Color.parseColor("#F97316"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#3B82F6"),
        Color.parseColor("#8B5CF6"),
        Color.parseColor("#10B981"),
        Color.parseColor("#64748B")
    };

    private int getCategoryColor(String categoryName, int fallbackIndex) {
        if (categoryName == null) return Color.parseColor("#64748B");
        String lower = categoryName.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("ăn") || lower.contains("uống") || lower.contains("food")) return Color.parseColor("#EF4444");
        if (lower.contains("di chuyển") || lower.contains("xe") || lower.contains("xăng") || lower.contains("đi lại")) return Color.parseColor("#F59E0B");
        if (lower.contains("phòng") || lower.contains("nhà")) return Color.parseColor("#3B82F6");
        if (lower.contains("mua") || lower.contains("sắm") || lower.contains("chợ") || lower.contains("siêu thị")) return Color.parseColor("#6366F1");
        if (lower.contains("điện") || lower.contains("nước") || lower.contains("hóa đơn")) return Color.parseColor("#10B981");
        if (lower.contains("giải trí") || lower.contains("phim") || lower.contains("party") || lower.contains("liên hoan")) return Color.parseColor("#EC4899");
        if (lower.contains("rút")) return Color.parseColor("#64748B");
        return FINTECH_PALETTE[Math.abs(fallbackIndex) % FINTECH_PALETTE.length];
    }

    private void renderCategoryBreakdown(Map<String, Double> map) {
        layoutHistoryCategoryBars.removeAllViews();
        if (map == null || map.isEmpty()) {
            if (donutHistoryChartView != null) {
                donutHistoryChartView.setData(map, FINTECH_PALETTE);
            }
            tvEmptyCategoryBars.setVisibility(View.VISIBLE);
            return;
        }

        double totalExpense = 0.0;
        for (Double amt : map.values()) {
            if (amt != null && amt > 0) {
                totalExpense += amt;
            }
        }

        if (totalExpense <= 0) {
            if (donutHistoryChartView != null) {
                donutHistoryChartView.setData(map, FINTECH_PALETTE);
            }
            tvEmptyCategoryBars.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyCategoryBars.setVisibility(View.GONE);

        List<com.sharedfund.app.ui.widget.DonutChartView.Slice> sliceList = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            double amt = entry.getValue() != null ? entry.getValue() : 0.0;
            if (amt <= 0) continue;
            float percent = (float) ((amt / totalExpense) * 100.0);
            int color = getCategoryColor(entry.getKey(), colorIdx);
            sliceList.add(new com.sharedfund.app.ui.widget.DonutChartView.Slice(entry.getKey(), amt, color, percent));
            colorIdx++;
        }
        java.util.Collections.sort(sliceList, (a, b) -> Double.compare(b.amount, a.amount));

        if (donutHistoryChartView != null) {
            donutHistoryChartView.setSlices(sliceList);
        }

        int limit = (!isCategoryListExpanded && sliceList.size() > 3) ? 3 : sliceList.size();
        for (int i = 0; i < limit; i++) {
            com.sharedfund.app.ui.widget.DonutChartView.Slice slice = sliceList.get(i);
            int percent = (int) Math.round(slice.percentage);
            int color = slice.color;

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 16, 0, 16);

            View square = new View(requireContext());
            LinearLayout.LayoutParams sqParams = new LinearLayout.LayoutParams(28, 28);
            sqParams.setMarginEnd(20);
            square.setLayoutParams(sqParams);
            android.graphics.drawable.GradientDrawable sqDrawable = new android.graphics.drawable.GradientDrawable();
            sqDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            sqDrawable.setCornerRadius(6);
            sqDrawable.setColor(color);
            square.setBackground(sqDrawable);

            TextView tvCategoryInfo = new TextView(requireContext());
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tvCategoryInfo.setLayoutParams(infoParams);
            tvCategoryInfo.setText(slice.label + " (" + percent + "%)");
            tvCategoryInfo.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvCategoryInfo.setTextSize(13);

            TextView tvCategoryAmount = new TextView(requireContext());
            tvCategoryAmount.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tvCategoryAmount.setText(df.format(slice.amount).replace(',', '.') + " ₫");
            tvCategoryAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvCategoryAmount.setTextSize(13);
            tvCategoryAmount.setTypeface(null, Typeface.BOLD);

            row.addView(square);
            row.addView(tvCategoryInfo);
            row.addView(tvCategoryAmount);
            layoutHistoryCategoryBars.addView(row);
        }

        if (sliceList.size() > 3) {
            TextView btnToggle = new TextView(requireContext());
            LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            toggleParams.setMargins(0, 10, 0, 4);
            btnToggle.setLayoutParams(toggleParams);
            btnToggle.setGravity(Gravity.CENTER);
            btnToggle.setPadding(0, 14, 0, 14);
            btnToggle.setTextSize(12.5f);
            btnToggle.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            btnToggle.setTypeface(null, Typeface.BOLD);
            btnToggle.setBackgroundResource(R.drawable.bg_card);

            if (!isCategoryListExpanded) {
                int remaining = sliceList.size() - 3;
                double remainingAmount = 0.0;
                for (int i = 3; i < sliceList.size(); i++) {
                    remainingAmount += sliceList.get(i).amount;
                }
                btnToggle.setText("Xem thêm " + remaining + " danh mục khác (" + df.format(remainingAmount).replace(',', '.') + " ₫)  ▼");
            } else {
                btnToggle.setText("Thu gọn danh mục  ▲");
            }

            btnToggle.setOnClickListener(v -> {
                isCategoryListExpanded = !isCategoryListExpanded;
                renderCategoryBreakdown(map);
            });
            layoutHistoryCategoryBars.addView(btnToggle);
        }
    }

    private void renderMembers(List<RoomMember> members) {
        if (members == null) return;
        memberAdapter.setMembers(members);
        tvMembersTabCount.setText(members.size() + " thành viên");

        double total = 0.0;
        for (RoomMember m : members) {
            total += m.getContributedAmount();
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat customDf = new DecimalFormat("#,##0", symbols);
        tvMembersTabTotalContributed.setText("Tổng đóng góp: " + customDf.format(total) + " ₫");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        btnHeaderQuickEdit.setOnClickListener(v -> showEditRoomDialog());
        if (btnChangeRoomBackground != null) {
            btnChangeRoomBackground.setOnClickListener(v -> showChangeBackgroundDialog());
        }
        btnSettingsEditRoom.setOnClickListener(v -> showEditRoomDialog());

        btnDetailCopyCode.setOnClickListener(v -> copyJoinCode());

        if (btnRoomTransfer != null) {
            btnRoomTransfer.setOnClickListener(v -> showRoomTransferSheet());
        }

        if (btnUnifiedTransfer != null) {
            btnUnifiedTransfer.setOnClickListener(v -> showUnifiedTransferSheet());
        }

        if (btnRoomUtilities != null) {
            btnRoomUtilities.setOnClickListener(v -> showRoomUtilitiesSheet());
        }

        if (tvSeeAllTransactions != null) {
            tvSeeAllTransactions.setOnClickListener(v -> bottomNavRoomDetail.setSelectedItemId(R.id.nav_room_history));
        }

        btnInviteMember.setOnClickListener(v -> showInviteMemberSheet());

        btnHistoryFilterAll.setOnClickListener(v -> updateHistoryFilter(RoomViewModel.FilterType.ALL));
        btnHistoryFilterIncome.setOnClickListener(v -> updateHistoryFilter(RoomViewModel.FilterType.INCOME));
        btnHistoryFilterExpense.setOnClickListener(v -> updateHistoryFilter(RoomViewModel.FilterType.EXPENSE));

        btnSettingsDeleteRoom.setOnClickListener(v -> confirmDeleteRoom());
        btnSettingsLeaveRoom.setOnClickListener(v -> confirmLeaveRoom());
    }

    private void showSplitBillDialog() {
        Room room = viewModel.getRoom().getValue();
        int count = 1;
        if (room != null && room.getMembers() != null && !room.getMembers().isEmpty()) {
            count = room.getMembers().size();
        } else {
            List<RoomMember> mList = viewModel.getMembers().getValue();
            if (mList != null && !mList.isEmpty()) count = mList.size();
        }
        final int memberCount = Math.max(1, count);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView tvDesc = new TextView(requireContext());
        tvDesc.setText("Chia đều hóa đơn cho " + memberCount + " thành viên trong quỹ");
        tvDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tvDesc.setTextSize(14);
        tvDesc.setPadding(0, 0, 0, pad / 2);
        layout.addView(tvDesc);

        TextInputLayout til = new TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        til.setHint("Tổng tiền hóa đơn (₫)");
        til.setBoxCornerRadii(
                12 * getResources().getDisplayMetrics().density,
                12 * getResources().getDisplayMetrics().density,
                12 * getResources().getDisplayMetrics().density,
                12 * getResources().getDisplayMetrics().density);

        TextInputEditText et = new TextInputEditText(til.getContext());
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setTextSize(18);
        et.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        til.addView(et);
        layout.addView(til);

        TextView tvResult = new TextView(requireContext());
        tvResult.setTextSize(15);
        tvResult.setTypeface(null, Typeface.BOLD);
        tvResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        tvResult.setPadding(0, pad / 2, 0, 0);
        tvResult.setText("Mỗi người: 0 ₫");
        layout.addView(tvResult);

        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s != null ? s.toString().trim() : "";
                try {
                    double amt = Double.parseDouble(str);
                    double each = amt / memberCount;
                    tvResult.setText("Mỗi người: " + df.format(each).replace(',', '.') + " ₫ (" + memberCount + " người)");
                } catch (Exception ex) {
                    tvResult.setText("Mỗi người: 0 ₫");
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chia đều chi phí nhóm")
                .setView(layout)
                .setPositiveButton("Sao chép", (dialog, which) -> {
                    String str = et.getText() != null ? et.getText().toString().trim() : "";
                    double amt = 0.0;
                    try { amt = Double.parseDouble(str); } catch (Exception ignored) {}
                    double each = amt / memberCount;
                    String roomName = room != null ? room.getName() : "Quỹ nhóm";
                    String text = "[SharedFund - Chia tiền nhóm]\n"
                            + "Quỹ: " + roomName + "\n"
                            + "Tổng tiền: " + df.format(amt).replace(',', '.') + " ₫\n"
                            + "Số người: " + memberCount + "\n"
                            + "Mỗi người: " + df.format(each).replace(',', '.') + " ₫";
                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("Split Bill", text));
                        AppToaster.show(RoomDetailFragment.this, AppToaster.ToastType.SUCCESS, "Đã sao chép: " + df.format(each).replace(',', '.') + " ₫/người");
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showRemindDialog() {
        Room room = viewModel.getRoom().getValue();
        String roomName = room != null ? room.getName() : "Quỹ nhóm";
        String joinCode = room != null && room.getJoinCode() != null ? room.getJoinCode() : "";

        String msg = "[SharedFund - Nhắc nhở đóng quỹ]\n"
                + "Thân gửi các thành viên quỹ '" + roomName + "',\n"
                + "Vui lòng kiểm tra và đóng quỹ đúng hạn nhé!\n"
                + "Mã tham gia quỹ: " + joinCode;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nhắc đóng quỹ nhóm")
                .setMessage(msg)
                .setPositiveButton("Sao chép", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("Fund Reminder", msg));
                        AppToaster.show(RoomDetailFragment.this, AppToaster.ToastType.SUCCESS, "Đã sao chép tin nhắn nhắc đóng quỹ!");
                    }
                })
                .setNeutralButton("Chia sẻ", (dialog, which) -> {
                    android.content.Intent sendIntent = new android.content.Intent();
                    sendIntent.setAction(android.content.Intent.ACTION_SEND);
                    sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, msg);
                    sendIntent.setType("text/plain");
                    startActivity(android.content.Intent.createChooser(sendIntent, "Chia sẻ lời nhắc đóng quỹ"));
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showChangeBackgroundDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://.../cover.jpg");
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đổi ảnh bìa quỹ")
                .setMessage("Nhập đường dẫn ảnh mới cho quỹ:")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!url.isEmpty()) {
                        requireContext().getSharedPreferences("shared_fund_room_bg", Context.MODE_PRIVATE)
                                .edit()
                                .putString("bg_" + roomId, url)
                                .apply();
                        loadRoomBackground();
                        AppToaster.success(requireActivity(), "Đã cập nhật ảnh bìa quỹ!");
                    }
                })
                .show();
    }

    private void loadRoomBackground() {
        if (ivRoomCover == null || roomId == null || getContext() == null) return;
        String savedBg = requireContext().getSharedPreferences("shared_fund_room_bg", Context.MODE_PRIVATE)
                .getString("bg_" + roomId, "");
        if (savedBg != null && !savedBg.trim().isEmpty()) {
            ivRoomCover.setVisibility(View.VISIBLE);
            com.sharedfund.app.ui.profile.AvatarImageLoader.load(ivRoomCover, null, savedBg);
        } else {
            ivRoomCover.setVisibility(View.GONE);
        }
    }

    private void startQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Quét mã QR VietQR để chuyển tiền");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setCaptureActivity(CaptureActivity.class);
        qrScanner.launch(options);
    }

    private void showRoomTransferSheet() {
        Room room = viewModel.getRoom().getValue();
        double balance = room != null ? room.getBalance() : 0.0;
        if (getActivity() instanceof com.sharedfund.app.MainActivity) {
            ((com.sharedfund.app.MainActivity) getActivity()).openRoomTransfer(roomId, balance);
        }
    }

    private void showTransferSheetWithQr(ScannedQrPayment parsed) {
        Room room = viewModel.getRoom().getValue();
        double balance = room != null ? room.getBalance() : 0.0;
        if (getActivity() instanceof com.sharedfund.app.MainActivity) {
            ((com.sharedfund.app.MainActivity) getActivity()).openRoomTransfer(roomId, balance);
        }
    }

    private void showUnifiedTransferSheet() {
        Room room = viewModel.getRoom().getValue();
        double balance = room != null ? room.getBalance() : 0.0;
        if (getActivity() instanceof com.sharedfund.app.MainActivity) {
            ((com.sharedfund.app.MainActivity) getActivity()).openFundAction(roomId, balance);
        }
    }

    private void showRoomUtilitiesSheet() {
        RoomUtilitiesBottomSheet sheet = RoomUtilitiesBottomSheet.newInstance();
        sheet.setListener(new RoomUtilitiesBottomSheet.OnUtilityActionListener() {
            @Override
            public void onSplitBillSelected() {
                showSplitBillDialog();
            }

            @Override
            public void onRemindSelected() {
                showRemindDialog();
            }
        });
        sheet.show(getChildFragmentManager(), "room_utilities_sheet");
    }

    private void showInviteMemberSheet() {
        Room room = viewModel.getRoom().getValue();
        String name = room != null ? room.getName() : "Quỹ nhóm";
        String code = room != null ? room.getJoinCode() : "";

        InviteMemberBottomSheet sheet = InviteMemberBottomSheet.newInstance(name, code);
        sheet.show(getChildFragmentManager(), "invite_sheet");
    }

    private void showEditRoomDialog() {
        Room room = viewModel.getRoom().getValue();
        if (room == null || getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_room, null);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilEditRoomName);
        TextInputEditText etName = dialogView.findViewById(R.id.etEditRoomName);
        TextInputLayout tilTarget = dialogView.findViewById(R.id.tilEditRoomTarget);
        TextInputEditText etTarget = dialogView.findViewById(R.id.etEditRoomTarget);
        TextInputLayout tilDesc = dialogView.findViewById(R.id.tilEditRoomDesc);
        TextInputEditText etDesc = dialogView.findViewById(R.id.etEditRoomDesc);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelEditRoom);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveEditRoom);

        etName.setText(room.getName());
        etDesc.setText(room.getDescription() != null ? room.getDescription() : "");
        if (room.getTargetAmount() > 0) {
            etTarget.setText(String.valueOf((long) room.getTargetAmount()));
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
            String newDesc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            String targetStr = etTarget.getText() != null ? etTarget.getText().toString().trim() : "";

            if (newName.isEmpty()) {
                tilName.setError("Tên quỹ không được để trống");
                return;
            }
            tilName.setError(null);

            double targetAmount = 0.0;
            if (!targetStr.isEmpty()) {
                try {
                    targetAmount = Double.parseDouble(targetStr);
                } catch (NumberFormatException e) {
                    tilTarget.setError("Số tiền mục tiêu không hợp lệ");
                    return;
                }
            }
            tilTarget.setError(null);

            dialog.dismiss();
            if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.VISIBLE);

            viewModel.updateRoom(newName, newDesc, targetAmount, new RoomRepository.DataCallback<Room>() {
                @Override
                public void onSuccess(Room value) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.GONE);
                        AppToaster.success(requireActivity(), "Cập nhật thông tin quỹ thành công!");
                    });
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.GONE);
                        AppToaster.error(requireActivity(), "Cập nhật thất bại: " + message);
                    });
                }
            });
        });

        dialog.show();
    }

    private void updateHistoryFilter(RoomViewModel.FilterType filter) {
        viewModel.setFilter(filter);
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int transparentColor = Color.TRANSPARENT;
        int activeTextColor = Color.WHITE;
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        setFilterButtonState(btnHistoryFilterAll, filter == RoomViewModel.FilterType.ALL,
                primaryColor, transparentColor, activeTextColor, inactiveTextColor);
        setFilterButtonState(btnHistoryFilterIncome, filter == RoomViewModel.FilterType.INCOME,
                primaryColor, transparentColor, activeTextColor, inactiveTextColor);
        setFilterButtonState(btnHistoryFilterExpense, filter == RoomViewModel.FilterType.EXPENSE,
                primaryColor, transparentColor, activeTextColor, inactiveTextColor);
    }

    private void setFilterButtonState(MaterialButton button, boolean selected, int primaryColor,
                                      int transparentColor, int activeTextColor, int inactiveTextColor) {
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? primaryColor : transparentColor));
        button.setTextColor(selected ? activeTextColor : inactiveTextColor);
    }

    private void applyRoleBasedUi(Room room) {
        String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        boolean isOwner = room != null && ("OWNER".equalsIgnoreCase(room.getCurrentUserRole())
                || (currentUserId != null && currentUserId.equals(room.getCreatedBy())));

        cardSettingsOwnerActions.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        cardSettingsMemberActions.setVisibility(!isOwner ? View.VISIBLE : View.GONE);
    }

    private void confirmDeleteRoom() {
        Room room = viewModel.getRoom().getValue();
        if (room == null || getContext() == null) return;
        if (room.getBalance() > 0) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Không thể đóng quỹ")
                    .setMessage("Quỹ hiện vẫn còn số dư (" + room.getFormattedBalance() + "). Vui lòng giải ngân hoặc chi hết số dư trước khi đóng quỹ.")
                    .setPositiveButton("Đã hiểu", null)
                    .show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận đóng quỹ")
                .setMessage("Bạn có chắc chắn muốn đóng quỹ \"" + room.getName() + "\"? Toàn bộ dữ liệu giao dịch và thành viên sẽ bị xóa vĩnh viễn.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đóng quỹ", (dialog, which) -> executeDeleteRoom())
                .show();
    }

    private void executeDeleteRoom() {
        if (viewModel == null || getContext() == null) return;
        String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.VISIBLE);

        viewModel.deleteRoom(currentUserId, new RoomRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.GONE);
                    AppToaster.success(requireActivity(), "Đã đóng quỹ thành công!");
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openHome();
                    } else if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.GONE);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Đóng quỹ thất bại")
                            .setMessage(message)
                            .setPositiveButton("Đóng", null)
                            .show();
                });
            }
        });
    }

    private void confirmLeaveRoom() {
        Room room = viewModel.getRoom().getValue();
        if (room == null || getContext() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận rời quỹ")
                .setMessage("Bạn có chắc chắn muốn rời khỏi quỹ \"" + room.getName() + "\"? Bạn sẽ không thể tiếp tục theo dõi hay thao tác trên quỹ này.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Rời quỹ", (dialog, which) -> executeLeaveRoom())
                .show();
    }

    private void executeLeaveRoom() {
        if (viewModel == null || getContext() == null) return;
        String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.VISIBLE);

        viewModel.leaveRoom(currentUserId, new RoomRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.GONE);
                    AppToaster.success(requireActivity(), "Đã rời khỏi quỹ!");
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openHome();
                    } else if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.GONE);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Rời quỹ thất bại")
                            .setMessage(message)
                            .setPositiveButton("Đóng", null)
                            .show();
                });
            }
        });
    }

    private boolean isCurrentUserAdmin() {
        Room room = viewModel != null ? viewModel.getRoom().getValue() : null;
        if (room == null) return false;
        String role = room.getCurrentUserRole();
        if (role != null) return "ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role);
        String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        if (currentUserId.equals(room.getCreatedBy())) return true;
        return RoomRepository.getInstance().isCurrentUserAdmin(roomId);
    }

    private void copyJoinCode() {
        Room r = viewModel.getRoom().getValue();
        if (r != null) {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Join Code", r.getJoinCode());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                AppToaster.info(requireActivity(), getString(R.string.copied_code) + " (" + r.getJoinCode() + ")");
            }
        }
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        TransactionDetailBottomSheet sheet = TransactionDetailBottomSheet.newInstance(transaction, isCurrentUserAdmin());
        sheet.setActionListener(new TransactionDetailBottomSheet.OnTransactionActionListener() {
            @Override
            public void onApprove(Transaction tx) {
                UserSessionManager session = UserSessionManager.getInstance(requireContext());
                String adminId = session.getUserId();
                RoomRepository.getInstance().configureAccessToken(session.getAccessToken());
                com.sharedfund.app.data.TransactionRepository remote = new com.sharedfund.app.data.TransactionRepository();
                remote.setAccessToken(session.getAccessToken());
                remote.approveExpense(adminId, tx.getId(), new com.sharedfund.app.data.TransactionRepository.DataCallback<com.google.gson.JsonObject>() {
                    @Override
                    public void onSuccess(com.google.gson.JsonObject data) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                RoomRepository.getInstance().loadUserProfile();
                                RoomRepository.getInstance().refreshRoomTransactions(roomId, new RoomRepository.DataCallback<List<Transaction>>() {
                                    @Override
                                    public void onSuccess(List<Transaction> value) {
                                        if (isAdded()) {
                                            requireActivity().runOnUiThread(() -> {
                                                if (viewModel != null) viewModel.refresh();
                                            });
                                        }
                                    }

                                    @Override
                                    public void onError(String message) {
                                        if (isAdded()) {
                                            requireActivity().runOnUiThread(() -> {
                                                if (viewModel != null) viewModel.refresh();
                                            });
                                        }
                                    }
                                });
                                AppToaster.success(requireActivity(), "Đã duyệt khoản chi thành công!");
                                if (viewModel != null) viewModel.refresh();
                            });
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> AppToaster.error(requireActivity(), "Phê duyệt thất bại: " + errorMessage));
                        }
                    }
                });
            }

            @Override
            public void onReject(Transaction tx, String reason) {
                UserSessionManager session = UserSessionManager.getInstance(requireContext());
                String adminId = session.getUserId();
                RoomRepository.getInstance().configureAccessToken(session.getAccessToken());
                com.sharedfund.app.data.TransactionRepository remote = new com.sharedfund.app.data.TransactionRepository();
                remote.setAccessToken(session.getAccessToken());
                remote.rejectExpense(adminId, tx.getId(), reason, new com.sharedfund.app.data.TransactionRepository.DataCallback<com.google.gson.JsonObject>() {
                    @Override
                    public void onSuccess(com.google.gson.JsonObject data) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                RoomRepository.getInstance().refreshRoomTransactions(roomId, new RoomRepository.DataCallback<List<Transaction>>() {
                                    @Override
                                    public void onSuccess(List<Transaction> value) {
                                        if (isAdded()) {
                                            requireActivity().runOnUiThread(() -> {
                                                if (viewModel != null) viewModel.refresh();
                                            });
                                        }
                                    }

                                    @Override
                                    public void onError(String message) {
                                        if (isAdded()) {
                                            requireActivity().runOnUiThread(() -> {
                                                if (viewModel != null) viewModel.refresh();
                                            });
                                        }
                                    }
                                });
                                AppToaster.info(requireActivity(), "Đã từ chối khoản chi!");
                                if (viewModel != null) viewModel.refresh();
                            });
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> AppToaster.error(requireActivity(), "Từ chối thất bại: " + errorMessage));
                        }
                    }
                });
            }
        });
        sheet.show(getChildFragmentManager(), "room_tx_detail");
    }
}
