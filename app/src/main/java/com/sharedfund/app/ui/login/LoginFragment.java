package com.sharedfund.app.ui.login;

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
import com.sharedfund.app.data.UserSessionManager;

/**
 * Màn hình Đăng nhập / Đăng ký (LoginFragment)
 * Hỗ trợ toggle giữa 2 chế độ: Login và Register với validation đầy đủ.
 * Tích hợp UserSessionManager để lưu phiên đăng nhập.
 * Giai đoạn sau: thay mock bằng Supabase Auth API thật (signIn / signUp).
 */
public class LoginFragment extends Fragment {

    private boolean isLoginMode = true;

    // Tab buttons
    private MaterialButton btnTabLogin;
    private MaterialButton btnTabRegister;

    // Title
    private TextView tvAuthTitle;
    private TextView tvAuthSubtitle;

    // Form fields
    private TextInputLayout tilFullName;
    private TextInputEditText etFullName;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etConfirmPassword;

    // Actions
    private ProgressBar pbAuthLoading;
    private MaterialButton btnAuthAction;
    private MaterialButton btnSkipLogin;

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
    }

    private void bindViews(View view) {
        btnTabLogin = view.findViewById(R.id.btnTabLogin);
        btnTabRegister = view.findViewById(R.id.btnTabRegister);
        tvAuthTitle = view.findViewById(R.id.tvAuthTitle);
        tvAuthSubtitle = view.findViewById(R.id.tvAuthSubtitle);
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
        btnSkipLogin = view.findViewById(R.id.btnSkipLogin);
    }

    // ==========================================
    // TAB TOGGLE LOGIC
    // ==========================================

    private void setupTabToggle() {
        btnTabLogin.setOnClickListener(v -> switchToMode(true));
        btnTabRegister.setOnClickListener(v -> switchToMode(false));
    }

    private void switchToMode(boolean loginMode) {
        isLoginMode = loginMode;
        clearAllErrors();

        if (loginMode) {
            // --- Login mode ---
            btnTabLogin.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.primary)));
            btnTabLogin.setTextColor(requireContext().getColor(R.color.white));
            btnTabRegister.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.TRANSPARENT));
            btnTabRegister.setTextColor(requireContext().getColor(R.color.text_secondary));

            tvAuthTitle.setText("Đăng nhập tài khoản");
            tvAuthSubtitle.setText("Quản lý chi tiêu quỹ chung cùng hội bạn");
            btnAuthAction.setText("Đăng nhập");

            tilFullName.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
        } else {
            // --- Register mode ---
            btnTabRegister.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.primary)));
            btnTabRegister.setTextColor(requireContext().getColor(R.color.white));
            btnTabLogin.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.TRANSPARENT));
            btnTabLogin.setTextColor(requireContext().getColor(R.color.text_secondary));

            tvAuthTitle.setText("Tạo tài khoản mới");
            tvAuthSubtitle.setText("Đăng ký để bắt đầu quản lý quỹ nhóm");
            btnAuthAction.setText("Đăng ký");

            tilFullName.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
        }
    }

    // ==========================================
    // TEXT WATCHERS - Xóa lỗi khi người dùng gõ
    // ==========================================

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

    // ==========================================
    // CLICK LISTENERS
    // ==========================================

    private void setupClickListeners() {
        btnAuthAction.setOnClickListener(v -> {
            if (isLoginMode) {
                validateAndLogin();
            } else {
                validateAndRegister();
            }
        });

        btnSkipLogin.setOnClickListener(v -> {
            // Đăng nhập khách - lưu phiên tạm
            UserSessionManager.getInstance(requireContext())
                    .saveSession("guest-001", "guest@sharedfund.vn", "Khách", "");
            goToHome();
        });
    }

    // ==========================================
    // VALIDATE & LOGIN
    // ==========================================

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

        // Hiển thị loading
        setLoadingState(true);

        // TODO: Giai đoạn sau - gọi Supabase Auth API signIn(email, pass)
        // Hiện tại: Mock login thành công
        btnAuthAction.postDelayed(() -> {
            if (!isAdded()) return;
            setLoadingState(false);
            UserSessionManager.getInstance(requireContext())
                    .saveSession("user-001", email, email.split("@")[0], "");
            Toast.makeText(requireContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            goToHome();
        }, 800);
    }

    // ==========================================
    // VALIDATE & REGISTER
    // ==========================================

    private void validateAndRegister() {
        clearAllErrors();
        String fullName = getText(etFullName);
        String email = getText(etEmail);
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

        // Hiển thị loading
        setLoadingState(true);

        // TODO: Giai đoạn sau - gọi Supabase Auth API signUp(email, pass, fullName)
        // Hiện tại: Mock register thành công
        btnAuthAction.postDelayed(() -> {
            if (!isAdded()) return;
            setLoadingState(false);
            UserSessionManager.getInstance(requireContext())
                    .saveSession("user-" + System.currentTimeMillis(), email, fullName, "");
            Toast.makeText(requireContext(), "Đăng ký thành công! Chào mừng " + fullName + "!", Toast.LENGTH_SHORT).show();
            goToHome();
        }, 1000);
    }

    // ==========================================
    // HELPER
    // ==========================================

    private void setLoadingState(boolean loading) {
        pbAuthLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnAuthAction.setEnabled(!loading);
        btnSkipLogin.setEnabled(!loading);
        btnTabLogin.setEnabled(!loading);
        btnTabRegister.setEnabled(!loading);
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
