package com.sharedfund.app.ui.home;

import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sharedfund.app.ui.room.DepositDialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.adapter.RoomAdapter;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.ui.profile.AvatarImageLoader;

import java.text.DecimalFormat;
import java.util.List;

public class HomeFragment extends Fragment implements RoomAdapter.OnRoomClickListener {

    private HomeViewModel viewModel;
    private RoomAdapter roomAdapter;

    private TextView tvTotalBalance;
    private TextView tvRoomCountSubtitle;
    private ImageView ivHomeAvatar;
    private TextView tvHomeAvatarInitials;
    private TextView tvTotalRoomBalanceMini;
    private MaterialButton btnHomeDeposit;
    private TextView tvRoomCountBadge;
    private MaterialButton btnCreateRoom;
    private MaterialButton btnJoinRoom;
    private MaterialButton btnEmptyCreateRoom;
    private RecyclerView rvRooms;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetryRooms;
    private ProgressBar pbLoading;

    private View btnUserProfile;
    private View accountDrawerScrim;
    private LinearLayout accountDrawer;
    private TextView tvDrawerUserName;
    private TextView tvDrawerUserEmail;
    private TextView tvDrawerThemeValue;
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
        drawerBackCallback = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { hideAccountDrawer(); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), drawerBackCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHomeAvatar();
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
        tvDrawerThemeValue = view.findViewById(R.id.tvDrawerThemeValue);
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        ivHomeAvatar = view.findViewById(R.id.ivHomeAvatar);
        tvHomeAvatarInitials = view.findViewById(R.id.tvHomeAvatarInitials);
        tvRoomCountSubtitle = view.findViewById(R.id.tvRoomCountSubtitle);
        tvTotalRoomBalanceMini = view.findViewById(R.id.tvTotalRoomBalanceMini);
        btnHomeDeposit = view.findViewById(R.id.btnHomeDeposit);
        tvRoomCountBadge = view.findViewById(R.id.tvRoomCountBadge);
        btnCreateRoom = view.findViewById(R.id.btnCreateRoom);
        btnJoinRoom = view.findViewById(R.id.btnJoinRoom);
        btnEmptyCreateRoom = view.findViewById(R.id.btnEmptyCreateRoom);
        rvRooms = view.findViewById(R.id.rvRooms);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        layoutErrorState = view.findViewById(R.id.layoutErrorState);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetryRooms = view.findViewById(R.id.btnRetryRooms);
        pbLoading = view.findViewById(R.id.pbLoading);

    }

    private void updateHomeAvatar() {
        if (ivHomeAvatar == null || getContext() == null) return;
        com.sharedfund.app.data.UserSessionManager session =
                com.sharedfund.app.data.UserSessionManager.getInstance(requireContext());
        String name = session.getCurrentUserName();
        if (name == null || name.trim().isEmpty()) {
            tvHomeAvatarInitials.setText("U");
        } else {
            String[] parts = name.trim().split("\\s+");
            String initials = parts.length > 1
                    ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0))
                    : String.valueOf(parts[0].charAt(0));
            tvHomeAvatarInitials.setText(initials.toUpperCase(java.util.Locale.ROOT));
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
                roomAdapter.setRooms(rooms);
                if (layoutErrorState != null) {
                    layoutErrorState.setVisibility(View.GONE);
                }
                if (rooms == null || rooms.isEmpty()) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    rvRooms.setVisibility(View.GONE);
                    tvRoomCountBadge.setText("0 quỹ");
                    tvRoomCountSubtitle.setText("0 quỹ hoạt động");
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvRooms.setVisibility(View.VISIBLE);
                    tvRoomCountBadge.setText(rooms.size() + " quỹ");
                    tvRoomCountSubtitle.setText(rooms.size() + " quỹ hoạt động");
                }
            }
        });

        viewModel.getPersonalWalletBalance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double walletBalance) {
                if (walletBalance != null && tvTotalBalance != null) {
                    DecimalFormat df = new DecimalFormat("#,###");
                    String formatted = df.format(walletBalance).replace(',', '.') + " ₫";
                    tvTotalBalance.setText(formatted);
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
                    } else {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        if (btnHomeDeposit != null) {
            btnHomeDeposit.setVisibility(View.VISIBLE);
            btnHomeDeposit.setOnClickListener(v -> {
                DepositDialogFragment dialog = DepositDialogFragment.newWalletInstance();
                dialog.setOnDepositCompleteListener(amount -> viewModel.refreshWalletBalance());
                dialog.show(getChildFragmentManager(), "wallet_deposit_dialog");
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
        viewClick(R.id.btnDrawerTheme, v -> showThemeChooser());
        viewClick(R.id.btnDrawerLogout, v -> confirmDrawerLogout());
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
            tvDrawerThemeValue.setText(((MainActivity) getActivity()).isDarkTheme() ? "Tối" : "Sáng");
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

    private void showThemeChooser() {
        boolean dark = getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isDarkTheme();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chỉnh theme")
                .setSingleChoiceItems(new String[]{"Sáng", "Tối"}, dark ? 1 : 0, (dialog, which) -> {
                    dialog.dismiss();
                    hideAccountDrawer();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setDarkTheme(which == 1);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDrawerLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất khỏi tài khoản này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    com.sharedfund.app.data.UserSessionManager.getInstance(requireContext()).clearSession();
                    com.sharedfund.app.data.RoomRepository.getInstance().setCurrentUser("", "");
                    hideAccountDrawer();
                    Toast.makeText(requireContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openLogin();
                })
                .show();
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
            Toast.makeText(requireContext(), getString(R.string.copied_code) + " (" + room.getJoinCode() + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCreateRoomDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_room, null);
        final TextInputLayout tilRoomName = dialogView.findViewById(R.id.tilDialogRoomName);
        final TextInputEditText etRoomName = dialogView.findViewById(R.id.etDialogRoomName);
        final TextInputLayout tilRoomDesc = dialogView.findViewById(R.id.tilDialogRoomDesc);
        final TextInputEditText etRoomDesc = dialogView.findViewById(R.id.etDialogRoomDesc);

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

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.btn_confirm, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String name = etRoomName.getText() != null ? etRoomName.getText().toString().trim() : "";
                String desc = etRoomDesc.getText() != null ? etRoomDesc.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    tilRoomName.setError("Tên quỹ không được để trống!");
                    return;
                }

                viewModel.createRoom(name, desc);
                dialog.dismiss();
            });
        });

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

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.btn_confirm, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String code = etJoinCode.getText() != null ? etJoinCode.getText().toString().trim() : "";

                if (code.isEmpty()) {
                    tilJoinCode.setError("Vui lòng nhập mã tham gia!");
                    return;
                }

                viewModel.joinRoom(code);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
