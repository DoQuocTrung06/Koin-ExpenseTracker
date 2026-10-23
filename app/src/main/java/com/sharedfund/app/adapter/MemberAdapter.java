package com.sharedfund.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sharedfund.app.R;
import com.sharedfund.app.model.RoomMember;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách thành viên trong Dialog thành viên quỹ.
 * Mỗi item gồm: avatar initials, tên, badge Trưởng quỹ, số tiền đóng góp, % đóng góp.
 */
public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<RoomMember> members = new ArrayList<>();
    private double totalContribution = 0;

    public void setMembers(List<RoomMember> members) {
        this.members = members != null ? members : new ArrayList<>();
        // Tính tổng đóng góp để tính phần trăm
        totalContribution = 0;
        for (RoomMember m : this.members) {
            totalContribution += m.getContributedAmount();
        }
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
        holder.bind(member, totalContribution);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitials;
        private final TextView tvName;
        private final TextView tvBadge;
        private final TextView tvContribution;
        private final TextView tvPercent;

        private final DecimalFormat df;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tvMemberInitials);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvBadge = itemView.findViewById(R.id.tvMemberBadge);
            tvContribution = itemView.findViewById(R.id.tvMemberContribution);
            tvPercent = itemView.findViewById(R.id.tvMemberPercent);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator('.');
            df = new DecimalFormat("#,##0", symbols);
        }

        void bind(RoomMember member, double totalContribution) {
            tvInitials.setText(member.getInitials());
            tvName.setText(member.getDisplayName());

            // Badge Trưởng quỹ
            if (member.isAdmin()) {
                tvBadge.setVisibility(View.VISIBLE);
            } else {
                tvBadge.setVisibility(View.GONE);
            }

            // Số tiền đóng góp
            String formattedAmount = df.format(member.getContributedAmount()) + " ₫";
            tvContribution.setText("Đã đóng: " + formattedAmount);

            // Phần trăm đóng góp
            if (totalContribution > 0) {
                double percent = (member.getContributedAmount() / totalContribution) * 100;
                tvPercent.setText(String.format(Locale.getDefault(), "%.0f%%", percent));
            } else {
                tvPercent.setText("0%");
            }
        }
    }
}
