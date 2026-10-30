package com.sharedfund.app.ui.login;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.data.AuthRepository;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Profile;

public class LoginFragment extends Fragment {

    private boolean isLoginMode = true;
    private AuthRepository authRepository;

    private TextView tvAuthTitle;
    private TextView tvAuthSubtitle;
    private TextView tvSwitchPrompt;
    private TextView tvSwitchAction;

    private TextInputLayout tilFullName;
    private TextInputEditText etFullName;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etConfirmPassword;

    private ProgressBar pbAuthLoading;
    private MaterialButton btnAuthAction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupTabToggle();
        setupTextWatchers();
        setupClickListeners();
        authRepository = new AuthRepository(requireContext());
    }

    private void bindViews(View view) {
        tvAuthTitle = view.findViewById(R.id.tvAuthTitle);
        tvAuthSubtitle = view.findViewById(R.id.tvAuthSubtitle);
        tvSwitchPrompt = view.findViewById(R.id.tvSwitchPrompt);
        tvSwitchAction = view.findViewById(R.id.tvSwitchAction);
        tilFullName = view.findViewById(R.id.tilFullName);
        etFullName = view.findViewById(R.id.etFullName);
        tilEmail = view.findViewById(R.id.tilEmail);
        etEmail = view.findViewById(R.id.etEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        etPassword = view.findViewById(R.id.etPassword);
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        pbAuthLoading = view.findViewById(R.id.pbAuthLoading);
        btnAuthAction = view.findViewById(R.id.btnAuthAction);
    }

    private void setupTabToggle() {
        tvSwitchAction.setOnClickListener(v -> switchToMode(!isLoginMode));
        switchToMode(true);
    }

    private void switchToMode(boolean loginMode) {
        isLoginMode = loginMode;
        clearAllErrors();

        if (loginMode) {
            tvAuthTitle.setText("Chào mừng trở lại");
            tvAuthSubtitle.setText("Đăng nhập để tiếp tục quản lý quỹ cùng bạn bè");
            btnAuthAction.setText("Đăng nhập");
            tvSwitchPrompt.setText("Chưa có tài khoản? ");
            tvSwitchAction.setText("Đăng ký miễn phí");

            tilFullName.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
        } else {
            tvAuthTitle.setText("Tạo tài khoản mới");
            tvAuthSubtitle.setText("Tham gia cùng bạn bè để quản lý quỹ chung");
            btnAuthAction.setText("Đăng ký");
            tvSwitchPrompt.setText("Đã có tài khoản? ");
            tvSwitchAction.setText("Đăng nhập");

            tilFullName.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
        }
    }

    private void setupTextWatchers() {
        attachClearError(etFullName, tilFullName);
        attachClearError(etEmail, tilEmail);
        attachClearError(etPassword, tilPassword);
        attachClearError(etConfirmPassword, tilConfirmPassword);
    }

    private void attachClearError(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) layout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        btnAuthAction.setOnClickListener(v -> {
            if (isLoginMode) {
                validateAndLogin();
            } else {
                validateAndRegister();
            }
        });
    }

    private void validateAndLogin() {
        clearAllErrors();
        String email = getText(etEmail);
        String pass = getText(etPassword);
        boolean hasError = false;

        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Định dạng email không hợp lệ");
            hasError = true;
        }

        if (pass.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            hasError = true;
        } else if (pass.length() < 6) {
            tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            hasError = true;
        }

        if (hasError) return;

        setLoadingState(true);
        authRepository.signIn(email, pass, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(Profile profile) {
                runOnUiThreadIfAttached(() -> {
                    setLoadingState(false);
                    if (profile == null || profile.getId() == null || profile.getId().trim().isEmpty()) {
                        showAuthError("Đăng nhập thành công nhưng không nhận được ID người dùng.");
                        return;
                    }
                    RoomRepository.getInstance().setCurrentUser(profile.getId(), profile.getFullName());
                    Toast.makeText(requireContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    goToHome();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThreadIfAttached(() -> {
                    setLoadingState(false);
                    showAuthError("Đăng nhập thất bại: " + errorMessage);
                });
            }
        });
    }

    private void validateAndRegister() {
        clearAllErrors();
        String fullName = getText(etFullName);
        String email = getText(emailEditText());
        String pass = getText(etPassword);
        String confirmPass = getText(etConfirmPassword);
        boolean hasError = false;

        if (fullName.isEmpty()) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            hasError = true;
        } else if (fullName.length() < 2) {
            tilFullName.setError("Họ tên phải có ít nhất 2 ký tự");
            hasError = true;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Định dạng email không hợp lệ");
            hasError = true;
        }

        if (pass.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            hasError = true;
        } else if (pass.length() < 6) {
            tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            hasError = true;
        }

        if (confirmPass.isEmpty()) {
            tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            hasError = true;
        } else if (!confirmPass.equals(pass)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            hasError = true;
        }

        if (hasError) return;

        setLoadingState(true);
        UserSessionManager.getInstance(requireContext()).clearSession();
        authRepository.signUp(email, pass, fullName, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(Profile profile) {
                runOnUiThreadIfAttached(() -> {
                    setLoadingState(false);
                    if (profile == null || profile.getId() == null || profile.getId().trim().isEmpty()) {
                        showAuthError("Supabase chưa trả về ID tài khoản. Hãy kiểm tra lại Auth Logs.");
                        return;
                    }

                    UserSessionManager sessions = UserSessionManager.getInstance(requireContext());
                    if (sessions.getAccessToken() == null || sessions.getAccessToken().isEmpty()) {
                        sessions.clearSession();
                        Toast.makeText(requireContext(),
                                "Tài khoản đã được tạo. Hãy xác nhận email rồi đăng nhập.",
                                Toast.LENGTH_LONG).show();
                        switchToMode(true);
                        return;
                    }

                    RoomRepository.getInstance().registerNewUser(profile.getId(), profile.getFullName());
                    Toast.makeText(requireContext(), "Đăng ký thành công! Chào mừng "
                            + profile.getFullName() + "!", Toast.LENGTH_SHORT).show();
                    goToHome();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThreadIfAttached(() -> {
                    setLoadingState(false);
                    showAuthError("Đăng ký thất bại: " + errorMessage);
                });
            }
        });
    }

    private TextInputEditText emailEditText() {
        return etEmail;
    }

    private void runOnUiThreadIfAttached(Runnable action) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isAdded()) action.run();
            });
        }
    }

    private void showAuthError(String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void setLoadingState(boolean loading) {
        pbAuthLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnAuthAction.setEnabled(!loading);
        tvSwitchAction.setEnabled(!loading);
    }

    private void clearAllErrors() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void goToHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openHome();
        }
    }
}