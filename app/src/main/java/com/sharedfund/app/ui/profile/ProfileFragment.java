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
import com.sharedfund.app.ui.widget.AppToaster;

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
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

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
    private View btnEditProfileInfo;
    private ImageView ivProfileCover;
    private ImageButton btnEditCover;
    private ImageButton btnEditAvatar;
    private ImageView ivProfileAvatar;
    private ProgressBar pbAvatarUpload;
    private ActivityResultLauncher<String> avatarPicker;
    private ActivityResultLauncher<String> coverPicker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        avatarPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) uploadAvatar(uri);
        });
        coverPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) saveProfileCoverUrl(uri.toString());
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
        btnEditProfileInfo = view.findViewById(R.id.btnEditProfileInfo);
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
        ivProfileCover = view.findViewById(R.id.ivProfileCover);
        btnEditCover = view.findViewById(R.id.btnEditCover);
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

            String initials = getInitials(fullName);
            tvAvatarInitials.setText(initials);
            tvProfileName.setText(fullName);
            tvProfileEmail.setText(email);
            tvProfileFullName.setText(fullName);
            tvProfileEmailDetail.setText(email.isEmpty() ? "—" : email);
            tvProfileUserId.setText(userId);
            AvatarImageLoader.load(ivProfileAvatar, tvAvatarInitials, profile.getAvatarUrl());
        }

        RoomRepository repo = RoomRepository.getInstance();
        int roomCount = repo.getRoomsLiveData().getValue() != null
                ? repo.getRoomsLiveData().getValue().size() : 0;

        double totalContributed = 0.0;
        List<Transaction> txList = repo.getTransactionsLiveData().getValue();
        if (txList != null) {
            String currentUserId = session.getUserId();
            String currentUserName = profile != null ? profile.getFullName() : null;
            for (Transaction tx : txList) {
                if (Transaction.TYPE_INCOME.equals(tx.getType())) {
                    String author = tx.getCreatedBy();
                    if (author == null || "Bạn".equalsIgnoreCase(author)
                            || (currentUserName != null && currentUserName.equalsIgnoreCase(author))
                            || (currentUserId != null && currentUserId.equals(author))) {
                        totalContributed += tx.getAmount();
                    }
                }
            }
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);

        tvProfileRoomCount.setText(String.valueOf(roomCount));
        tvProfileTxCount.setText(df.format(totalContributed) + " ₫");
    }

    private void setupClickListeners() {
        btnProfileBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        if (btnEditProfileInfo != null) {
            btnEditProfileInfo.setOnClickListener(v -> showEditProfileDialog());
        }
        btnEditAvatar.setOnClickListener(v -> showAvatarOptions());
        if (btnEditCover != null) btnEditCover.setOnClickListener(v -> showCoverOptions());
    }

    private void showEditProfileDialog() {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        Profile profile = session.getCurrentUser();
        String currentName = profile != null ? profile.getFullName() : "";
        String currentEmail = profile != null ? profile.getEmail() : "";

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, pad / 2);

        com.google.android.material.textfield.TextInputLayout tilName = new com.google.android.material.textfield.TextInputLayout(
                requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        tilName.setHint("Họ và tên");
        com.google.android.material.textfield.TextInputEditText etName = new com.google.android.material.textfield.TextInputEditText(tilName.getContext());
        etName.setText(currentName);
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        tilName.addView(etName);
        layout.addView(tilName);

        com.google.android.material.textfield.TextInputLayout tilEmail = new com.google.android.material.textfield.TextInputLayout(
                requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        tilEmail.setHint("Email / Số điện thoại");
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
        tilEmail.setLayoutParams(lp);
        com.google.android.material.textfield.TextInputEditText etEmail = new com.google.android.material.textfield.TextInputEditText(tilEmail.getContext());
        etEmail.setText(currentEmail);
        etEmail.setInputType(InputType.TYPE_CLASS_TEXT);
        tilEmail.addView(etEmail);
        layout.addView(tilEmail);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chỉnh sửa thông tin cá nhân")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu thay đổi", (dialog, which) -> {
                    String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String newEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                    if (newName.isEmpty()) {
                        AppToaster.error(requireActivity(), "Họ và tên không được để trống!");
                        return;
                    }
                    session.updateProfile(newName, newEmail);
                    RoomRepository.getInstance().setCurrentUser(session.getUserId(), newName);
                    loadProfileData();
                    AppToaster.success(requireActivity(), "Đã cập nhật thông tin cá nhân thành công!");
                    String token = session.getAccessToken();
                    if (token != null && !token.trim().isEmpty()) {
                        com.sharedfund.app.data.AuthRepository authRepo = new com.sharedfund.app.data.AuthRepository(requireContext());
                        Profile updated = new Profile(session.getUserId(), newEmail, newName, session.getAvatarUrl());
                        authRepo.updateProfile(updated, token, null);
                    }
                })
                .show();
    }

    private void showCoverOptions() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ảnh bìa cá nhân")
                .setItems(new String[]{"Chọn ảnh từ thiết bị", "Dán link ảnh"}, (dialog, which) -> {
                    if (which == 0) coverPicker.launch("image/*");
                    else showCoverUrlInput();
                })
                .show();
    }

    private void showCoverUrlInput() {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://.../cover.jpg");
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Dán link ảnh bìa")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!url.isEmpty()) {
                        saveProfileCoverUrl(url);
                    }
                })
                .show();
    }

    private void saveProfileCoverUrl(String url) {
        requireContext().getSharedPreferences("shared_fund_profile_bg", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("profile_cover_url", url)
                .apply();
        loadProfileCover();
        AppToaster.success(requireActivity(), "Đã cập nhật ảnh bìa cá nhân.");
    }

    private void loadProfileCover() {
        if (ivProfileCover == null || getContext() == null) return;
        String url = requireContext().getSharedPreferences("shared_fund_profile_bg", android.content.Context.MODE_PRIVATE)
                .getString("profile_cover_url", "");
        if (url != null && !url.trim().isEmpty()) {
            ivProfileCover.setVisibility(View.VISIBLE);
            AvatarImageLoader.load(ivProfileCover, null, url);
        } else {
            ivProfileCover.setVisibility(View.GONE);
        }
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
                        AppToaster.error(requireActivity(), "Link ảnh phải bắt đầu bằng http:// hoặc https://");
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
                    AppToaster.success(requireActivity(), "Đã cập nhật ảnh đại diện.");
                });
            }

            @Override public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    setAvatarLoading(false);
                    AppToaster.error(requireActivity(), "Không cập nhật được ảnh đại diện: " + message);
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