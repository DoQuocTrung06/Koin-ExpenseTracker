package com.sharedfund.app.ui.profile;

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

import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Profile;

/**
 * Màn hình Thông tin cá nhân (ProfileFragment)
 * Hiển thị avatar initials, email, họ tên, số quỹ tham gia và số giao dịch.
 * Xử lý chức năng Đăng xuất an toàn: xóa phiên đăng nhập -> về LoginFragment.
 */
public class ProfileFragment extends Fragment {

    private TextView tvAvatarInitials;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileFullName;
    private TextView tvProfileEmailDetail;
    private TextView tvProfileUserId;
    private TextView tvProfileRoomCount;
    private TextView tvProfileTxCount;
    private ImageButton btnProfileBack;
    private MaterialButton btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        loadProfileData();
        setupClickListeners();
    }

    private void bindViews(View view) {
        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileFullName = view.findViewById(R.id.tvProfileFullName);
        tvProfileEmailDetail = view.findViewById(R.id.tvProfileEmailDetail);
        tvProfileUserId = view.findViewById(R.id.tvProfileUserId);
        tvProfileRoomCount = view.findViewById(R.id.tvProfileRoomCount);
        tvProfileTxCount = view.findViewById(R.id.tvProfileTxCount);
        btnProfileBack = view.findViewById(R.id.btnProfileBack);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void loadProfileData() {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        Profile profile = session.getCurrentUser();

        if (profile != null) {
            String fullName = profile.getFullName() != null && !profile.getFullName().isEmpty()
                    ? profile.getFullName() : "Người dùng";
            String email = profile.getEmail() != null ? profile.getEmail() : "";
            String userId = profile.getId() != null ? profile.getId() : "—";

            // Avatar: lấy chữ cái đầu họ tên
            String initials = getInitials(fullName);
            tvAvatarInitials.setText(initials);
            tvProfileName.setText(fullName);
            tvProfileEmail.setText(email);
            tvProfileFullName.setText(fullName);
            tvProfileEmailDetail.setText(email.isEmpty() ? "—" : email);
            tvProfileUserId.setText(userId);
        }

        // Thống kê số quỹ và giao dịch từ Repository (mock data)
        RoomRepository repo = RoomRepository.getInstance();
        int roomCount = repo.getRoomsLiveData().getValue() != null
                ? repo.getRoomsLiveData().getValue().size() : 0;
        int txCount = repo.getTransactionsLiveData().getValue() != null
                ? repo.getTransactionsLiveData().getValue().size() : 0;

        tvProfileRoomCount.setText(String.valueOf(roomCount));
        tvProfileTxCount.setText(String.valueOf(txCount));
    }

    private void setupClickListeners() {
        btnProfileBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất khỏi tài khoản này?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLogout() {
        UserSessionManager.getInstance(requireContext()).clearSession();
        Toast.makeText(requireContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openLogin();
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split(" ");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return String.valueOf(name.charAt(0)).toUpperCase();
    }
}
