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

/**
 * Kiểm thử tính khớp nối 2 chiều (End-to-End) giữa giao diện (ViewModels/UI Logic)
 * và tầng dữ liệu Backend (RoomRepository, Data Models, Realtime Stream).
 */
public class UiBackendIntegrationTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RoomRepository repository;

    @Before
    public void setUp() {
        repository = RoomRepository.getInstance();
    }

    @Test
    public void testHomeViewModelSyncWithBackend() {
        HomeViewModel homeViewModel = new HomeViewModel();

        // 1. Kiểm tra ViewModel nhận đúng danh sách Room ban đầu từ Repository
        List<Room> initialRooms = homeViewModel.getRooms().getValue();
        assertNotNull("HomeViewModel must receive initial rooms", initialRooms);
        assertTrue("Initial rooms must contain mock rooms", initialRooms.size() >= 3);

        // 2. Kiểm tra tổng số dư hệ thống phản ánh đúng dữ liệu mock
        Double totalBalance = homeViewModel.getTotalBalance().getValue();
        assertNotNull("Total balance must not be null", totalBalance);

        // 3. Thực hiện thao tác tạo quỹ mới trên UI ViewModel
        int countBefore = initialRooms.size();
        homeViewModel.createRoom("Quỹ Hội Đồng Hương", "Gom tiền hội họp bạn bè cuối năm");

        List<Room> updatedRooms = homeViewModel.getRooms().getValue();
        assertNotNull(updatedRooms);
        assertEquals("Room count must increase by 1", countBefore + 1, updatedRooms.size());

        Room created = updatedRooms.get(0);
        assertEquals("Quỹ Hội Đồng Hương", created.getName());
        assertNotNull("Join code must be generated", created.getJoinCode());

        // 4. Thực hiện tham gia quỹ bằng mã Join Code
        homeViewModel.joinRoom(created.getJoinCode());
        // Quỹ mới đã tham gia thành công
        Room found = repository.getRoomSync(created.getId());
        assertNotNull(found);
    }

    @Test
    public void testRoomViewModelAndTransactionFlow() {
        RoomViewModel roomViewModel = new RoomViewModel();
        TransactionViewModel txViewModel = new TransactionViewModel();

        String roomId = "room-1"; // Quỹ Du Lịch Đà Lạt
        roomViewModel.setRoomId(roomId);
        txViewModel.setRoomId(roomId);

        // Kiểm tra số dư và thông tin quỹ ban đầu
        Room room = roomViewModel.getRoom().getValue();
        assertNotNull(room);
        assertEquals("Quỹ Du Lịch Đà Lạt 2026", room.getName());

        double initialBalance = room.getBalance();
        double initialIncome = roomViewModel.getTotalIncome().getValue();
        double initialExpense = roomViewModel.getTotalExpense().getValue();

        // 1. Thêm một khoản thu mới qua TransactionViewModel (mô phỏng form AddTransactionFragment)
        boolean incomeAdded = txViewModel.submitTransaction(
                roomId,
                Transaction.TYPE_INCOME,
                "500000",
                "Đóng quỹ",
                "Tuấn Anh đóng thêm tiền vé máy bay",
                "Tuấn Anh"
        );
        assertTrue("Transaction income should be valid and submitted", incomeAdded);

        // Kiểm tra tính Reactive: RoomViewModel tự động cập nhật
        roomViewModel.refresh();
        assertEquals(initialIncome + 500000.0, roomViewModel.getTotalIncome().getValue(), 0.001);

        // 2. Thêm một khoản chi mới qua TransactionViewModel
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

        // Kiểm tra số dư quỹ đã tính đúng chênh lệch (+500k -200k = +300k)
        double expectedBalance = initialBalance + 300000.0;
        assertEquals(expectedBalance, roomViewModel.getRoom().getValue().getBalance(), 0.001);

        // 3. Kiểm tra danh sách giao dịch gần đây của RoomViewModel
        List<Transaction> recent = roomViewModel.getRecentTransactions().getValue();
        assertNotNull(recent);
        assertTrue(recent.size() > 0);
        assertEquals("Mua bánh tráng nướng Đà Lạt", recent.get(0).getNote());
    }

    @Test
    public void testTransactionValidationErrors() {
        TransactionViewModel txViewModel = new TransactionViewModel();

        // Kiểm tra validation: số tiền trống
        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "", "Ăn uống", "Note", "Bạn"));

        // Kiểm tra validation: số tiền âm hoặc bằng 0
        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "0", "Ăn uống", "Note", "Bạn"));
        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "-50000", "Ăn uống", "Note", "Bạn"));

        // Kiểm tra validation: thiếu ghi chú / nội dung
        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "50000", "Ăn uống", "", "Bạn"));

        // Kiểm tra validation: thiếu danh mục
        assertFalse(txViewModel.submitTransaction("room-1", Transaction.TYPE_EXPENSE, "50000", "", "Note", "Bạn"));
    }

    @Test
    public void testStatisticsViewModelSync() {
        StatisticsViewModel statViewModel = new StatisticsViewModel();
        String roomId = "room-2"; // Quỹ Tiền Nhà
        statViewModel.setRoomId(roomId);

        // Kiểm tra dữ liệu thống kê
        Double income = statViewModel.getTotalIncome().getValue();
        Double expense = statViewModel.getTotalExpense().getValue();
        Double net = statViewModel.getNetBalance().getValue();

        assertNotNull(income);
        assertNotNull(expense);
        assertNotNull(net);
        assertEquals(income - expense, net, 0.001);

        // Kiểm tra phân loại danh mục
        Map<String, Double> categoryStats = statViewModel.getCategoryStats().getValue();
        assertNotNull(categoryStats);
        assertTrue(categoryStats.containsKey("Tiền nhà") || categoryStats.containsKey("Điện nước"));

        // Kiểm tra danh sách Top 5 khoản chi lớn nhất
        List<Transaction> topExpenses = statViewModel.getTopExpenses().getValue();
        assertNotNull(topExpenses);
        assertTrue(topExpenses.size() <= 5);

        // Kiểm tra sắp xếp giảm dần theo số tiền
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

        // Lọc tất cả
        txViewModel.setFilter(TransactionViewModel.FilterType.ALL);
        List<Transaction> all = txViewModel.getTransactions().getValue();
        assertNotNull(all);

        // Lọc chỉ khoản Thu
        txViewModel.setFilter(TransactionViewModel.FilterType.INCOME);
        List<Transaction> incomes = txViewModel.getTransactions().getValue();
        assertNotNull(incomes);
        for (Transaction tx : incomes) {
            assertTrue(tx.isIncome());
        }

        // Lọc chỉ khoản Chi
        txViewModel.setFilter(TransactionViewModel.FilterType.EXPENSE);
        List<Transaction> expenses = txViewModel.getTransactions().getValue();
        assertNotNull(expenses);
        for (Transaction tx : expenses) {
            assertTrue(tx.isExpense());
        }

        // Tổng số thu + chi bằng tổng số giao dịch
        assertEquals(all.size(), incomes.size() + expenses.size());
    }
}
