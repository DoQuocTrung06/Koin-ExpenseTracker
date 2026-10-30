package com.sharedfund.app.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillSplitHelper {

    public static class SplitItem {
        private final String userId;
        private final double amount;

        public SplitItem(String userId, double amount) {
            this.userId = userId;
            this.amount = amount;
        }

        public String getUserId() {
            return userId;
        }

        public double getAmount() {
            return amount;
        }
    }

    public static class SplitResult {
        private final double totalAmount;
        private final int memberCount;
        private final double baseAmountPerPerson;
        private final double remainder;
        private final List<SplitItem> splitItems;

        public SplitResult(double totalAmount, int memberCount, double baseAmountPerPerson,
                           double remainder, List<SplitItem> splitItems) {
            this.totalAmount = totalAmount;
            this.memberCount = memberCount;
            this.baseAmountPerPerson = baseAmountPerPerson;
            this.remainder = remainder;
            this.splitItems = splitItems;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public int getMemberCount() {
            return memberCount;
        }

        public double getBaseAmountPerPerson() {
            return baseAmountPerPerson;
        }

        public double getRemainder() {
            return remainder;
        }

        public List<SplitItem> getSplitItems() {
            return splitItems;
        }
    }

    public static SplitResult splitEqually(double totalAmount, List<String> memberUserIds) {
        if (memberUserIds == null || memberUserIds.isEmpty() || totalAmount <= 0) {
            return new SplitResult(totalAmount, 0, 0, 0, Collections.emptyList());
        }

        int count = memberUserIds.size();
        long totalCents = Math.round(totalAmount);
        long baseShare = totalCents / count;
        long rem = totalCents % count;

        List<SplitItem> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long personAmount = baseShare + (i < rem ? 1 : 0);
            items.add(new SplitItem(memberUserIds.get(i), (double) personAmount));
        }

        return new SplitResult(totalAmount, count, (double) baseShare, (double) rem, items);
    }
}
