package com.sharedfund.app.ui.room;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.ui.profile.AvatarImageLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MemberDelegationDialogFragment extends DialogFragment {

    private static final String ARG_ROOM_ID = "arg_room_id";

    private String roomId;
    private LinearLayout layoutMembers;
    private TextView tvEmpty;
    private TextView tvMembersCount;

    public static MemberDelegationDialogFragment newInstance(String roomId) {
        MemberDelegationDialogFragment fragment = new MemberDelegationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_SharedFund);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_fullscreen_delegation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton btnBack = view.findViewById(R.id.btnBackDelegation);
        layoutMembers = view.findViewById(R.id.layoutDelegationMemberList);
        tvEmpty = view.findViewById(R.id.tvEmptyDelegationMembers);
        tvMembersCount = view.findViewById(R.id.tvDelegationMembersCount);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dismiss());
        }

        loadAndRenderMembers();
    }

    public void loadAndRenderMembers() {
        if (getContext() == null || roomId == null) return;
        RoomRepository.getInstance().loadRoomMembers(roomId, new RoomRepository.DataCallback<List<RoomMember>>() {
            @Override
            public void onSuccess(List<RoomMember> members) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> renderMembers(members));
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (tvEmpty != null) {
                        tvEmpty.setText("Không thể tải danh sách thành viên: " + error);
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                    if (layoutMembers != null) layoutMembers.setVisibility(View.GONE);
                });
            }
        });
    }

    private void renderMembers(List<RoomMember> members) {
        if (getContext() == null) return;
        String currentUserId = UserSessionManager.getInstance(requireContext()).getUserId();
        Room currentRoom = RoomRepository.getInstance().getRoomSync(roomId);

        List<RoomMember> nonOwnerMembers = new ArrayList<>();
        if (members != null) {
            for (RoomMember m : members) {
                if (m == null) continue;
                if (!"OWNER".equalsIgnoreCase(m.getRole()) && (currentUserId == null || !currentUserId.equals(m.getUserId()))) {
                    nonOwnerMembers.add(m);
                }
            }
        }

        int treasurerCount = getRoomTreasurerIds(roomId).size();
        if (tvMembersCount != null) {
            tvMembersCount.setText("Chọn thành viên để ủy quyền (Phó quỹ: " + treasurerCount + "/3)");
        }

        if (nonOwnerMembers.isEmpty()) {
            if (layoutMembers != null) layoutMembers.setVisibility(View.GONE);
            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
        if (layoutMembers == null) return;
        layoutMembers.setVisibility(View.VISIBLE);
        layoutMembers.removeAllViews();

        for (int i = 0; i < nonOwnerMembers.size(); i++) {
            RoomMember m = nonOwnerMembers.get(i);
            boolean isTreasurer = "TREASURER".equalsIgnoreCase(m.getRole()) || isSavedTreasurer(roomId, m.getUserId());

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

            TypedValue outValue = new TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            row.setBackgroundResource(outValue.resourceId);
            row.setClickable(true);
            row.setFocusable(true);

            FrameLayout avatarFrame = new FrameLayout(requireContext());
            LinearLayout.LayoutParams lpAvatar = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
            avatarFrame.setLayoutParams(lpAvatar);

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
            tvInitials.setText(m.getInitials());

            avatarFrame.addView(ivAv);
            avatarFrame.addView(tvInitials);
            AvatarImageLoader.load(ivAv, tvInitials, m.getAvatarUrl());

            LinearLayout textCol = new LinearLayout(requireContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lpText.setMarginStart(dpToPx(12));
            textCol.setLayoutParams(lpText);

            TextView tvName = new TextView(requireContext());
            tvName.setText(m.getDisplayName());
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvName.setTextSize(15f);
            tvName.setTypeface(null, Typeface.BOLD);

            TextView tvRole = new TextView(requireContext());
            LinearLayout.LayoutParams lpRole = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lpRole.topMargin = dpToPx(3);
            tvRole.setLayoutParams(lpRole);

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

            textCol.addView(tvName);
            textCol.addView(tvRole);

            ImageView ivChevron = new ImageView(requireContext());
            LinearLayout.LayoutParams lpChevron = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
            lpChevron.setMarginStart(dpToPx(8));
            ivChevron.setLayoutParams(lpChevron);
            ivChevron.setImageResource(R.drawable.ic_chevron_right);
            ivChevron.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_tertiary)));

            row.addView(avatarFrame);
            row.addView(textCol);
            row.addView(ivChevron);

            row.setOnClickListener(v -> {
                MemberActionBottomSheet actionSheet = MemberActionBottomSheet.newInstance(roomId, m);
                actionSheet.setListener(new MemberActionBottomSheet.OnMemberActionListener() {
                    @Override
                    public void onUpdated() {
                        loadAndRenderMembers();
                    }

                    @Override
                    public void onTransferred() {
                        dismiss();
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    }
                });
                actionSheet.show(getChildFragmentManager(), "MemberAction");
            });

            layoutMembers.addView(row);

            if (i < nonOwnerMembers.size() - 1) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
                lpDiv.setMarginStart(dpToPx(68));
                divider.setLayoutParams(lpDiv);
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.border));
                layoutMembers.addView(divider);
            }
        }
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

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
