package com.sharedfund.app.ui.login;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
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
import com.sharedfund.app.ui.widget.AppToaster;
import com.sharedfund.app.util.ErrorMessageHelper;
import com.sharedfund.app.util.PasswordStrengthEvaluator;

public class LoginFragment extends Fragment {

    private boolean isLoginMode = true;
    private AuthRepository authRepository;
    private NestedScrollView scrollLogin;
    private LinearLayout layoutAuthContainer;
    private View spacerTop;
    private View cardAuth;
    private View layoutSwitchPrompt;
    private ValueAnimator spacerAnimator;
    private int lastCalculatedSpacer = -1;
    private boolean isKeyboardOpen = false;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;

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
    private View layoutPasswordStrength;
    private TextView tvStrengthLabel;
    private ProgressBar pbPasswordStrength;
    private TextView tvRuleLength;
    private TextView tvRuleCase;
    private TextView tvRuleDigit;
    private TextView tvRuleSpecial;
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
        setupKeyboardHandling(view);
        setupTabToggle();
        setupTextWatchers();
        setupClickListeners();
        authRepository = new AuthRepository(requireContext());
    }

    private void bindViews(View view) {
        scrollLogin = view.findViewById(R.id.scrollLogin);
        layoutAuthContainer = view.findViewById(R.id.layoutAuthContainer);
        spacerTop = view.findViewById(R.id.spacerTop);
        cardAuth = view.findViewById(R.id.cardAuth);
        layoutSwitchPrompt = view.findViewById(R.id.layoutSwitchPrompt);
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
        layoutPasswordStrength = view.findViewById(R.id.layoutPasswordStrength);
        tvStrengthLabel = view.findViewById(R.id.tvStrengthLabel);
        pbPasswordStrength = view.findViewById(R.id.pbPasswordStrength);
        tvRuleLength = view.findViewById(R.id.tvRuleLength);
        tvRuleCase = view.findViewById(R.id.tvRuleCase);
        tvRuleDigit = view.findViewById(R.id.tvRuleDigit);
        tvRuleSpecial = view.findViewById(R.id.tvRuleSpecial);
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
        if (layoutPasswordStrength != null) {
            layoutPasswordStrength.setVisibility(View.GONE);
        }
        if (etFullName != null) etFullName.setText("");
        if (etEmail != null) etEmail.setText("");
        if (etPassword != null) etPassword.setText("");
        if (etConfirmPassword != null) etConfirmPassword.setText("");

        if (getActivity() != null) {
            View focus = getActivity().getCurrentFocus();
            if (focus != null) {
                focus.clearFocus();
            }
        }

        if (loginMode) {
            tvAuthTitle.setText("Chào mừng trở lại");
            tvAuthSubtitle.setText("Đăng nhập để tiếp tục quản lý quỹ cùng bạn bè");
            btnAuthAction.setText("Đăng nhập");
            tvSwitchPrompt.setText("Chưa có tài khoản? ");
            tvSwitchAction.setText("Đăng ký miễn phí");

            tilFullName.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);

            String lastEmail = UserSessionManager.getInstance(requireContext()).getCurrentUserEmail();
            if (lastEmail != null && !lastEmail.trim().isEmpty() && etEmail != null) {
                etEmail.setText(lastEmail.trim());
                etEmail.setSelection(lastEmail.trim().length());
            }
        } else {
            tvAuthTitle.setText("Tạo tài khoản mới");
            tvAuthSubtitle.setText("Tham gia cùng bạn bè để quản lý quỹ chung");
            btnAuthAction.setText("Đăng ký");
            tvSwitchPrompt.setText("Đã có tài khoản? ");
            tvSwitchAction.setText("Đăng nhập");

            tilFullName.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
        }

        if (scrollLogin != null) {
            scrollLogin.scrollTo(0, 0);
        }
        if (getView() != null) {
            getView().post(() -> {
                if (keyboardLayoutListener != null) {
                    keyboardLayoutListener.onGlobalLayout();
                }
            });
        }
    }

    private void setupTextWatchers() {
        attachClearError(etFullName, tilFullName);
        attachClearError(etEmail, tilEmail);
        attachClearError(etConfirmPassword, tilConfirmPassword);

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilPassword.getError() != null) {
                    tilPassword.setError(null);
                }
                if (!isLoginMode) {
                    updatePasswordStrength(s != null ? s.toString() : "");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etFullName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showKeyboard(etFullName);
                if (scrollLogin != null) {
                    scrollLogin.postDelayed(() -> {
                        if (isAdded() && scrollLogin != null) {
                            scrollLogin.smoothScrollTo(0, 0);
                        }
                    }, 160);
                }
            }
        });

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showKeyboard(etEmail);
                if (scrollLogin != null) {
                    scrollLogin.postDelayed(() -> {
                        if (isAdded() && scrollLogin != null) {
                            scrollLogin.smoothScrollTo(0, 0);
                        }
                    }, 160);
                }
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showKeyboard(etPassword);
                if (!isLoginMode) {
                    layoutPasswordStrength.setVisibility(View.VISIBLE);
                    updatePasswordStrength(getRawPassword(etPassword));
                }
                if (scrollLogin != null) {
                    scrollLogin.postDelayed(() -> {
                        if (isAdded() && scrollLogin != null && tilPassword != null) {
                            scrollLogin.smoothScrollTo(0, tilPassword.getTop());
                        }
                    }, 160);
                }
            } else {
                if (!isLoginMode && etConfirmPassword != null && !etConfirmPassword.hasFocus()) {
                    layoutPasswordStrength.setVisibility(View.GONE);
                }
            }
        });

        etConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showKeyboard(etConfirmPassword);
                if (scrollLogin != null) {
                    scrollLogin.postDelayed(() -> {
                        if (isAdded() && scrollLogin != null && tilConfirmPassword != null) {
                            scrollLogin.smoothScrollTo(0, tilConfirmPassword.getTop());
                        }
                    }, 160);
                }
            }
        });

        etFullName.setOnClickListener(v -> showKeyboard(etFullName));
        etEmail.setOnClickListener(v -> showKeyboard(etEmail));
        etPassword.setOnClickListener(v -> {
            showKeyboard(etPassword);
            if (!isLoginMode) {
                layoutPasswordStrength.setVisibility(View.VISIBLE);
                updatePasswordStrength(getRawPassword(etPassword));
            }
        });
        etConfirmPassword.setOnClickListener(v -> showKeyboard(etConfirmPassword));

        tilFullName.setOnClickListener(v -> showKeyboard(etFullName));
        tilEmail.setOnClickListener(v -> showKeyboard(etEmail));
        tilPassword.setOnClickListener(v -> {
            showKeyboard(etPassword);
            if (!isLoginMode) {
                layoutPasswordStrength.setVisibility(View.VISIBLE);
                updatePasswordStrength(getRawPassword(etPassword));
            }
        });
        tilConfirmPassword.setOnClickListener(v -> showKeyboard(etConfirmPassword));
    }

    private void attachClearError(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updatePasswordStrength(String password) {
        if (layoutPasswordStrength == null || !isAdded()) return;

        PasswordStrengthEvaluator.Evaluation eval = PasswordStrengthEvaluator.evaluate(password);
        tvStrengthLabel.setText(eval.getLabel());
        pbPasswordStrength.setProgress(eval.getProgressPercent());

        int color;
        if (eval.getLevel() == PasswordStrengthEvaluator.Level.STRONG) {
            color = ContextCompat.getColor(requireContext(), R.color.accent_income);
        } else if (eval.getLevel() == PasswordStrengthEvaluator.Level.MEDIUM) {
            color = ContextCompat.getColor(requireContext(), R.color.accent_amber);
        } else if (eval.getLevel() == PasswordStrengthEvaluator.Level.WEAK) {
            color = ContextCompat.getColor(requireContext(), R.color.accent_expense);
        } else {
            color = ContextCompat.getColor(requireContext(), R.color.text_tertiary);
        }

        tvStrengthLabel.setTextColor(color);

        Drawable progressDrawable = pbPasswordStrength.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            Drawable item = ((LayerDrawable) progressDrawable).findDrawableByLayerId(android.R.id.progress);
            if (item != null) {
                item.mutate().setTint(color);
            }
        } else {
            pbPasswordStrength.setProgressTintList(ColorStateList.valueOf(color));
        }

        updateRuleText(tvRuleLength, eval.hasMinLength(), "Tối thiểu 8 ký tự");
        updateRuleText(tvRuleCase, eval.hasUpperAndLower(), "Chữ hoa & chữ thường");
        updateRuleText(tvRuleDigit, eval.hasDigit(), "Có ít nhất 1 chữ số");
        updateRuleText(tvRuleSpecial, eval.hasSpecial(), "Ký tự đặc biệt (!@#...)");
    }

    private void updateRuleText(TextView tv, boolean satisfied, String text) {
        if (tv == null || !isAdded()) return;
        int passedColor = ContextCompat.getColor(requireContext(), R.color.accent_income);
        int normalColor = ContextCompat.getColor(requireContext(), R.color.text_tertiary);
        if (satisfied) {
            tv.setText("✓ " + text);
            tv.setTextColor(passedColor);
        } else {
            tv.setText("• " + text);
            tv.setTextColor(normalColor);
        }
    }

    private void setupClickListeners() {
        btnAuthAction.setOnClickListener(v -> {
            if (isLoginMode) {
                validateAndLogin();
            } else {
                validateAndRegister();
            }
        });

        View.OnClickListener dismissKeyboard = v -> {
            if (getActivity() != null) {
                View focus = getActivity().getCurrentFocus();
                if (focus != null) {
                    InputMethodManager imm =
                            (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                    }
                    focus.clearFocus();
                }
            }
        };
        if (scrollLogin != null) {
            scrollLogin.setOnClickListener(dismissKeyboard);
        }
        if (layoutAuthContainer != null) {
            layoutAuthContainer.setOnClickListener(dismissKeyboard);
        }
    }

    private void validateAndLogin() {
        clearAllErrors();
        String email = getText(etEmail);
        String pass = getRawPassword(etPassword);
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
                    goToHome();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThreadIfAttached(() -> {
                    setLoadingState(false);
                    showAuthError(ErrorMessageHelper.getFriendlyMessage(errorMessage));
                });
            }
        });
    }

    private void validateAndRegister() {
        clearAllErrors();
        String fullName = getText(etFullName);
        String email = getText(etEmail);
        String pass = getRawPassword(etPassword);
        String confirmPass = getRawPassword(etConfirmPassword);
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
            layoutPasswordStrength.setVisibility(View.VISIBLE);
            updatePasswordStrength("");
            hasError = true;
        } else if (pass.length() < 8) {
            tilPassword.setError("Mật khẩu phải có ít nhất 8 ký tự");
            layoutPasswordStrength.setVisibility(View.VISIBLE);
            updatePasswordStrength(pass);
            hasError = true;
        } else if (!PasswordStrengthEvaluator.hasLettersAndNumbers(pass)) {
            tilPassword.setError("Mật khẩu phải bao gồm cả chữ và số");
            layoutPasswordStrength.setVisibility(View.VISIBLE);
            updatePasswordStrength(pass);
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
                        AppToaster.info(requireActivity(), "Tài khoản đã được tạo. Hãy xác nhận email rồi đăng nhập.");
                        switchToMode(true);
                        return;
                    }

                    RoomRepository.getInstance().registerNewUser(profile.getId(), profile.getFullName());
                    goToHome();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThreadIfAttached(() -> {
                    setLoadingState(false);
                    showAuthError(ErrorMessageHelper.getFriendlyMessage(errorMessage));
                });
            }
        });
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
        AppToaster.error(requireActivity(), message);
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
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String getRawPassword(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString() : "";
    }

    private void setupKeyboardHandling(View rootView) {
        keyboardLayoutListener = () -> {
            if (!isAdded() || getView() == null || spacerTop == null || cardAuth == null || layoutSwitchPrompt == null) {
                return;
            }
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);

            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            boolean keyboardNowVisible = keypadHeight > screenHeight * 0.15;
            int visibleHeight = r.bottom - r.top;

            int contentHeight = cardAuth.getHeight() + layoutSwitchPrompt.getHeight();
            if (layoutSwitchPrompt.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) layoutSwitchPrompt.getLayoutParams();
                contentHeight += mlp.topMargin + mlp.bottomMargin;
            }

            if (contentHeight <= 0) {
                cardAuth.post(() -> {
                    if (isAdded() && keyboardLayoutListener != null) {
                        keyboardLayoutListener.onGlobalLayout();
                    }
                });
                return;
            }

            float density = getResources().getDisplayMetrics().density;
            int minSpacer = (int) (14 * density);
            int targetSpacer;

            if (keyboardNowVisible) {
                if (contentHeight + minSpacer * 2 < visibleHeight) {
                    targetSpacer = (visibleHeight - contentHeight) / 2;
                } else {
                    targetSpacer = minSpacer;
                }
            } else {
                targetSpacer = Math.max(minSpacer, (visibleHeight - contentHeight) / 2);
            }

            if (lastCalculatedSpacer < 0) {
                lastCalculatedSpacer = targetSpacer;
                ViewGroup.LayoutParams lp = spacerTop.getLayoutParams();
                lp.height = targetSpacer;
                spacerTop.setLayoutParams(lp);
                isKeyboardOpen = keyboardNowVisible;
                return;
            }

            if (lastCalculatedSpacer != targetSpacer || isKeyboardOpen != keyboardNowVisible) {
                if (!keyboardNowVisible && scrollLogin != null) {
                    scrollLogin.smoothScrollTo(0, 0);
                }
                isKeyboardOpen = keyboardNowVisible;
                lastCalculatedSpacer = targetSpacer;
                animateSpacerHeight(targetSpacer);
            }
        };
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }

    private void animateSpacerHeight(int targetHeight) {
        if (spacerTop == null) return;
        if (spacerAnimator != null && spacerAnimator.isRunning()) {
            spacerAnimator.cancel();
        }
        int currentHeight = spacerTop.getLayoutParams().height;
        if (currentHeight == targetHeight) return;

        spacerAnimator = ValueAnimator.ofInt(currentHeight, targetHeight);
        spacerAnimator.setDuration(260);
        spacerAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        spacerAnimator.addUpdateListener(animation -> {
            if (spacerTop != null) {
                ViewGroup.LayoutParams lp = spacerTop.getLayoutParams();
                lp.height = (int) animation.getAnimatedValue();
                spacerTop.setLayoutParams(lp);
            }
        });
        spacerAnimator.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (spacerAnimator != null && spacerAnimator.isRunning()) {
            spacerAnimator.cancel();
            spacerAnimator = null;
        }
        if (keyboardLayoutListener != null && getView() != null) {
            getView().getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
            keyboardLayoutListener = null;
        }
    }

    private void showKeyboard(View view) {
        if (view == null || getContext() == null) return;
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void goToHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openHome();
        }
    }
}
