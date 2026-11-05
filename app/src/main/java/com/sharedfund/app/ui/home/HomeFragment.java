package com.sharedfund.app.ui.home;

import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.sharedfund.app.ui.widget.AppToaster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.app.Dialog;
import android.view.Window;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.adapter.RoomAdapter;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.util.CopyToast;
import com.sharedfund.app.ui.profile.AvatarImageLoader;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements RoomAdapter.OnRoomClickListener {

    private HomeViewModel viewModel;
    private RoomAdapter roomAdapter;

    private TextView tvHomeUserName;
    private View layoutHomeUserId;
    private TextView tvHomeUserIdTag;
    private TextView tvTotalBalance;
    private ImageButton btnToggleBalanceVisibility;
    private MaterialButton btnHomeDepositWithdraw;
    private TextView tvRoomCountSubtitle;
    private ImageView ivHomeAvatar;
    private TextView tvHomeAvatarInitials;
    private TextView tvTotalRoomBalanceMini;
    private TextView tvRoomCountBadge;
    private EditText etHomeSearchRooms;
    private ImageButton btnClearSearch;
    private final List<Room> allRoomsList = new ArrayList<>();
    private MaterialButton btnWalletHistory;
    private MaterialButton btnCreateRoom;
    private MaterialButton btnJoinRoom;
    private MaterialButton btnEmptyCreateRoom;
    private RecyclerView rvRooms;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetryRooms;
    private ProgressBar pbLoading;

    private static final String PREFS_NAME = "shared_fund_prefs";
    private static final String KEY_BALANCE_HIDDEN = "key_balance_hidden";
    private boolean isBalanceHidden = false;
    private double currentWalletBalance = 0.0;

    private View btnUserProfile;
    private View accountDrawerScrim;
    private LinearLayout accountDrawer;
    private TextView tvDrawerUserName;
    private TextView tvDrawerUserEmail;
    private com.google.android.material.materialswitch.MaterialSwitch switchDrawerTheme;
    private OnBackPressedCallback drawerBackCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        if (session.isLoggedIn()) {
            RoomRepository.getInstance().setCurrentUser(session.getUserId(), session.getFullName());
        }

        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupClickListeners();
        updateUserProfileHeader();
        renderWalletBalance();
        drawerBackCallback = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { hideAccountDrawer(); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), drawerBackCallback);
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
        updateUserProfileHeader();
        renderWalletBalance();
        if (viewModel != null) {
            viewModel.refreshWalletBalance();
            Double roomTotal = viewModel.getTotalRoomBalance().getValue();
            if (roomTotal != null && tvTotalRoomBalanceMini != null) {
                DecimalFormat df = new DecimalFormat("#,###");
                tvTotalRoomBalanceMini.setText(df.format(roomTotal).replace(',', '.') + " ₫");
            }
        }
    }

    private void initViews(View view) {
        btnUserProfile = view.findViewById(R.id.btnUserProfile);
        accountDrawerScrim = view.findViewById(R.id.accountDrawerScrim);
        accountDrawer = view.findViewById(R.id.accountDrawer);
        tvDrawerUserName = view.findViewById(R.id.tvDrawerUserName);
        tvDrawerUserEmail = view.findViewById(R.id.tvDrawerUserEmail);
        switchDrawerTheme = view.findViewById(R.id.switchDrawerTheme);
        tvHomeUserName = view.findViewById(R.id.tvHomeUserName);
        layoutHomeUserId = view.findViewById(R.id.layoutHomeUserId);
        tvHomeUserIdTag = view.findViewById(R.id.tvHomeUserIdTag);
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        btnToggleBalanceVisibility = view.findViewById(R.id.btnToggleBalanceVisibility);
        btnHomeDepositWithdraw = view.findViewById(R.id.btnHomeDepositWithdraw);
        ivHomeAvatar = view.findViewById(R.id.ivHomeAvatar);
        tvHomeAvatarInitials = view.findViewById(R.id.tvHomeAvatarInitials);
        tvRoomCountSubtitle = view.findViewById(R.id.tvRoomCountSubtitle);
        tvTotalRoomBalanceMini = view.findViewById(R.id.tvTotalRoomBalanceMini);
        tvRoomCountBadge = view.findViewById(R.id.tvRoomCountBadge);
        etHomeSearchRooms = view.findViewById(R.id.etHomeSearchRooms);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        btnWalletHistory = view.findViewById(R.id.btnWalletHistory);
        btnCreateRoom = view.findViewById(R.id.btnCreateRoom);
        btnJoinRoom = view.findViewById(R.id.btnJoinRoom);
        btnEmptyCreateRoom = view.findViewById(R.id.btnEmptyCreateRoom);
        rvRooms = view.findViewById(R.id.rvRooms);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        layoutErrorState = view.findViewById(R.id.layoutErrorState);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetryRooms = view.findViewById(R.id.btnRetryRooms);
        pbLoading = view.findViewById(R.id.pbLoading);

        if (getContext() != null) {
            isBalanceHidden = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_BALANCE_HIDDEN, false);
        }
    }

    private String formatAccountNumber(String name, String userId) {
        if ("DO QUOC TRUNG".equalsIgnoreCase(name)) {
            return "1047061615";
        }
        if (userId == null || userId.trim().isEmpty()) {
            return "1047061615";
        }
        long hash = Math.abs((long) userId.hashCode());
        long num = 1000000000L + (hash % 9000000000L);
        return String.valueOf(num);
    }

    private void updateUserProfileHeader() {
        if (getContext() == null) return;
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String name = session.getCurrentUserName();
        if (tvHomeUserName != null) {
            tvHomeUserName.setText(name != null && !name.trim().isEmpty() ? name : "Người dùng");
        }
        String userId = session.getUserId();
        if (tvHomeUserIdTag != null) {
            final String stk = formatAccountNumber(name != null ? name.trim() : "", userId);
            tvHomeUserIdTag.setText(stk);
            if (layoutHomeUserId != null) {
                layoutHomeUserId.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Số tài khoản", stk);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        CopyToast.show(requireContext(), requireView());
                    }
                });
            }
        }
        updateHomeAvatar();
    }

    private void renderWalletBalance() {
        if (tvTotalBalance == null || getContext() == null) return;
        if (isBalanceHidden) {
            tvTotalBalance.setText("•••••• ₫");
            if (btnToggleBalanceVisibility != null) {
                btnToggleBalanceVisibility.setImageResource(R.drawable.ic_visibility_off);
            }
        } else {
            DecimalFormat df = new DecimalFormat("#,###");
            String formatted = df.format(currentWalletBalance).replace(',', '.') + " ₫";
            tvTotalBalance.setText(formatted);
            if (btnToggleBalanceVisibility != null) {
                btnToggleBalanceVisibility.setImageResource(R.drawable.ic_visibility);
            }
        }
    }

    private void updateHomeAvatar() {
        if (ivHomeAvatar == null || getContext() == null) return;
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String name = session.getCurrentUserName();
        if (name == null || name.trim().isEmpty()) {
            tvHomeAvatarInitials.setText("U");
        } else {
            String[] parts = name.trim().split("\\s+");
            String initials = parts.length > 1
                    ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0))
                    : String.valueOf(parts[0].charAt(0));
            tvHomeAvatarInitials.setText(initials.toUpperCase(Locale.ROOT));
        }
        tvHomeAvatarInitials.setVisibility(View.VISIBLE);
        AvatarImageLoader.load(ivHomeAvatar, tvHomeAvatarInitials, session.getAvatarUrl());
    }

    private void setupRecyclerView() {
        roomAdapter = new RoomAdapter(this);
        rvRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRooms.setAdapter(roomAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        UserSessionManager activeSession = UserSessionManager.getInstance(requireContext());
        if (activeSession.isLoggedIn()) {
            viewModel.configureSupabase(activeSession.getUserId(), activeSession.getFullName(),
                    activeSession.getAccessToken());
        }

        viewModel.getRooms().observe(getViewLifecycleOwner(), new Observer<List<Room>>() {
            @Override
            public void onChanged(List<Room> rooms) {
                allRoomsList.clear();
                if (rooms != null) {
                    allRoomsList.addAll(rooms);
                }
                if (layoutErrorState != null) {
                    layoutErrorState.setVisibility(View.GONE);
                }
                String query = etHomeSearchRooms != null && etHomeSearchRooms.getText() != null
                        ? etHomeSearchRooms.getText().toString() : "";
                filterRooms(query);
            }
        });

        viewModel.getPersonalWalletBalance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double walletBalance) {
                if (walletBalance != null) {
                    currentWalletBalance = walletBalance;
                    renderWalletBalance();
                }
            }
        });

        viewModel.getTotalRoomBalance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double roomTotal) {
                if (roomTotal != null && tvTotalRoomBalanceMini != null) {
                    DecimalFormat df = new DecimalFormat("#,###");
                    String formatted = df.format(roomTotal).replace(',', '.') + " ₫";
                    tvTotalRoomBalanceMini.setText(formatted);
                }
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                pbLoading.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getActionMessage().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String msg) {
                if (msg != null && !msg.isEmpty()) {
                    if (msg.startsWith("Tạo quỹ thất bại:")) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Không tạo được quỹ")
                                .setMessage(msg)
                                .setPositiveButton("Đóng", null)
                                .show();
                    } else if (msg.toLowerCase().contains("thất bại") || msg.toLowerCase().contains("lỗi")) {
                        AppToaster.error(requireActivity(), msg);
                    }
                }
            }
        });

        viewModel.getPendingApprovalRoom().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String roomName) {
                if (roomName != null && !roomName.isEmpty()) {
                    showPendingApprovalDialog(roomName);
                }
            }
        });
    }

    private void setupClickListeners() {
        if (btnToggleBalanceVisibility != null) {
            btnToggleBalanceVisibility.setOnClickListener(v -> {
                isBalanceHidden = !isBalanceHidden;
                if (getContext() != null) {
                    requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_BALANCE_HIDDEN, isBalanceHidden)
                            .apply();
                }
                renderWalletBalance();
            });
        }

        if (btnHomeDepositWithdraw != null) {
            btnHomeDepositWithdraw.setOnClickListener(v -> {
                if (getActivity() instanceof com.sharedfund.app.MainActivity) {
                    ((com.sharedfund.app.MainActivity) getActivity()).openWalletTransfer();
                }
            });
        }

        if (btnWalletHistory != null) {
            btnWalletHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof com.sharedfund.app.MainActivity) {
                        ((com.sharedfund.app.MainActivity) getActivity()).openWalletTransactions();
                    }
                }
            });
        }

        btnCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateRoomDialog();
            }
        });

        if (btnEmptyCreateRoom != null) {
            btnEmptyCreateRoom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCreateRoomDialog();
                }
            });
        }

        btnJoinRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showJoinRoomDialog();
            }
        });

        if (btnRetryRooms != null) {
            btnRetryRooms.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    layoutErrorState.setVisibility(View.GONE);
                    viewModel.loadRemoteData();
                }
            });
        }

        if (btnUserProfile != null) {
            btnUserProfile.setOnClickListener(v -> showAccountDrawer());
        }
        accountDrawerScrim.setOnClickListener(v -> hideAccountDrawer());
        viewClick(R.id.btnCloseAccountDrawer, v -> hideAccountDrawer());
        viewClick(R.id.btnDrawerAccount, v -> {
            hideAccountDrawer();
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openProfile();
        });
        if (switchDrawerTheme != null) {
            switchDrawerTheme.setOnCheckedChangeListener((btn, isChecked) -> {
                if (btn.isPressed() && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setDarkTheme(isChecked);
                }
            });
        }
        viewClick(R.id.btnDrawerLogout, v -> confirmDrawerLogout());

        if (etHomeSearchRooms != null) {
            etHomeSearchRooms.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterRooms(s != null ? s.toString() : "");
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etHomeSearchRooms != null) {
                    etHomeSearchRooms.setText("");
                }
            });
        }
    }

    private void filterRooms(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (btnClearSearch != null) btnClearSearch.setVisibility(View.GONE);
            roomAdapter.setRooms(allRoomsList);
            if (btnEmptyCreateRoom != null) btnEmptyCreateRoom.setVisibility(View.VISIBLE);
            if (allRoomsList.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
                rvRooms.setVisibility(View.GONE);
                tvRoomCountBadge.setText("0 quỹ");
                tvRoomCountSubtitle.setText("0 quỹ hoạt động");
            } else {
                layoutEmptyState.setVisibility(View.GONE);
                rvRooms.setVisibility(View.VISIBLE);
                tvRoomCountBadge.setText(allRoomsList.size() + " quỹ");
                tvRoomCountSubtitle.setText(allRoomsList.size() + " quỹ hoạt động");
            }
            return;
        }

        if (btnClearSearch != null) btnClearSearch.setVisibility(View.VISIBLE);
        List<Room> filtered = com.sharedfund.app.data.SearchHelper.filterRooms(allRoomsList, query);

        roomAdapter.setRooms(filtered);
        if (filtered.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvRooms.setVisibility(View.GONE);
            if (btnEmptyCreateRoom != null) btnEmptyCreateRoom.setVisibility(View.GONE);
            tvRoomCountBadge.setText("0 quỹ");
            tvRoomCountSubtitle.setText("0/" + allRoomsList.size() + " quỹ khớp");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvRooms.setVisibility(View.VISIBLE);
            if (btnEmptyCreateRoom != null) btnEmptyCreateRoom.setVisibility(View.VISIBLE);
            tvRoomCountBadge.setText(filtered.size() + " quỹ");
            tvRoomCountSubtitle.setText(filtered.size() + "/" + allRoomsList.size() + " quỹ khớp");
        }
    }

    private interface DrawerAction { void run(View view); }

    private void viewClick(int id, DrawerAction action) {
        View item = requireView().findViewById(id);
        if (item != null) item.setOnClickListener(action::run);
    }

    private void showAccountDrawer() {
        if (accountDrawer == null || accountDrawerScrim == null) return;
        com.sharedfund.app.data.UserSessionManager session =
                com.sharedfund.app.data.UserSessionManager.getInstance(requireContext());
        tvDrawerUserName.setText(session.getCurrentUserName());
        tvDrawerUserEmail.setText(session.getCurrentUserEmail());
        if (getActivity() instanceof MainActivity) {
            if (switchDrawerTheme != null) switchDrawerTheme.setChecked(((MainActivity) getActivity()).isDarkTheme());
        }
        accountDrawerScrim.setVisibility(View.VISIBLE);
        accountDrawerScrim.setAlpha(0f);
        accountDrawerScrim.animate().alpha(1f).setDuration(180).start();
        accountDrawer.setVisibility(View.VISIBLE);
        accountDrawer.setAlpha(1f);
        accountDrawer.post(() -> {
            if (accountDrawer == null || !isAdded()) return;
            accountDrawer.setTranslationX(accountDrawer.getWidth());
            accountDrawer.animate().translationX(0f).setDuration(220).start();
        });
        if (drawerBackCallback != null) drawerBackCallback.setEnabled(true);
    }

    private void hideAccountDrawer() {
        if (accountDrawer == null || accountDrawer.getVisibility() != View.VISIBLE) return;
        int slideDistance = accountDrawer.getWidth();
        accountDrawer.animate().translationX(slideDistance).setDuration(180).withEndAction(() -> {
            if (accountDrawer != null) {
                accountDrawer.setVisibility(View.GONE);
                accountDrawer.setTranslationX(0f);
            }
        }).start();
        accountDrawerScrim.animate().alpha(0f).setDuration(180)
                .withEndAction(() -> accountDrawerScrim.setVisibility(View.GONE)).start();
        if (drawerBackCallback != null) drawerBackCallback.setEnabled(false);
    }

    

    private void confirmDrawerLogout() {
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
        if (tvTitle != null) tvTitle.setText("Đăng xuất khỏi tài khoản?");
        if (tvMessage != null) tvMessage.setText("Bạn sẽ cần đăng nhập lại để tiếp tục quản lý các quỹ chung.");
        if (btnAction != null) {
            btnAction.setText("Đăng xuất");
            btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_expense)));
            btnAction.setOnClickListener(v -> {
                dialog.dismiss();
                com.sharedfund.app.data.SupabaseRealtimeClient.getInstance().disconnect();
                com.sharedfund.app.data.UserSessionManager.getInstance(requireContext()).clearSession();
                com.sharedfund.app.data.RoomRepository.getInstance().setCurrentUser("", "");
                hideAccountDrawer();
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openLogin();
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    @Override
    public void onRoomClick(Room room) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openRoomDetail(room.getId());
        }
    }

    @Override
    public void onCopyCodeClick(Room room) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Join Code", room.getJoinCode());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            CopyToast.show(requireContext(), requireView());
        }
    }

    private void showCreateRoomDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_room, null);
        final TextInputLayout tilRoomName = dialogView.findViewById(R.id.tilDialogRoomName);
        final TextInputEditText etRoomName = dialogView.findViewById(R.id.etDialogRoomName);
        final TextInputLayout tilRoomDesc = dialogView.findViewById(R.id.tilDialogRoomDesc);
        final TextInputEditText etRoomDesc = dialogView.findViewById(R.id.etDialogRoomDesc);
        final TextInputLayout tilRoomTarget = dialogView.findViewById(R.id.tilDialogRoomTarget);
        final TextInputEditText etRoomTarget = dialogView.findViewById(R.id.etDialogRoomTarget);

        etRoomName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilRoomName.getError() != null) {
                    tilRoomName.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (etRoomTarget != null) {
            etRoomTarget.addTextChangedListener(new TextWatcher() {
                private boolean isFormatting = false;
                private final DecimalFormat formatter;
                {
                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                    symbols.setGroupingSeparator('.');
                    formatter = new DecimalFormat("#,##0", symbols);
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isFormatting || s == null) return;
                    isFormatting = true;
                    String raw = s.toString().replaceAll("[^0-9]", "");
                    if (!raw.isEmpty()) {
                        try {
                            long val = Long.parseLong(raw);
                            String formatted = formatter.format(val);
                            if (!formatted.equals(s.toString())) {
                                s.replace(0, s.length(), formatted);
                                etRoomTarget.setSelection(s.length());
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    isFormatting = false;
                }
            });
        }

        final View btnCancel = dialogView.findViewById(R.id.btnCancelCreateRoom);
        final View btnConfirm = dialogView.findViewById(R.id.btnConfirmCreateRoom);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .setCancelable(true)
                .create();

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String name = etRoomName.getText() != null ? etRoomName.getText().toString().trim() : "";
                String desc = etRoomDesc != null && etRoomDesc.getText() != null ? etRoomDesc.getText().toString().trim() : "";
                String targetStr = etRoomTarget != null && etRoomTarget.getText() != null ? etRoomTarget.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    tilRoomName.setError("Tên quỹ không được để trống!");
                    return;
                }

                double targetAmount = 0.0;
                if (!targetStr.isEmpty()) {
                    String cleanDigits = targetStr.replaceAll("[^0-9]", "");
                    if (!cleanDigits.isEmpty()) {
                        try {
                            targetAmount = Double.parseDouble(cleanDigits);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                viewModel.createRoom(name, desc, targetAmount);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showJoinRoomDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_room, null);
        final TextInputLayout tilJoinCode = dialogView.findViewById(R.id.tilDialogJoinCode);
        final TextInputEditText etJoinCode = dialogView.findViewById(R.id.etDialogJoinCode);

        etJoinCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilJoinCode.getError() != null) {
                    tilJoinCode.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        final View btnCancel = dialogView.findViewById(R.id.btnCancelJoinRoom);
        final View btnConfirm = dialogView.findViewById(R.id.btnConfirmJoinRoom);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .setCancelable(true)
                .create();

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String code = etJoinCode.getText() != null ? etJoinCode.getText().toString().trim() : "";

                if (code.isEmpty()) {
                    tilJoinCode.setError("Vui lòng nhập mã tham gia!");
                    return;
                }

                viewModel.joinRoom(code);
                dialog.dismiss();
            });
        }

        dialog.show();
    }
    private void showPendingApprovalDialog(String roomName) {
        if (getContext() == null) return;
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_koin_notice);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        TextView tvTitle = dialog.findViewById(R.id.tvNoticeDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvNoticeDialogMessage);
        View btnClose = dialog.findViewById(R.id.btnNoticeDialogClose);

        if (tvTitle != null) {
            tvTitle.setText("Đã gửi yêu cầu tham gia");
        }
        if (tvMessage != null) {
            tvMessage.setText("Quỹ \"" + roomName + "\" đang ở chế độ riêng tư. Yêu cầu tham gia của bạn đã được gửi, vui lòng chờ Chủ quỹ hoặc Phó quỹ phê duyệt.");
        }
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

}
