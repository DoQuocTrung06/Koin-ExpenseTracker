package com.sharedfund.app.ui.room;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sharedfund.app.model.Room;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class RoomHeaderViewBinder {

    private final TextView tvHeaderRoomName;
    private final TextView tvDetailBalance;
    private final TextView tvDetailJoinCode;
    private final View layoutHomeTarget;
    private final TextView tvHomeTargetText;
    private final ProgressBar pbHomeTarget;
    private final TextView tvHistoryNetBalance;
    private final TextView tvHistoryTotalIncome;
    private final TextView tvHistoryTotalExpense;

    private final View cardSettingsPrivacy;
    private final View cardSettingsRoomInfo;
    private final View cardSettingsCloseFund;
    private final View cardMemberManagement;
    private final TextView tvMemberManagementHeader;
    private final View btnMemberDelegation;
    private final View dividerMemberDelegation;
    private final View btnMemberJoinRequests;
    private final View cardSettingsMemberActions;
    private final View btnHeaderQuickEdit;
    private final View btnChangeRoomBackground;

    private final DecimalFormat df;

    public RoomHeaderViewBinder(
            TextView tvHeaderRoomName,
            TextView tvDetailBalance,
            TextView tvDetailJoinCode,
            View layoutHomeTarget,
            TextView tvHomeTargetText,
            ProgressBar pbHomeTarget,
            TextView tvHistoryNetBalance,
            TextView tvHistoryTotalIncome,
            TextView tvHistoryTotalExpense,
            View cardSettingsPrivacy,
            View cardSettingsRoomInfo,
            View cardSettingsCloseFund,
            View cardMemberManagement,
            TextView tvMemberManagementHeader,
            View btnMemberDelegation,
            View dividerMemberDelegation,
            View btnMemberJoinRequests,
            View cardSettingsMemberActions,
            View btnHeaderQuickEdit,
            View btnChangeRoomBackground,
            DecimalFormat df
    ) {
        this.tvHeaderRoomName = tvHeaderRoomName;
        this.tvDetailBalance = tvDetailBalance;
        this.tvDetailJoinCode = tvDetailJoinCode;
        this.layoutHomeTarget = layoutHomeTarget;
        this.tvHomeTargetText = tvHomeTargetText;
        this.pbHomeTarget = pbHomeTarget;
        this.tvHistoryNetBalance = tvHistoryNetBalance;
        this.tvHistoryTotalIncome = tvHistoryTotalIncome;
        this.tvHistoryTotalExpense = tvHistoryTotalExpense;
        this.cardSettingsPrivacy = cardSettingsPrivacy;
        this.cardSettingsRoomInfo = cardSettingsRoomInfo;
        this.cardSettingsCloseFund = cardSettingsCloseFund;
        this.cardMemberManagement = cardMemberManagement;
        this.tvMemberManagementHeader = tvMemberManagementHeader;
        this.btnMemberDelegation = btnMemberDelegation;
        this.dividerMemberDelegation = dividerMemberDelegation;
        this.btnMemberJoinRequests = btnMemberJoinRequests;
        this.cardSettingsMemberActions = cardSettingsMemberActions;
        this.btnHeaderQuickEdit = btnHeaderQuickEdit;
        this.btnChangeRoomBackground = btnChangeRoomBackground;
        this.df = df != null ? df : new DecimalFormat("#,##0");
    }

    public void bindRoomHeader(Room room) {
        if (room == null) return;
        if (tvHeaderRoomName != null) tvHeaderRoomName.setText(room.getName());
        if (tvDetailBalance != null) tvDetailBalance.setText(room.getFormattedBalance());
        if (tvDetailJoinCode != null) {
            tvDetailJoinCode.setText("Mã: " + (room.getJoinCode() != null ? room.getJoinCode() : "—"));
        }

        if (layoutHomeTarget != null && tvHomeTargetText != null && pbHomeTarget != null) {
            if (room.getTargetAmount() > 0) {
                layoutHomeTarget.setVisibility(View.VISIBLE);
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                symbols.setGroupingSeparator('.');
                DecimalFormat customDf = new DecimalFormat("#,##0", symbols);
                double percent = (room.getBalance() / room.getTargetAmount()) * 100;
                int progress = (int) Math.min(100, Math.max(0, percent));
                tvHomeTargetText.setText("Mục tiêu: " + customDf.format(room.getTargetAmount()) + " ₫ (" + progress + "%)");
                pbHomeTarget.setProgress(progress);
            } else {
                layoutHomeTarget.setVisibility(View.GONE);
            }
        }
    }

    public void updateNetBalance(Double income, Double expense) {
        double inc = income != null ? income : 0.0;
        double exp = expense != null ? expense : 0.0;
        double net = inc - exp;
        String prefix = net >= 0 ? "+" : "";

        if (tvHistoryNetBalance != null) {
            tvHistoryNetBalance.setText(prefix + df.format(net).replace(',', '.') + " ₫");
        }
        if (tvHistoryTotalIncome != null) {
            tvHistoryTotalIncome.setText("Tổng thu: +" + df.format(inc).replace(',', '.') + " ₫");
        }
        if (tvHistoryTotalExpense != null) {
            tvHistoryTotalExpense.setText("-" + df.format(exp).replace(',', '.') + " ₫");
        }
    }

    public void applyRoleBasedUi(boolean isOwner, boolean isTreasurer) {
        if (cardSettingsPrivacy != null) cardSettingsPrivacy.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        if (cardSettingsRoomInfo != null) cardSettingsRoomInfo.setVisibility(View.VISIBLE);
        if (cardSettingsCloseFund != null) cardSettingsCloseFund.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        if (isOwner) {
            if (cardMemberManagement != null) cardMemberManagement.setVisibility(View.VISIBLE);
            if (tvMemberManagementHeader != null) tvMemberManagementHeader.setVisibility(View.VISIBLE);
            if (btnMemberDelegation != null) btnMemberDelegation.setVisibility(View.VISIBLE);
            if (dividerMemberDelegation != null) dividerMemberDelegation.setVisibility(View.VISIBLE);
            if (btnMemberJoinRequests != null) btnMemberJoinRequests.setVisibility(View.VISIBLE);
        } else if (isTreasurer) {
            if (cardMemberManagement != null) cardMemberManagement.setVisibility(View.VISIBLE);
            if (tvMemberManagementHeader != null) tvMemberManagementHeader.setVisibility(View.VISIBLE);
            if (btnMemberDelegation != null) btnMemberDelegation.setVisibility(View.GONE);
            if (dividerMemberDelegation != null) dividerMemberDelegation.setVisibility(View.GONE);
            if (btnMemberJoinRequests != null) btnMemberJoinRequests.setVisibility(View.VISIBLE);
        } else {
            if (cardMemberManagement != null) cardMemberManagement.setVisibility(View.GONE);
            if (tvMemberManagementHeader != null) tvMemberManagementHeader.setVisibility(View.GONE);
            if (btnMemberDelegation != null) btnMemberDelegation.setVisibility(View.GONE);
            if (dividerMemberDelegation != null) dividerMemberDelegation.setVisibility(View.GONE);
            if (btnMemberJoinRequests != null) btnMemberJoinRequests.setVisibility(View.GONE);
        }

        if (cardSettingsMemberActions != null) cardSettingsMemberActions.setVisibility(!isOwner ? View.VISIBLE : View.GONE);
        if (btnHeaderQuickEdit != null) {
            btnHeaderQuickEdit.setVisibility((isOwner || isTreasurer) ? View.VISIBLE : View.GONE);
        }
        if (btnChangeRoomBackground != null) {
            btnChangeRoomBackground.setVisibility(View.VISIBLE);
        }
    }
}
