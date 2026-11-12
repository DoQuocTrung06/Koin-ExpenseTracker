package com.sharedfund.app.ui.room;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomPrivacyManager;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.ui.widget.AppToaster;
import com.sharedfund.app.util.ErrorMessageHelper;

import java.util.ArrayList;
import java.util.List;

public class JoinRequestsBottomSheet extends DialogFragment {

    private static final String ARG_ROOM_ID = "arg_room_id";

    public interface OnJoinRequestsChangeListener {
        void onRequestHandled();
    }

    private String roomId;
    private OnJoinRequestsChangeListener changeListener;
    private TextView tvTitle;
    private LinearLayout layoutList;
    private View layoutEmpty;
    private final List<Room.JoinRequestMeta> currentRequests = new ArrayList<>();

    public static JoinRequestsBottomSheet newInstance(String roomId) {
        JoinRequestsBottomSheet fragment = new JoinRequestsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setChangeListener(OnJoinRequestsChangeListener listener) {
        this.changeListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_SharedFund);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_fullscreen_join_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btnBackJoinRequests);
        tvTitle = view.findViewById(R.id.tvJoinRequestsHeaderTitle);
        layoutList = view.findViewById(R.id.layoutJoinRequestsList);
        layoutEmpty = view.findViewById(R.id.layoutEmptyJoinRequests);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dismiss());
        }

        refreshDataFromSource();
    }

    public void refreshDataFromSource() {
        if (!isAdded() || getContext() == null) return;
        currentRequests.clear();
        if (roomId != null && !roomId.trim().isEmpty()) {
            Room r = RoomRepository.getInstance().getRoomSync(roomId);
            if (r != null && r.getPendingJoinRequests() != null) {
                currentRequests.addAll(r.getPendingJoinRequests());
            }
        }
        renderList();
    }

    private void renderList() {
        if (!isAdded() || getContext() == null) return;
        if (tvTitle != null) {
            tvTitle.setText("Yêu cầu tham gia quỹ (" + currentRequests.size() + ")");
        }
        if (layoutList == null) return;
        layoutList.removeAllViews();

        if (currentRequests.isEmpty()) {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);

        for (int i = 0; i < currentRequests.size(); i++) {
            final Room.JoinRequestMeta req = currentRequests.get(i);
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpCard.setMargins(0, 0, 0, dpToPx(12));
            card.setLayoutParams(lpCard);
            card.setRadius(dpToPx(16));
            card.setCardElevation(2f);
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.border));
            card.setStrokeWidth(dpToPx(1));
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface));

            LinearLayout cardContent = new LinearLayout(requireContext());
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

            LinearLayout topRow = new LinearLayout(requireContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            FrameLayout avatarFrame = new FrameLayout(requireContext());
            avatarFrame.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)));

            ImageView ivAv = new ImageView(requireContext());
            ivAv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ivAv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivAv.setBackgroundResource(R.drawable.bg_avatar_circle);
            ivAv.setClipToOutline(true);

            TextView tvInitials = new TextView(requireContext());
            tvInitials.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tvInitials.setGravity(Gravity.CENTER);
            tvInitials.setTextColor(Color.WHITE);
            tvInitials.setTextSize(15f);
            tvInitials.setTypeface(null, Typeface.BOLD);
            tvInitials.setBackgroundResource(R.drawable.bg_avatar_circle);

            String applicantInitials = "U";
            if (req.applicantName != null && !req.applicantName.trim().isEmpty()) {
                String[] parts = req.applicantName.trim().split("\\s+");
                if (parts.length >= 2) {
                    applicantInitials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
                } else if (parts[0].length() > 0) {
                    applicantInitials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
                }
            }
            tvInitials.setText(applicantInitials);
            avatarFrame.addView(ivAv);
            avatarFrame.addView(tvInitials);

            LinearLayout textCol = new LinearLayout(requireContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lpText.setMarginStart(dpToPx(12));
            textCol.setLayoutParams(lpText);

            TextView tvName = new TextView(requireContext());
            tvName.setText(req.applicantName != null ? req.applicantName : "Thành viên mới");
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvName.setTextSize(15f);
            tvName.setTypeface(null, Typeface.BOLD);

            TextView tvEmail = new TextView(requireContext());
            tvEmail.setText(req.email != null ? req.email : "");
            tvEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvEmail.setTextSize(13f);

            TextView tvTime = new TextView(requireContext());
            tvTime.setText(req.time != null ? req.time : "Vừa xong");
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvTime.setTextSize(12.5f);

            textCol.addView(tvName);
            textCol.addView(tvEmail);
            textCol.addView(tvTime);

            topRow.addView(avatarFrame);
            topRow.addView(textCol);
            cardContent.addView(topRow);

            if (req.message != null && !req.message.isEmpty()) {
                LinearLayout bubble = new LinearLayout(requireContext());
                LinearLayout.LayoutParams lpBubble = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lpBubble.setMargins(0, dpToPx(8), 0, dpToPx(8));
                bubble.setLayoutParams(lpBubble);
                bubble.setOrientation(LinearLayout.VERTICAL);
                bubble.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
                bubble.setBackgroundResource(R.drawable.bg_edit_text_inline);

                TextView tvMsgLabel = new TextView(requireContext());
                tvMsgLabel.setText("Lời nhắn:");
                tvMsgLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                tvMsgLabel.setTextSize(12f);
                tvMsgLabel.setTypeface(null, Typeface.BOLD);

                TextView tvMsg = new TextView(requireContext());
                tvMsg.setText(req.message);
                tvMsg.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                tvMsg.setTextSize(14f);

                bubble.addView(tvMsgLabel);
                bubble.addView(tvMsg);
                cardContent.addView(bubble);
            }

            LinearLayout btnRow = new LinearLayout(requireContext());
            LinearLayout.LayoutParams lpBtnRow = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnRow.setLayoutParams(lpBtnRow);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            MaterialButton btnReject = new MaterialButton(requireContext());
            LinearLayout.LayoutParams lpReject = new LinearLayout.LayoutParams(0, dpToPx(44), 1f);
            lpReject.setMarginEnd(dpToPx(8));
            btnReject.setLayoutParams(lpReject);
            btnReject.setText("Từ chối");
            btnReject.setTextColor(Color.WHITE);
            btnReject.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_expense)));
            btnReject.setCornerRadius(dpToPx(10));

            MaterialButton btnAccept = new MaterialButton(requireContext());
            LinearLayout.LayoutParams lpAccept = new LinearLayout.LayoutParams(0, dpToPx(44), 1.2f);
            btnAccept.setLayoutParams(lpAccept);
            btnAccept.setText("Duyệt tham gia");
            btnAccept.setTextColor(Color.WHITE);
            btnAccept.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            btnAccept.setCornerRadius(dpToPx(10));

            btnReject.setOnClickListener(v -> showRejectConfirmDialog(req, card, btnReject, btnAccept));
            btnAccept.setOnClickListener(v -> executeDecision(req, true, card, btnReject, btnAccept));

            btnRow.addView(btnReject);
            btnRow.addView(btnAccept);
            cardContent.addView(btnRow);

            card.addView(cardContent);
            layoutList.addView(card);
        }
    }

    private void showRejectConfirmDialog(Room.JoinRequestMeta req, View card, MaterialButton btnReject, MaterialButton btnAccept) {
        if (getContext() == null) return;
        Dialog confirmDialog = new Dialog(requireContext());
        confirmDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        confirmDialog.setContentView(R.layout.dialog_koin_action_confirm);
        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            confirmDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvTitle = confirmDialog.findViewById(R.id.tvConfirmDialogTitle);
        TextView tvMsg = confirmDialog.findViewById(R.id.tvConfirmDialogMessage);
        MaterialButton btnCancel = confirmDialog.findViewById(R.id.btnConfirmCancel);
        MaterialButton btnAction = confirmDialog.findViewById(R.id.btnConfirmAction);

        if (tvTitle != null) tvTitle.setText("Từ chối yêu cầu tham gia?");
        if (tvMsg != null) {
            String displayName = req.applicantName != null && !req.applicantName.isEmpty() ? req.applicantName : "thành viên này";
            tvMsg.setText("Bạn có chắc chắn muốn từ chối yêu cầu tham gia quỹ của " + displayName + " không? Người này sẽ không thể tham gia quỹ.");
        }
        if (btnAction != null) {
            btnAction.setText("Từ chối");
            btnAction.setTextColor(Color.WHITE);
            btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_expense)));
            btnAction.setOnClickListener(actView -> {
                confirmDialog.dismiss();
                executeDecision(req, false, card, btnReject, btnAccept);
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(cView -> confirmDialog.dismiss());
        }
        confirmDialog.show();
    }

    private void executeDecision(Room.JoinRequestMeta req, boolean approve, View card, MaterialButton btnReject, MaterialButton btnAccept) {
        btnReject.setEnabled(false);
        btnReject.setAlpha(0.45f);
        btnAccept.setEnabled(false);
        btnAccept.setAlpha(0.45f);

        final String targetRoomId = roomId;
        final Context appCtx = requireContext().getApplicationContext();
        RoomRepository.getInstance().processJoinRequestDecision(targetRoomId, req.userId, req.email, approve, new RoomRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    float targetX = card.getWidth() > 0 ? (float) (approve ? 1 : -1) * card.getWidth() * 1.2f : (approve ? 1000f : -1000f);
                    card.animate()
                            .translationX(targetX)
                            .alpha(0f)
                            .setDuration(280)
                            .setInterpolator(new FastOutSlowInInterpolator())
                            .withEndAction(() -> {
                                RoomPrivacyManager.removePendingRequest(targetRoomId, req.userId, req.email);
                                currentRequests.remove(req);
                                String approverName = RoomRepository.getInstance().getCurrentUserName();
                                RoomRepository.getInstance().dispatchJoinResultNotification(appCtx, targetRoomId, req.userId, req.applicantName, approve, approverName);
                                if (changeListener != null) {
                                    changeListener.onRequestHandled();
                                }
                                renderList();
                            })
                            .start();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    AppToaster.error(requireActivity(), ErrorMessageHelper.getFriendlyMessage(message));
                    if (message != null && (message.contains("quản trị viên khác") || message.contains("đã được xử lý"))) {
                        RoomPrivacyManager.removePendingRequest(targetRoomId, req.userId, req.email);
                        currentRequests.remove(req);
                        if (changeListener != null) {
                            changeListener.onRequestHandled();
                        }
                        renderList();
                    } else {
                        btnReject.setEnabled(true);
                        btnReject.setAlpha(1.0f);
                        btnAccept.setEnabled(true);
                        btnAccept.setAlpha(1.0f);
                    }
                });
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
