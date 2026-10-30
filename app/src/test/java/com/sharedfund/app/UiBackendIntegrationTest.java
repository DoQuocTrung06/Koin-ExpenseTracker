package com.sharedfund.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.home.HomeViewModel;
import com.sharedfund.app.ui.room.RoomViewModel;
import com.sharedfund.app.ui.statistics.StatisticsViewModel;
import com.sharedfund.app.ui.transaction.TransactionViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class UiBackendIntegrationTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RoomRepository repository;

    @Before
    public void setUp() {
        repository = RoomRepository.getInstance();
        repository.setCurrentUser("user-001", "Bạn");
    }

    @Test
    public void testHomeViewModelSyncWithBackend() {
        HomeViewModel homeViewModel = new HomeViewModel();

        List<Room> initialRooms = homeViewModel.getRooms().getValue();
        assertNotNull("HomeViewModel must receive initial rooms", initialRooms);
        assertTrue("Initial rooms must contain mock rooms", initialRooms.size() >= 3);

        Double totalBalance = homeViewModel.getTotalBalance().getValue();
        assertNotNull("Total balance must not be null", totalBalance);

        int countBefore = initialRooms.size();
        homeViewModel.createRoom("Quỹ Hội Đồng Hương", "Gom tiền hội họp bạn bè cuối năm");

        List<Room> updatedRooms = homeViewModel.getRooms().getValue();
        assertNotNull(updatedRooms);
        assertEquals("Room count must increase by 1", countBefore + 1, updatedRooms.size());

        Room created = updatedRooms.get(0);
        assertEquals("Quỹ Hội Đồng Hương", created.getName());
        assertNotNull("Join code must be generated", created.getJoinCode());

        homeViewModel.joinRoom(created.getJoinCode());
        Room found = repository.getRoomSync(created.getId());
        assertNotNull(found);
    }

    @Test
    public void testRoomViewModelAndTransactionFlow() {
        RoomViewModel roomViewModel = new RoomViewModel();
        TransactionViewModel txViewModel = new TransactionViewModel();

        String roomId = "room-1";
        roomViewModel.setRoomId(roomId);
        txViewModel.setRoomId(roomId);

        Room room = roomViewModel.getRoom().getValue();
        assertNotNull(room);
        assertEquals("Quỹ Du Lịch Đà Lạt 2026", room.getName());

        double initialBalance = room.getBalance();
        double initialIncome = roomViewModel.getTotalIncome().getValue();
        double initialExpense = roomViewModel.getTotalExpense().getValue();

        boolean incomeAdded = txViewModel.submitTransaction(
                roomId,
                Transaction.TYPE_INCOME,
                "500000",
                "Đóng quỹ",
                "Tuấn Anh đóng thêm tiền vé máy bay",
                "Tuấn Anh"
        );
        assertTrue("Transaction income should be valid and submitted", incomeAdded);

        roomViewModel.refresh();
        assertEquals(initialIncome + 500000.0, roomViewModel.getTotalIncome().getValue(), 0.001);

        boolean expenseAdded = txViewModel.submitTransaction(
                roomId,
                Transaction.TYPE_EXPENSE,
                "200000",
                "Ăn uống",
                "Mua bánh tráng nướng Đà Lạt",
                "Bạn"
        );
        assertTrue("Transaction expense should be valid and submitted", expenseAdded);

        roomViewModel.refresh();
        assertEquals(initialExpense + 200000.0, roomViewModel.getTotalExpense().getValue(), 0.001);

        double expectedBalance = initialBalance + 300000.0;
        assertEquals(expectedBalance, roomViewModel.getRoom().getValue().getBalance(), 0.001);

        List<Transaction> recent = roomViewModel.getRecentTransactions().getValue();
        assertNotNull(recent);
        assertTrue(recent.size() > 0);
        assertEquals("Mua bánh tráng nướng Đà Lạt", recent.get(0).getNote());
    }

    @Test
    public void testTransactionValidationErrors() {
        TransactionViewModel txViewModel = new TransactionViewModel();

        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "", "Ăn uống", "Note", "Bạn"));

        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "0", "Ăn uống", "Note", "Bạn"));
        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "-50000", "Ăn uống", "Note", "Bạn"));

        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "50000", "Ăn uống", "", "Bạn"));

        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "50000", "", "Note", "Bạn"));
    }

    @Test
    public void testStatisticsViewModelSync() {
        StatisticsViewModel statViewModel = new StatisticsViewModel();
        String roomId = "room-2";
        statViewModel.setRoomId(roomId);

        Double income = statViewModel.getTotalIncome().getValue();
        Double expense = statViewModel.getTotalExpense().getValue();
        Double net = statViewModel.getNetBalance().getValue();

        assertNotNull(income);
        assertNotNull(expense);
        assertNotNull(net);
        assertEquals(income - expense, net, 0.001);

        Map<String, Double> categoryStats = statViewModel.getCategoryStats().getValue();
        assertNotNull(categoryStats);
        assertTrue(categoryStats.containsKey("Tiền nhà") || categoryStats.containsKey("Điện nước"));

        List<Transaction> topExpenses = statViewModel.getTopExpenses().getValue();
        assertNotNull(topExpenses);
        assertTrue(topExpenses.size() <= 5);

        for (int i = 0; i < topExpenses.size() - 1; i++) {
            assertTrue("Top expenses must be sorted descending",
                    topExpenses.get(i).getAmount() >= topExpenses.get(i + 1).getAmount());
        }
    }

    @Test
    public void testTransactionFiltering() {
        TransactionViewModel txViewModel = new TransactionViewModel();
        String roomId = "room-1";
        txViewModel.setRoomId(roomId);

        txViewModel.setFilter(TransactionViewModel.FilterType.ALL);
        List<Transaction> all = txViewModel.getTransactions().getValue();
        assertNotNull(all);

        txViewModel.setFilter(TransactionViewModel.FilterType.INCOME);
        List<Transaction> incomes = txViewModel.getTransactions().getValue();
        assertNotNull(incomes);
        for (Transaction tx : incomes) {
            assertTrue(tx.isIncome());
        }

        txViewModel.setFilter(TransactionViewModel.FilterType.EXPENSE);
        List<Transaction> expenses = txViewModel.getTransactions().getValue();
        assertNotNull(expenses);
        for (Transaction tx : expenses) {
            assertTrue(tx.isExpense());
        }

        assertEquals(all.size(), incomes.size() + expenses.size());
    }

    @Test
    public void testNewUserRegistrationStartsEmptyAndCanJoinOrReceiveFunds() {
        String newUserId = "user-test-new-" + System.currentTimeMillis();
        String newUserName = "Nguyễn Văn Test";
        repository.registerNewUser(newUserId, newUserName);

        HomeViewModel homeViewModel = new HomeViewModel();

        List<Room> newRooms = homeViewModel.getRooms().getValue();
        assertNotNull(newRooms);
        assertEquals("New user must have 0 rooms initially", 0, newRooms.size());

        Double newTotalBalance = homeViewModel.getTotalBalance().getValue();
        assertNotNull(newTotalBalance);
        assertEquals("New user balance must be 0", 0.0, newTotalBalance, 0.001);

        homeViewModel.joinRoom("DALAT6");
        List<Room> roomsAfterJoin = homeViewModel.getRooms().getValue();
        assertNotNull(roomsAfterJoin);
        assertEquals("Room count must be 1 after joining via invite code", 1, roomsAfterJoin.size());
        assertEquals("DALAT6", roomsAfterJoin.get(0).getJoinCode());
        assertTrue("New user name should be in room members",
                roomsAfterJoin.get(0).getMembers().contains(newUserName));

        homeViewModel.createRoom("Quỹ Tiệc Đầy Tháng", "Quỹ riêng của gia đình");
        List<Room> roomsAfterCreate = homeViewModel.getRooms().getValue();
        assertNotNull(roomsAfterCreate);
        assertEquals("Room count must be 2 after creating a new room", 2, roomsAfterCreate.size());

        repository.setCurrentUser("user-001", "Bạn");
    }
}
