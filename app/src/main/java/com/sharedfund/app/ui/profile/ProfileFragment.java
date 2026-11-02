package com.sharedfund.app.ui.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.ui.widget.AppToaster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
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
    private TextView tvProfileNickname;
    private TextView tvProfileOccupation;
    private TextView tvProfileEducation;
    private TextView tvProfileBio;
    private EditText etProfileNickname;
    private EditText etProfileOccupation;
    private EditText etProfileEducation;
    private EditText etProfileBio;
    private TextView tvProfileRoomCount;
    private TextView tvProfileTxCount;
    private View layoutProfileTopBar;
    private View rowProfileUserId;
    private View cardPersonalDetails;
    private ImageButton btnProfileBack;
    private MaterialButton btnEditProfileInfo;
    private MaterialButton btnCancelEditProfileInfo;
    private ImageView ivProfileCover;
    private ImageButton btnEditCover;
    private ImageButton btnEditAvatar;
    private ImageView ivProfileAvatar;
    private ProgressBar pbAvatarUpload;
    private ActivityResultLauncher<String> avatarPicker;
    private ActivityResultLauncher<String> coverPicker;

    private boolean isEditingProfile = false;

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
        setupWindowInsets(view);
        loadProfileCover();
        loadProfileData();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().getWindow() != null) {
            android.view.Window window = getActivity().getWindow();
            window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.primary));
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                controller.setAppearanceLightStatusBars(false);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null && getActivity().getWindow() != null) {
            android.view.Window window = getActivity().getWindow();
            window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.surface));
            boolean isDark = false;
            if (getActivity() instanceof MainActivity) {
                isDark = ((MainActivity) getActivity()).isDarkTheme();
            }
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                controller.setAppearanceLightStatusBars(!isDark);
            }
        }
    }

    private void setupWindowInsets(View view) {
        if (layoutProfileTopBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                Insets cutouts = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
                int statusBarHeight = Math.max(statusBars.top, cutouts.top);
                if (statusBarHeight <= 0 && getContext() != null) {
                    int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (resId > 0) {
                        statusBarHeight = getResources().getDimensionPixelSize(resId);
                    }
                }
                if (statusBarHeight <= 0) {
                    statusBarHeight = Math.round(24 * getResources().getDisplayMetrics().density);
                }
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutProfileTopBar.getLayoutParams();
                if (lp != null) {
                    lp.topMargin = statusBarHeight + Math.round(4 * getResources().getDisplayMetrics().density);
                    layoutProfileTopBar.setLayoutParams(lp);
                }
                return insets;
            });
            ViewCompat.requestApplyInsets(view);
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
        etProfileNickname = view.findViewById(R.id.etProfileNickname);
        etProfileOccupation = view.findViewById(R.id.etProfileOccupation);
        etProfileEducation = view.findViewById(R.id.etProfileEducation);
        etProfileBio = view.findViewById(R.id.etProfileBio);
        tvProfileRoomCount = view.findViewById(R.id.tvProfileRoomCount);
        tvProfileTxCount = view.findViewById(R.id.tvProfileTxCount);
        btnProfileBack = view.findViewById(R.id.btnProfileBack);
        btnEditProfileInfo = view.findViewById(R.id.btnEditProfileInfo);
        btnCancelEditProfileInfo = view.findViewById(R.id.btnCancelEditProfileInfo);
        cardPersonalDetails = view.findViewById(R.id.cardPersonalDetails);
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
        ivProfileCover = view.findViewById(R.id.ivProfileCover);
        btnEditCover = view.findViewById(R.id.btnEditCover);
        pbAvatarUpload = view.findViewById(R.id.pbAvatarUpload);

        if (btnProfileBack != null) {
            btnProfileBack.setColorFilter(Color.WHITE);
        }
        if (btnEditCover != null) {
            btnEditCover.setColorFilter(Color.WHITE);
        }
        if (btnEditAvatar != null) {
            btnEditAvatar.setColorFilter(Color.WHITE);
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
            btnEditProfileInfo.setOnClickListener(v -> toggleInlineProfileEdit());
        }

        if (btnCancelEditProfileInfo != null) {
            btnCancelEditProfileInfo.setOnClickListener(v -> cancelInlineProfileEdit());
        }

        btnEditAvatar.setOnClickListener(v -> showAvatarOptions());
        if (btnEditCover != null) {
            btnEditCover.setOnClickListener(v -> showCoverOptions());
        }
        if (rowProfileUserId != null) {
            rowProfileUserId.setOnClickListener(v -> copyUserIdToClipboard());
        }
    }

    private void toggleInlineProfileEdit() {
        if (!isEditingProfile) {
            startInlineProfileEdit();
        } else {
            saveInlineProfileEdit();
        }
    }

    private void startInlineProfileEdit() {
        isEditingProfile = true;
        UserSessionManager session = UserSessionManager.getInstance(requireContext());

        String nick = session.getNickname();
        String occ = session.getOccupation();
        String edu = session.getEducation();
        String bio = session.getBio();

        if (etProfileNickname != null) etProfileNickname.setText(nick != null ? nick : "");
        if (etProfileOccupation != null) etProfileOccupation.setText(occ != null ? occ : "");
        if (etProfileEducation != null) etProfileEducation.setText(edu != null ? edu : "");
        if (etProfileBio != null) etProfileBio.setText(bio != null ? bio : "");

        setFieldVisibility(true);

        if (btnEditProfileInfo != null) {
            btnEditProfileInfo.setText("Lưu thay đổi");
            btnEditProfileInfo.setIconResource(R.drawable.ic_check);
        }
        if (btnCancelEditProfileInfo != null) {
            btnCancelEditProfileInfo.setVisibility(View.VISIBLE);
        }

        if (etProfileNickname != null) {
            etProfileNickname.requestFocus();
            etProfileNickname.setSelection(etProfileNickname.getText().length());
            showKeyboard(etProfileNickname);
        }
    }

    private void saveInlineProfileEdit() {
        UserSessionManager session = UserSessionManager.getInstance(requireContext());

        String nickname = etProfileNickname != null ? etProfileNickname.getText().toString().trim() : "";
        String occupation = etProfileOccupation != null ? etProfileOccupation.getText().toString().trim() : "";
        String education = etProfileEducation != null ? etProfileEducation.getText().toString().trim() : "";
        String bio = etProfileBio != null ? etProfileBio.getText().toString().trim() : "";

        session.updateCustomProfile(nickname, occupation, education, bio);

        isEditingProfile = false;
        setFieldVisibility(false);

        if (btnEditProfileInfo != null) {
            btnEditProfileInfo.setText("Chỉnh sửa");
            btnEditProfileInfo.setIconResource(R.drawable.ic_edit);
        }
        if (btnCancelEditProfileInfo != null) {
            btnCancelEditProfileInfo.setVisibility(View.GONE);
        }

        hideKeyboard();
        loadProfileData();
        AppToaster.success(requireActivity(), "Đã lưu thay đổi thông tin cá nhân!");
    }

    private void cancelInlineProfileEdit() {
        isEditingProfile = false;
        setFieldVisibility(false);

        if (btnEditProfileInfo != null) {
            btnEditProfileInfo.setText("Chỉnh sửa");
            btnEditProfileInfo.setIconResource(R.drawable.ic_edit);
        }
        if (btnCancelEditProfileInfo != null) {
            btnCancelEditProfileInfo.setVisibility(View.GONE);
        }

        hideKeyboard();
        loadProfileData();
    }

    private void setFieldVisibility(boolean isEditing) {
        if (tvProfileNickname != null) tvProfileNickname.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        if (etProfileNickname != null) etProfileNickname.setVisibility(isEditing ? View.VISIBLE : View.GONE);

        if (tvProfileOccupation != null) tvProfileOccupation.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        if (etProfileOccupation != null) etProfileOccupation.setVisibility(isEditing ? View.VISIBLE : View.GONE);

        if (tvProfileEducation != null) tvProfileEducation.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        if (etProfileEducation != null) etProfileEducation.setVisibility(isEditing ? View.VISIBLE : View.GONE);

        if (tvProfileBio != null) tvProfileBio.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        if (etProfileBio != null) etProfileBio.setVisibility(isEditing ? View.VISIBLE : View.GONE);
    }

    private void showKeyboard(View view) {
        if (view != null && getContext() != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            }
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

    private void showCoverOptions() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đổi ảnh bìa")
                .setItems(new String[]{"Chọn ảnh từ thiết bị", "Dán link ảnh"}, (dialog, which) -> {
                    if (which == 0) {
                        coverPicker.launch("image/*");
                    } else {
                        showCoverUrlDialog();
                    }
                })
                .show();
    }

    private void showCoverUrlDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("https://.../cover.jpg");
        input.setSingleLine(true);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Dán link ảnh bìa")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        saveProfileCoverUrl(url);
                    }
                })
                .show();
    }

    private void saveProfileCoverUrl(String url) {
        if (getContext() == null) return;
        requireContext().getSharedPreferences("koin_profile_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("profile_cover_url", url)
                .apply();
        loadProfileCover();
        AppToaster.success(requireActivity(), "Đã cập nhật ảnh bìa thành công!");
    }

    private void loadProfileCover() {
        if (getContext() == null || ivProfileCover == null) return;
        String url = requireContext().getSharedPreferences("koin_profile_prefs", Context.MODE_PRIVATE)
                .getString("profile_cover_url", null);
        if (url != null && !url.trim().isEmpty()) {
            ivProfileCover.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    android.graphics.Bitmap bmp = null;
                    if (url.startsWith("content://") || url.startsWith("file://")) {
                        android.content.ContentResolver cr = requireContext().getContentResolver();
                        java.io.InputStream is = cr.openInputStream(Uri.parse(url));
                        bmp = android.graphics.BitmapFactory.decodeStream(is);
                        if (is != null) is.close();
                    } else if (url.startsWith("http://") || url.startsWith("https://")) {
                        java.io.InputStream in = new java.net.URL(url).openStream();
                        bmp = android.graphics.BitmapFactory.decodeStream(in);
                        if (in != null) in.close();
                    }
                    if (bmp != null) {
                        final android.graphics.Bitmap finalBmp = bmp;
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> ivProfileCover.setImageBitmap(finalBmp));
                        }
                    }
                } catch (Exception ignored) {
                }
            }).start();
        } else {
            ivProfileCover.setVisibility(View.GONE);
        }
    }

    private void showAvatarOptions() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đổi ảnh đại diện")
                .setItems(new String[]{"Chọn ảnh từ thiết bị", "Dán link ảnh"}, (dialog, which) -> {
                    if (which == 0) {
                        avatarPicker.launch("image/*");
                    } else {
                        showAvatarUrlDialog();
                    }
                })
                .show();
    }

    private void showAvatarUrlDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("https://.../avatar.jpg");
        input.setSingleLine(true);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Dán link ảnh đại diện")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        persistAvatarUrl(url);
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
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
