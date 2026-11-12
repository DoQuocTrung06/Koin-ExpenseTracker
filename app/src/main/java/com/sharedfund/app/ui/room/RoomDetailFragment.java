package com.sharedfund.app.ui.room;

import com.sharedfund.app.ui.report.StatementReportFragment;








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



import androidx.core.util.Pair;



import com.google.android.material.datepicker.MaterialDatePicker;



import com.sharedfund.app.ui.widget.CustomDateRangeDialog;



import com.sharedfund.app.ui.widget.TimeFilterBottomSheet;



import java.text.SimpleDateFormat;



import java.util.Calendar;



import java.util.Date;



import java.util.Locale;















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
    private static final String ARG_AUTO_OPEN_JOIN_REQUESTS = "arg_auto_open_join_requests";
    private boolean pendingAutoOpenJoinRequests;






    private String roomId;



    private RoomViewModel viewModel;







    private TransactionAdapter recentTransactionAdapter;



    private WalletStatementAdapter historyTransactionAdapter;



    private MemberAdapter memberAdapter;







    private View layoutRoomDetailHeader;



    private ImageButton btnBack;



    private TextView tvHeaderRoomName;
    private RoomHeaderViewBinder headerViewBinder;
    private RoomCategoryBreakdownBinder categoryBreakdownBinder;



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
    private TextView tvUtilityOverdueBadge;



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






    private TextView tvEmptyCategoryBars;



    private MaterialButton btnHistoryFilterAll;



    private MaterialButton btnHistoryFilterIncome;



    private MaterialButton btnHistoryFilterExpense;



    private MaterialButton btnRoomTimeAll;



    private MaterialButton btnRoomTimeWeek;



    private MaterialButton btnRoomTimeMonth;



    private MaterialButton btnRoomTimeCustom;



    private MaterialCardView cardRoomCustomRangeBadge;



    private TextView tvRoomCustomRange;



    private ImageButton btnClearRoomCustomRange;



    private Long roomCustomStartMillis = null;



    private Long roomCustomEndMillis = null;







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



    public static RoomDetailFragment newInstance(String roomId) {
        return newInstance(roomId, false);
    }

    public static RoomDetailFragment newInstance(String roomId, boolean autoOpenJoinRequests) {
        RoomDetailFragment fragment = new RoomDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putBoolean(ARG_AUTO_OPEN_JOIN_REQUESTS, autoOpenJoinRequests);
        fragment.setArguments(args);
        return fragment;
    }







    @Override



    public void onCreate(@Nullable Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);



        if (getArguments() != null) {



            roomId = getArguments().getString(ARG_ROOM_ID);
            pendingAutoOpenJoinRequests = getArguments().getBoolean(ARG_AUTO_OPEN_JOIN_REQUESTS, false);



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



        updateRoomTimeFilterButtons();



        updateHistoryFilter(RoomViewModel.FilterType.ALL);

        if (pendingAutoOpenJoinRequests) {
            pendingAutoOpenJoinRequests = false;
            view.post(() -> {
                selectNavTab(2, false);
                showJoinRequestsDialog();
            });
        }

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
        if (getEffectiveRoomId() != null) {
            RoomRepository.getInstance().refreshRoomTransactions(getEffectiveRoomId(), null);
        }
        checkAndUpdateUtilityOverdueBadge();



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
        tvUtilityOverdueBadge = view.findViewById(R.id.tvUtilityOverdueBadge);



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



        com.google.android.material.button.MaterialButton btnExportRoomReport = view.findViewById(R.id.btnExportRoomReport);



        if (btnExportRoomReport != null) {



            btnExportRoomReport.setOnClickListener(v -> handleExportRoomReport());



        }



        tvEmptyCategoryBars = view.findViewById(R.id.tvEmptyCategoryBars);



        btnHistoryFilterAll = view.findViewById(R.id.btnHistoryFilterAll);



        btnHistoryFilterIncome = view.findViewById(R.id.btnHistoryFilterIncome);



        btnHistoryFilterExpense = view.findViewById(R.id.btnHistoryFilterExpense);



        btnRoomTimeAll = view.findViewById(R.id.btnRoomTimeAll);



        btnRoomTimeWeek = view.findViewById(R.id.btnRoomTimeWeek);



        btnRoomTimeMonth = view.findViewById(R.id.btnRoomTimeMonth);



        btnRoomTimeCustom = view.findViewById(R.id.btnRoomTimeCustom);



        cardRoomCustomRangeBadge = view.findViewById(R.id.cardRoomCustomRangeBadge);



        tvRoomCustomRange = view.findViewById(R.id.tvRoomCustomRange);



        btnClearRoomCustomRange = view.findViewById(R.id.btnClearRoomCustomRange);







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
        headerViewBinder = new RoomHeaderViewBinder(
                tvHeaderRoomName, tvDetailBalance, tvDetailJoinCode,
                layoutHomeTarget, tvHomeTargetText, pbHomeTarget,
                tvHistoryNetBalance, tvHistoryTotalIncome, tvHistoryTotalExpense,
                cardSettingsPrivacy, cardSettingsRoomInfo, cardSettingsCloseFund,
                cardMemberManagement, tvMemberManagementHeader, btnMemberDelegation,
                dividerMemberDelegation, btnMemberJoinRequests, cardSettingsMemberActions,
                btnHeaderQuickEdit, btnChangeRoomBackground, df
        );
        categoryBreakdownBinder = new RoomCategoryBreakdownBinder(donutHistoryChartView, tvEmptyCategoryBars, layoutHistoryCategoryBars, df);







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
                    checkAndUpdateUtilityOverdueBadge();



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
        if (headerViewBinder != null) {
            headerViewBinder.bindRoomHeader(room);
        }
        loadRoomBackground();
        applyRoleBasedUi(room);
        if (viewModel != null) {
            initPendingJoinRequests();
            if (viewModel.getMembers().getValue() != null) {
                renderMembers(viewModel.getMembers().getValue());
            }
            updatePendingApprovalBanner(viewModel.getPendingTransactions().getValue());
        }
    }

    private void updateNetBalance() {
        if (headerViewBinder != null && viewModel != null) {
            headerViewBinder.updateNetBalance(viewModel.getTotalIncome().getValue(), viewModel.getTotalExpense().getValue());
        }
    }

    private void renderCategoryBreakdown(Map<String, Double> map) {
        if (categoryBreakdownBinder != null && getContext() != null) {
            categoryBreakdownBinder.renderCategoryBreakdown(requireContext(), map);
        }
    }

    private Set<String> getRoomTreasurerIds(String roomId) {



        if (roomId == null) return new HashSet<>();



        Room r = RoomRepository.getInstance().getRoomSync(roomId);



        if (r == null && viewModel != null) {



            r = viewModel.getRoom().getValue();



        }



        if (r != null && r.hasServerTreasurers()) {



            Set<String> serverSet = r.getTreasurerIds();



            if (getContext() != null) {



                requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)



                        .edit()



                        .putStringSet("treasurer_ids", new HashSet<>(serverSet))



                        .apply();



            }



            return new HashSet<>(serverSet);



        }



        if (getContext() == null) return new HashSet<>();



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



        RoomRepository.getInstance().syncRoomTreasurers(roomId, set);



        RoomRepository.getInstance().updateMemberRole(roomId, userId, "TREASURER", null);



    }







    private void removeRoomTreasurer(String roomId, String userId) {



        removeRoomTreasurer(roomId, userId, true);



    }



    private void removeRoomTreasurer(String roomId, String userId, boolean syncMemberRole) {



        if (getContext() == null || roomId == null || userId == null) return;



        Set<String> set = getRoomTreasurerIds(roomId);



        set.remove(userId);



        requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)



                .edit()



                .putStringSet("treasurer_ids", set)



                .remove("treasurer_" + userId)



                .apply();



        RoomRepository.getInstance().syncRoomTreasurers(roomId, set);



        if (syncMemberRole) {



            RoomRepository.getInstance().updateMemberRole(roomId, userId, "MEMBER", null);



        }



    }







    private boolean isSavedTreasurer(String roomId, String userId) {



        if (roomId == null || userId == null) return false;



        Room r = RoomRepository.getInstance().getRoomSync(roomId);



        if (r == null && viewModel != null) {



            r = viewModel.getRoom().getValue();



        }



        if (r != null) {



            return RoomRepository.getInstance().isUserRoomTreasurer(roomId, userId);



        }



        if (getContext() == null) return false;



        return getRoomTreasurerIds(roomId).contains(userId)



                || requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)



                .getBoolean("treasurer_" + userId, false);



    }







    private boolean isCurrentUserOwner() {



        Room room = viewModel.getRoom().getValue();



        String rId = room != null ? room.getId() : this.roomId;



        if (room == null && rId != null) {



            room = RoomRepository.getInstance().getRoomSync(rId);



        }



        String currentUserId = getContext() != null ? UserSessionManager.getInstance(requireContext()).getUserId() : null;



        if (currentUserId == null || currentUserId.trim().isEmpty()) {



            currentUserId = RoomRepository.getInstance().getCurrentUserId();



        }



        if (room != null) {



            if (room.getCreatedBy() != null && !room.getCreatedBy().trim().isEmpty()) {



                return currentUserId != null && currentUserId.equalsIgnoreCase(room.getCreatedBy());



            }



            return "OWNER".equalsIgnoreCase(room.getCurrentUserRole());



        }



        if (rId != null && getContext() != null) {



            String savedOwnerId = requireContext().getSharedPreferences("room_roles_" + rId, Context.MODE_PRIVATE)



                    .getString("owner_id", null);



            if (savedOwnerId != null) {



                return currentUserId != null && currentUserId.equals(savedOwnerId);



            }



        }



        return false;



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



        if (room == null && roomId != null) {



            room = RoomRepository.getInstance().getRoomSync(roomId);



        }



        if (room == null || isCurrentUserOwner()) return false;



        String currentUserId = getContext() != null ? UserSessionManager.getInstance(requireContext()).getUserId() : null;



        if (currentUserId == null || currentUserId.trim().isEmpty()) {



            currentUserId = RoomRepository.getInstance().getCurrentUserId();



        }



        if (currentUserId == null) return false;



        if (RoomRepository.getInstance().isUserRoomTreasurer(room.getId(), currentUserId)) return true;



        List<RoomMember> members = viewModel.getMembers().getValue();



        if (members != null) {



            for (RoomMember m : members) {



                if (m != null && currentUserId.equalsIgnoreCase(m.getUserId()) && "TREASURER".equalsIgnoreCase(m.getRole())) {



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



            String ownerId = (room != null && room.getCreatedBy() != null && !room.getCreatedBy().isEmpty())



                    ? room.getCreatedBy() : savedOwnerId;



            for (RoomMember m : members) {



                if (ownerId != null && m.getUserId() != null && m.getUserId().equalsIgnoreCase(ownerId)) {



                    m.setRole("OWNER");



                } else if ((room != null && room.getTreasurerIds().contains(m.getUserId()))



                        || ((room == null || !room.hasServerTreasurers()) && "TREASURER".equalsIgnoreCase(m.getRole()))



                        || (room == null && isSavedTreasurer(rId, m.getUserId()))) {



                    m.setRole("TREASURER");



                } else {



                    m.setRole("MEMBER");



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







        if (btnRoomTimeAll != null) {



            btnRoomTimeAll.setOnClickListener(v -> setRoomTimeFilter(RoomViewModel.TimeFilterType.ALL, null, null));



            attachTouchEffect(btnRoomTimeAll);



        }



        if (btnRoomTimeWeek != null) {



            btnRoomTimeWeek.setOnClickListener(v -> setRoomTimeFilter(RoomViewModel.TimeFilterType.THIS_WEEK, null, null));



            attachTouchEffect(btnRoomTimeWeek);



        }



        if (btnRoomTimeMonth != null) {



            btnRoomTimeMonth.setOnClickListener(v -> setRoomTimeFilter(RoomViewModel.TimeFilterType.THIS_MONTH, null, null));



            attachTouchEffect(btnRoomTimeMonth);



        }



        if (btnRoomTimeCustom != null) {



            btnRoomTimeCustom.setOnClickListener(v -> showRoomDateRangePicker());



            attachTouchEffect(btnRoomTimeCustom);



        }



        if (btnClearRoomCustomRange != null) btnClearRoomCustomRange.setOnClickListener(v -> setRoomTimeFilter(RoomViewModel.TimeFilterType.ALL, null, null));







        btnHistoryFilterAll.setOnClickListener(v -> updateHistoryFilter(RoomViewModel.FilterType.ALL));



        btnHistoryFilterIncome.setOnClickListener(v -> updateHistoryFilter(RoomViewModel.FilterType.INCOME));



        btnHistoryFilterExpense.setOnClickListener(v -> updateHistoryFilter(RoomViewModel.FilterType.EXPENSE));







        attachTouchEffect(btnHistoryFilterAll);



        attachTouchEffect(btnHistoryFilterIncome);



        attachTouchEffect(btnHistoryFilterExpense);







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
        String roomName = room != null ? room.getName() : "Quỹ nhóm";
        RoomSplitBillDialog.show(requireContext(), requireView(), roomName, count);
    }

    private void showRemindDialog() {
        Room room = viewModel.getRoom().getValue();
        String roomName = room != null ? room.getName() : "Quỹ nhóm";
        String joinCode = room != null && room.getJoinCode() != null ? room.getJoinCode() : "";
        RoomReminderDialog.show(requireContext(), requireView(), roomName, joinCode);
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



        RoomUtilitiesBottomSheet sheet = RoomUtilitiesBottomSheet.newInstance(getEffectiveRoomId());



        sheet.setListener(new RoomUtilitiesBottomSheet.OnUtilityActionListener() {



            @Override



            public void onSplitBillSelected() {



                showSplitBillDialog();



            }









            @Override
            public void onRecurringSelected() {
                if (getActivity() instanceof com.sharedfund.app.MainActivity) {
                    com.sharedfund.app.model.Room r = viewModel.getRoom().getValue();
                    String rName = r != null ? r.getName() : "Quỹ nhóm";
                    ((com.sharedfund.app.MainActivity) getActivity()).openRecurringRules(getEffectiveRoomId(), rName);
                }
            }



        });



        sheet.show(getChildFragmentManager(), "room_utilities_sheet");



    }

    private void checkAndUpdateUtilityOverdueBadge() {
        String rId = getEffectiveRoomId();
        if (rId == null || rId.trim().isEmpty() || tvUtilityOverdueBadge == null) return;
        new com.sharedfund.app.data.TransactionRepository().getRecurringRules(null, rId, new com.sharedfund.app.data.TransactionRepository.DataCallback<java.util.List<com.sharedfund.app.model.RecurringRule>>() {
            @Override
            public void onSuccess(java.util.List<com.sharedfund.app.model.RecurringRule> rules) {
                if (!isAdded() || getContext() == null) return;
                int overdueCount = 0;
                if (rules != null) {
                    for (com.sharedfund.app.model.RecurringRule rule : rules) {
                        if (rule != null && rule.isActive() && !rule.isPaidInCurrentCycle() && rule.getDaysDiffCurrentCycle() >= 0) {
                            overdueCount++;
                        }
                    }
                }
                int finalCount = overdueCount;
                requireActivity().runOnUiThread(() -> {
                    if (tvUtilityOverdueBadge != null) {
                        if (finalCount > 0) {
                            tvUtilityOverdueBadge.setVisibility(View.VISIBLE);
                            tvUtilityOverdueBadge.setText(String.valueOf(finalCount));
                        } else {
                            tvUtilityOverdueBadge.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
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







    private void setRoomTimeFilter(RoomViewModel.TimeFilterType type, Long start, Long end) {



        roomCustomStartMillis = start;



        roomCustomEndMillis = end;



        if (type != RoomViewModel.TimeFilterType.CUSTOM_RANGE) {



            if (cardRoomCustomRangeBadge != null) {



                cardRoomCustomRangeBadge.setVisibility(View.GONE);



            }



        }



        viewModel.setTimeFilter(type, start, end);



        updateRoomTimeFilterButtons();



    }







    private void updateRoomTimeFilterButtons() {

        if (!isAdded() || getContext() == null || viewModel == null) return;

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);

        int surfaceColor = ContextCompat.getColor(requireContext(), R.color.surface);

        int transparentColor = ContextCompat.getColor(requireContext(), android.R.color.transparent);

        int textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary);



        RoomViewModel.TimeFilterType current = viewModel.getCurrentTimeFilter();

        applySegmentTabState(btnRoomTimeAll, current == RoomViewModel.TimeFilterType.ALL, primaryColor, surfaceColor, transparentColor, textPrimary);

        applySegmentTabState(btnRoomTimeWeek, current == RoomViewModel.TimeFilterType.THIS_WEEK, primaryColor, surfaceColor, transparentColor, textPrimary);

        applySegmentTabState(btnRoomTimeMonth, current == RoomViewModel.TimeFilterType.THIS_MONTH, primaryColor, surfaceColor, transparentColor, textPrimary);

        applySegmentTabState(btnRoomTimeCustom, (current == RoomViewModel.TimeFilterType.CUSTOM_RANGE || current == RoomViewModel.TimeFilterType.TODAY), primaryColor, surfaceColor, transparentColor, textPrimary);

    }



    private void applySegmentTabState(MaterialButton btn, boolean selected, int primaryColor, int surfaceColor, int transparentColor, int textPrimary) {

        if (btn == null) return;

        if (selected) {

            btn.setBackgroundResource(R.drawable.bg_segment_tab_glass);

            btn.setBackgroundTintList(null);

            btn.setTextColor(primaryColor);

            btn.setElevation(dpToPx(2));

        } else {

            btn.setBackgroundResource(0);

            btn.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            btn.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));

            btn.setTextColor(textPrimary);

            btn.setElevation(0);

        }

        if (btn == btnRoomTimeCustom) {

            btn.setIconTint(ColorStateList.valueOf(selected ? primaryColor : textPrimary));

        }

    }



    private void attachTouchEffect(View v) {

        if (v == null) return;

        v.setOnTouchListener((view, event) -> {

            switch (event.getAction()) {

                case android.view.MotionEvent.ACTION_DOWN:

                    view.setAlpha(0.75f);

                    break;

                case android.view.MotionEvent.ACTION_UP:

                case android.view.MotionEvent.ACTION_CANCEL:

                    view.setAlpha(1.0f);

                    break;

            }

            return false;

        });

    }



    private void showRoomDateRangePicker() {

        CustomDateRangeDialog dialog = new CustomDateRangeDialog(requireContext(), roomCustomStartMillis, roomCustomEndMillis, (startMillis, endMillis, displayLabel) -> {

            roomCustomStartMillis = startMillis;

            roomCustomEndMillis = endMillis;

            if (tvRoomCustomRange != null) tvRoomCustomRange.setText(displayLabel);

            if (cardRoomCustomRangeBadge != null) cardRoomCustomRangeBadge.setVisibility(View.VISIBLE);

            setRoomTimeFilter(RoomViewModel.TimeFilterType.CUSTOM_RANGE, startMillis, endMillis);

        });

        dialog.show();

    }



    private void updateHistoryFilter(RoomViewModel.FilterType filter) {

        if (viewModel == null) return;

        viewModel.setFilter(filter);

        if (!isAdded() || getContext() == null) return;

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);

        int surfaceColor = ContextCompat.getColor(requireContext(), R.color.surface);

        int borderColor = ContextCompat.getColor(requireContext(), R.color.border);

        int textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary);

        int accentBlueLight = ContextCompat.getColor(requireContext(), R.color.accent_blue_light);



        applySubChipState(btnHistoryFilterAll, filter == RoomViewModel.FilterType.ALL, primaryColor, accentBlueLight, surfaceColor, borderColor, textPrimary);

        applySubChipState(btnHistoryFilterIncome, filter == RoomViewModel.FilterType.INCOME, primaryColor, accentBlueLight, surfaceColor, borderColor, textPrimary);

        applySubChipState(btnHistoryFilterExpense, filter == RoomViewModel.FilterType.EXPENSE, primaryColor, accentBlueLight, surfaceColor, borderColor, textPrimary);

    }



    private void applySubChipState(MaterialButton btn, boolean selected, int activeColor, int activeBgColor, int surfaceColor, int borderColor, int unselectedTextColor) {

        if (btn == null) return;

        btn.setBackgroundTintList(ColorStateList.valueOf(selected ? activeBgColor : surfaceColor));

        btn.setStrokeColor(ColorStateList.valueOf(selected ? activeColor : borderColor));

        btn.setStrokeWidth(dpToPx(1));

        btn.setTextColor(selected ? activeColor : unselectedTextColor);

    }





    private void applyRoleBasedUi(Room room) {
        if (headerViewBinder != null) {
            headerViewBinder.applyRoleBasedUi(isCurrentUserOwner(), isCurrentUserTreasurer());
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







    private Runnable activeJoinRequestsDialogRender;







    private void initPendingJoinRequests() {



        pendingJoinRequests.clear();



        Room r = RoomRepository.getInstance().getRoomSync(getEffectiveRoomId());



        if (r == null && viewModel != null) r = viewModel.getRoom().getValue();



        isRoomPrivate = (r != null && r.isPrivateRoom()) || RoomPrivacyManager.isRoomPrivate(getEffectiveRoomId());



        if (tvSettingsPrivacyStatus != null) {



            tvSettingsPrivacyStatus.setText(isRoomPrivate ? "Riêng tư ▾" : "Công khai ▾");



        }



        if (tvSettingsPrivacyDesc != null) {



            tvSettingsPrivacyDesc.setText(isRoomPrivate



                    ? "Thành viên mới cần bạn phê duyệt để tham gia quỹ."



                    : "Bất kỳ ai có mã tham gia đều có thể vào quỹ ngay lập tức.");



        }



        if (r != null) {



            List<Room.JoinRequestMeta> cloudReqs = r.getPendingJoinRequests();



            for (Room.JoinRequestMeta item : cloudReqs) {



                pendingJoinRequests.add(new PendingJoinRequest(item.userId, item.applicantName, item.email, item.time, item.message));



            }



        }



        List<RoomPrivacyManager.JoinRequestItem> localSaved = RoomPrivacyManager.getPendingRequests(getEffectiveRoomId());



        for (RoomPrivacyManager.JoinRequestItem item : localSaved) {



            boolean exists = false;



            for (PendingJoinRequest p : pendingJoinRequests) {



                if ((item.userId != null && item.userId.equals(p.userId)) || (item.email != null && item.email.equalsIgnoreCase(p.email))) {



                    exists = true;



                    break;



                }



            }



            if (!exists) {



                pendingJoinRequests.add(new PendingJoinRequest(item.userId, item.applicantName, item.email, item.time, item.message));



            }



        }



        updateJoinRequestsBadge();



        if (activeJoinRequestsDialogRender != null) {



            activeJoinRequestsDialogRender.run();



        }



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



            RoomRepository.getInstance().setRoomPrivacy(getEffectiveRoomId(), isRoomPrivate);



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











    private void handleExportRoomReport() {
        if (!isAdded() || getContext() == null) return;
        List<Transaction> currentList = historyTransactionAdapter != null ? historyTransactionAdapter.getTransactions() : null;
        if (currentList == null || currentList.isEmpty()) {
            Toast.makeText(requireContext(), "Không có giao dịch để xuất báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        Room currentRoomObj = viewModel.getRoom().getValue();
        String roomName = (currentRoomObj != null && currentRoomObj.getName() != null) ? currentRoomObj.getName().trim() : "Nhóm Koin";
        String joinCode = (currentRoomObj != null && currentRoomObj.getJoinCode() != null) ? currentRoomObj.getJoinCode().trim() : "";
        String targetSubtitle = !joinCode.isEmpty() ? "Mã tham gia: " + joinCode : "";

        RoomViewModel.TimeFilterType timeFilter = (viewModel != null) ? viewModel.getCurrentTimeFilter() : RoomViewModel.TimeFilterType.ALL;
        String dateRangeLabel;
        if (timeFilter == RoomViewModel.TimeFilterType.THIS_WEEK) {
            dateRangeLabel = "Tuần này";
        } else if (timeFilter == RoomViewModel.TimeFilterType.THIS_MONTH) {
            dateRangeLabel = "Tháng này";
        } else if (timeFilter == RoomViewModel.TimeFilterType.CUSTOM_RANGE) {
            dateRangeLabel = (tvRoomCustomRange != null && tvRoomCustomRange.getText() != null)
                    ? tvRoomCustomRange.getText().toString()
                    : "Khoảng thời gian tùy chọn";
        } else {
            dateRangeLabel = "Toàn bộ thời gian";
        }

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String currentUserName = session.getCurrentUserName();

        String roleLabel = "Thành viên";
        if (isCurrentUserOwner()) {
            roleLabel = "Chủ quỹ";
        } else if (isCurrentUserTreasurer()) {
            roleLabel = "Phó quỹ";
        }

        StatementReportFragment reportFragment = StatementReportFragment.newInstance(
                "Quỹ " + roomName,
                targetSubtitle,
                dateRangeLabel,
                currentUserName,
                roleLabel,
                new ArrayList<>(currentList)
        );

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(reportFragment, true);
        } else {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, reportFragment)
                    .addToBackStack(null)
                    .commit();
        }
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
        if (getContext() == null || member == null || viewModel.getRoom().getValue() == null) return;
        MemberActionBottomSheet sheet = MemberActionBottomSheet.newInstance(viewModel.getRoom().getValue().getId(), member);
        sheet.setListener(new MemberActionBottomSheet.OnMemberActionListener() {
            @Override
            public void onUpdated() {
                viewModel.loadMembers();
                if (onUpdated != null) onUpdated.run();
            }

            @Override
            public void onTransferred() {
                viewModel.loadMembers();
                if (onTransferred != null) {
                    onTransferred.run();
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });
        sheet.show(getChildFragmentManager(), "MemberAction");
    }

    private void showDelegationDialog() {
        if (getContext() == null || viewModel.getRoom().getValue() == null) return;
        MemberDelegationDialogFragment dialog = MemberDelegationDialogFragment.newInstance(viewModel.getRoom().getValue().getId());
        dialog.show(getChildFragmentManager(), "MemberDelegation");
    }

    private void showJoinRequestsDialog() {
        if (getContext() == null || getEffectiveRoomId() == null) return;
        JoinRequestsBottomSheet sheet = JoinRequestsBottomSheet.newInstance(getEffectiveRoomId());
        sheet.setChangeListener(() -> {
            initPendingJoinRequests();
            updateJoinRequestsBadge();
            if (viewModel != null) {
                viewModel.loadMembers();
            }
        });
        sheet.show(getChildFragmentManager(), "join_requests_dialog");
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



        String myId = getContext() != null ? UserSessionManager.getInstance(requireContext()).getUserId() : null;



        if (myId == null || myId.trim().isEmpty()) {



            myId = RoomRepository.getInstance().getCurrentUserId();



        }



        boolean isSelf = myId != null && transaction != null && transaction.getUserId() != null && myId.equalsIgnoreCase(transaction.getUserId());



        boolean requesterPrivileged = transaction != null && transaction.getUserId() != null



                && (RoomRepository.getInstance().isUserRoomOwner(roomId, transaction.getUserId())



                || RoomRepository.getInstance().isUserRoomTreasurer(roomId, transaction.getUserId()));



        boolean canReview = isCurrentUserAdmin() && !isSelf && (isCurrentUserOwner() || !requesterPrivileged);



        TransactionDetailBottomSheet sheet = TransactionDetailBottomSheet.newInstance(transaction, canReview);



        sheet.setActionListener(new TransactionDetailBottomSheet.OnTransactionActionListener() {



            @Override



            public void onApprove(Transaction tx) {



                UserSessionManager session = UserSessionManager.getInstance(requireContext());



                String adminId = session.getUserId();



                if (adminId == null || adminId.isEmpty()) {



                    adminId = RoomRepository.getInstance().getCurrentUserId();



                }



                final Context appCtx = requireContext().getApplicationContext();



                final String approverId = adminId;



                final String roleLabel = RoomRepository.getInstance().isUserRoomOwner(roomId, adminId) ? "Chủ quỹ" : "Phó quỹ";



                RoomRepository.getInstance().configureAccessToken(session.getAccessToken());



                com.sharedfund.app.data.TransactionRepository remote = new com.sharedfund.app.data.TransactionRepository();



                remote.setAccessToken(session.getAccessToken());



                remote.approveExpense(adminId, tx.getId(), new com.sharedfund.app.data.TransactionRepository.DataCallback<com.google.gson.JsonObject>() {



                    @Override



                    public void onSuccess(com.google.gson.JsonObject data) {



                        RoomRepository.getInstance().markRecurringRulePaidIfMatched(roomId, tx);



                        RoomRepository.getInstance().dispatchExpenseResultNotification(appCtx, roomId, tx, true, null, approverId, roleLabel);



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



                final Context appCtx = requireContext().getApplicationContext();



                final String approverId = adminId;



                final String roleLabel = RoomRepository.getInstance().isUserRoomOwner(roomId, adminId) ? "Chủ quỹ" : "Phó quỹ";



                RoomRepository.getInstance().configureAccessToken(session.getAccessToken());



                com.sharedfund.app.data.TransactionRepository remote = new com.sharedfund.app.data.TransactionRepository();



                remote.setAccessToken(session.getAccessToken());



                remote.rejectExpense(adminId, tx.getId(), reason, new com.sharedfund.app.data.TransactionRepository.DataCallback<com.google.gson.JsonObject>() {



                    @Override



                    public void onSuccess(com.google.gson.JsonObject data) {



                        RoomRepository.getInstance().dispatchExpenseResultNotification(appCtx, roomId, tx, false, reason, approverId, roleLabel);



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



