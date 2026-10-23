package com.sharedfund.app.ui.home;

import android.app.AlertDialog;
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
import com.sharedfund.app.model.Room;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Màn hình Trang chủ (HomeFragment)
 * Quản lý danh sách quỹ, tổng số dư toàn bộ, trạng thái tải và các thao tác Tạo / Tham gia quỹ.
 * Đã chuẩn hóa kiểm thử giao diện: không vỡ màn hình, cuộn danh sách mượt mà, số tiền không bị cắt,
 * thông báo thiếu dữ liệu bằng TextInputLayout, nút bấm phản hồi tốt, empty & loading state đầy đủ.
 */
public class HomeFragment extends Fragment implements RoomAdapter.OnRoomClickListener {

    private HomeViewModel viewModel;
    private RoomAdapter roomAdapter;

    private TextView tvTotalBalance;
    private TextView tvRoomCountSubtitle;
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

        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserAvatar();
    }

    private void updateUserAvatar() {
        if (tvUserInitials != null && getContext() != null) {
            String name = com.sharedfund.app.data.UserSessionManager.getInstance(requireContext()).getCurrentUserName();
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

        // Quan sát danh sách quỹ
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

        // Quan sát tổng số dư
        viewModel.getTotalBalance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double balance) {
                if (balance != null) {
                    DecimalFormat df = new DecimalFormat("#,###");
                    String formatted = df.format(balance).replace(',', '.') + " ₫";
                    tvTotalBalance.setText(formatted);
                }
            }
        });

        // Trạng thái tải
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                pbLoading.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
            }
        });

        // Thông báo kết quả thao tác
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
                    pbLoading.setVisibility(View.VISIBLE);
                    // Trigger refresh
                    rvRooms.setVisibility(View.VISIBLE);
                    pbLoading.setVisibility(View.GONE);
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

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.btn_confirm, null) // Sẽ ghi đè listener để chặn auto-dismiss khi có lỗi
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

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
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
