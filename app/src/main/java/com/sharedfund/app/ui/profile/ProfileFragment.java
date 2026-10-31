package com.sharedfund.app.ui.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import com.sharedfund.app.ui.widget.AppToaster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
    private TextView tvProfileNickname;
    private TextView tvProfileOccupation;
    private TextView tvProfileEducation;
    private TextView tvProfileBio;
    private TextView tvProfileRoomCount;
    private TextView tvProfileTxCount;
    private View layoutProfileTopBar;
    private View rowProfileUserId;
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
        setupWindowInsets();
        loadProfileCover();
        loadProfileData();
        setupClickListeners();
    }

    private void setupWindowInsets() {
        if (layoutProfileTopBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(layoutProfileTopBar, (v, insets) -> {
                Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                int extraTop = Math.round(16 * getResources().getDisplayMetrics().density);
                v.setPadding(v.getPaddingLeft(), statusBars.top + extraTop, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(layoutProfileTopBar);
        }
    }

    private void bindViews(View view) {
        layoutProfileTopBar = view.findViewById(R.id.layoutProfileTopBar);
        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials);
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileFullName = view.findViewById(R.id.tvProfileFullName);
        tvProfileEmailDetail = view.findViewById(R.id.tvProfileEmailDetail);
        tvProfileUserId = view.findViewById(R.id.tvProfileUserId);
        rowProfileUserId = view.findViewById(R.id.rowProfileUserId);
        tvProfileNickname = view.findViewById(R.id.tvProfileNickname);
        tvProfileOccupation = view.findViewById(R.id.tvProfileOccupation);
        tvProfileEducation = view.findViewById(R.id.tvProfileEducation);
        tvProfileBio = view.findViewById(R.id.tvProfileBio);
        tvProfileRoomCount = view.findViewById(R.id.tvProfileRoomCount);
        tvProfileTxCount = view.findViewById(R.id.tvProfileTxCount);
        btnProfileBack = view.findViewById(R.id.btnProfileBack);
        btnEditProfileInfo = view.findViewById(R.id.btnEditProfileInfo);
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
        ivProfileCover = view.findViewById(R.id.ivProfileCover);
        btnEditCover = view.findViewById(R.id.btnEditCover);
        pbAvatarUpload = view.findViewById(R.id.pbAvatarUpload);

        if (btnEditCover != null) {
            btnEditCover.setColorFilter(Color.WHITE);
        }
    }

    private void loadProfileData() {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        Profile profile = session.getCurrentUser();

        if (profile != null) {
            String fullName = profile.getFullName() != null && !profile.getFullName().isEmpty()
                    ? profile.getFullName() : "Người dùng";
            String email = profile.getEmail() != null ? profile.getEmail() : "";
            String userId = profile.getId() != null ? profile.getId() : "—";
            String nickname = session.getNickname();
            String occupation = session.getOccupation();
            String education = session.getEducation();
            String bio = session.getBio();

            String initials = getInitials(fullName);
            tvAvatarInitials.setText(initials);
            if (nickname != null && !nickname.trim().isEmpty()) {
                tvProfileName.setText(nickname.trim());
            } else {
                tvProfileName.setText(fullName);
            }
            tvProfileEmail.setText(email);
            tvProfileFullName.setText(fullName);
            tvProfileEmailDetail.setText(email.isEmpty() ? "—" : email);
            tvProfileUserId.setText(userId);

            if (tvProfileNickname != null) {
                tvProfileNickname.setText(nickname != null && !nickname.trim().isEmpty() ? nickname.trim() : "Chưa thiết lập");
            }
            if (tvProfileOccupation != null) {
                tvProfileOccupation.setText(occupation != null && !occupation.trim().isEmpty() ? occupation.trim() : "Chưa thiết lập");
            }
            if (tvProfileEducation != null) {
                tvProfileEducation.setText(education != null && !education.trim().isEmpty() ? education.trim() : "Chưa thiết lập");
            }
            if (tvProfileBio != null) {
                tvProfileBio.setText(bio != null && !bio.trim().isEmpty() ? bio.trim() : "Chưa thiết lập");
            }

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
        if (btnEditCover != null) {
            btnEditCover.setOnClickListener(v -> showCoverOptions());
        }
        if (rowProfileUserId != null) {
            rowProfileUserId.setOnClickListener(v -> copyUserIdToClipboard());
        }
    }

    private void copyUserIdToClipboard() {
        if (tvProfileUserId != null && getContext() != null) {
            String uid = tvProfileUserId.getText().toString();
            if (!uid.isEmpty() && !"—".equals(uid)) {
                ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("User ID", uid));
                    AppToaster.info(requireActivity(), "Đã sao chép ID người dùng vào bộ nhớ tạm");
                }
            }
        }
    }

    private void showEditProfileDialog() {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        String currentNickname = session.getNickname();
        String currentOccupation = session.getOccupation();
        String currentEducation = session.getEducation();
        String currentBio = session.getBio();

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, pad / 2);
        scrollView.addView(layout);

        TextInputLayout tilNickname = new TextInputLayout(
                requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        tilNickname.setHint("Biệt danh (Tên thường gọi)");
        TextInputEditText etNickname = new TextInputEditText(tilNickname.getContext());
        etNickname.setText(currentNickname);
        etNickname.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        tilNickname.addView(etNickname);
        layout.addView(tilNickname);

        TextInputLayout tilOccupation = new TextInputLayout(
                requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        tilOccupation.setHint("Nghề nghiệp");
        LinearLayout.LayoutParams lpOccupation = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpOccupation.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
        tilOccupation.setLayoutParams(lpOccupation);
        TextInputEditText etOccupation = new TextInputEditText(tilOccupation.getContext());
        etOccupation.setText(currentOccupation);
        etOccupation.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        tilOccupation.addView(etOccupation);
        layout.addView(tilOccupation);

        TextInputLayout tilEducation = new TextInputLayout(
                requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        tilEducation.setHint("Học vấn");
        LinearLayout.LayoutParams lpEducation = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpEducation.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
        tilEducation.setLayoutParams(lpEducation);
        TextInputEditText etEducation = new TextInputEditText(tilEducation.getContext());
        etEducation.setText(currentEducation);
        etEducation.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        tilEducation.addView(etEducation);
        layout.addView(tilEducation);

        TextInputLayout tilBio = new TextInputLayout(
                requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        tilBio.setHint("Tiểu sử / Lời giới thiệu");
        LinearLayout.LayoutParams lpBio = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBio.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
        tilBio.setLayoutParams(lpBio);
        TextInputEditText etBio = new TextInputEditText(tilBio.getContext());
        etBio.setText(currentBio);
        etBio.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etBio.setMaxLines(3);
        tilBio.addView(etBio);
        layout.addView(tilBio);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chỉnh sửa thông tin cá nhân")
                .setView(scrollView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu thay đổi", (dialog, which) -> {
                    String nickname = etNickname.getText() != null ? etNickname.getText().toString().trim() : "";
                    String occupation = etOccupation.getText() != null ? etOccupation.getText().toString().trim() : "";
                    String education = etEducation.getText() != null ? etEducation.getText().toString().trim() : "";
                    String bio = etBio.getText() != null ? etBio.getText().toString().trim() : "";

                    session.updateCustomProfile(nickname, occupation, education, bio);
                    loadProfileData();
                    AppToaster.success(requireActivity(), "Đã cập nhật thông tin cá nhân thành công!");
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
        requireContext().getSharedPreferences("shared_fund_profile_bg", Context.MODE_PRIVATE)
                .edit()
                .putString("profile_cover_url", url)
                .apply();
        loadProfileCover();
        AppToaster.success(requireActivity(), "Đã cập nhật ảnh bìa cá nhân.");
    }

    private void loadProfileCover() {
        if (ivProfileCover == null || getContext() == null) return;
        String url = requireContext().getSharedPreferences("shared_fund_profile_bg", Context.MODE_PRIVATE)
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