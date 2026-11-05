package com.sharedfund.app.ui.room;

import com.sharedfund.app.data.RoomPrivacyManager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;

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


import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.app.Dialog;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.EditText;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sharedfund.app.ui.home.WalletStatementAdapter;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.InputStream;
import android.widget.FrameLayout;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.util.CopyToast;
import com.sharedfund.app.util.ErrorMessageHelper;
import com.sharedfund.app.adapter.MemberAdapter;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.transaction.TransactionAdapter;
import com.sharedfund.app.ui.transaction.TransactionDetailBottomSheet;
import com.sharedfund.app.ui.widget.AppToaster;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomDetailFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private static final String ARG_ROOM_ID = "arg_room_id";

    private String roomId;
    private RoomViewModel viewModel;

    private TransactionAdapter recentTransactionAdapter;
    private WalletStatementAdapter historyTransactionAdapter;
    private MemberAdapter memberAdapter;

    private View layoutRoomDetailHeader;
    private ImageButton btnBack;
    private TextView tvHeaderRoomName;
    private ImageButton btnHeaderQuickEdit;
    private View btnDetailCopyCode;
    private TextView tvDetailJoinCode;

    private View cardFloatingBottomNav;
    private View viewNavSlidingFrame;
    private FrameLayout tabNavHome;
    private FrameLayout tabNavHistory;
    private FrameLayout tabNavMembers;
    private FrameLayout tabNavSettings;
    private ImageView ivNavHome;
    private ImageView ivNavHistory;
    private ImageView ivNavMembers;
    private ImageView ivNavSettings;
    private int currentTabIndex = 0;
    private ActivityResultLauncher<String> roomCoverPicker;
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
    private LinearLayout layoutEmptyRecentTx;
    private View cardRecentTransactions;
    private RecyclerView rvRecentTransactions;
    private View layoutPendingApprovalBanner;
    private TextView tvPendingApprovalCount;
    private TextView tvPendingApprovalSummary;


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
    private MaterialButton btnInviteMember;
    private RecyclerView rvMembersList;

    private View cardSettingsPrivacy;
    private View btnSettingsPrivacyToggle;
    private TextView tvSettingsPrivacyStatus;
    private TextView tvSettingsPrivacyDesc;
    private View cardSettingsRoomInfo;
    private View btnSettingsEditRoom;
    private View cardSettingsCloseFund;
    private View btnSettingsDeleteRoom;
    private View cardMemberManagement;
    private TextView tvMemberManagementHeader;
    private View btnMemberDelegation;
    private View btnMemberJoinRequests;
    private View dividerMemberDelegation;
    private TextView badgeJoinRequestsCount;
    private boolean isRoomPrivate = true;
    private MaterialCardView cardSettingsMemberActions;
    private View btnSettingsLeaveRoom;

    private static class PendingJoinRequest {
        final String userId;
        final String applicantName;
        final String email;
        final String time;
        final String message;

        PendingJoinRequest(String userId, String applicantName, String email, String time, String message) {
            this.userId = userId;
            this.applicantName = applicantName;
            this.email = email;
            this.time = time;
            this.message = message;
        }

        PendingJoinRequest(String applicantName, String email, String time, String message) {
            this("mock-id", applicantName, email, time, message);
        }
    }
    private final List<PendingJoinRequest> pendingJoinRequests = new ArrayList<>();

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
        roomCoverPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && getContext() != null) {
                String targetName = "koin_cover_room_" + (roomId != null ? roomId : "default") + ".jpg";
                com.sharedfund.app.util.ImageCropHelper.cropAndSaveCoverAsync(requireContext(), uri, targetName, new com.sharedfund.app.util.ImageCropHelper.OnCropFinishedListener() {
                    @Override
                    public void onSuccess(String fileUri, android.graphics.Bitmap bitmap) {
                        saveRoomBackground(fileUri);
                        if (ivRoomCover != null) {
                            ivRoomCover.setImageBitmap(bitmap);
                            ivRoomCover.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        saveRoomBackground(uri.toString());
                    }
                });
            }
        });
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
        viewNavSlidingFrame = view.findViewById(R.id.viewNavSlidingFrame);
        tabNavHome = view.findViewById(R.id.tabNavHome);
        tabNavHistory = view.findViewById(R.id.tabNavHistory);
        tabNavMembers = view.findViewById(R.id.tabNavMembers);
        tabNavSettings = view.findViewById(R.id.tabNavSettings);
        ivNavHome = view.findViewById(R.id.ivNavHome);
        ivNavHistory = view.findViewById(R.id.ivNavHistory);
        ivNavMembers = view.findViewById(R.id.ivNavMembers);
        ivNavSettings = view.findViewById(R.id.ivNavSettings);
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
        layoutEmptyRecentTx = view.findViewById(R.id.layoutEmptyRecentTx);
        cardRecentTransactions = view.findViewById(R.id.cardRecentTransactions);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);
        layoutPendingApprovalBanner = view.findViewById(R.id.layoutPendingApprovalBanner);
        tvPendingApprovalCount = view.findViewById(R.id.tvPendingApprovalCount);
        tvPendingApprovalSummary = view.findViewById(R.id.tvPendingApprovalSummary);
        if (layoutPendingApprovalBanner != null) {
            layoutPendingApprovalBanner.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.985f).scaleY(0.985f).alpha(0.92f).setDuration(120).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                        v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(150).withEndAction(() -> {
                            if (isAdded()) {
                                openExpenseApprovalScreen();
                            }
                        }).start();
                        v.performClick();
                        break;
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(150).start();
                        break;
                }
                return true;
            });
        }

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
        btnInviteMember = view.findViewById(R.id.btnInviteMember);
        rvMembersList = view.findViewById(R.id.rvMembersList);

        cardSettingsPrivacy = view.findViewById(R.id.cardSettingsPrivacy);
        btnSettingsPrivacyToggle = view.findViewById(R.id.btnSettingsPrivacyToggle);
        tvSettingsPrivacyStatus = view.findViewById(R.id.tvSettingsPrivacyStatus);
        tvSettingsPrivacyDesc = view.findViewById(R.id.tvSettingsPrivacyDesc);
        cardSettingsRoomInfo = view.findViewById(R.id.cardSettingsRoomInfo);
        btnSettingsEditRoom = view.findViewById(R.id.btnSettingsEditRoom);
        cardSettingsCloseFund = view.findViewById(R.id.cardSettingsCloseFund);
        btnSettingsDeleteRoom = view.findViewById(R.id.btnSettingsDeleteRoom);
        cardMemberManagement = view.findViewById(R.id.cardMemberManagement);
        tvMemberManagementHeader = view.findViewById(R.id.tvMemberManagementHeader);
        btnMemberDelegation = view.findViewById(R.id.btnMemberDelegation);
        dividerMemberDelegation = view.findViewById(R.id.dividerMemberDelegation);
        btnMemberJoinRequests = view.findViewById(R.id.btnMemberJoinRequests);
        badgeJoinRequestsCount = view.findViewById(R.id.badgeJoinRequestsCount);
        cardSettingsMemberActions = view.findViewById(R.id.cardSettingsMemberActions);
        btnSettingsLeaveRoom = view.findViewById(R.id.btnSettingsLeaveRoom);

        initPendingJoinRequests();
    }

    private void setupAdapters() {
        recentTransactionAdapter = new TransactionAdapter(this);
        recentTransactionAdapter.setDisplayMode(TransactionAdapter.MODE_RECENT_CARDS);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTransactions.setAdapter(recentTransactionAdapter);

        historyTransactionAdapter = new WalletStatementAdapter(this::onTransactionClick);
        historyTransactionAdapter.setDisplayMode(WalletStatementAdapter.MODE_ROOM);
        rvHistoryTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistoryTransactions.setAdapter(historyTransactionAdapter);

        memberAdapter = new MemberAdapter();
        memberAdapter.setOnMemberMoreClickListener(this::showMemberCustomizeBottomSheet);
        rvMembersList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMembersList.setAdapter(memberAdapter);
    }

    private void setupBottomNav() {
        if (cardFloatingBottomNav != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(cardFloatingBottomNav, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                if (mlp != null) {
                    mlp.bottomMargin = Math.round(8 * getResources().getDisplayMetrics().density);
                    v.setLayoutParams(mlp);
                    int extraPad = Math.round(76 * getResources().getDisplayMetrics().density) + systemBars.bottom;
                    if (layoutTabHome != null) layoutTabHome.setPadding(0, 0, 0, extraPad);
                    if (layoutTabHistory != null) layoutTabHistory.setPadding(0, 0, 0, extraPad);
                    if (layoutTabMembers != null) layoutTabMembers.setPadding(0, 0, 0, extraPad);
                    if (layoutTabSettings != null) layoutTabSettings.setPadding(0, 0, 0, extraPad);
                }
                return insets;
            });
        }

        setupTabTouch(tabNavHome, 0);
        setupTabTouch(tabNavHistory, 1);
        setupTabTouch(tabNavMembers, 2);
        setupTabTouch(tabNavSettings, 3);

        if (cardFloatingBottomNav != null) {
            cardFloatingBottomNav.post(() -> selectNavTab(currentTabIndex, false));
        }
    }

    private void setupTabTouch(FrameLayout tab, int index) {
        if (tab == null) return;
        tab.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    onTabTouchDown(index);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                    v.performClick();
                    onTabTouchUp(index);
                    return true;
                case android.view.MotionEvent.ACTION_CANCEL:
                    onTabTouchCancel(index);
                    return true;
            }
            return false;
        });
    }

    private void onTabTouchDown(int index) {
        if (cardFloatingBottomNav != null) {
            cardFloatingBottomNav.animate()
                    .scaleX(1.025f)
                    .scaleY(1.035f)
                    .setDuration(130)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
        if (viewNavSlidingFrame != null) {
            viewNavSlidingFrame.setBackgroundResource(R.drawable.bg_nav_active_frame_glass);
            viewNavSlidingFrame.animate()
                    .scaleX(1.20f)
                    .scaleY(1.16f)
                    .setDuration(130)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
    }

    private void onTabTouchUp(int index) {
        if (cardFloatingBottomNav != null) {
            cardFloatingBottomNav.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(280)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                    .start();
        }
        if (viewNavSlidingFrame != null) {
            viewNavSlidingFrame.setBackgroundResource(R.drawable.bg_nav_active_frame);
            viewNavSlidingFrame.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(280)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.35f))
                    .start();
        }
        selectNavTab(index, true);
    }

    private void onTabTouchCancel(int index) {
        if (cardFloatingBottomNav != null) {
            cardFloatingBottomNav.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start();
        }
        if (viewNavSlidingFrame != null) {
            viewNavSlidingFrame.setBackgroundResource(R.drawable.bg_nav_active_frame);
            viewNavSlidingFrame.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start();
        }
    }

    public void selectNavTab(int index, boolean animate) {
        currentTabIndex = index;
        switchTab(index);

        FrameLayout[] tabs = new FrameLayout[]{tabNavHome, tabNavHistory, tabNavMembers, tabNavSettings};
        ImageView[] icons = new ImageView[]{ivNavHome, ivNavHistory, ivNavMembers, ivNavSettings};

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_primary);

        for (int i = 0; i < icons.length; i++) {
            if (icons[i] == null) continue;
            boolean isActive = (i == index);
            icons[i].setColorFilter(isActive ? primaryColor : inactiveColor);
            icons[i].setScaleX(1.0f);
            icons[i].setScaleY(1.0f);
        }

        if (viewNavSlidingFrame != null && index >= 0 && index < tabs.length && tabs[index] != null) {
            FrameLayout targetTab = tabs[index];
            int frameW = viewNavSlidingFrame.getWidth();
            if (frameW <= 0) {
                frameW = Math.round(60 * getResources().getDisplayMetrics().density);
            }
            float targetX = targetTab.getLeft() + (targetTab.getWidth() - frameW) / 2f;
            if (animate) {
                viewNavSlidingFrame.animate()
                        .translationX(targetX)
                        .setDuration(240)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .start();
            } else {
                viewNavSlidingFrame.setTranslationX(targetX);
            }
        }
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
                } else {
                    if (isAdded() && getActivity() != null && !isStateSaved()) {
                        AppToaster.info(requireActivity(), "Phòng quỹ này đã bị đóng hoặc không còn tồn tại.");
                        getParentFragmentManager().popBackStack();
                    }
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
                } else {
                    if (layoutEmptyRecentTx != null) layoutEmptyRecentTx.setVisibility(View.GONE);
                    if (rvRecentTransactions != null) rvRecentTransactions.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getPendingTransactions().observe(getViewLifecycleOwner(), new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> pendingList) {
                updatePendingApprovalBanner(pendingList);
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
        if (room == null) return;
        if (this.roomId == null || this.roomId.isEmpty()) {
            this.roomId = room.getId();
            initPendingJoinRequests();
        }
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



        applyRoleBasedUi(room);
        if (viewModel != null) {
            updatePendingApprovalBanner(viewModel.getPendingTransactions().getValue());
        }
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
            row.setPadding(0, dpToPx(8), 0, dpToPx(8));

            View square = new View(requireContext());
            LinearLayout.LayoutParams sqParams = new LinearLayout.LayoutParams(dpToPx(12), dpToPx(12));
            sqParams.setMarginEnd(dpToPx(8));
            square.setLayoutParams(sqParams);
            android.graphics.drawable.GradientDrawable sqDrawable = new android.graphics.drawable.GradientDrawable();
            sqDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            sqDrawable.setCornerRadius(dpToPx(3));
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
            toggleParams.setMargins(0, dpToPx(4), 0, dpToPx(2));
            btnToggle.setLayoutParams(toggleParams);
            btnToggle.setGravity(Gravity.CENTER);
            btnToggle.setPadding(0, dpToPx(6), 0, dpToPx(6));
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

    private Set<String> getRoomTreasurerIds(String roomId) {
        if (getContext() == null || roomId == null) return new HashSet<>();
        Set<String> set = requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                .getStringSet("treasurer_ids", new HashSet<>());
        return new HashSet<>(set);
    }

    private void addRoomTreasurer(String roomId, String userId) {
        if (getContext() == null || roomId == null || userId == null) return;
        Set<String> set = getRoomTreasurerIds(roomId);
        set.add(userId);
        requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                .edit()
                .putStringSet("treasurer_ids", set)
                .putBoolean("treasurer_" + userId, true)
                .apply();
    }

    private void removeRoomTreasurer(String roomId, String userId) {
        if (getContext() == null || roomId == null || userId == null) return;
        Set<String> set = getRoomTreasurerIds(roomId);
        set.remove(userId);
        requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                .edit()
                .putStringSet("treasurer_ids", set)
                .remove("treasurer_" + userId)
                .apply();
    }

    private boolean isSavedTreasurer(String roomId, String userId) {
        if (getContext() == null || roomId == null || userId == null) return false;
        return getRoomTreasurerIds(roomId).contains(userId)
                || requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                .getBoolean("treasurer_" + userId, false);
    }

    private boolean isCurrentUserOwner() {
        Room room = viewModel.getRoom().getValue();
        String rId = room != null ? room.getId() : this.roomId;
        if (rId != null && getContext() != null) {
            String savedOwnerId = requireContext().getSharedPreferences("room_roles_" + rId, Context.MODE_PRIVATE)
                    .getString("owner_id", null);
            if (savedOwnerId != null) {
                String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
                return currentUserId != null && currentUserId.equals(savedOwnerId);
            }
        }
        String currentUserId = getContext() != null ? UserSessionManager.getInstance(requireContext()).getUserId() : null;
        return room != null && ("OWNER".equalsIgnoreCase(room.getCurrentUserRole())
                || (currentUserId != null && currentUserId.equals(room.getCreatedBy())));
    }

    private void updatePendingApprovalBanner(List<Transaction> pendingList) {
        if (layoutPendingApprovalBanner == null) return;
        boolean canApprove = isCurrentUserAdmin();
        if (!canApprove || pendingList == null || pendingList.isEmpty()) {
            layoutPendingApprovalBanner.setVisibility(View.GONE);
            return;
        }
        layoutPendingApprovalBanner.setVisibility(View.VISIBLE);
        int count = pendingList.size();
        double totalAmount = 0.0;
        for (Transaction tx : pendingList) {
            totalAmount += Math.abs(tx.getAmount());
        }
        if (tvPendingApprovalCount != null) {
            tvPendingApprovalCount.setText(String.valueOf(count));
        }
        if (tvPendingApprovalSummary != null) {
            String formattedAmount = df.format(totalAmount).replace(',', '.') + " ₫";
            tvPendingApprovalSummary.setText(String.format(Locale.getDefault(), "Yêu cầu chờ duyệt  •  %s", formattedAmount));
        }
    }

    private void openExpenseApprovalScreen() {
        if (getActivity() instanceof com.sharedfund.app.MainActivity) {
            ((com.sharedfund.app.MainActivity) getActivity()).openExpenseApproval(roomId);
        }
    }

    private boolean isCurrentUserTreasurer() {
        Room room = viewModel.getRoom().getValue();
        if (room == null) return false;
        if ("TREASURER".equalsIgnoreCase(room.getCurrentUserRole())) return true;
        String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        if (currentUserId == null) return false;
        if (isSavedTreasurer(room.getId(), currentUserId)) return true;
        List<RoomMember> members = viewModel.getMembers().getValue();
        if (members != null) {
            for (RoomMember m : members) {
                if (currentUserId.equals(m.getUserId()) && "TREASURER".equalsIgnoreCase(m.getRole())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUserAdminOrTreasurer() {
        return isCurrentUserOwner() || isCurrentUserTreasurer();
    }

    private void renderMembers(List<RoomMember> members) {
        if (members == null) return;
        Room room = viewModel.getRoom().getValue();
        String rId = room != null ? room.getId() : this.roomId;
        if (rId != null && getContext() != null) {
            String savedOwnerId = requireContext().getSharedPreferences("room_roles_" + rId, Context.MODE_PRIVATE)
                    .getString("owner_id", null);
            String ownerId = savedOwnerId != null ? savedOwnerId : (room != null ? room.getCreatedBy() : null);
            for (RoomMember m : members) {
                if (ownerId != null) {
                    if (m.getUserId() != null && m.getUserId().equals(ownerId)) {
                        m.setRole("OWNER");
                    } else if ("OWNER".equalsIgnoreCase(m.getRole()) || "ADMIN".equalsIgnoreCase(m.getRole())) {
                        m.setRole("MEMBER");
                    }
                }
                if (!"OWNER".equalsIgnoreCase(m.getRole()) && isSavedTreasurer(rId, m.getUserId())) {
                    m.setRole("TREASURER");
                }
            }
        }
        Collections.sort(members, (m1, m2) -> {
            int r1 = getRoleRank(m1 != null ? m1.getRole() : null);
            int r2 = getRoleRank(m2 != null ? m2.getRole() : null);
            if (r1 != r2) return Integer.compare(r1, r2);
            String n1 = m1 != null ? m1.getDisplayName() : "";
            String n2 = m2 != null ? m2.getDisplayName() : "";
            return n1.compareToIgnoreCase(n2);
        });
        String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        memberAdapter.setCurrentUserPermissions(currentUserId, isCurrentUserOwner(), isCurrentUserTreasurer());
        memberAdapter.setMembers(members);
        tvMembersTabCount.setText(members.size() + " thành viên");
        applyRoleBasedUi(room);
        if (viewModel != null) {
            updatePendingApprovalBanner(viewModel.getPendingTransactions().getValue());
        }
    }

    private static int getRoleRank(String role) {
        if (role == null) return 2;
        if ("OWNER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) return 0;
        if ("TREASURER".equalsIgnoreCase(role) || "VICE".equalsIgnoreCase(role)) return 1;
        return 2;
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        btnHeaderQuickEdit.setOnClickListener(v -> showEditRoomDialog());
        if (btnChangeRoomBackground != null) {
            btnChangeRoomBackground.setOnClickListener(v -> showChangeBackgroundDialog());
        }
        if (btnSettingsEditRoom != null) btnSettingsEditRoom.setOnClickListener(v -> showEditRoomDialog());
        if (btnSettingsPrivacyToggle != null) btnSettingsPrivacyToggle.setOnClickListener(v -> showPrivacyModeDialog());
        if (btnMemberDelegation != null) btnMemberDelegation.setOnClickListener(v -> showDelegationDialog());
        if (btnMemberJoinRequests != null) btnMemberJoinRequests.setOnClickListener(v -> showJoinRequestsDialog());

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



        btnInviteMember.setOnClickListener(v -> showInviteMemberSheet());

        btnHistoryFilterAll.setOnClickListener(v -> updateHistoryFilter(RoomViewModel.FilterType.ALL));
        btnHistoryFilterIncome.setOnClickListener(v -> updateHistoryFilter(RoomViewModel.FilterType.INCOME));
        btnHistoryFilterExpense.setOnClickListener(v -> updateHistoryFilter(RoomViewModel.FilterType.EXPENSE));

        if (btnSettingsDeleteRoom != null) btnSettingsDeleteRoom.setOnClickListener(v -> confirmDeleteRoom());
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
        final String roomName = room != null ? room.getName() : "Quỹ nhóm";

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_koin_split_bill);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvSubtitle = dialog.findViewById(R.id.tvSplitDialogSubtitle);
        TextInputEditText etAmount = dialog.findViewById(R.id.etSplitAmount);
        TextView tvResultAmount = dialog.findViewById(R.id.tvSplitResultAmount);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnSplitCancel);
        MaterialButton btnCopy = dialog.findViewById(R.id.btnSplitCopy);

        if (tvSubtitle != null) {
            tvSubtitle.setText("Chia đều hóa đơn cho " + memberCount + " thành viên trong quỹ");
        }

        final double[] currentEach = {0.0};
        final double[] currentAmt = {0.0};

        if (etAmount != null) {
            etAmount.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String str = s != null ? s.toString().trim() : "";
                    try {
                        double amt = Double.parseDouble(str);
                        currentAmt[0] = amt;
                        currentEach[0] = amt / memberCount;
                        if (tvResultAmount != null) {
                            tvResultAmount.setText(df.format(currentEach[0]).replace(',', '.') + " ₫");
                        }
                    } catch (Exception ex) {
                        currentAmt[0] = 0.0;
                        currentEach[0] = 0.0;
                        if (tvResultAmount != null) {
                            tvResultAmount.setText("0 ₫");
                        }
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                String text = "🧾 CHIA TIỀN NHÓM - " + roomName + "\n"
                        + "• Tổng hóa đơn: " + df.format(currentAmt[0]).replace(',', '.') + " ₫\n"
                        + "• Số thành viên: " + memberCount + " người\n"
                        + "👉 Mỗi người: " + df.format(currentEach[0]).replace(',', '.') + " ₫";
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Split Bill", text));
                    CopyToast.show(requireContext(), requireView());
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showRemindDialog() {
        Room room = viewModel.getRoom().getValue();
        String roomName = room != null ? room.getName() : "Quỹ nhóm";
        String joinCode = room != null && room.getJoinCode() != null ? room.getJoinCode() : "";

        String shareMsg = "🔔 LỜI NHẮC ĐÓNG QUỸ CHUNG\n"
                + "Quỹ: \"" + roomName + "\"\n"
                + "Chào cả nhà, quỹ đã đến kỳ hạn đóng góp. Mọi người kiểm tra và hoàn thành phần quỹ đúng hạn nhé!\n"
                + "👉 Mã tham gia: " + joinCode + "\n"
                + "Cảm ơn mọi người nhiều!";

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_koin_remind);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvMsg = dialog.findViewById(R.id.tvRemindDialogMessage);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnRemindCancel);
        MaterialButton btnShare = dialog.findViewById(R.id.btnRemindShare);
        MaterialButton btnCopy = dialog.findViewById(R.id.btnRemindCopy);

        if (tvMsg != null) {
            tvMsg.setText(shareMsg);
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                android.content.Intent sendIntent = new android.content.Intent();
                sendIntent.setAction(android.content.Intent.ACTION_SEND);
                sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMsg);
                sendIntent.setType("text/plain");
                startActivity(android.content.Intent.createChooser(sendIntent, "Chia sẻ lời nhắc đóng quỹ"));
                dialog.dismiss();
            });
        }

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Fund Reminder", shareMsg));
                    CopyToast.show(requireContext(), requireView());
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }
    private void showChangeBackgroundDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_koin_options, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        View btnPick = dialogView.findViewById(R.id.layoutOptionPick);
        View btnUrl = dialogView.findViewById(R.id.layoutOptionUrl);
        View btnClose = dialogView.findViewById(R.id.btnDialogClose);

        tvTitle.setText("Đổi ảnh bìa");
        tvSubtitle.setText("Chọn phương thức cập nhật ảnh bìa phòng quỹ");

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .create();

        btnPick.setOnClickListener(v -> {
            dialog.dismiss();
            if (roomCoverPicker != null) {
                roomCoverPicker.launch("image/*");
            }
        });

        btnUrl.setOnClickListener(v -> {
            dialog.dismiss();
            showRoomCoverUrlDialog();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showRoomCoverUrlDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_koin_url_input, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvUrlDialogTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvUrlDialogSubtitle);
        EditText etInput = dialogView.findViewById(R.id.etUrlInput);
        View btnCancel = dialogView.findViewById(R.id.btnUrlCancel);
        View btnSave = dialogView.findViewById(R.id.btnUrlSave);

        tvTitle.setText("Dán link ảnh bìa");
        tvSubtitle.setText("Nhập đường dẫn hình ảnh trực tuyến");
        etInput.setHint("https://.../cover.jpg");

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String url = etInput.getText() != null ? etInput.getText().toString().trim() : "";
            if (!url.isEmpty()) {
                dialog.dismiss();
                saveRoomBackground(url);
            }
        });

        dialog.show();
    }

    private void saveRoomBackground(String url) {
        if (getContext() == null || roomId == null) return;
        requireContext().getSharedPreferences("shared_fund_room_bg", Context.MODE_PRIVATE)
                .edit()
                .putString("bg_" + roomId, url)
                .apply();
        loadRoomBackground();
            }

    private void loadRoomBackground() {
        if (ivRoomCover == null || roomId == null || getContext() == null) return;
        String savedBg = requireContext().getSharedPreferences("shared_fund_room_bg", Context.MODE_PRIVATE)
                .getString("bg_" + roomId, "");
        if (savedBg != null && !savedBg.trim().isEmpty()) {
            ivRoomCover.setVisibility(View.VISIBLE);
            if (savedBg.startsWith("content://") || savedBg.startsWith("file://")) {
                new Thread(() -> {
                    try {
                        android.content.ContentResolver cr = requireContext().getContentResolver();
                        InputStream is = cr.openInputStream(Uri.parse(savedBg));
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (is != null) is.close();
                        if (bmp != null) {
                            ivRoomCover.post(() -> ivRoomCover.setImageBitmap(bmp));
                        }
                    } catch (Exception ignored) {}
                }).start();
            } else {
                com.sharedfund.app.ui.profile.AvatarImageLoader.load(ivRoomCover, null, savedBg);
            }
        } else {
            ivRoomCover.setVisibility(View.GONE);
        }
    }

    private void showRoomTransferSheet() {
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

        Dialog dialog = new Dialog(requireContext(), R.style.Theme_SharedFund);
        dialog.setContentView(R.layout.dialog_fullscreen_room_info);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        ImageButton btnBack = dialog.findViewById(R.id.btnBackRoomInfo);
        TextView tvCode = dialog.findViewById(R.id.tvInfoJoinCode);
        MaterialButton btnCopy = dialog.findViewById(R.id.btnInfoCopyCode);
        EditText etName = dialog.findViewById(R.id.etInfoRoomName);
        EditText etTarget = dialog.findViewById(R.id.etInfoRoomTarget);
        EditText etDesc = dialog.findViewById(R.id.etInfoRoomDesc);
        View layoutBottomSave = dialog.findViewById(R.id.layoutBottomSave);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveRoomInfo);
        TextView tvRoleNotice = dialog.findViewById(R.id.tvInfoRoleNotice);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dialog.dismiss());
        }

        if (tvCode != null) {
            tvCode.setText(room.getJoinCode() != null ? room.getJoinCode() : "");
        }

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && room.getJoinCode() != null) {
                    ClipData clip = ClipData.newPlainText("Join Code", room.getJoinCode());
                    clipboard.setPrimaryClip(clip);
                    CopyToast.show(requireContext(), v);
                }
            });
        }

        if (etName != null) {
            etName.setText(room.getName());
        }
        if (etTarget != null) {
            if (room.getTargetAmount() > 0) {
                etTarget.setText(String.valueOf((long) room.getTargetAmount()));
            } else {
                etTarget.setText("");
            }
        }
        if (etDesc != null) {
            etDesc.setText(room.getDescription() != null ? room.getDescription() : "");
        }

        boolean canEdit = isUserAdminOrTreasurer();
        if (!canEdit) {
            if (etName != null) {
                etName.setEnabled(false);
                etName.setFocusable(false);
            }
            if (etTarget != null) {
                etTarget.setEnabled(false);
                etTarget.setFocusable(false);
            }
            if (etDesc != null) {
                etDesc.setEnabled(false);
                etDesc.setFocusable(false);
            }
            if (layoutBottomSave != null) {
                layoutBottomSave.setVisibility(View.GONE);
            }
            if (tvRoleNotice != null) {
                tvRoleNotice.setVisibility(View.VISIBLE);
            }
        } else {
            if (layoutBottomSave != null) {
                layoutBottomSave.setVisibility(View.VISIBLE);
            }
            if (tvRoleNotice != null) {
                tvRoleNotice.setVisibility(View.GONE);
            }
        }

        if (btnSave != null && canEdit) {
            btnSave.setOnClickListener(v -> {
                String newName = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
                String targetStr = etTarget != null && etTarget.getText() != null ? etTarget.getText().toString().trim() : "";
                String newDesc = etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

                if (newName.isEmpty()) {
                    AppToaster.error(requireActivity(), "Tên quỹ không được để trống");
                    return;
                }

                double targetAmount = 0.0;
                if (!targetStr.isEmpty()) {
                    try {
                        targetAmount = Double.parseDouble(targetStr);
                    } catch (NumberFormatException e) {
                        AppToaster.error(requireActivity(), "Số tiền mục tiêu không hợp lệ");
                        return;
                    }
                }

                dialog.dismiss();
                if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.VISIBLE);

                viewModel.updateRoom(newName, newDesc, targetAmount, new RoomRepository.DataCallback<Room>() {
                    @Override
                    public void onSuccess(Room value) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.GONE);
                                                    });
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.GONE);
                            AppToaster.error(requireActivity(), "Cập nhật thất bại: " + ErrorMessageHelper.getFriendlyMessage(message));
                        });
                    }
                });
            });
        }

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
        boolean isOwner = isCurrentUserOwner();
        boolean isTreasurer = isCurrentUserTreasurer();

        if (cardSettingsPrivacy != null) cardSettingsPrivacy.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        if (cardSettingsRoomInfo != null) cardSettingsRoomInfo.setVisibility(View.VISIBLE);
        if (cardSettingsCloseFund != null) cardSettingsCloseFund.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        if (isOwner) {
            if (cardMemberManagement != null) cardMemberManagement.setVisibility(View.VISIBLE);
            if (tvMemberManagementHeader != null) tvMemberManagementHeader.setVisibility(View.VISIBLE);
            if (btnMemberDelegation != null) btnMemberDelegation.setVisibility(View.VISIBLE);
            if (dividerMemberDelegation != null) dividerMemberDelegation.setVisibility(View.VISIBLE);
            if (btnMemberJoinRequests != null) btnMemberJoinRequests.setVisibility(View.VISIBLE);
        } else if (isTreasurer) {
            if (cardMemberManagement != null) cardMemberManagement.setVisibility(View.VISIBLE);
            if (tvMemberManagementHeader != null) tvMemberManagementHeader.setVisibility(View.VISIBLE);
            if (btnMemberDelegation != null) btnMemberDelegation.setVisibility(View.GONE);
            if (dividerMemberDelegation != null) dividerMemberDelegation.setVisibility(View.GONE);
            if (btnMemberJoinRequests != null) btnMemberJoinRequests.setVisibility(View.VISIBLE);
        } else {
            if (cardMemberManagement != null) cardMemberManagement.setVisibility(View.GONE);
            if (tvMemberManagementHeader != null) tvMemberManagementHeader.setVisibility(View.GONE);
            if (btnMemberDelegation != null) btnMemberDelegation.setVisibility(View.GONE);
            if (dividerMemberDelegation != null) dividerMemberDelegation.setVisibility(View.GONE);
            if (btnMemberJoinRequests != null) btnMemberJoinRequests.setVisibility(View.GONE);
        }

        if (cardSettingsMemberActions != null) cardSettingsMemberActions.setVisibility(!isOwner ? View.VISIBLE : View.GONE);
        if (btnHeaderQuickEdit != null) {
            btnHeaderQuickEdit.setVisibility((isOwner || isTreasurer) ? View.VISIBLE : View.GONE);
        }
        if (btnChangeRoomBackground != null) {
            btnChangeRoomBackground.setVisibility(View.VISIBLE);
        }
    }

    private void confirmDeleteRoom() {
        Room room = viewModel.getRoom().getValue();
        if (room == null || getContext() == null) return;
        if (room.getBalance() > 0) {
            Dialog dialog = new Dialog(requireContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_fund_close_blocked);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }
            TextView tvMsg = dialog.findViewById(R.id.tvBlockedMessage);
            View btnOk = dialog.findViewById(R.id.btnBlockedUnderstood);
            if (tvMsg != null) {
                tvMsg.setText("Quỹ hiện vẫn còn số dư (" + room.getFormattedBalance() + "). Vui lòng giải ngân hoặc chi hết số dư trước khi đóng quỹ.");
            }
            if (btnOk != null) {
                btnOk.setOnClickListener(v -> dialog.dismiss());
            }
            dialog.show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_fund_close_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelClose);
        MaterialButton btnSubmit = dialog.findViewById(R.id.btnSubmitClose);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                dialog.dismiss();
                executeDeleteRoom();
            });
        }
        dialog.show();
    }

    private String getEffectiveRoomId() {
        if (roomId != null && !roomId.isEmpty()) {
            return roomId;
        }
        if (viewModel != null && viewModel.getRoom().getValue() != null) {
            return viewModel.getRoom().getValue().getId();
        }
        return "";
    }

    private Set<String> getResolvedJoinRequests(Context context, String targetRoomId) {
        if (context == null) return new HashSet<>();
        String effectiveId = (targetRoomId != null && !targetRoomId.isEmpty()) ? targetRoomId : getEffectiveRoomId();
        Set<String> set = context.getSharedPreferences("shared_fund_join_requests", Context.MODE_PRIVATE)
                .getStringSet("resolved_" + effectiveId, new HashSet<>());
        return new HashSet<>(set);
    }

    private void markJoinRequestResolved(Context context, String targetRoomId, PendingJoinRequest request) {
        if (context == null || request == null) return;
        String effectiveId = (targetRoomId != null && !targetRoomId.isEmpty()) ? targetRoomId : getEffectiveRoomId();
        Set<String> set = getResolvedJoinRequests(context, effectiveId);
        if (request.email != null) set.add(request.email);
        if (request.applicantName != null) set.add(request.applicantName);
        context.getSharedPreferences("shared_fund_join_requests", Context.MODE_PRIVATE)
                .edit()
                .putStringSet("resolved_" + effectiveId, set)
                .apply();
    }

    private boolean isJoinRequestResolved(Context context, String targetRoomId, String email, String applicantName) {
        if (context == null) return false;
        String effectiveId = (targetRoomId != null && !targetRoomId.isEmpty()) ? targetRoomId : getEffectiveRoomId();
        Set<String> set = getResolvedJoinRequests(context, effectiveId);
        return (email != null && set.contains(email)) || (applicantName != null && set.contains(applicantName));
    }

    private void initPendingJoinRequests() {
        pendingJoinRequests.clear();
        isRoomPrivate = RoomPrivacyManager.isRoomPrivate(getEffectiveRoomId());
        if (tvSettingsPrivacyStatus != null) {
            tvSettingsPrivacyStatus.setText(isRoomPrivate ? "Riêng tư ▾" : "Công khai ▾");
        }
        if (tvSettingsPrivacyDesc != null) {
            tvSettingsPrivacyDesc.setText(isRoomPrivate 
                    ? "Thành viên mới cần bạn phê duyệt để tham gia quỹ."
                    : "Bất kỳ ai có mã tham gia đều có thể vào quỹ ngay lập tức.");
        }
        if (isRoomPrivate) {
            List<RoomPrivacyManager.JoinRequestItem> saved = RoomPrivacyManager.getPendingRequests(getEffectiveRoomId());
            for (RoomPrivacyManager.JoinRequestItem item : saved) {
                if (!isJoinRequestResolved(getContext(), getEffectiveRoomId(), item.email, item.applicantName)) {
                    pendingJoinRequests.add(new PendingJoinRequest(item.userId, item.applicantName, item.email, item.time, item.message));
                }
            }
            if (pendingJoinRequests.isEmpty() && !isJoinRequestResolved(getContext(), getEffectiveRoomId(), "user3_test_realtime@sharedfund.com", "Lê Hoàng Nam")) {
                pendingJoinRequests.add(new PendingJoinRequest("mock-nam-id", "Lê Hoàng Nam", "user3_test_realtime@sharedfund.com", "Vừa xong", "Xin vào quỹ ăn sáng chung nhóm"));
            }
        }
        updateJoinRequestsBadge();
    }

    private void updateJoinRequestsBadge() {
        if (badgeJoinRequestsCount == null) return;
        int count = pendingJoinRequests.size();
        if (count > 0) {
            badgeJoinRequestsCount.setText(String.valueOf(count));
            badgeJoinRequestsCount.setVisibility(View.VISIBLE);
        } else {
            badgeJoinRequestsCount.setVisibility(View.GONE);
        }
    }

    private void showPrivacyModeDialog() {
        if (getContext() == null) return;
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), R.style.Theme_App_BottomSheet);
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_fund_privacy, null);
        bottomSheetDialog.setContentView(sheetView);

        View cardPrivate = sheetView.findViewById(R.id.cardPrivacyPrivate);
        View cardPublic = sheetView.findViewById(R.id.cardPrivacyPublic);
        ImageView ivRadioPrivate = sheetView.findViewById(R.id.ivRadioPrivate);
        ImageView ivRadioPublic = sheetView.findViewById(R.id.ivRadioPublic);
        View btnClose = sheetView.findViewById(R.id.btnClosePrivacySheet);
        MaterialButton btnConfirm = sheetView.findViewById(R.id.btnConfirmPrivacy);

        final boolean[] selectedPrivate = new boolean[]{isRoomPrivate};

        Runnable updateSelectionUi = () -> {
            if (selectedPrivate[0]) {
                cardPrivate.setBackgroundResource(R.drawable.bg_card_radio_selected);
                ivRadioPrivate.setImageResource(R.drawable.ic_radio_checked);
                cardPublic.setBackgroundResource(R.drawable.bg_card_radio_unselected);
                ivRadioPublic.setImageResource(R.drawable.ic_radio_unchecked);
            } else {
                cardPrivate.setBackgroundResource(R.drawable.bg_card_radio_unselected);
                ivRadioPrivate.setImageResource(R.drawable.ic_radio_unchecked);
                cardPublic.setBackgroundResource(R.drawable.bg_card_radio_selected);
                ivRadioPublic.setImageResource(R.drawable.ic_radio_checked);
            }
        };

        updateSelectionUi.run();

        cardPrivate.setOnClickListener(v -> {
            selectedPrivate[0] = true;
            updateSelectionUi.run();
        });

        cardPublic.setOnClickListener(v -> {
            selectedPrivate[0] = false;
            updateSelectionUi.run();
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        btnConfirm.setOnClickListener(v -> {
            isRoomPrivate = selectedPrivate[0];
            RoomPrivacyManager.setRoomPrivate(getEffectiveRoomId(), isRoomPrivate);
            if (tvSettingsPrivacyStatus != null) {
                tvSettingsPrivacyStatus.setText(isRoomPrivate ? "Riêng tư ▾" : "Công khai ▾");
            }
            if (tvSettingsPrivacyDesc != null) {
                tvSettingsPrivacyDesc.setText(isRoomPrivate 
                        ? "Thành viên mới cần bạn phê duyệt để tham gia quỹ."
                        : "Bất kỳ ai có mã tham gia đều có thể vào quỹ ngay lập tức.");
            }
            initPendingJoinRequests();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showKoinConfirmDialog(int iconRes, int iconTint, int iconBgRes,
                                       String title, String message,
                                       String positiveText, int positiveBgTint,
                                       Runnable onPositive) {
        if (getContext() == null) return;
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_koin_action_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvTitle = dialog.findViewById(R.id.tvConfirmDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvConfirmDialogMessage);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnConfirmCancel);
        MaterialButton btnAction = dialog.findViewById(R.id.btnConfirmAction);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);
        if (btnAction != null) {
            btnAction.setText(positiveText);
            btnAction.setBackgroundTintList(ColorStateList.valueOf(positiveBgTint));
            btnAction.setOnClickListener(v -> {
                dialog.dismiss();
                if (onPositive != null) onPositive.run();
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showMemberCustomizeBottomSheet(RoomMember member) {
        if (getContext() == null || member == null) return;
        Room currentRoom = viewModel.getRoom().getValue();
        if (currentRoom == null) return;
        String roomId = currentRoom.getId();

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        sheet.setContentView(R.layout.bottom_sheet_member_customize);

        View btnClose = sheet.findViewById(R.id.btnCloseCustomizeSheet);
        View btnKick = sheet.findViewById(R.id.btnSheetKickMember);

        if (btnClose != null) btnClose.setOnClickListener(v -> sheet.dismiss());

        if (btnKick != null) {
            btnKick.setOnClickListener(v -> {
                sheet.dismiss();
                showKoinConfirmDialog(
                        R.drawable.ic_member_leave,
                        ContextCompat.getColor(requireContext(), R.color.accent_expense),
                        R.drawable.bg_role_badge_pill,
                        "Mời " + member.getDisplayName() + " ra khỏi quỹ?",
                        "Thành viên này sẽ không còn quyền truy cập vào phòng quỹ và lịch sử hoạt động. Bạn có chắc chắn muốn thực hiện?",
                        "Mời ra khỏi quỹ",
                        ContextCompat.getColor(requireContext(), R.color.accent_expense),
                        () -> {
                            removeRoomTreasurer(roomId, member.getUserId());
                            viewModel.removeMember(member.getUserId(), new RoomRepository.DataCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean value) {
                                    if (getActivity() == null) return;
                                    requireActivity().runOnUiThread(() -> {
                                        viewModel.loadMembers();
                                        AppToaster.success(requireActivity(), "Đã mời " + member.getDisplayName() + " ra khỏi quỹ.");
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    if (getActivity() == null) return;
                                    requireActivity().runOnUiThread(() -> {
                                        AppToaster.error(requireActivity(), "Không thể xóa thành viên: " + ErrorMessageHelper.getFriendlyMessage(message));
                                    });
                                }
                            });
                        }
                );
            });
        }

        sheet.show();
    }

    private void showMemberActionBottomSheet(RoomMember member, Runnable onUpdated, Runnable onTransferred) {
        if (getContext() == null || member == null) return;
        if (!isCurrentUserOwner()) return;
        Room currentRoom = viewModel.getRoom().getValue();
        if (currentRoom == null) return;
        String roomId = currentRoom.getId();

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        sheet.setContentView(R.layout.bottom_sheet_member_actions);

        ImageView ivAvatar = sheet.findViewById(R.id.ivSheetMemberAvatar);
        TextView tvInitials = sheet.findViewById(R.id.tvSheetMemberInitials);
        TextView tvName = sheet.findViewById(R.id.tvSheetMemberName);
        TextView tvRole = sheet.findViewById(R.id.tvSheetMemberRole);
        View btnClose = sheet.findViewById(R.id.btnCloseMemberActionSheet);

        View btnToggleTreasurer = sheet.findViewById(R.id.btnSheetToggleTreasurer);
        ImageView ivTreasurerIcon = sheet.findViewById(R.id.ivSheetTreasurerIcon);
        TextView tvTreasurerTitle = sheet.findViewById(R.id.tvSheetTreasurerTitle);
        TextView tvTreasurerDesc = sheet.findViewById(R.id.tvSheetTreasurerDesc);

        View btnTransferOwner = sheet.findViewById(R.id.btnSheetTransferOwner);

        if (btnClose != null) btnClose.setOnClickListener(v -> sheet.dismiss());

        if (tvName != null) tvName.setText(member.getDisplayName());
        if (tvInitials != null) tvInitials.setText(member.getInitials());
        if (ivAvatar != null) {
            com.sharedfund.app.ui.profile.AvatarImageLoader.load(ivAvatar, tvInitials, member.getAvatarUrl());
        }

        boolean isTreasurer = "TREASURER".equalsIgnoreCase(member.getRole()) || isSavedTreasurer(roomId, member.getUserId());
        int treasurerCount = getRoomTreasurerIds(roomId).size();

        if (tvRole != null) {
            if (isTreasurer) {
                tvRole.setVisibility(View.VISIBLE);
                tvRole.setText("★ Phó quỹ");
                tvRole.setBackgroundResource(R.drawable.bg_role_badge_vice);
                tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_vice_text));
                tvRole.setTextSize(11.5f);
                tvRole.setTypeface(null, Typeface.BOLD);
                int padH = dpToPx(8);
                int padV = dpToPx(2);
                tvRole.setPadding(padH, padV, padH, padV);
            } else {
                tvRole.setVisibility(View.GONE);
            }
        }

        if (isTreasurer) {
            if (tvTreasurerTitle != null) tvTreasurerTitle.setText("Bãi nhiệm Phó quỹ");
            if (tvTreasurerDesc != null) tvTreasurerDesc.setText("Thu hồi quyền phó quỹ, chuyển về thành viên thường");
            if (ivTreasurerIcon != null) {
                ivTreasurerIcon.setImageResource(R.drawable.ic_shield_minus);
                ivTreasurerIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_secondary)));
            }
        } else {
            if (tvTreasurerTitle != null) tvTreasurerTitle.setText("Bổ nhiệm làm Phó quỹ");
            if (tvTreasurerDesc != null) tvTreasurerDesc.setText("Cấp quyền duyệt chi và chỉnh sửa thông tin quỹ (" + treasurerCount + "/3 Phó quỹ)");
            if (ivTreasurerIcon != null) {
                ivTreasurerIcon.setImageResource(R.drawable.ic_shield_star);
                ivTreasurerIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            }
        }

        if (btnToggleTreasurer != null) {
            btnToggleTreasurer.setOnClickListener(v -> {
                sheet.dismiss();
                if (isTreasurer) {
                    showKoinConfirmDialog(
                            R.drawable.ic_shield_minus,
                            Color.parseColor("#64748B"),
                            R.drawable.bg_role_badge_vice,
                            "Bãi nhiệm Phó quỹ?",
                            "Bạn có chắc muốn bãi nhiệm vai trò Phó quỹ của " + member.getDisplayName() + "? Thành viên này sẽ trở về vai trò thành viên thường và không còn quyền duyệt chi tiêu.",
                            "Bãi nhiệm",
                            ContextCompat.getColor(requireContext(), R.color.primary),
                            () -> {
                                removeRoomTreasurer(roomId, member.getUserId());
                                RoomRepository.getInstance().updateMemberRole(roomId, member.getUserId(), "MEMBER", null);
                                member.setRole("MEMBER");
                                if (memberAdapter != null) memberAdapter.notifyDataSetChanged();
                                applyRoleBasedUi(currentRoom);
                                AppToaster.success(requireActivity(), "Đã chuyển " + member.getDisplayName() + " về vai trò Thành viên thường.");
                                if (onUpdated != null) onUpdated.run();
                            }
                    );
                } else {
                    if (getRoomTreasurerIds(roomId).size() >= 3) {
                        AppToaster.error(requireActivity(), "Mỗi phòng quỹ chỉ được tối đa 3 Phó quỹ. Vui lòng bãi nhiệm phó quỹ hiện tại nếu muốn thêm mới!");
                        return;
                    }
                    showKoinConfirmDialog(
                            R.drawable.ic_shield_star,
                            ContextCompat.getColor(requireContext(), R.color.primary),
                            R.drawable.bg_role_badge_pill,
                            "Bổ nhiệm Phó quỹ?",
                            "Bạn có chắc muốn bổ nhiệm " + member.getDisplayName() + " làm Phó quỹ? Thành viên này sẽ có quyền duyệt chi tiêu và quản trị quỹ cùng bạn.",
                            "Bổ nhiệm",
                            ContextCompat.getColor(requireContext(), R.color.primary),
                            () -> {
                                addRoomTreasurer(roomId, member.getUserId());
                                RoomRepository.getInstance().updateMemberRole(roomId, member.getUserId(), "TREASURER", null);
                                member.setRole("TREASURER");
                                if (memberAdapter != null) memberAdapter.notifyDataSetChanged();
                                applyRoleBasedUi(currentRoom);
                                AppToaster.success(requireActivity(), "Đã bổ nhiệm " + member.getDisplayName() + " làm Phó quỹ thành công!");
                                if (onUpdated != null) onUpdated.run();
                            }
                    );
                }
            });
        }

        if (btnTransferOwner != null) {
            btnTransferOwner.setOnClickListener(v -> {
                sheet.dismiss();
                showKoinConfirmDialog(
                        R.drawable.ic_crown,
                        Color.parseColor("#B45309"),
                        R.drawable.bg_role_badge_owner,
                        "Chuyển giao quyền Chủ quỹ",
                        "Bạn có chắc chắn muốn chuyển giao toàn quyền Chủ quỹ cho " + member.getDisplayName() + "? Bạn sẽ trở thành thành viên thường sau thao tác này.",
                        "Chuyển quyền",
                        ContextCompat.getColor(requireContext(), R.color.primary),
                        () -> {
                            String myCurrentId = UserSessionManager.getInstance(requireContext()).getUserId();
                            if (getContext() != null) {
                                requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("owner_id", member.getUserId())
                                        .apply();
                            }
                            removeRoomTreasurer(roomId, member.getUserId());
                            RoomRepository.getInstance().transferRoomOwnership(roomId, myCurrentId, member.getUserId(), null);
                            if (currentRoom != null) {
                                currentRoom.setCurrentUserRole("MEMBER");
                                currentRoom.setCreatedBy(member.getUserId());
                            }
                            Room rSync = RoomRepository.getInstance().getRoomSync(roomId);
                            if (rSync != null) {
                                rSync.setCurrentUserRole("MEMBER");
                                rSync.setCreatedBy(member.getUserId());
                            }
                            if (viewModel != null && viewModel.getMembers().getValue() != null) {
                                for (RoomMember m : viewModel.getMembers().getValue()) {
                                    if (m.getUserId() != null && m.getUserId().equals(member.getUserId())) {
                                        m.setRole("OWNER");
                                    } else if ("OWNER".equalsIgnoreCase(m.getRole())) {
                                        m.setRole("MEMBER");
                                    }
                                }
                            }
                            member.setRole("OWNER");
                            if (memberAdapter != null) {
                                memberAdapter.setCurrentUserPermissions(myCurrentId, false, false);
                                memberAdapter.notifyDataSetChanged();
                            }
                            applyRoleBasedUi(currentRoom);
                            AppToaster.success(requireActivity(), "Đã chuyển quyền Chủ quỹ cho " + member.getDisplayName());
                            if (onTransferred != null) {
                                onTransferred.run();
                            } else if (onUpdated != null) {
                                onUpdated.run();
                            }
                        }
                );
            });
        }

        sheet.show();
    }

    private void showDelegationDialog() {
        if (getContext() == null) return;
        Room currentRoom = viewModel.getRoom().getValue();
        if (currentRoom == null) return;

        Dialog dialog = new Dialog(requireContext(), R.style.Theme_SharedFund);
        dialog.setContentView(R.layout.dialog_fullscreen_delegation);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        ImageButton btnBack = dialog.findViewById(R.id.btnBackDelegation);
        LinearLayout layoutMembers = dialog.findViewById(R.id.layoutDelegationMemberList);
        TextView tvEmpty = dialog.findViewById(R.id.tvEmptyDelegationMembers);
        TextView tvMembersCount = dialog.findViewById(R.id.tvDelegationMembersCount);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dialog.dismiss());
        }

        Runnable renderList = new Runnable() {
            @Override
            public void run() {
                if (!isCurrentUserOwner()) {
                    dialog.dismiss();
                    return;
                }
                List<RoomMember> members = viewModel.getMembers().getValue();
                List<RoomMember> nonOwnerMembers = new ArrayList<>();
                String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
                if (members != null) {
                    for (RoomMember m : members) {
                        if (!"OWNER".equalsIgnoreCase(m.getRole()) && (currentUserId == null || !currentUserId.equals(m.getUserId()))) {
                            nonOwnerMembers.add(m);
                        }
                    }
                }

                int treasurerCount = getRoomTreasurerIds(currentRoom.getId()).size();
                if (tvMembersCount != null) {
                    tvMembersCount.setText("Chọn thành viên để ủy quyền (Phó quỹ: " + treasurerCount + "/3)");
                }

                if (nonOwnerMembers.isEmpty()) {
                    if (layoutMembers != null) layoutMembers.setVisibility(View.GONE);
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                if (layoutMembers == null) return;
                layoutMembers.setVisibility(View.VISIBLE);
                layoutMembers.removeAllViews();

                for (int i = 0; i < nonOwnerMembers.size(); i++) {
                    RoomMember m = nonOwnerMembers.get(i);
                    boolean isTreasurer = "TREASURER".equalsIgnoreCase(m.getRole()) || isSavedTreasurer(currentRoom.getId(), m.getUserId());

                    LinearLayout row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
                    TypedValue outValue = new TypedValue();
                    requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                    row.setBackgroundResource(outValue.resourceId);
                    row.setClickable(true);
                    row.setFocusable(true);

                    FrameLayout avatarFrame = new FrameLayout(requireContext());
                    LinearLayout.LayoutParams lpAvatar = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
                    avatarFrame.setLayoutParams(lpAvatar);

                    ImageView ivAv = new ImageView(requireContext());
                    ivAv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    ivAv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivAv.setBackgroundResource(R.drawable.bg_avatar_circle);
                    ivAv.setClipToOutline(true);

                    TextView tvInitials = new TextView(requireContext());
                    tvInitials.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    tvInitials.setGravity(Gravity.CENTER);
                    tvInitials.setTextColor(Color.WHITE);
                    tvInitials.setTextSize(15f);
                    tvInitials.setTypeface(null, Typeface.BOLD);
                    tvInitials.setBackgroundResource(R.drawable.bg_avatar_circle);
                    tvInitials.setText(m.getInitials());

                    avatarFrame.addView(ivAv);
                    avatarFrame.addView(tvInitials);
                    com.sharedfund.app.ui.profile.AvatarImageLoader.load(ivAv, tvInitials, m.getAvatarUrl());

                    LinearLayout textCol = new LinearLayout(requireContext());
                    textCol.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    lpText.setMarginStart(dpToPx(12));
                    textCol.setLayoutParams(lpText);

                    TextView tvName = new TextView(requireContext());
                    tvName.setText(m.getDisplayName());
                    tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                    tvName.setTextSize(15f);
                    tvName.setTypeface(null, Typeface.BOLD);

                    TextView tvRole = new TextView(requireContext());
                    LinearLayout.LayoutParams lpRole = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    lpRole.topMargin = dpToPx(3);
                    tvRole.setLayoutParams(lpRole);

                    if (isTreasurer) {
                        tvRole.setVisibility(View.VISIBLE);
                        tvRole.setText("★ Phó quỹ");
                        tvRole.setBackgroundResource(R.drawable.bg_role_badge_vice);
                        tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_vice_text));
                        tvRole.setTextSize(11.5f);
                        tvRole.setTypeface(null, Typeface.BOLD);
                        int padH = dpToPx(8);
                        int padV = dpToPx(2);
                        tvRole.setPadding(padH, padV, padH, padV);
                    } else {
                        tvRole.setVisibility(View.GONE);
                    }

                    textCol.addView(tvName);
                    textCol.addView(tvRole);

                    ImageView ivChevron = new ImageView(requireContext());
                    LinearLayout.LayoutParams lpChevron = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
                    lpChevron.setMarginStart(dpToPx(8));
                    ivChevron.setLayoutParams(lpChevron);
                    ivChevron.setImageResource(R.drawable.ic_chevron_right);
                    ivChevron.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_tertiary)));

                    row.addView(avatarFrame);
                    row.addView(textCol);
                    row.addView(ivChevron);

                    row.setOnClickListener(v -> {
                        showMemberActionBottomSheet(m, this, () -> dialog.dismiss());
                    });

                    layoutMembers.addView(row);

                    if (i < nonOwnerMembers.size() - 1) {
                        View divider = new View(requireContext());
                        LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
                        lpDiv.setMarginStart(dpToPx(68));
                        divider.setLayoutParams(lpDiv);
                        divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.border));
                        layoutMembers.addView(divider);
                    }
                }
            }
        };

        renderList.run();
        dialog.show();
    }

    private void showJoinRequestsDialog() {
        if (getContext() == null) return;
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_SharedFund);
        dialog.setContentView(R.layout.dialog_fullscreen_join_requests);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        ImageButton btnBack = dialog.findViewById(R.id.btnBackJoinRequests);
        TextView tvTitle = dialog.findViewById(R.id.tvJoinRequestsHeaderTitle);
        LinearLayout layoutList = dialog.findViewById(R.id.layoutJoinRequestsList);
        View layoutEmpty = dialog.findViewById(R.id.layoutEmptyJoinRequests);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dialog.dismiss());
        }

        Runnable renderList = new Runnable() {
            @Override
            public void run() {
                if (tvTitle != null) {
                    tvTitle.setText("Yêu cầu tham gia quỹ (" + pendingJoinRequests.size() + ")");
                }
                if (layoutList == null) return;
                layoutList.removeAllViews();

                if (pendingJoinRequests.isEmpty()) {
                    if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                    return;
                }
                if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);

                for (int i = 0; i < pendingJoinRequests.size(); i++) {
                    final int idx = i;
                    PendingJoinRequest req = pendingJoinRequests.get(i);

                    MaterialCardView card = new MaterialCardView(requireContext());
                    LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lpCard.setMargins(0, 0, 0, dpToPx(12));
                    card.setLayoutParams(lpCard);
                    card.setRadius(dpToPx(16));
                    card.setCardElevation(2f);
                    card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.border));
                    card.setStrokeWidth(dpToPx(1));
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface));

                    LinearLayout cardContent = new LinearLayout(requireContext());
                    cardContent.setOrientation(LinearLayout.VERTICAL);
                    cardContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

                    LinearLayout topRow = new LinearLayout(requireContext());
                    topRow.setOrientation(LinearLayout.HORIZONTAL);
                    topRow.setGravity(Gravity.CENTER_VERTICAL);

                    FrameLayout avatarFrame = new FrameLayout(requireContext());
                    avatarFrame.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)));

                    ImageView ivAv = new ImageView(requireContext());
                    ivAv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    ivAv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivAv.setBackgroundResource(R.drawable.bg_avatar_circle);
                    ivAv.setClipToOutline(true);

                    TextView tvInitials = new TextView(requireContext());
                    tvInitials.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    tvInitials.setGravity(Gravity.CENTER);
                    tvInitials.setTextColor(Color.WHITE);
                    tvInitials.setTextSize(15f);
                    tvInitials.setTypeface(null, Typeface.BOLD);
                    tvInitials.setBackgroundResource(R.drawable.bg_avatar_circle);
                    tvInitials.setText("LN");

                    avatarFrame.addView(ivAv);
                    avatarFrame.addView(tvInitials);

                    LinearLayout textCol = new LinearLayout(requireContext());
                    textCol.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    lpText.setMarginStart(dpToPx(12));
                    textCol.setLayoutParams(lpText);

                    TextView tvName = new TextView(requireContext());
                    tvName.setText(req.applicantName);
                    tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                    tvName.setTextSize(15f);
                    tvName.setTypeface(null, Typeface.BOLD);

                    TextView tvEmail = new TextView(requireContext());
                    tvEmail.setText(req.email);
                    tvEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    tvEmail.setTextSize(13f);

                    TextView tvTime = new TextView(requireContext());
                    tvTime.setText(req.time);
                    tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    tvTime.setTextSize(12.5f);

                    textCol.addView(tvName);
                    textCol.addView(tvEmail);
                    textCol.addView(tvTime);

                    topRow.addView(avatarFrame);
                    topRow.addView(textCol);

                    cardContent.addView(topRow);

                    if (req.message != null && !req.message.isEmpty()) {
                        LinearLayout bubble = new LinearLayout(requireContext());
                        LinearLayout.LayoutParams lpBubble = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lpBubble.setMargins(0, dpToPx(8), 0, dpToPx(8));
                        bubble.setLayoutParams(lpBubble);
                        bubble.setOrientation(LinearLayout.VERTICAL);
                        bubble.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
                        bubble.setBackgroundResource(R.drawable.bg_edit_text_inline);

                        TextView tvMsgLabel = new TextView(requireContext());
                        tvMsgLabel.setText("Lời nhắn:");
                        tvMsgLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                        tvMsgLabel.setTextSize(12f);
                        tvMsgLabel.setTypeface(null, Typeface.BOLD);

                        TextView tvMsg = new TextView(requireContext());
                        tvMsg.setText(req.message);
                        tvMsg.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                        tvMsg.setTextSize(14f);

                        bubble.addView(tvMsgLabel);
                        bubble.addView(tvMsg);
                        cardContent.addView(bubble);
                    }

                    LinearLayout btnRow = new LinearLayout(requireContext());
                    LinearLayout.LayoutParams lpBtnRow = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    btnRow.setLayoutParams(lpBtnRow);
                    btnRow.setOrientation(LinearLayout.HORIZONTAL);

                    MaterialButton btnReject = new MaterialButton(requireContext());
                    LinearLayout.LayoutParams lpReject = new LinearLayout.LayoutParams(0, dpToPx(44), 1f);
                    lpReject.setMarginEnd(dpToPx(8));
                    btnReject.setLayoutParams(lpReject);
                    btnReject.setText("Từ chối");
                    btnReject.setTextColor(Color.WHITE);
                    btnReject.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_expense)));
                    btnReject.setCornerRadius(dpToPx(10));

                    MaterialButton btnAccept = new MaterialButton(requireContext());
                    LinearLayout.LayoutParams lpAccept = new LinearLayout.LayoutParams(0, dpToPx(44), 1.2f);
                    btnAccept.setLayoutParams(lpAccept);
                    btnAccept.setText("Duyệt tham gia");
                    btnAccept.setTextColor(Color.WHITE);
                    btnAccept.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
                    btnAccept.setCornerRadius(dpToPx(10));

                    btnReject.setOnClickListener(v -> {
                        RoomPrivacyManager.removePendingRequest(getEffectiveRoomId(), req.userId, req.email);
                        markJoinRequestResolved(dialog.getContext(), getEffectiveRoomId(), req);
                        pendingJoinRequests.remove(req);
                        updateJoinRequestsBadge();
                        run();
                    });

                    btnAccept.setOnClickListener(v -> {
                        RoomPrivacyManager.removePendingRequest(getEffectiveRoomId(), req.userId, req.email);
                        markJoinRequestResolved(dialog.getContext(), getEffectiveRoomId(), req);
                        pendingJoinRequests.remove(req);
                        updateJoinRequestsBadge();
                        Room currentRoom = RoomRepository.getInstance().getRoomSync(getEffectiveRoomId());
                        if (currentRoom != null && req.userId != null && !req.userId.startsWith("mock-")) {
                            RoomRepository.getInstance().addMember(currentRoom, req.userId, "MEMBER", new RoomRepository.DataCallback<Room>() {
                                @Override
                                public void onSuccess(Room r) {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() -> {
                                        viewModel.loadMembers();
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                }
                            });
                        }
                        viewModel.loadMembers();
                        run();
                    });

                    btnRow.addView(btnReject);
                    btnRow.addView(btnAccept);
                    cardContent.addView(btnRow);

                    card.addView(cardContent);
                    layoutList.addView(card);
                }
            }
        };

        renderList.run();
        dialog.show();
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
        String roomName = room.getName() != null ? room.getName() : "quỹ này";

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_koin_action_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        TextView tvTitle = dialog.findViewById(R.id.tvConfirmDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvConfirmDialogMessage);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnConfirmCancel);
        MaterialButton btnAction = dialog.findViewById(R.id.btnConfirmAction);
        if (tvTitle != null) tvTitle.setText("Rời khỏi quỹ này?");
        if (tvMessage != null) tvMessage.setText("Bạn có chắc chắn muốn rời khỏi quỹ \"" + roomName + "\"? Bạn sẽ không thể tiếp tục theo dõi biến động số dư hay thao tác trên quỹ này.");
        if (btnAction != null) {
            btnAction.setText("Rời quỹ");
            btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_expense)));
            btnAction.setOnClickListener(v -> {
                dialog.dismiss();
                executeLeaveRoom();
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }
    private void executeLeaveRoom() {
        if (viewModel == null || getContext() == null) return;
        String rawUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        if (rawUserId == null || rawUserId.isEmpty()) {
            rawUserId = RoomRepository.getInstance().getCurrentUserId();
        }
        final String currentUserId = rawUserId;
        if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.VISIBLE);

        viewModel.leaveRoom(currentUserId, new RoomRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                removeRoomTreasurer(roomId, currentUserId);
                if (getContext() != null) {
                    requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply();
                }
                Room currentRoom = viewModel != null ? viewModel.getRoom().getValue() : null;
                if (currentRoom != null) {
                    currentRoom.setCurrentUserRole(null);
                }
                Room cachedRoom = RoomRepository.getInstance().getRoomSync(roomId);
                if (cachedRoom != null) {
                    cachedRoom.setCurrentUserRole(null);
                }
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (pbDetailLoading != null) pbDetailLoading.setVisibility(View.GONE);
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
        return isCurrentUserOwner() || isCurrentUserTreasurer();
    }

    private void copyJoinCode() {
        Room r = viewModel.getRoom().getValue();
        if (r != null) {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Join Code", r.getJoinCode());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                CopyToast.show(requireContext(), requireView());
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
                if (adminId == null || adminId.isEmpty()) {
                    adminId = RoomRepository.getInstance().getCurrentUserId();
                }
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
                                if (viewModel != null) viewModel.refresh();
                                AppToaster.success(requireActivity(), "Đã duyệt khoản chi thành công!");
                            });
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> AppToaster.error(requireActivity(), "Phê duyệt thất bại: " + ErrorMessageHelper.getFriendlyMessage(errorMessage)));
                        }
                    }
                });
            }

            @Override
            public void onReject(Transaction tx, String reason) {
                UserSessionManager session = UserSessionManager.getInstance(requireContext());
                String adminId = session.getUserId();
                if (adminId == null || adminId.isEmpty()) {
                    adminId = RoomRepository.getInstance().getCurrentUserId();
                }
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
                                if (viewModel != null) viewModel.refresh();
                                AppToaster.info(requireActivity(), "Đã từ chối khoản chi!");
                            });
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> AppToaster.error(requireActivity(), "Từ chối thất bại: " + ErrorMessageHelper.getFriendlyMessage(errorMessage)));
                        }
                    }
                });
            }
        });
        sheet.show(getChildFragmentManager(), "room_tx_detail");
    }
}
