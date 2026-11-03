package com.sharedfund.app.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sharedfund.app.R;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.ui.profile.AvatarImageLoader;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    public interface OnMemberMoreClickListener {
        void onMemberMoreClick(RoomMember member);
    }

    private List<RoomMember> members = new ArrayList<>();
    private OnMemberMoreClickListener moreClickListener;
    private String currentUserId;
    private boolean isCurrentUserOwner;
    private boolean isCurrentUserTreasurer;

    public void setOnMemberMoreClickListener(OnMemberMoreClickListener listener) {
        this.moreClickListener = listener;
    }

    public void setCurrentUserPermissions(String currentUserId, boolean isOwner, boolean isTreasurer) {
        this.currentUserId = currentUserId;
        this.isCurrentUserOwner = isOwner;
        this.isCurrentUserTreasurer = isTreasurer;
        notifyDataSetChanged();
    }

    public void setMembers(List<RoomMember> members) {
        this.members = members != null ? members : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        RoomMember member = members.get(position);
        holder.bind(member);

        boolean isSelf = currentUserId != null && currentUserId.equals(member.getUserId());
        boolean targetIsOwner = "OWNER".equalsIgnoreCase(member.getRole());
        boolean targetIsTreasurer = "TREASURER".equalsIgnoreCase(member.getRole());

        boolean canShowMore = false;
        if (!isSelf) {
            if (isCurrentUserOwner) {
                canShowMore = !targetIsOwner;
            } else if (isCurrentUserTreasurer) {
                canShowMore = !targetIsOwner && !targetIsTreasurer;
            }
        }

        if (holder.btnMemberMore != null) {
            holder.btnMemberMore.setVisibility(canShowMore ? View.VISIBLE : View.GONE);
            holder.btnMemberMore.setOnClickListener(v -> {
                if (moreClickListener != null) {
                    moreClickListener.onMemberMoreClick(member);
                }
            });
        }
        if (holder.dividerMember != null) {
            holder.dividerMember.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
        }

    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvInitials;
        private final TextView tvName;
        private final TextView tvBadge;
        private final TextView tvContribution;
        final ImageView btnMemberMore;
        final View dividerMember;

        private final DecimalFormat df;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivMemberAvatar);
            tvInitials = itemView.findViewById(R.id.tvMemberInitials);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvBadge = itemView.findViewById(R.id.tvMemberBadge);
            tvContribution = itemView.findViewById(R.id.tvMemberContribution);
            btnMemberMore = itemView.findViewById(R.id.btnMemberMore);
            dividerMember = itemView.findViewById(R.id.dividerMember);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator('.');
            df = new DecimalFormat("#,##0", symbols);
        }

        void bind(RoomMember member) {
            tvInitials.setText(member.getInitials());
            tvInitials.setVisibility(View.VISIBLE);
            if (ivAvatar != null) {
                AvatarImageLoader.load(ivAvatar, tvInitials, member.getAvatarUrl());
            }
            tvName.setText(member.getDisplayName());

            String role = member.getRole();
            if ("OWNER".equalsIgnoreCase(role)) {
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText("★ Chủ quỹ");
                tvBadge.setBackgroundResource(R.drawable.bg_role_badge_owner);
                tvBadge.setTextColor(Color.parseColor("#B45309"));
            } else if ("TREASURER".equalsIgnoreCase(role)) {
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText("★ Phó quỹ");
                tvBadge.setBackgroundResource(R.drawable.bg_role_badge_vice);
                tvBadge.setTextColor(Color.parseColor("#334155"));
            } else {
                tvBadge.setVisibility(View.GONE);
            }

            String formattedAmount = df.format(member.getContributedAmount()) + " ₫";
            tvContribution.setText("Đã góp: " + formattedAmount);
        }
    }
}
