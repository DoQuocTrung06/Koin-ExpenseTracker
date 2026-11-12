package com.sharedfund.app.data;

import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.util.List;

public class RoomStatisticsManager {

    private static volatile RoomStatisticsManager instance;

    private RoomStatisticsManager() {}

    public static RoomStatisticsManager getInstance() {
        if (instance == null) {
            synchronized (RoomStatisticsManager.class) {
                if (instance == null) {
                    instance = new RoomStatisticsManager();
                }
            }
        }
        return instance;
    }

    public boolean affectsBalance(Transaction transaction) {
        if (transaction == null) return false;
        String status = transaction.getStatus();
        return status == null || status.trim().isEmpty()
                || "APPROVED".equalsIgnoreCase(status)
                || "SUCCESS".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status)
                || "CONFIRMED".equalsIgnoreCase(status)
                || "PAID".equalsIgnoreCase(status);
    }

    public double calculateRoomTotalIncome(String roomId, List<Transaction> transactionList) {
        double sum = 0;
        if (roomId == null || transactionList == null) return sum;
        for (Transaction tx : transactionList) {
            if (tx == null) continue;
            if (roomId.equals(tx.getRoomId()) && affectsBalance(tx) && tx.isIncome()) {
                sum += tx.getAmount();
            }
        }
        return sum;
    }

    public double calculateRoomTotalExpense(String roomId, List<Transaction> transactionList) {
        double sum = 0;
        if (roomId == null || transactionList == null) return sum;
        for (Transaction tx : transactionList) {
            if (tx == null) continue;
            if (roomId.equals(tx.getRoomId()) && affectsBalance(tx) && tx.isExpense()) {
                sum += tx.getAmount();
            }
        }
        return sum;
    }

    public double recalculateBalances(List<Room> roomList, List<Transaction> transactionList, List<Room> userRooms) {
        if (roomList != null) {
            for (Room r : roomList) {
                if (r == null) continue;
                double income = 0;
                double expense = 0;
                if (transactionList != null) {
                    for (Transaction tx : transactionList) {
                        if (tx == null) continue;
                        if (r.getId().equals(tx.getRoomId()) && affectsBalance(tx)) {
                            if (tx.isIncome()) {
                                income += tx.getAmount();
                            } else if (tx.isExpense() && (tx.isApproved() || tx.getStatus() == null)) {
                                expense += tx.getAmount();
                            }
                        }
                    }
                }
                double balance = income - expense;
                r.setBalance(balance);
            }
        }

        double userTotal = 0;
        if (userRooms != null) {
            for (Room r : userRooms) {
                if (r != null) {
                    userTotal += r.getBalance();
                }
            }
        }
        return userTotal;
    }
}
