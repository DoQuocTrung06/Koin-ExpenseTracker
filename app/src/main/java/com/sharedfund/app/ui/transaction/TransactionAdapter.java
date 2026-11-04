package com.sharedfund.app.ui.transaction;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Profile;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.profile.AvatarImageLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int MODE_MB_BANK = 0;
    public static final int MODE_MOMO_HISTORY = 1;
    public static final int MODE_RECENT_CARDS = 2;

    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_HEADER = 1;
    public static final int VIEW_TYPE_SOCIAL_CARD = 2;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public static class DisplayItem {
        public final int type;
        public final String headerDate;
        public final Transaction transaction;

        public DisplayItem(String headerDate) {
            this.type = VIEW_TYPE_HEADER;
            this.headerDate = headerDate;
            this.transaction = null;
        }

        public DisplayItem(Transaction transaction) {
            this.type = VIEW_TYPE_ITEM;
            this.headerDate = null;
            this.transaction = transaction;
        }

        public DisplayItem(Transaction transaction, int type) {
            this.type = type;
            this.headerDate = null;
            this.transaction = transaction;
        }
    }

    private final List<Transaction> rawTransactions = new ArrayList<>();
    private final List<DisplayItem> displayItems = new ArrayList<>();
    private final OnTransactionClickListener listener;
    private int displayMode = MODE_MB_BANK;

    public TransactionAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    public void setDisplayMode(int mode) {
        this.displayMode = mode;
        rebuildDisplayItems();
        notifyDataSetChanged();
    }

    public void setTransactions(List<Transaction> newTransactions) {
        this.rawTransactions.clear();
        if (newTransactions != null) {
            this.rawTransactions.addAll(newTransactions);
        }
        rebuildDisplayItems();
        notifyDataSetChanged();
    }

    public List<Transaction> getTransactions() {
        return new ArrayList<>(rawTransactions);
    }

    private void rebuildDisplayItems() {
        displayItems.clear();
        if (rawTransactions.isEmpty()) {
            return;
        }

        if (displayMode == MODE_RECENT_CARDS) {
            for (Transaction tx : rawTransactions) {
                displayItems.add(new DisplayItem(tx, VIEW_TYPE_SOCIAL_CARD));
            }
        } else if (displayMode == MODE_MOMO_HISTORY) {
            List<Transaction> sorted = new ArrayList<>(rawTransactions);
            Collections.sort(sorted, new Comparator<Transaction>() {
                @Override
                public int compare(Transaction o1, Transaction o2) {
                    Date d1 = parseDate(o1.getCreatedAt());
                    Date d2 = parseDate(o2.getCreatedAt());
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1);
                }
            });

            Map<String, List<Transaction>> groups = new LinkedHashMap<>();
            for (Transaction tx : sorted) {
                String dateKey = formatDateOnly(tx.getCreatedAt());
                if (!groups.containsKey(dateKey)) {
                    groups.put(dateKey, new ArrayList<>());
                }
                groups.get(dateKey).add(tx);
            }

            for (Map.Entry<String, List<Transaction>> entry : groups.entrySet()) {
                displayItems.add(new DisplayItem(entry.getKey()));
                for (Transaction tx : entry.getValue()) {
                    displayItems.add(new DisplayItem(tx));
                }
            }
        } else {
            for (Transaction tx : rawTransactions) {
                displayItems.add(new DisplayItem(tx));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == VIEW_TYPE_SOCIAL_CARD) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_social_card, parent, false);
            return new RecentSocialCardViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = displayItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.headerDate, position > 0);
        } else if (holder instanceof RecentSocialCardViewHolder && item.transaction != null) {
            ((RecentSocialCardViewHolder) holder).bind(item.transaction, listener);
        } else if (holder instanceof TransactionViewHolder && item.transaction != null) {
            ((TransactionViewHolder) holder).bind(item.transaction, listener, displayMode);
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    public static Date parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            String clean = raw.replace("Z", "");
            if (clean.contains("+")) clean = clean.substring(0, clean.indexOf("+"));
            if (clean.contains(".")) clean = clean.substring(0, clean.indexOf("."));
            if (clean.contains("T")) {
                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                iso.setTimeZone(TimeZone.getTimeZone("UTC"));
                return iso.parse(clean);
            } else {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                return df.parse(clean);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String formatDateTime(String raw) {
        Date d = parseDate(raw);
        if (d != null) {
            SimpleDateFormat out = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
            out.setTimeZone(TimeZone.getDefault());
            return out.format(d);
        }
        return raw != null ? raw : "";
    }

    public static String formatDateOnly(String raw) {
        Date d = parseDate(raw);
        if (d != null) {
            SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            out.setTimeZone(TimeZone.getDefault());
            return out.format(d);
        }
        return "Giao dịch gần đây";
    }

    public static String formatOnlyTime(String raw) {
        Date d = parseDate(raw);
        if (d != null) {
            SimpleDateFormat out = new SimpleDateFormat("HH:mm", Locale.getDefault());
            out.setTimeZone(TimeZone.getDefault());
            return out.format(d);
        }
        return "";
    }

    private static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "K";
        String clean = name.trim();
        String[] parts = clean.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final View viewDateDivider;
        private final TextView tvDateHeader;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            viewDateDivider = itemView.findViewById(R.id.viewDateDivider);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
        }

        public void bind(String headerDate, boolean showDivider) {
            if (viewDateDivider != null) {
                viewDateDivider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
            }
            if (tvDateHeader != null) {
                tvDateHeader.setText(headerDate);
            }
        }
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivTxAvatar;
        private final TextView tvTxInitials;
        private final TextView tvTxTime;
        private final TextView tvTxNote;
        private final TextView tvTxCreator;
        private final TextView tvTxAmount;
        private final TextView tvTxCategory;
        private final TextView tvTxStatus;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTxAvatar = itemView.findViewById(R.id.ivTxAvatar);
            tvTxInitials = itemView.findViewById(R.id.tvTxInitials);
            tvTxTime = itemView.findViewById(R.id.tvTxTime);
            tvTxNote = itemView.findViewById(R.id.tvTxNote);
            tvTxCreator = itemView.findViewById(R.id.tvTxCreator);
            tvTxAmount = itemView.findViewById(R.id.tvTxAmount);
            tvTxCategory = itemView.findViewById(R.id.tvTxCategory);
            tvTxStatus = itemView.findViewById(R.id.tvTxStatus);
        }

        public void bind(final Transaction tx, final OnTransactionClickListener listener, int mode) {
            String note = tx.getNote();
            if (note == null || note.trim().isEmpty()) {
                if ("Nạp ví SePay".equalsIgnoreCase(tx.getCategory())) {
                    note = "Nạp tiền vào ví Koin";
                } else if ("Rút tiền".equalsIgnoreCase(tx.getCategory())) {
                    note = "Rút tiền về ngân hàng";
                } else {
                    note = tx.getCategory() != null ? tx.getCategory() : "Giao dịch";
                }
            } else if (note.toUpperCase().startsWith("SF NAP")) {
                note = "Nạp tiền vào ví Koin";
            }
            tvTxNote.setText(note);

            String creator = tx.getCreatedBy();
            if (creator == null || creator.trim().isEmpty() || "Member".equalsIgnoreCase(creator)) {
                creator = tx.getRoomId() == null ? "Ví cá nhân" : "Thành viên";
            }

            if (mode == MODE_MOMO_HISTORY) {
                if (tvTxInitials != null) {
                    tvTxInitials.setVisibility(View.GONE);
                }
                if (ivTxAvatar != null) {
                    ivTxAvatar.setVisibility(View.VISIBLE);
                    ivTxAvatar.setBackgroundResource(R.drawable.bg_circle_border_light);
                    if (tx.isIncome()) {
                        ivTxAvatar.setImageResource(R.drawable.ic_fund_flow_in);
                    } else {
                        ivTxAvatar.setImageResource(R.drawable.ic_fund_flow_out);
                    }
                    ivTxAvatar.setColorFilter(null);
                    androidx.core.widget.ImageViewCompat.setImageTintList(ivTxAvatar, null);
                    int p = Math.round(7 * itemView.getResources().getDisplayMetrics().density);
                    ivTxAvatar.setPadding(p, p, p, p);
                    ivTxAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }

                String actionTitle = (creator == null || creator.trim().isEmpty() || "Member".equalsIgnoreCase(creator)) ? "Thành viên" : creator;
                if (tx.isIncome()) {
                    actionTitle += " góp quỹ";
                } else {
                    String rawNote = tx.getNote() != null ? tx.getNote().trim() : "";
                    String rawCat = tx.getCategory() != null ? tx.getCategory().trim() : "";
                    if (rawNote.contains("[STK:") || rawCat.toLowerCase().contains("chuyển đến") || rawCat.toLowerCase().contains("ngân hàng")) {
                        actionTitle += " chuyển tiền";
                    } else if (!rawCat.isEmpty() && !rawCat.equalsIgnoreCase("Giao dịch") && !rawCat.equalsIgnoreCase("Khác") && !rawCat.toLowerCase().contains("rút")) {
                        actionTitle += " chi " + rawCat.toLowerCase();
                    } else {
                        actionTitle += " rút quỹ";
                    }
                }
                tvTxNote.setText(actionTitle);

                if (tvTxTime != null) {
                    tvTxTime.setVisibility(View.VISIBLE);
                    String timeStr = formatOnlyTime(tx.getCreatedAt());
                    tvTxTime.setText(timeStr.isEmpty() ? "00:00" : timeStr);
                }

                String userNote = tx.getNote();
                if (userNote != null && !userNote.trim().isEmpty() && !userNote.equalsIgnoreCase(actionTitle)) {
                    tvTxCreator.setVisibility(View.VISIBLE);
                    tvTxCreator.setText(userNote);
                } else if (tx.getCategory() != null && !tx.getCategory().trim().isEmpty() && !tx.getCategory().equalsIgnoreCase("Giao dịch")) {
                    tvTxCreator.setVisibility(View.VISIBLE);
                    tvTxCreator.setText(tx.getCategory());
                } else {
                    tvTxCreator.setVisibility(View.GONE);
                }
            } else {
                if (ivTxAvatar != null) {
                    ivTxAvatar.setVisibility(View.VISIBLE);
                    ivTxAvatar.setBackgroundResource(R.drawable.bg_avatar_circle);
                    ivTxAvatar.clearColorFilter();
                    androidx.core.widget.ImageViewCompat.setImageTintList(ivTxAvatar, null);
                    ivTxAvatar.setPadding(0, 0, 0, 0);
                    ivTxAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivTxAvatar.setClipToOutline(true);
                }
                if (tvTxInitials != null) {
                    tvTxInitials.setVisibility(View.VISIBLE);
                    String initials = getInitials(creator);
                    tvTxInitials.setText(initials);
                    tvTxInitials.setBackgroundResource(R.drawable.bg_avatar_circle);
                }

                if (ivTxAvatar != null && tvTxInitials != null) {
                    String avatarUrl = RoomRepository.getInstance().getUserAvatar(tx.getUserId());
                    if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                        AvatarImageLoader.load(ivTxAvatar, tvTxInitials, avatarUrl);
                    } else {
                        ivTxAvatar.setImageDrawable(null);
                        tvTxInitials.setVisibility(View.VISIBLE);
                        if (tx.getUserId() != null && !tx.getUserId().trim().isEmpty()) {
                            RoomRepository.getInstance().loadUserProfile(tx.getUserId(), new RoomRepository.DataCallback<Profile>() {
                                @Override
                                public void onSuccess(Profile profile) {
                                    if (profile != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().trim().isEmpty()) {
                                        RoomRepository.getInstance().cacheUserAvatar(tx.getUserId(), profile.getAvatarUrl());
                                        ivTxAvatar.post(() -> AvatarImageLoader.load(ivTxAvatar, tvTxInitials, profile.getAvatarUrl()));
                                    }
                                }
                                @Override
                                public void onError(String message) {}
                            });
                        }
                    }
                }

                if (tvTxTime != null) {
                    tvTxTime.setVisibility(View.VISIBLE);
                    tvTxTime.setText(formatDateTime(tx.getCreatedAt()));
                }
                tvTxCreator.setText(creator);
            }

            tvTxAmount.setText(tx.getFormattedAmount());

            if (tx.isIncome()) {
                tvTxAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_income));
            } else {
                tvTxAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            }

            if (tvTxCategory != null) {
                tvTxCategory.setVisibility(View.GONE);
            }

            if (tvTxStatus != null) {
                if (tx.isPending()) {
                    tvTxStatus.setVisibility(View.VISIBLE);
                    tvTxStatus.setText("Chờ duyệt");
                    tvTxStatus.setBackgroundResource(R.drawable.bg_pending_badge);
                    tvTxStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_amber));
                } else if (tx.isRejected()) {
                    tvTxStatus.setVisibility(View.VISIBLE);
                    tvTxStatus.setText("Từ chối");
                    tvTxStatus.setBackgroundResource(R.drawable.bg_expense_badge);
                    tvTxStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_expense));
                } else {
                    tvTxStatus.setVisibility(View.GONE);
                }
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onTransactionClick(tx);
                    }
                }
            });
        }
    }

    public static class RecentSocialCardViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivSocialAvatar;
        private final TextView tvSocialInitials;
        private final ImageView ivSocialMiniBadge;
        private final TextView tvSocialCardName;
        private final TextView tvSocialCardTime;
        private final TextView tvSocialCardAmount;
        private final TextView tvSocialCardAction;
        private final TextView tvSocialCardNoteBubble;

        public RecentSocialCardViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSocialAvatar = itemView.findViewById(R.id.ivSocialAvatar);
            tvSocialInitials = itemView.findViewById(R.id.tvSocialInitials);
            ivSocialMiniBadge = itemView.findViewById(R.id.ivSocialMiniBadge);
            tvSocialCardName = itemView.findViewById(R.id.tvSocialCardName);
            tvSocialCardTime = itemView.findViewById(R.id.tvSocialCardTime);
            tvSocialCardAmount = itemView.findViewById(R.id.tvSocialCardAmount);
            tvSocialCardAction = itemView.findViewById(R.id.tvSocialCardAction);
            tvSocialCardNoteBubble = itemView.findViewById(R.id.tvSocialCardNoteBubble);
        }

        public void bind(final Transaction tx, final OnTransactionClickListener listener) {
            String currentUserId = null;
            try {
                currentUserId = UserSessionManager.getInstance(itemView.getContext()).getUserId();
            } catch (Exception ignored) {}

            boolean isSelf = (currentUserId != null && tx.getUserId() != null && currentUserId.equals(tx.getUserId()));
            String memberName = tx.getCreatedBy();
            if (memberName == null || memberName.trim().isEmpty() || "Member".equalsIgnoreCase(memberName)) {
                memberName = isSelf ? "Bạn" : "Thành viên";
            } else if (isSelf) {
                memberName = "Bạn";
            }
            tvSocialCardName.setText(memberName);

            tvSocialCardTime.setText(formatDateTime(tx.getCreatedAt()));

            tvSocialCardAmount.setText(tx.getFormattedAmount());
            if (tx.isIncome()) {
                tvSocialCardAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_income));
            } else {
                tvSocialCardAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            }

            if (ivSocialMiniBadge != null) {
                if (tx.isIncome()) {
                    ivSocialMiniBadge.setImageResource(R.drawable.ic_mini_arrow_in);
                    ivSocialMiniBadge.setBackgroundResource(R.drawable.bg_mini_badge_circle_income);
                } else {
                    ivSocialMiniBadge.setImageResource(R.drawable.ic_mini_arrow_out);
                    ivSocialMiniBadge.setBackgroundResource(R.drawable.bg_mini_badge_circle_expense);
                }
            }

            String avatarUrl = RoomRepository.getInstance().getUserAvatar(tx.getUserId());
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                AvatarImageLoader.load(ivSocialAvatar, tvSocialInitials, avatarUrl);
            } else {
                ivSocialAvatar.setImageDrawable(null);
                tvSocialInitials.setVisibility(View.VISIBLE);
                String initials = getInitials(tx.getCreatedBy());
                tvSocialInitials.setText(initials);
                tvSocialInitials.setBackgroundResource(R.drawable.bg_avatar_circle);

                if (tx.getUserId() != null && !tx.getUserId().trim().isEmpty()) {
                    RoomRepository.getInstance().loadUserProfile(tx.getUserId(), new RoomRepository.DataCallback<Profile>() {
                        @Override
                        public void onSuccess(Profile profile) {
                            if (profile != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().trim().isEmpty()) {
                                RoomRepository.getInstance().cacheUserAvatar(tx.getUserId(), profile.getAvatarUrl());
                                ivSocialAvatar.post(() -> AvatarImageLoader.load(ivSocialAvatar, tvSocialInitials, profile.getAvatarUrl()));
                            }
                        }
                        @Override
                        public void onError(String message) {}
                    });
                }
            }

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
                    } else {
                        displayNote = rawNote;
                    }
                    if (!bankInfo.isEmpty()) {
                        actionText = "Chuyển tiền (" + bankInfo + ")";
                    } else if (!rawCat.isEmpty() && !rawCat.equalsIgnoreCase("Giao dịch")) {
                        actionText = "Chuyển tiền " + rawCat;
                    } else {
                        actionText = "Chuyển tiền ngân hàng";
                    }
                } else if (rawCat.toLowerCase().contains("rút") || rawNote.toLowerCase().contains("rút")) {
                    actionText = "Rút quỹ về Ví Koin";
                    if (rawNote.equalsIgnoreCase("Rút quỹ về Ví Koin") || rawNote.equalsIgnoreCase("Rút tiền về Ví Koin") || rawNote.toUpperCase().startsWith("SF RUT")) {
                        displayNote = "";
                    } else {
                        displayNote = rawNote;
                    }
                } else if (!rawCat.isEmpty() && !rawCat.equalsIgnoreCase("Giao dịch") && !rawCat.equalsIgnoreCase("Khác")) {
                    actionText = "Chi tiêu " + rawCat;
                    displayNote = rawNote;
                } else {
                    actionText = "Chi tiêu quỹ";
                    displayNote = rawNote;
                }
            }

            tvSocialCardAction.setText(actionText);

            if (!displayNote.isEmpty() && !displayNote.equalsIgnoreCase(actionText) && !displayNote.equalsIgnoreCase("Góp quỹ từ Ví Koin") && !displayNote.equalsIgnoreCase("Rút tiền về Ví Koin") && !displayNote.equalsIgnoreCase("Rút quỹ về Ví Koin")) {
                tvSocialCardNoteBubble.setVisibility(View.VISIBLE);
                tvSocialCardNoteBubble.setText(displayNote);
            } else if (!rawCat.isEmpty() && !rawCat.equalsIgnoreCase("Giao dịch") && !rawCat.equalsIgnoreCase("Khác") && !rawCat.equalsIgnoreCase(actionText)) {
                tvSocialCardNoteBubble.setVisibility(View.VISIBLE);
                tvSocialCardNoteBubble.setText(rawCat);
            } else {
                tvSocialCardNoteBubble.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(tx);
                }
            });
        }
    }
}
