package com.sharedfund.app.ui.home;

import androidx.appcompat.app.AlertDialog;
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

import java.text.DecimalFormat;
import java.util.List;

public class HomeFragment extends Fragment implements RoomAdapter.OnRoomClickListener {

    private HomeViewModel viewModel;
    private RoomAdapter roomAdapter;

    private TextView tvTotalBalance;
    private TextView tvRoomCountSubtitle;
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
    private TextView tvUserInitials;

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
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserAvatar();
        if (viewModel != null) {
            Double walletBal = viewModel.getUserWalletBalance().getValue();
            if (walletBal != null && tvTotalBalance != null) {
                DecimalFormat df = new DecimalFormat("#,###");
                tvTotalBalance.setText(df.format(walletBal).replace(',', '.') + " ₫");
            }
            Double roomTotal = viewModel.getTotalRoomBalance().getValue();
            if (roomTotal != null && tvTotalRoomBalanceMini != null) {
                DecimalFormat df = new DecimalFormat("#,###");
                tvTotalRoomBalanceMini.setText(df.format(roomTotal).replace(',', '.') + " ₫");
            }
        }
    }

    private void updateUserAvatar() {
        if (tvUserInitials != null && getContext() != null) {
            String name = UserSessionManager.getInstance(requireContext()).getCurrentUserName();
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split(" ");
                if (parts.length >= 2) {
                    tvUserInitials.setText(("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase());
                } else {
                    tvUserInitials.setText(("" + name.charAt(0)).toUpperCase());
                }
            } else {
                tvUserInitials.setText("U");
            }
        }
    }

    private void initViews(View view) {
        btnUserProfile = view.findViewById(R.id.btnUserProfile);
        tvUserInitials = view.findViewById(R.id.tvUserInitials);
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
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

        updateUserAvatar();
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

        viewModel.getUserWalletBalance().observe(getViewLifecycleOwner(), new Observer<Double>() {
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
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        if (btnHomeDeposit != null) {
            btnHomeDeposit.setVisibility(View.VISIBLE);
            btnHomeDeposit.setOnClickListener(v -> {
                DepositDialogFragment dialog = DepositDialogFragment.newWalletInstance();
                dialog.show(getChildFragmentManager(), "home_wallet_deposit");
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
            btnUserProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openProfile();
                    }
                }
            });
        }
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
