package com.sharedfund.app.ui.room;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.ui.profile.AvatarImageLoader;
import com.sharedfund.app.ui.widget.AppToaster;

import java.util.HashSet;
import java.util.Set;

public class MemberActionBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "arg_room_id";
    private static final String ARG_MEMBER = "arg_member";

    public interface OnMemberActionListener {
        void onUpdated();
        void onTransferred();
    }

    private String roomId;
    private RoomMember member;
    private OnMemberActionListener listener;

    public static MemberActionBottomSheet newInstance(String roomId, RoomMember member) {
        MemberActionBottomSheet fragment = new MemberActionBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putSerializable(ARG_MEMBER, member);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(OnMemberActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
            member = (RoomMember) getArguments().getSerializable(ARG_MEMBER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_member_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null || member == null || roomId == null) {
            dismiss();
            return;
        }

        ImageView ivAvatar = view.findViewById(R.id.ivSheetMemberAvatar);
        TextView tvInitials = view.findViewById(R.id.tvSheetMemberInitials);
        TextView tvName = view.findViewById(R.id.tvSheetMemberName);
        TextView tvRole = view.findViewById(R.id.tvSheetMemberRole);
        View btnClose = view.findViewById(R.id.btnCloseMemberActionSheet);
        View btnToggleTreasurer = view.findViewById(R.id.btnSheetToggleTreasurer);
        ImageView ivTreasurerIcon = view.findViewById(R.id.ivSheetTreasurerIcon);
        TextView tvTreasurerTitle = view.findViewById(R.id.tvSheetTreasurerTitle);
        TextView tvTreasurerDesc = view.findViewById(R.id.tvSheetTreasurerDesc);
        View btnTransferOwner = view.findViewById(R.id.btnSheetTransferOwner);

        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());
        if (tvName != null) tvName.setText(member.getDisplayName());
        if (tvInitials != null) tvInitials.setText(member.getInitials());
        if (ivAvatar != null) {
            AvatarImageLoader.load(ivAvatar, tvInitials, member.getAvatarUrl());
        }

        boolean isTreasurer = "TREASURER".equalsIgnoreCase(member.getRole()) || isSavedTreasurer(roomId, member.getUserId());
        int treasurerCount = getRoomTreasurerIds(roomId).size();

        if (tvRole != null) {
            if (isTreasurer) {
                tvRole.setVisibility(View.VISIBLE);
                tvRole.setText("★ Phó quỹ");
                tvRole.setBackgroundResource(R.drawable.bg_role_badge_vice);
                tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_vice_text));
                tvRole.setTextSize(11.5f);
                tvRole.setTypeface(null, Typeface.BOLD);
                int padH = dpToPx(8);
                int padV = dpToPx(2);
                tvRole.setPadding(padH, padV, padH, padV);
            } else {
                tvRole.setVisibility(View.GONE);
            }
        }

        if (isTreasurer) {
            if (tvTreasurerTitle != null) tvTreasurerTitle.setText("Bãi nhiệm Phó quỹ");
            if (tvTreasurerDesc != null) tvTreasurerDesc.setText("Thu hồi quyền phó quỹ, chuyển về thành viên thường");
            if (ivTreasurerIcon != null) {
                ivTreasurerIcon.setImageResource(R.drawable.ic_shield_minus);
                ivTreasurerIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_secondary)));
            }
        } else {
            if (tvTreasurerTitle != null) tvTreasurerTitle.setText("Bổ nhiệm làm Phó quỹ");
            if (tvTreasurerDesc != null) tvTreasurerDesc.setText("Cấp quyền duyệt chi và chỉnh sửa thông tin quỹ (" + treasurerCount + "/3 Phó quỹ)");
            if (ivTreasurerIcon != null) {
                ivTreasurerIcon.setImageResource(R.drawable.ic_shield_star);
                ivTreasurerIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            }
        }

        if (btnToggleTreasurer != null) {
            btnToggleTreasurer.setOnClickListener(v -> {
                dismiss();
                if (isTreasurer) {
                    showConfirmDialog(
                            "Bãi nhiệm Phó quỹ?",
                            "Bạn có chắc muốn bãi nhiệm vai trò Phó quỹ của " + member.getDisplayName() + "? Thành viên này sẽ trở về vai trò thành viên thường và không còn quyền duyệt chi tiêu.",
                            "Bãi nhiệm",
                            ContextCompat.getColor(requireContext(), R.color.primary),
                            () -> {
                                removeRoomTreasurer(roomId, member.getUserId());
                                member.setRole("MEMBER");
                                if (getActivity() != null) {
                                    AppToaster.success(requireActivity(), "Đã chuyển " + member.getDisplayName() + " về vai trò Thành viên thường.");
                                }
                                if (listener != null) listener.onUpdated();
                            }
                    );
                } else {
                    if (getRoomTreasurerIds(roomId).size() >= 3) {
                        if (getActivity() != null) {
                            AppToaster.error(requireActivity(), "Mỗi phòng quỹ chỉ được tối đa 3 Phó quỹ. Vui lòng bãi nhiệm phó quỹ hiện tại nếu muốn thêm mới!");
                        }
                        return;
                    }
                    showConfirmDialog(
                            "Bổ nhiệm Phó quỹ?",
                            "Bạn có chắc muốn bổ nhiệm " + member.getDisplayName() + " làm Phó quỹ? Thành viên này sẽ có quyền duyệt chi tiêu và quản trị quỹ cùng bạn.",
                            "Bổ nhiệm",
                            ContextCompat.getColor(requireContext(), R.color.primary),
                            () -> {
                                addRoomTreasurer(roomId, member.getUserId());
                                member.setRole("TREASURER");
                                if (getActivity() != null) {
                                    AppToaster.success(requireActivity(), "Đã bổ nhiệm " + member.getDisplayName() + " làm Phó quỹ thành công!");
                                }
                                if (listener != null) listener.onUpdated();
                            }
                    );
                }
            });
        }

        if (btnTransferOwner != null) {
            btnTransferOwner.setOnClickListener(v -> {
                dismiss();
                showConfirmDialog(
                        "Chuyển giao quyền Chủ quỹ",
                        "Bạn có chắc chắn muốn chuyển giao toàn quyền Chủ quỹ cho " + member.getDisplayName() + "? Bạn sẽ trở thành thành viên thường sau thao tác này.",
                        "Chuyển quyền",
                        ContextCompat.getColor(requireContext(), R.color.primary),
                        () -> {
                            String myCurrentId = UserSessionManager.getInstance(requireContext()).getUserId();
                            if (getContext() != null) {
                                requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("owner_id", member.getUserId())
                                        .apply();
                            }
                            removeRoomTreasurer(roomId, member.getUserId());
                            RoomRepository.getInstance().transferRoomOwnership(roomId, myCurrentId, member.getUserId(), null);
                            Room rSync = RoomRepository.getInstance().getRoomSync(roomId);
                            if (rSync != null) {
                                rSync.setCurrentUserRole("MEMBER");
                                rSync.setCreatedBy(member.getUserId());
                            }
                            member.setRole("OWNER");
                            if (getActivity() != null) {
                                AppToaster.success(requireActivity(), "Đã chuyển quyền Chủ quỹ cho " + member.getDisplayName());
                            }
                            if (listener != null) listener.onTransferred();
                        }
                );
            });
        }
    }

    private void showConfirmDialog(String title, String message,
                                   String positiveText, int positiveBgTint,
                                   Runnable onPositive) {
        if (getContext() == null) return;
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_koin_action_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvTitle = dialog.findViewById(R.id.tvConfirmDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvConfirmDialogMessage);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnConfirmCancel);
        MaterialButton btnOk = dialog.findViewById(R.id.btnConfirmAction);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMsg != null) tvMsg.setText(message);
        if (btnOk != null) {
            btnOk.setText(positiveText);
            btnOk.setBackgroundTintList(ColorStateList.valueOf(positiveBgTint));
            btnOk.setOnClickListener(v -> {
                dialog.dismiss();
                if (onPositive != null) onPositive.run();
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private Set<String> getRoomTreasurerIds(String roomId) {
        if (roomId == null) return new HashSet<>();
        Room r = RoomRepository.getInstance().getRoomSync(roomId);
        if (r != null && r.getTreasurerIds() != null) {
            return new HashSet<>(r.getTreasurerIds());
        }
        if (getContext() != null) {
            Set<String> set = requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                    .getStringSet("treasurer_ids", null);
            if (set != null) return new HashSet<>(set);
        }
        return new HashSet<>();
    }

    private boolean isSavedTreasurer(String roomId, String userId) {
        if (roomId == null || userId == null) return false;
        Set<String> treasurers = getRoomTreasurerIds(roomId);
        for (String id : treasurers) {
            if (id != null && id.equalsIgnoreCase(userId.trim())) return true;
        }
        return false;
    }

    private void addRoomTreasurer(String roomId, String userId) {
        if (getContext() == null || roomId == null || userId == null) return;
        Set<String> set = getRoomTreasurerIds(roomId);
        set.add(userId);
        requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                .edit()
                .putStringSet("treasurer_ids", set)
                .putBoolean("treasurer_" + userId, true)
                .apply();
        RoomRepository.getInstance().syncRoomTreasurers(roomId, set);
        RoomRepository.getInstance().updateMemberRole(roomId, userId, "TREASURER", null);
    }

    private void removeRoomTreasurer(String roomId, String userId) {
        if (getContext() == null || roomId == null || userId == null) return;
        Set<String> set = getRoomTreasurerIds(roomId);
        set.remove(userId);
        requireContext().getSharedPreferences("room_roles_" + roomId, Context.MODE_PRIVATE)
                .edit()
                .putStringSet("treasurer_ids", set)
                .remove("treasurer_" + userId)
                .apply();
        RoomRepository.getInstance().syncRoomTreasurers(roomId, set);
        RoomRepository.getInstance().updateMemberRole(roomId, userId, "MEMBER", null);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
