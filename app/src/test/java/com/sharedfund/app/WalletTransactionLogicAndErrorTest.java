package com.sharedfund.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.home.WalletTransactionsFragment;
import com.sharedfund.app.util.ErrorMessageHelper;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class WalletTransactionLogicAndErrorTest {

    @Test
    public void testErrorMessageHelper() {
        assertEquals("Số dư không đủ để thực hiện giao dịch.",
                ErrorMessageHelper.getFriendlyMessage("insufficient balance"));
        assertEquals("Số dư không đủ để thực hiện giao dịch.",
                ErrorMessageHelper.getFriendlyMessage("Error: Insufficient user balance for transfer"));

        assertEquals("Số dư quỹ không đủ để thực hiện giao dịch.",
                ErrorMessageHelper.getFriendlyMessage("insufficient room balance"));

        assertEquals("Hệ thống đang xử lý, vui lòng thử lại sau ít phút.",
                ErrorMessageHelper.getFriendlyMessage("Database error 42P17"));
        assertEquals("Hệ thống đang xử lý, vui lòng thử lại sau ít phút.",
                ErrorMessageHelper.getFriendlyMessage("PGRST201 error"));
        assertEquals("Hệ thống đang xử lý, vui lòng thử lại sau ít phút.",
                ErrorMessageHelper.getFriendlyMessage("PostgREST pgrst error"));

        assertEquals("Kết nối không ổn định, vui lòng kiểm tra mạng và thử lại.",
                ErrorMessageHelper.getFriendlyMessage("Network timeout connecting to server"));
        assertEquals("Kết nối không ổn định, vui lòng kiểm tra mạng và thử lại.",
                ErrorMessageHelper.getFriendlyMessage("Failed to connect to host"));

        assertEquals("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.",
                ErrorMessageHelper.getFriendlyMessage("HTTP 401 Unauthorized"));
        assertEquals("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.",
                ErrorMessageHelper.getFriendlyMessage("HTTP 403 Forbidden"));

        assertEquals("Lỗi máy chủ, vui lòng thử lại sau.",
                ErrorMessageHelper.getFriendlyMessage("HTTP 500 Internal server error"));

        assertEquals("Không tìm thấy thông tin yêu cầu.",
                ErrorMessageHelper.getFriendlyMessage("HTTP 404 Not found"));

        assertEquals("Đã xảy ra lỗi, vui lòng thử lại.",
                ErrorMessageHelper.getFriendlyMessage(null));
        assertEquals("Đã xảy ra lỗi, vui lòng thử lại.",
                ErrorMessageHelper.getFriendlyMessage("Some weird unknown error"));
    }

    @Test
    public void testIsWithdrawToWallet() {
        Transaction tx1 = new Transaction();
        tx1.setRoomId("room-1");
        tx1.setType(Transaction.TYPE_EXPENSE);
        tx1.setNote("Rút quỹ về Ví Koin");
        assertTrue(WalletTransactionsFragment.isWithdrawToWallet(tx1));

        Transaction tx2 = new Transaction();
        tx2.setRoomId("room-1");
        tx2.setType(Transaction.TYPE_EXPENSE);
        tx2.setNote("Rút tiền về Ví Koin");
        assertTrue(WalletTransactionsFragment.isWithdrawToWallet(tx2));

        Transaction tx3 = new Transaction();
        tx3.setRoomId("room-1");
        tx3.setType(Transaction.TYPE_EXPENSE);
        tx3.setNote("SF RUT 0123");
        assertTrue(WalletTransactionsFragment.isWithdrawToWallet(tx3));

        Transaction tx4 = new Transaction();
        tx4.setRoomId("room-1");
        tx4.setType(Transaction.TYPE_EXPENSE);
        tx4.setCategory("Khác");
        tx4.setNote("Rút về ví cá nhân");
        assertTrue(WalletTransactionsFragment.isWithdrawToWallet(tx4));

        Transaction tx5 = new Transaction();
        tx5.setRoomId("room-1");
        tx5.setType(Transaction.TYPE_EXPENSE);
        tx5.setCategory("Ăn uống");
        tx5.setNote("Tiền ăn lẩu hải sản");
        assertFalse(WalletTransactionsFragment.isWithdrawToWallet(tx5));

        Transaction tx6 = new Transaction();
        tx6.setRoomId("room-1");
        tx6.setType(Transaction.TYPE_EXPENSE);
        tx6.setNote("rut quy ve vi");
        assertTrue(WalletTransactionsFragment.isWithdrawToWallet(tx6));

        Transaction tx7 = new Transaction();
        tx7.setRoomId("room-1");
        tx7.setType(Transaction.TYPE_EXPENSE);
        tx7.setCategory("khac");
        tx7.setNote("rut vi");
        assertTrue(WalletTransactionsFragment.isWithdrawToWallet(tx7));
    }

    @Test
    public void testWalletTransactionFiltering() {
        Transaction depositWallet = new Transaction();
        depositWallet.setRoomId(null);
        depositWallet.setType(Transaction.TYPE_INCOME);
        assertTrue(WalletTransactionsFragment.isRetainedWalletTransaction(depositWallet));
        assertTrue(WalletTransactionsFragment.isWalletIncome(depositWallet));

        Transaction withdrawWalletToBank = new Transaction();
        withdrawWalletToBank.setRoomId(null);
        withdrawWalletToBank.setType(Transaction.TYPE_EXPENSE);
        assertTrue(WalletTransactionsFragment.isRetainedWalletTransaction(withdrawWalletToBank));
        assertFalse(WalletTransactionsFragment.isWalletIncome(withdrawWalletToBank));

        Transaction contributeToFund = new Transaction();
        contributeToFund.setRoomId("room-1");
        contributeToFund.setType(Transaction.TYPE_INCOME);
        assertTrue(WalletTransactionsFragment.isRetainedWalletTransaction(contributeToFund));
        assertFalse(WalletTransactionsFragment.isWalletIncome(contributeToFund));

        Transaction withdrawFundToWallet = new Transaction();
        withdrawFundToWallet.setRoomId("room-1");
        withdrawFundToWallet.setType(Transaction.TYPE_EXPENSE);
        withdrawFundToWallet.setStatus(Transaction.STATUS_APPROVED);
        withdrawFundToWallet.setNote("Rút quỹ về Ví Koin");
        assertTrue(WalletTransactionsFragment.isRetainedWalletTransaction(withdrawFundToWallet));

        Transaction withdrawFundPending = new Transaction();
        withdrawFundPending.setRoomId("room-1");
        withdrawFundPending.setType(Transaction.TYPE_EXPENSE);
        withdrawFundPending.setStatus(Transaction.STATUS_PENDING);
        withdrawFundPending.setNote("Rút quỹ về Ví Koin");
        assertFalse(WalletTransactionsFragment.isRetainedWalletTransaction(withdrawFundPending));
        assertTrue(WalletTransactionsFragment.isWalletIncome(withdrawFundToWallet));

        Transaction internalFundExpense = new Transaction();
        internalFundExpense.setRoomId("room-1");
        internalFundExpense.setType(Transaction.TYPE_EXPENSE);
        internalFundExpense.setCategory("Mua sắm");
        internalFundExpense.setNote("Mua đồ dùng chung phòng");
        assertFalse(WalletTransactionsFragment.isRetainedWalletTransaction(internalFundExpense));
    }

    @Test
    public void testCumulativeBalanceCalculation() {
        List<Transaction> transactions = new ArrayList<>();

        Transaction txNewest = new Transaction();
        txNewest.setRoomId("room-1");
        txNewest.setType(Transaction.TYPE_INCOME);
        txNewest.setAmount(200000.0);
        transactions.add(txNewest);

        Transaction txMiddle = new Transaction();
        txMiddle.setRoomId(null);
        txMiddle.setType(Transaction.TYPE_INCOME);
        txMiddle.setAmount(500000.0);
        transactions.add(txMiddle);

        Transaction txOldest = new Transaction();
        txOldest.setRoomId(null);
        txOldest.setType(Transaction.TYPE_EXPENSE);
        txOldest.setAmount(100000.0);
        transactions.add(txOldest);

        double currentWalletBalance = 1000000.0;
        double running = currentWalletBalance;

        for (Transaction tx : transactions) {
            tx.setBalanceAfter(Math.max(0, running));
            double amt = tx.getAmount();
            if (WalletTransactionsFragment.isWalletIncome(tx)) {
                running = running - amt;
            } else {
                running = running + amt;
            }
        }

        assertEquals(1000000.0, txNewest.getBalanceAfter(), 0.001);
        assertEquals(1200000.0, txMiddle.getBalanceAfter(), 0.001);
        assertEquals(700000.0, txOldest.getBalanceAfter(), 0.001);
        assertEquals(800000.0, running, 0.001);
    }

    @Test
    public void testRecentActivitiesIncludeRejectedTransactions() {
        Transaction txSuccess = new Transaction();
        txSuccess.setId("tx-1");
        txSuccess.setRoomId("room-1");
        txSuccess.setStatus(Transaction.STATUS_APPROVED);

        Transaction txRejected = new Transaction();
        txRejected.setId("tx-2");
        txRejected.setRoomId("room-1");
        txRejected.setStatus(Transaction.STATUS_REJECTED);

        Transaction txPending = new Transaction();
        txPending.setId("tx-3");
        txPending.setRoomId("room-1");
        txPending.setStatus(Transaction.STATUS_PENDING);

        List<Transaction> all = new ArrayList<>();
        all.add(txSuccess);
        all.add(txRejected);
        all.add(txPending);

        List<Transaction> confirmedHistory = new ArrayList<>();
        List<Transaction> recent = new ArrayList<>();
        List<Transaction> pending = new ArrayList<>();

        for (Transaction tx : all) {
            if (tx.isPending()) {
                pending.add(tx);
            } else {
                if (!Transaction.STATUS_REJECTED.equalsIgnoreCase(tx.getStatus())) {
                    confirmedHistory.add(tx);
                }
                if (recent.size() < 5) {
                    recent.add(tx);
                }
            }
        }

        assertEquals(1, confirmedHistory.size());
        assertEquals("tx-1", confirmedHistory.get(0).getId());

        assertEquals(2, recent.size());
        assertEquals("tx-1", recent.get(0).getId());
        assertEquals("tx-2", recent.get(1).getId());

        assertEquals(1, pending.size());
        assertEquals("tx-3", pending.get(0).getId());
    }
}
