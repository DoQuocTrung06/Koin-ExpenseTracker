package com.sharedfund.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.SupabaseConfig;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class SupabaseApiTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        RoomRepository.getInstance().setCurrentUser("user-001", "Bạn");
    }

    @After
    public void tearDown() {
        RoomRepository.getInstance().setCurrentUser("user-001", "Bạn");
    }

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

    @Test
    public void testSupabaseRealtimeMultiTableHooks() {
        RoomRepository repository = RoomRepository.getInstance();

        Room testRoom = new Room("room-rt-1", "Test Room Realtime", "RT123", 100000.0, "Desc", 1, null);
        testRoom.setCurrentUserRole("OWNER");
        repository.onRemoteRoomReceived(testRoom);

        Room fetched = repository.getRoomSync("room-rt-1");
        assertNotNull(fetched);
        assertEquals("Test Room Realtime", fetched.getName());

        testRoom.setName("Test Room Updated");
        repository.onRemoteRoomReceived(testRoom);
        assertEquals("Test Room Updated", repository.getRoomSync("room-rt-1").getName());

        Transaction tx = new Transaction("tx-rt-1", "room-rt-1", 50000.0, Transaction.TYPE_INCOME, "Góp quỹ", "Realtime income", "User 2", "03/11/2026 10:00");
        repository.onRemoteTransactionReceived(tx);
        assertTrue(repository.getTransactionsForRoom("room-rt-1").size() > 0);

        repository.onRemoteTransactionDeleted("tx-rt-1");
        boolean txDeleted = true;
        for (Transaction t : repository.getTransactionsForRoom("room-rt-1")) {
            if ("tx-rt-1".equals(t.getId())) {
                txDeleted = false;
                break;
            }
        }
        assertTrue(txDeleted);

        repository.onRemoteRoomDeleted("room-rt-1");
        assertNull(repository.getRoomSync("room-rt-1"));
    }

    @Test
    public void testRoleBasedAccessControlRestrictions() {
        RoomRepository repository = RoomRepository.getInstance();

        Room ownerRoom = new Room("room-rbac-owner", "Owner Room", "OWN01", 0.0, "Desc", 1, null);
        ownerRoom.setCurrentUserRole("OWNER");
        repository.onRemoteRoomReceived(ownerRoom);

        final boolean[] leaveErrorCalled = {false};
        repository.leaveRoom("room-rbac-owner", "user-owner", new RoomRepository.DataCallback<Boolean>() {
            @Override public void onSuccess(Boolean value) {
            }
            @Override public void onError(String message) {
                leaveErrorCalled[0] = true;
            }
        });
        assertTrue(leaveErrorCalled[0]);

        Room memberRoom = new Room("room-rbac-member", "Member Room", "MEM01", 0.0, "Desc", 2, null);
        memberRoom.setCurrentUserRole("MEMBER");
        memberRoom.setCreatedBy("another-owner-id");
        repository.onRemoteRoomReceived(memberRoom);

        final boolean[] deleteErrorCalled = {false};
        repository.deleteRoom("room-rbac-member", "user-member-non-owner", new RoomRepository.DataCallback<Boolean>() {
            @Override public void onSuccess(Boolean value) {
            }
            @Override public void onError(String message) {
                deleteErrorCalled[0] = true;
            }
        });
        assertTrue(deleteErrorCalled[0]);

        assertTrue(repository.isCurrentUserAdmin("room-rbac-owner"));
        assertFalse(repository.isCurrentUserAdmin("room-rbac-member"));
        assertFalse(repository.isCurrentUserAdmin("room-non-existent"));

        Room treasurerRoom = new Room("room-rbac-treasurer", "Treasurer Room", "TRS01", 0.0, "Desc", 2, null);
        treasurerRoom.setCurrentUserRole("TREASURER");
        repository.onRemoteRoomReceived(treasurerRoom);
        assertTrue(repository.isCurrentUserAdmin("room-rbac-treasurer"));

        Room adminRoom = new Room("room-rbac-admin", "Admin Room", "ADM01", 0.0, "Desc", 2, null);
        adminRoom.setCurrentUserRole("ADMIN");
        repository.onRemoteRoomReceived(adminRoom);
        assertTrue(repository.isCurrentUserAdmin("room-rbac-admin"));

        Room creatorRoom = new Room("room-rbac-creator", "Creator Room", "CRE01", 0.0, "Desc", 1, null);
        creatorRoom.setCreatedBy("user-001");
        creatorRoom.setCurrentUserRole(null);
        repository.onRemoteRoomReceived(creatorRoom);
        assertTrue(repository.isCurrentUserAdmin("room-rbac-creator"));
    }

    @Test
    public void testSupabaseRealtimeMemberMultiUserEdgeCases() {
        RoomRepository repository = RoomRepository.getInstance();

        repository.onRemoteMemberChanged(null, "DELETE", null);

        Room testRoom = new Room("room-multi-user", "Multi User Room", "MU123", 50000.0, "Desc", 1, null);
        testRoom.setCurrentUserRole("OWNER");
        repository.onRemoteRoomReceived(testRoom);

        repository.onRemoteMemberChanged("room-multi-user", "INSERT", "user-2");
        repository.onRemoteMemberChanged("room-multi-user", "INSERT", "user-3");
        assertNotNull(repository.getRoomSync("room-multi-user"));

        repository.onRemoteMemberChanged("room-multi-user", "DELETE", "user-2");
        repository.onRemoteMemberChanged("room-multi-user", "DELETE", null);
        assertNotNull(repository.getRoomSync("room-multi-user"));

        repository.onRemoteRoomDeleted("room-multi-user");
        assertNull(repository.getRoomSync("room-multi-user"));
    }

    @Test
    public void testStrangerRoomAndTransactionSecurityFiltering() {
        RoomRepository repository = RoomRepository.getInstance();
        repository.setCurrentUser("user-logged-in-1", "Test User");

        Room strangerRoom = new Room("room-stranger-99", "Quỹ Người Lạ", "STR99", 500000.0, "Mô tả", 1, null);
        strangerRoom.setCreatedBy("another-stranger-id");
        repository.onRemoteRoomReceived(strangerRoom);

        assertNull("Stranger room should never be added to room list", repository.getRoomSync("room-stranger-99"));
        assertTrue("Stranger room should not be in user rooms", !repository.getUserRoomIds().contains("room-stranger-99"));

        Room ownRoom = new Room("room-own-99", "Quỹ Của Tôi", "OWN99", 0.0, "Mô tả", 1, null);
        ownRoom.setCreatedBy("user-logged-in-1");
        repository.onRemoteRoomReceived(ownRoom);

        assertNotNull("Own room should be added", repository.getRoomSync("room-own-99"));
        assertEquals("OWNER", repository.getRoomSync("room-own-99").getCurrentUserRole());
        assertTrue("Own room must be in user room ids", repository.getUserRoomIds().contains("room-own-99"));

        Transaction strangerTx = new Transaction("tx-stranger-1", "room-stranger-99", 200000.0,
                Transaction.TYPE_EXPENSE, "Ăn uống", "Stranger transaction", "Stranger", "03/11/2026 14:00");
        repository.onRemoteTransactionReceived(strangerTx);

        List<Transaction> strangerRoomTx = repository.getTransactionsForRoom("room-stranger-99");
        assertEquals(0, strangerRoomTx.size());

        Transaction ownTx = new Transaction("tx-own-1", "room-own-99", 150000.0,
                Transaction.TYPE_INCOME, "Đóng quỹ", "Own transaction", "Test User", "03/11/2026 14:30");
        repository.onRemoteTransactionReceived(ownTx);

        List<Transaction> ownRoomTx = repository.getTransactionsForRoom("room-own-99");
        assertEquals(1, ownRoomTx.size());
        assertEquals("tx-own-1", ownRoomTx.get(0).getId());

        repository.onRemoteMemberChanged("room-own-99", "DELETE", "user-logged-in-1");
        assertNull("Room should be removed after member kicked", repository.getRoomSync("room-own-99"));
        assertTrue("Room ID should no longer be in user rooms", !repository.getUserRoomIds().contains("room-own-99"));
        assertEquals("Transactions for room should be removed", 0, repository.getTransactionsForRoom("room-own-99").size());

        repository.setCurrentUser("user-001", "Bạn");
    }
}
