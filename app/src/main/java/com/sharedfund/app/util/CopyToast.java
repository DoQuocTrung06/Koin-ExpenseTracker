package com.sharedfund.app.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sharedfund.app.R;

public class CopyToast {

    private static PopupWindow currentPopup;
    private static Runnable dismissRunnable;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void show(Context context, View anchorView) {
        show(context, anchorView, null);
    }

    public static void show(View anchorView) {
        if (anchorView != null) {
            show(anchorView.getContext(), anchorView, null);
        }
    }

    public static void show(Context context, View anchorView, String message) {
        if (context == null || anchorView == null) {
            return;
        }
        if (anchorView.getWindowToken() == null) {
            anchorView.post(() -> show(context, anchorView, message));
            return;
        }

        if (dismissRunnable != null) {
            mainHandler.removeCallbacks(dismissRunnable);
            dismissRunnable = null;
        }

        if (currentPopup != null && currentPopup.isShowing()) {
            try {
                currentPopup.dismiss();
            } catch (Exception ignored) {
            }
            currentPopup = null;
        }

        View toastView = LayoutInflater.from(context).inflate(R.layout.toast_copy_success, null);
        if (message != null && !message.trim().isEmpty()) {
            TextView tvMessage = toastView.findViewById(R.id.tv_toast_message);
            if (tvMessage != null) {
                tvMessage.setText(message);
            }
        }

        final PopupWindow popup = new PopupWindow(
                toastView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
        );
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setTouchable(false);
        popup.setElevation(context.getResources().getDisplayMetrics().density * 6);
        currentPopup = popup;

        float density = context.getResources().getDisplayMetrics().density;
        int yOffset = (int) (80 * density);
        int slideDistance = (int) (24 * density);

        toastView.setAlpha(0f);
        toastView.setTranslationY(slideDistance);

        try {
            popup.showAtLocation(anchorView.getRootView(), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, yOffset);
        } catch (Exception e) {
            return;
        }

        toastView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        dismissRunnable = () -> {
            if (popup.isShowing()) {
                toastView.animate()
                        .alpha(0f)
                        .translationY(slideDistance)
                        .setDuration(220)
                        .setInterpolator(new AccelerateInterpolator())
                        .withEndAction(() -> {
                            try {
                                if (popup.isShowing()) {
                                    popup.dismiss();
                                }
                            } catch (Exception ignored) {
                            }
                            if (currentPopup == popup) {
                                currentPopup = null;
                            }
                        })
                        .start();
            }
        };

        mainHandler.postDelayed(dismissRunnable, 1200);
    }
}
