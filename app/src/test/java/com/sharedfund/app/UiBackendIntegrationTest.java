package com.sharedfund.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.home.HomeViewModel;
import com.sharedfund.app.ui.room.RoomViewModel;

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

        Double totalBalance = homeViewModel.getTotalRoomBalance().getValue();
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
    public void testNewUserRegistrationStartsEmptyAndCanJoinOrReceiveFunds() {
        String newUserId = "user-test-new-" + System.currentTimeMillis();
        String newUserName = "Nguyễn Văn Test";
        repository.registerNewUser(newUserId, newUserName);

        HomeViewModel homeViewModel = new HomeViewModel();

        List<Room> newRooms = homeViewModel.getRooms().getValue();
        assertNotNull(newRooms);
        assertEquals("New user must have 0 rooms initially", 0, newRooms.size());

        Double newTotalBalance = homeViewModel.getTotalRoomBalance().getValue();
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

    @Test
    public void testDeleteRoomOwnerAndBalanceValidation() {
        RoomViewModel roomViewModel = new RoomViewModel();
        roomViewModel.setRoomId("room-1");
        final String[] errorHolder = new String[1];
        roomViewModel.deleteRoom("user-not-owner", new RoomRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {}

            @Override
            public void onError(String message) {
                errorHolder[0] = message;
            }
        });
        assertNotNull(errorHolder[0]);
    }

    @Test
    public void testRoomViewModel4TabsData() {
        RoomViewModel roomViewModel = new RoomViewModel();
        String roomId = "room-1";
        roomViewModel.setRoomId(roomId);

        Room room = roomViewModel.getRoom().getValue();
        assertNotNull(room);
        assertEquals("room-1", roomViewModel.getRoomId());
        assertNotNull(roomViewModel.getTotalIncome().getValue());
        assertNotNull(roomViewModel.getTotalExpense().getValue());

        List<Transaction> recent = roomViewModel.getRecentTransactions().getValue();
        assertNotNull(recent);
        assertTrue(recent.size() <= 5);

        roomViewModel.setFilter(RoomViewModel.FilterType.ALL);
        List<Transaction> all = roomViewModel.getFilteredTransactions().getValue();
        assertNotNull(all);

        roomViewModel.setFilter(RoomViewModel.FilterType.INCOME);
        List<Transaction> incomes = roomViewModel.getFilteredTransactions().getValue();
        assertNotNull(incomes);
        for (Transaction tx : incomes) {
            assertTrue(tx.isIncome());
        }

        roomViewModel.setFilter(RoomViewModel.FilterType.EXPENSE);
        List<Transaction> expenses = roomViewModel.getFilteredTransactions().getValue();
        assertNotNull(expenses);
        for (Transaction tx : expenses) {
            assertTrue(tx.isExpense());
        }

        Map<String, Double> categoryStats = roomViewModel.getCategoryStats().getValue();
        assertNotNull(categoryStats);

        roomViewModel.loadMembers();
        List<com.sharedfund.app.model.RoomMember> members = roomViewModel.getMembers().getValue();
        assertNotNull(members);
        assertTrue(members.size() > 0);

        final Room[] updatedHolder = new Room[1];
        roomViewModel.updateRoom("Quỹ Đà Lạt 2026 Mới", "Mô tả cập nhật", 8000000.0, new RoomRepository.DataCallback<Room>() {
            @Override
            public void onSuccess(Room value) {
                updatedHolder[0] = value;
            }

            @Override
            public void onError(String message) {}
        });

        assertNotNull(updatedHolder[0]);
        assertEquals("Quỹ Đà Lạt 2026 Mới", updatedHolder[0].getName());
        assertEquals("Mô tả cập nhật", updatedHolder[0].getDescription());
        assertEquals(8000000.0, updatedHolder[0].getTargetAmount(), 0.001);

        Room refreshed = roomViewModel.getRoom().getValue();
        assertNotNull(refreshed);
        assertEquals("Quỹ Đà Lạt 2026 Mới", refreshed.getName());
    }

    @Test
    public void testLeaveRoomConstraint() {
        Room testRoom = repository.createRoom("Quỹ Test Rời Quỹ", "Dùng để kiểm thử rời quỹ");
        assertNotNull(testRoom);

        RoomViewModel roomViewModel = new RoomViewModel();
        roomViewModel.setRoomId(testRoom.getId());

        testRoom.setCurrentUserRole("OWNER");

        final String[] errorHolder = new String[1];
        roomViewModel.leaveRoom("user-001", new RoomRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {}

            @Override
            public void onError(String message) {
                errorHolder[0] = message;
            }
        });
        assertNotNull(errorHolder[0]);

        testRoom.setCurrentUserRole("MEMBER");
        final boolean[] successHolder = new boolean[1];
        roomViewModel.leaveRoom("user-001", new RoomRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                successHolder[0] = (value != null && value);
            }

            @Override
            public void onError(String message) {}
        });
        assertTrue(successHolder[0]);
    }

    @Test
    public void testCreateRoomWithTargetAmount() {
        HomeViewModel homeViewModel = new HomeViewModel();
        homeViewModel.createRoom("Quỹ Tiệc Tất Niên 2026", "Tất niên công ty", 15000000.0);

        List<Room> rooms = homeViewModel.getRooms().getValue();
        assertNotNull(rooms);
        assertTrue(rooms.size() > 0);

        Room created = rooms.get(0);
        assertEquals("Quỹ Tiệc Tất Niên 2026", created.getName());
        assertEquals(15000000.0, created.getTargetAmount(), 0.001);
    }

    @Test
    public void testRoomSearchAndFilterLogic() {
        HomeViewModel homeViewModel = new HomeViewModel();
        homeViewModel.createRoom("Quỹ Du Lịch Đà Nẵng", "Chuyến đi mùa hè");

        List<Room> allRooms = homeViewModel.getRooms().getValue();
        assertNotNull(allRooms);
        assertTrue(allRooms.size() > 0);

        String targetCode = allRooms.get(0).getJoinCode();
        assertNotNull(targetCode);

        List<Room> matchedUnaccented = com.sharedfund.app.data.SearchHelper.filterRooms(allRooms, "da nang");
        assertEquals(1, matchedUnaccented.size());
        assertEquals("Quỹ Du Lịch Đà Nẵng", matchedUnaccented.get(0).getName());

        List<Room> matchedAccented = com.sharedfund.app.data.SearchHelper.filterRooms(allRooms, "đà nẵng");
        assertEquals(1, matchedAccented.size());
        assertEquals("Quỹ Du Lịch Đà Nẵng", matchedAccented.get(0).getName());

        String partialCode = targetCode.substring(0, Math.min(3, targetCode.length())).toLowerCase();
        List<Room> matchedCode = com.sharedfund.app.data.SearchHelper.filterRooms(allRooms, partialCode);
        assertTrue(matchedCode.size() >= 1);
        assertTrue(matchedCode.contains(allRooms.get(0)));
    }

    @Test
    public void testUserProfileUpdate() {
        repository.setCurrentUser("user-test-01", "Nguyễn Văn Test");
        assertEquals("Nguyễn Văn Test", repository.getCurrentUserName());

        repository.setCurrentUser("user-test-01", "Trần Thị Cập Nhật");
        assertEquals("Trần Thị Cập Nhật", repository.getCurrentUserName());
    }
}
