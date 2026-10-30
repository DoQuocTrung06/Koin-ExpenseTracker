package com.sharedfund.app.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sharedfund.app.R;
import com.sharedfund.app.data.ProfileRepository;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Profile;

/**
 * Màn hình Thông tin cá nhân (ProfileFragment)
 * Hiển thị avatar initials, email, họ tên, số quỹ tham gia và số giao dịch.
 * Cho phép đổi ảnh đại diện bằng ảnh tải lên hoặc URL.
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
    private ImageButton btnEditAvatar;
    private ImageView ivProfileAvatar;
    private ProgressBar pbAvatarUpload;
    private ActivityResultLauncher<String> avatarPicker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        avatarPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) uploadAvatar(uri);
        });
    }

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
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileFullName = view.findViewById(R.id.tvProfileFullName);
        tvProfileEmailDetail = view.findViewById(R.id.tvProfileEmailDetail);
        tvProfileUserId = view.findViewById(R.id.tvProfileUserId);
        tvProfileRoomCount = view.findViewById(R.id.tvProfileRoomCount);
        tvProfileTxCount = view.findViewById(R.id.tvProfileTxCount);
        btnProfileBack = view.findViewById(R.id.btnProfileBack);
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
        pbAvatarUpload = view.findViewById(R.id.pbAvatarUpload);
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
            AvatarImageLoader.load(ivProfileAvatar, tvAvatarInitials, profile.getAvatarUrl());
        }

        // Thống kê từ cache dữ liệu đã tải trong phiên hiện tại.
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
        btnEditAvatar.setOnClickListener(v -> showAvatarOptions());
    }

    private void showAvatarOptions() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ảnh đại diện")
                .setItems(new String[]{"Chọn ảnh từ thiết bị", "Dán link ảnh"}, (dialog, which) -> {
                    if (which == 0) avatarPicker.launch("image/*");
                    else showAvatarUrlInput();
                })
                .show();
    }

    private void showAvatarUrlInput() {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://.../avatar.jpg");
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Dán link ảnh")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    Uri parsed = Uri.parse(url);
                    if (("https".equalsIgnoreCase(parsed.getScheme()) || "http".equalsIgnoreCase(parsed.getScheme()))
                            && parsed.getHost() != null) {
                        persistAvatarUrl(url);
                    } else {
                        Toast.makeText(requireContext(), "Link ảnh phải bắt đầu bằng http:// hoặc https://", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void uploadAvatar(Uri imageUri) {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        setAvatarLoading(true);
        new ProfileRepository().uploadAvatar(requireContext().getContentResolver(), imageUri,
                session.getUserId(), session.getAccessToken(), avatarCallback());
    }

    private void persistAvatarUrl(String url) {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        setAvatarLoading(true);
        new ProfileRepository().saveAvatarUrl(session.getUserId(), session.getAccessToken(), url, avatarCallback());
    }

    private ProfileRepository.Callback avatarCallback() {
        return new ProfileRepository.Callback() {
            @Override public void onSuccess(String avatarUrl) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    setAvatarLoading(false);
                    UserSessionManager.getInstance(requireContext()).saveAvatarUrl(avatarUrl);
                    tvAvatarInitials.setVisibility(View.VISIBLE);
                    AvatarImageLoader.load(ivProfileAvatar, tvAvatarInitials, avatarUrl);
                    Toast.makeText(requireContext(), "Đã cập nhật ảnh đại diện.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    setAvatarLoading(false);
                    Toast.makeText(requireContext(), "Không cập nhật được ảnh đại diện: " + message,
                            Toast.LENGTH_LONG).show();
                });
            }
        };
    }

    private void setAvatarLoading(boolean loading) {
        if (btnEditAvatar != null) btnEditAvatar.setEnabled(!loading);
        if (pbAvatarUpload != null) pbAvatarUpload.setVisibility(loading ? View.VISIBLE : View.GONE);
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
