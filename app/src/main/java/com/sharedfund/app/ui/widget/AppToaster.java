package com.sharedfund.app.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.sharedfund.app.R;

import java.lang.ref.WeakReference;

public class AppToaster {

    public enum ToastType {
        SUCCESS,
        INFO,
        ERROR
    }

    private static WeakReference<View> currentToastView;
    private static WeakReference<ValueAnimator> currentProgressAnimator;

    public static void show(Activity activity, ToastType type, String title, String message) {
        show(activity, type, title, message, 3200);
    }

    public static void show(Activity activity, ToastType type, String message) {
        show(activity, type, null, message, 3200);
    }

    public static void show(Activity activity, ToastType type, String title, String message, int durationMs) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> show(activity, type, title, message, durationMs));
            return;
        }

        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) {
            View decor = activity.getWindow().getDecorView();
            if (decor instanceof ViewGroup) {
                root = (ViewGroup) decor;
            }
        }
        if (root == null) return;

        showOnViewGroup(root, type, title, message, durationMs);
    }

    public static void show(Fragment fragment, ToastType type, String title, String message) {
        show(fragment, type, title, message, 3200);
    }

    public static void show(Fragment fragment, ToastType type, String message) {
        show(fragment, type, null, message, 3200);
    }

    public static void show(Fragment fragment, ToastType type, String title, String message, int durationMs) {
        if (fragment == null) return;
        if (fragment.isAdded()) {
            if (fragment instanceof DialogFragment) {
                Dialog dialog = ((DialogFragment) fragment).getDialog();
                if (dialog != null && dialog.getWindow() != null) {
                    View decor = dialog.getWindow().getDecorView();
                    if (decor instanceof ViewGroup) {
                        showOnViewGroup((ViewGroup) decor, type, title, message, durationMs);
                        return;
                    }
                }
            }
        }
        if (fragment.getActivity() != null) {
            show(fragment.getActivity(), type, title, message, durationMs);
        } else if (fragment.getView() != null) {
            show(fragment.getView(), type, title, message);
        }
    }

    public static void show(View anchorView, ToastType type, String title, String message) {
        if (anchorView == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> show(anchorView, type, title, message));
            return;
        }
        Activity activity = getActivity(anchorView.getContext());
        View root = anchorView.getRootView();
        if (activity != null && (root == null || root == activity.getWindow().getDecorView())) {
            show(activity, type, title, message, 3200);
            return;
        }
        if (root instanceof ViewGroup) {
            showOnViewGroup((ViewGroup) root, type, title, message, 3200);
            return;
        }
        if (activity != null) {
            show(activity, type, title, message, 3200);
        }
    }

    public static void show(Context context, ToastType type, String message) {
        show(context, type, null, message);
    }

    public static void show(Context context, ToastType type, String title, String message) {
        Activity activity = getActivity(context);
        if (activity != null) {
            show(activity, type, title, message);
        } else if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    private static void showOnViewGroup(ViewGroup root, ToastType type, String title, String message, int durationMs) {
        if (root == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> showOnViewGroup(root, type, title, message, durationMs));
            return;
        }

        dismissCurrentImmediately();

        Context context = root.getContext();
        View toastView = LayoutInflater.from(context).inflate(R.layout.layout_app_toast, root, false);

        MaterialCardView cardView = toastView.findViewById(R.id.toastCardView);
        ImageView ivIcon = toastView.findViewById(R.id.ivToastIcon);
        TextView tvTitle = toastView.findViewById(R.id.tvToastTitle);
        TextView tvMessage = toastView.findViewById(R.id.tvToastMessage);
        ImageView btnClose = toastView.findViewById(R.id.btnToastClose);
        FrameLayout flTrack = toastView.findViewById(R.id.flToastProgressTrack);
        View viewFill = toastView.findViewById(R.id.viewToastProgressFill);

        if (cardView != null) {
            cardView.setClipToOutline(true);
        }
        if (flTrack != null) {
            flTrack.setClipToOutline(true);
        }

        int iconRes;
        int trackColor;
        int fillColor;
        String defaultTitle;

        if (type == ToastType.INFO) {
            iconRes = R.drawable.ic_toast_info;
            trackColor = ContextCompat.getColor(context, R.color.accent_blue_light);
            fillColor = ContextCompat.getColor(context, R.color.accent_blue);
            defaultTitle = "Thông báo";
        } else if (type == ToastType.ERROR) {
            iconRes = R.drawable.ic_toast_error;
            trackColor = ContextCompat.getColor(context, R.color.accent_expense_light);
            fillColor = ContextCompat.getColor(context, R.color.accent_expense);
            defaultTitle = "Thông báo lỗi";
        } else {
            iconRes = R.drawable.ic_toast_success;
            trackColor = ContextCompat.getColor(context, R.color.accent_income_light);
            fillColor = ContextCompat.getColor(context, R.color.accent_income);
            defaultTitle = "Thành công";
        }

        ivIcon.setImageResource(iconRes);
        flTrack.setBackgroundColor(trackColor);
        viewFill.setBackgroundColor(fillColor);

        boolean hasTitle = title != null && !title.trim().isEmpty();
        if (hasTitle) {
            tvTitle.setText(title);
            tvTitle.setVisibility(View.VISIBLE);
            tvMessage.setText(message != null ? message : "");
            tvMessage.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        } else {
            tvTitle.setVisibility(View.GONE);
            tvMessage.setText(message != null ? message : defaultTitle);
            tvMessage.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        }

        ViewGroup.MarginLayoutParams lp;
        if (root instanceof FrameLayout) {
            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            flp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp = flp;
        } else if (root instanceof CoordinatorLayout) {
            CoordinatorLayout.LayoutParams clp = new CoordinatorLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            clp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp = clp;
        } else {
            lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        int sideMargin = dpToPx(context, 16);
        lp.leftMargin = sideMargin;
        lp.rightMargin = sideMargin;
        lp.topMargin = getCalculatedTopMargin(context, root);
        toastView.setLayoutParams(lp);

        toastView.setAlpha(0f);
        toastView.setTranslationY(-dpToPx(context, 48));
        toastView.setElevation(dpToPx(context, 12));
        toastView.setTranslationZ(dpToPx(context, 12));

        root.addView(toastView);

        toastView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        currentToastView = new WeakReference<>(toastView);

        viewFill.setPivotX(0f);
        viewFill.setScaleX(1f);

        ValueAnimator progressAnimator = ValueAnimator.ofFloat(1f, 0f);
        progressAnimator.setDuration(durationMs > 0 ? durationMs : 3200);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            viewFill.setScaleX(scale);
        });
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean isCanceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                isCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isCanceled) {
                    dismissToast(toastView, root);
                }
            }
        });
        currentProgressAnimator = new WeakReference<>(progressAnimator);
        progressAnimator.start();

        btnClose.setOnClickListener(v -> dismissToast(toastView, root));
        toastView.setOnClickListener(v -> dismissToast(toastView, root));
    }

    private static void dismissToast(View view, ViewGroup parent) {
        if (view == null || parent == null) return;
        if (view.getTag(R.id.toastCardView) != null) return;
        view.setTag(R.id.toastCardView, Boolean.TRUE);

        if (currentProgressAnimator != null && currentProgressAnimator.get() != null) {
            ValueAnimator anim = currentProgressAnimator.get();
            anim.removeAllListeners();
            anim.cancel();
            currentProgressAnimator = null;
        }

        view.animate()
                .alpha(0f)
                .translationY(-dpToPx(view.getContext(), 48))
                .setDuration(220)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    try {
                        parent.removeView(view);
                    } catch (Exception ignored) {
                    }
                    if (currentToastView != null && currentToastView.get() == view) {
                        currentToastView = null;
                    }
                })
                .start();
    }

    public static void dismissCurrentImmediately() {
        if (currentProgressAnimator != null && currentProgressAnimator.get() != null) {
            ValueAnimator anim = currentProgressAnimator.get();
            anim.removeAllListeners();
            anim.removeAllUpdateListeners();
            anim.cancel();
            currentProgressAnimator = null;
        }
        if (currentToastView != null && currentToastView.get() != null) {
            View v = currentToastView.get();
            v.animate().cancel();
            ViewParent p = v.getParent();
            if (p instanceof ViewGroup) {
                ((ViewGroup) p).removeView(v);
            }
            currentToastView = null;
        }
    }

    public static void success(Activity activity, String message) {
    }

    public static void success(Activity activity, String title, String message) {
    }

    public static void success(Fragment fragment, String message) {
    }

    public static void success(Fragment fragment, String title, String message) {
    }

    public static void success(Context context, String message) {
    }

    public static void success(Context context, String title, String message) {
    }

    public static void info(Activity activity, String message) {
        show(activity, ToastType.INFO, null, message);
    }

    public static void info(Activity activity, String title, String message) {
        show(activity, ToastType.INFO, title, message);
    }

    public static void info(Fragment fragment, String message) {
        show(fragment, ToastType.INFO, null, message);
    }

    public static void info(Fragment fragment, String title, String message) {
        show(fragment, ToastType.INFO, title, message);
    }

    public static void info(Context context, String message) {
        show(context, ToastType.INFO, null, message);
    }

    public static void info(Context context, String title, String message) {
        show(context, ToastType.INFO, title, message);
    }

    public static String sanitizeErrorMessage(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Đã xảy ra lỗi, vui lòng thử lại sau.";
        }
        String lower = raw.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("failed to connect") || lower.contains("timeout") || lower.contains("unable to resolve") || lower.contains("network") || lower.contains("connection")) {
            return "Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại mạng.";
        }
        if (lower.contains("jwt") || lower.contains("unauthorized") || lower.contains("expired") || lower.contains("token")) {
            return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
        }
        if (lower.contains("invalid login") || lower.contains("invalid credentials") || lower.contains("invalid_credentials") || lower.contains("invalid_grant") || lower.contains("wrong password") || lower.contains("email not confirmed") || lower.contains("email hoặc mật khẩu")) {
            return "Email hoặc mật khẩu không đúng.";
        }
        if (lower.contains("user already registered") || lower.contains("email already exists") || lower.contains("email này đã")) {
            return "Email này đã được đăng ký.";
        }
        if (lower.contains("insufficient") || lower.contains("số dư không đủ")) {
            return "Số dư không đủ để thực hiện giao dịch.";
        }
        if (lower.contains("http") || lower.contains("pgrst") || lower.contains("syntax") || lower.contains("relation") || lower.contains("column") || lower.contains("failed to run sql") || lower.contains("{") || lower.contains("}")) {
            return "Hệ thống đang bận hoặc gặp sự cố xử lý. Vui lòng thử lại sau.";
        }
        return raw;
    }

    public static void error(Activity activity, String message) {
        show(activity, ToastType.ERROR, null, sanitizeErrorMessage(message));
    }

    public static void error(Activity activity, String title, String message) {
        show(activity, ToastType.ERROR, title, sanitizeErrorMessage(message));
    }

    public static void error(Fragment fragment, String message) {
        show(fragment, ToastType.ERROR, null, sanitizeErrorMessage(message));
    }

    public static void error(Fragment fragment, String title, String message) {
        show(fragment, ToastType.ERROR, title, sanitizeErrorMessage(message));
    }

    public static void error(Context context, String message) {
        show(context, ToastType.ERROR, null, sanitizeErrorMessage(message));
    }

    public static void error(Context context, String title, String message) {
        show(context, ToastType.ERROR, title, sanitizeErrorMessage(message));
    }

    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    private static int getCalculatedTopMargin(Context context, ViewGroup root) {
        int baseMargin = dpToPx(context, 16);
        Activity activity = getActivity(context);
        if (activity == null) return baseMargin;

        int statusBarHeight = 0;
        View decor = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
        if (decor != null) {
            androidx.core.view.WindowInsetsCompat insets = androidx.core.view.ViewCompat.getRootWindowInsets(decor);
            if (insets != null) {
                androidx.core.graphics.Insets sb = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                statusBarHeight = sb.top;
                androidx.core.graphics.Insets cutout = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout());
                if (cutout.top > statusBarHeight) {
                    statusBarHeight = cutout.top;
                }
            }
        }
        if (statusBarHeight <= 0) {
            int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resId > 0) {
                statusBarHeight = context.getResources().getDimensionPixelSize(resId);
            }
        }
        if (statusBarHeight <= 0) {
            statusBarHeight = dpToPx(context, 24);
        }

        return statusBarHeight + baseMargin;
    }

    public static Activity getActivity(Context context) {
        if (context == null) return null;
        if (context instanceof Activity) return (Activity) context;
        if (context instanceof ContextWrapper) {
            return getActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }
}
