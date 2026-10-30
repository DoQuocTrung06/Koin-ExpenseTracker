package com.sharedfund.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.SupabaseConfig;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class SupabaseApiTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void testRoomCreationAndBalanceCalculations() {
        RoomRepository repository = RoomRepository.getInstance();
        assertNotNull("Repository instance should not be null", repository);

        String roomName = "Phòng Trọ 524h0132";
        String roomDesc = "Quỹ test kiểm thử tự động";
        Room newRoom = repository.createRoom(roomName, roomDesc);

        assertNotNull("Created room must not be null", newRoom);
        assertNotNull("Room id must be generated", newRoom.getId());
        assertNotNull("Join code must be generated", newRoom.getJoinCode());
        assertEquals("Room name matches input", roomName, newRoom.getName());

        Room foundRoom = repository.getRoomSync(newRoom.getId());
        assertNotNull("Should find created room by ID", foundRoom);
        assertEquals(roomName, foundRoom.getName());

        Room joinedRoom = repository.joinRoomByCode(newRoom.getJoinCode());
        assertNotNull("Should find room by join code", joinedRoom);
        assertEquals(newRoom.getId(), joinedRoom.getId());

        Transaction incomeTx = new Transaction(newRoom.getId(), "Bạn", 500000.0,
                Transaction.TYPE_INCOME, "Đóng quỹ", "Đóng quỹ tháng 10");
        boolean incomeAdded = repository.addTransaction(incomeTx);
        assertTrue("Income transaction should be added successfully", incomeAdded);

        Transaction expenseTx = new Transaction(newRoom.getId(), "Bạn", 200000.0,
                Transaction.TYPE_EXPENSE, "Ăn uống", "Tiệc pizza nhóm");
        boolean expenseAdded = repository.addTransaction(expenseTx);
        assertTrue("Expense transaction should be added successfully", expenseAdded);

        double totalIncome = repository.getRoomTotalIncome(newRoom.getId());
        double totalExpense = repository.getRoomTotalExpense(newRoom.getId());
        assertEquals(500000.0, totalIncome, 0.001);
        assertEquals(200000.0, totalExpense, 0.001);

        assertEquals(300000.0, newRoom.getBalance(), 0.001);
        assertTrue("Formatted balance should contain 300.000", newRoom.getFormattedBalance().contains("300.000"));

        Map<String, Double> categoryMap = repository.getCategoryBreakdown(newRoom.getId());
        assertNotNull(categoryMap);
        assertTrue(categoryMap.containsKey("Ăn uống"));
        assertEquals(200000.0, categoryMap.get("Ăn uống"), 0.001);

        List<Transaction> txList = repository.getTransactionsForRoom(newRoom.getId());
        assertTrue(txList.size() >= 2);
    }

    @Test
    public void testModelFormattingAndSerialization() {
        Transaction txIncome = new Transaction("tx-1", "room-1", 1500000.0, Transaction.TYPE_INCOME,
                "Đóng quỹ", "Nạp quỹ", "Bạn", "27/10/2026 10:00");
        assertEquals("+1.500.000 ₫", txIncome.getFormattedAmount());
        assertTrue(txIncome.isIncome());

        Transaction txExpense = new Transaction("tx-2", "room-1", 450000.0, Transaction.TYPE_EXPENSE,
                "Ăn uống", "Ăn tối", "Bạn", "27/10/2026 19:00");
        assertEquals("-450.000 ₫", txExpense.getFormattedAmount());
        assertTrue(txExpense.isExpense());

        Room room = new Room("room-1", "Quỹ Đà Lạt", "DALAT6", 2850000.0,
                "Du lịch", 5, null, "20/10/2026");
        assertEquals("2.850.000 ₫", room.getFormattedBalance());

        Gson gson = new Gson();
        String json = gson.toJson(txIncome);
        Transaction parsedTx = gson.fromJson(json, Transaction.class);
        assertNotNull(parsedTx);
        assertEquals(txIncome.getId(), parsedTx.getId());
        assertEquals(txIncome.getAmount(), parsedTx.getAmount(), 0.001);
    }

    @Test
    public void testSupabaseRealtimeIntegrationHooks() {
        RoomRepository repository = RoomRepository.getInstance();

        Transaction remoteTx = new Transaction("tx-remote-99", "room-1", 88000.0,
                Transaction.TYPE_EXPENSE, "Ăn uống", "Remote realtime test", "Huy", "27/10/2026 15:30");
        repository.onRemoteTransactionReceived(remoteTx);

        List<Transaction> roomTx = repository.getTransactionsForRoom("room-1");
        boolean found = false;
        for (Transaction tx : roomTx) {
            if ("tx-remote-99".equals(tx.getId())) {
                found = true;
                break;
            }
        }
        assertTrue("Remote realtime transaction should be incorporated into repository", found);

        assertNotNull(SupabaseConfig.SUPABASE_URL);
        assertNotNull(SupabaseConfig.SUPABASE_ANON_KEY);
        assertNotNull(SupabaseConfig.REST_URL);
        assertNotNull(SupabaseConfig.REALTIME_WS_URL);
    }

    @Test
    public void testTransactionRepositoryCreation() {
        TransactionRepository txRepo = new TransactionRepository();
        assertNotNull("TransactionRepository should instantiate without error", txRepo);
    }
}
