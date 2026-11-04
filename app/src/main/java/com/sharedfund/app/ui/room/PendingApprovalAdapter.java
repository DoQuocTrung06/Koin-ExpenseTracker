package com.sharedfund.app.ui.room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Profile;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.profile.AvatarImageLoader;
import com.sharedfund.app.ui.transaction.TransactionAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PendingApprovalAdapter extends RecyclerView.Adapter<PendingApprovalAdapter.ViewHolder> {

    public interface OnApprovalActionListener {
        void onApprove(Transaction tx, int position);
        void onReject(Transaction tx, int position);
    }

    private final List<Transaction> items = new ArrayList<>();
    private final String currentUserId;
    private final OnApprovalActionListener listener;

    public PendingApprovalAdapter(String currentUserId, OnApprovalActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setItems(List<Transaction> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size() - position);
        }
    }

    public int getItemCount() {
        return items.size();
    }

    public List<Transaction> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_approval, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction tx = items.get(position);
        holder.bind(tx, currentUserId, listener);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivPendingAvatar;
        private final TextView tvPendingInitials;
        private final ImageView ivPendingMiniBadge;
        private final TextView tvPendingCreator;
        private final TextView tvPendingTime;
        private final TextView tvPendingAmount;
        private final TextView tvPendingAction;
        private final TextView tvPendingNote;
        private final MaterialButton btnRejectExpense;
        private final MaterialButton btnApproveExpense;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPendingAvatar = itemView.findViewById(R.id.ivPendingAvatar);
            tvPendingInitials = itemView.findViewById(R.id.tvPendingInitials);
            ivPendingMiniBadge = itemView.findViewById(R.id.ivPendingMiniBadge);
            tvPendingCreator = itemView.findViewById(R.id.tvPendingCreator);
            tvPendingTime = itemView.findViewById(R.id.tvPendingTime);
            tvPendingAmount = itemView.findViewById(R.id.tvPendingAmount);
            tvPendingAction = itemView.findViewById(R.id.tvPendingAction);
            tvPendingNote = itemView.findViewById(R.id.tvPendingNote);
            btnRejectExpense = itemView.findViewById(R.id.btnRejectExpense);
            btnApproveExpense = itemView.findViewById(R.id.btnApproveExpense);
        }

        public void bind(final Transaction tx, String currentUserId, final OnApprovalActionListener listener) {
            String creator = tx.getCreatedBy();
            if (creator == null || creator.trim().isEmpty() || "Member".equalsIgnoreCase(creator)) {
                creator = "Thành viên";
            }
            tvPendingCreator.setText(creator);
            tvPendingTime.setText(TransactionAdapter.formatDateTime(tx.getCreatedAt()));

            tvPendingAmount.setText(tx.getFormattedAmount());
            tvPendingAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));

            String rawNote = tx.getNote() != null ? tx.getNote().trim() : "";
            String rawCat = tx.getCategory() != null ? tx.getCategory().trim() : "";
            String actionText = "";
            String displayNote = "";

            if (tx.isIncome()) {
                actionText = "Góp quỹ";
                if (rawNote.equalsIgnoreCase("Góp quỹ từ Ví Koin") || rawNote.equalsIgnoreCase("Góp quỹ") || rawNote.toUpperCase().startsWith("SF GOP")) {
                    displayNote = "";
                } else {
                    displayNote = rawNote;
                }
            } else {
                if (rawNote.contains("[STK:") || rawCat.toLowerCase().contains("chuyển đến") || rawCat.toLowerCase().contains("ngân hàng")) {
                    String bankInfo = "";
                    if (rawNote.contains("[STK:")) {
                        int idx = rawNote.indexOf("[STK:");
                        int endIdx = rawNote.indexOf("]", idx);
                        if (endIdx > idx) {
                            bankInfo = rawNote.substring(idx + 1, endIdx);
                            displayNote = rawNote.substring(0, idx).trim();
                        } else {
                            displayNote = rawNote;
                        }
                    }
                    actionText = "Chuyển tiền " + (bankInfo.isEmpty() ? "" : "(" + bankInfo + ")");
                } else if (!rawCat.isEmpty() && !rawCat.equalsIgnoreCase("Giao dịch") && !rawCat.equalsIgnoreCase("Khác") && !rawCat.toLowerCase().contains("rút")) {
                    actionText = "Chi tiêu " + rawCat;
                    displayNote = rawNote;
                } else if (rawCat.toLowerCase().contains("rút") || rawNote.toLowerCase().contains("rút")) {
                    actionText = "Rút tiền về Ví Koin";
                    displayNote = "";
                } else {
                    actionText = "Chi tiêu quỹ";
                    displayNote = rawNote;
                }
            }

            tvPendingAction.setText(actionText);

            if (!displayNote.isEmpty() && !displayNote.equalsIgnoreCase(actionText) && !displayNote.equalsIgnoreCase(rawCat) && !displayNote.equalsIgnoreCase("Góp quỹ từ Ví Koin") && !displayNote.equalsIgnoreCase("Rút tiền về Ví Koin") && !displayNote.equalsIgnoreCase("Rút quỹ về Ví Koin")) {
                tvPendingNote.setVisibility(View.VISIBLE);
                tvPendingNote.setText(displayNote);
            } else {
                tvPendingNote.setVisibility(View.GONE);
            }

            tvPendingInitials.setVisibility(View.VISIBLE);
            tvPendingInitials.setText(getInitials(creator));
            tvPendingInitials.setBackgroundResource(R.drawable.bg_avatar_circle);

            String avatarUrl = RoomRepository.getInstance().getUserAvatar(tx.getUserId());
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                AvatarImageLoader.load(ivPendingAvatar, tvPendingInitials, avatarUrl);
            } else {
                ivPendingAvatar.setImageDrawable(null);
                if (tx.getUserId() != null && !tx.getUserId().trim().isEmpty()) {
                    RoomRepository.getInstance().loadUserProfile(tx.getUserId(), new RoomRepository.DataCallback<Profile>() {
                        @Override
                        public void onSuccess(Profile profile) {
                            if (profile != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().trim().isEmpty()) {
                                RoomRepository.getInstance().cacheUserAvatar(tx.getUserId(), profile.getAvatarUrl());
                                ivPendingAvatar.post(() -> AvatarImageLoader.load(ivPendingAvatar, tvPendingInitials, profile.getAvatarUrl()));
                            }
                        }

                        @Override
                        public void onError(String message) {}
                    });
                }
            }

            boolean isSelfRequest = (currentUserId != null && tx.getUserId() != null && currentUserId.equals(tx.getUserId()));
            if (isSelfRequest) {
                btnApproveExpense.setEnabled(false);
                btnApproveExpense.setAlpha(0.45f);
                btnApproveExpense.setText("Không thể tự duyệt");
            } else {
                btnApproveExpense.setEnabled(true);
                btnApproveExpense.setAlpha(1.0f);
                btnApproveExpense.setText("Phê duyệt");
            }

            btnApproveExpense.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApprove(tx, getBindingAdapterPosition());
                }
            });

            btnRejectExpense.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(tx, getBindingAdapterPosition());
                }
            });
        }

        private String getInitials(String name) {
            if (name == null || name.trim().isEmpty()) return "K";
            String clean = name.trim();
            String[] parts = clean.split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
            }
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }
    }
}
