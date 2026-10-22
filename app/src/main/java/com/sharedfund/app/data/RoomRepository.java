package com.sharedfund.app.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Repository cung cấp dữ liệu cho ứng dụng SharedFund.
 * Giai đoạn 3: Sử dụng nguồn dữ liệu mẫu (Mock data) phong phú và chuẩn hóa.
 * Điểm kết nối (Integration Point): Sẵn sàng thay thế và tích hợp Supabase REST API & PostgreSQL Realtime.
 * Kiến trúc Reactive: Mọi thay đổi dữ liệu (từ người dùng nội bộ hoặc sự kiện Realtime bên ngoài)
 * sẽ tự động đồng bộ đến tất cả các ViewModel qua LiveData streams.
 */
public class RoomRepository {
    private static volatile RoomRepository instance;

    // In-memory data store mô phỏng database
    private final List<Room> roomList = new ArrayList<>();
    private final List<Transaction> transactionList = new ArrayList<>();

    // LiveData streams
    private final MutableLiveData<List<Room>> roomsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalBalanceLiveData = new MutableLiveData<>();

    private RoomRepository() {
        initMockData();
    }

    public static synchronized RoomRepository getInstance() {
        if (instance == null) {
            instance = new RoomRepository();
        }
        return instance;
    }

    /**
     * Khởi tạo dữ liệu mẫu thực tế cho quá trình kiểm thử
     */
    private void initMockData() {
        // Mock Rooms
        Room r1 = new Room("room-1", "Quỹ Du Lịch Đà Lạt 2026", "DALAT6", 0, "Quỹ gom tiền đi du lịch Đà Lạt mùa hoa dã quỳ của nhóm bạn thân.", 5,
                new ArrayList<>(Arrays.asList("Bạn (Trưởng quỹ)", "Tuấn Anh", "Bảo Linh", "Minh Quân", "Thu Hương")), "01/10/2026");

        Room r2 = new Room("room-2", "Quỹ Tiền Nhà & Sinh Hoạt Trọ", "TRO102", 0, "Quỹ đóng tiền phòng, điện nước và đồ dùng chung phòng 302.", 3,
                new ArrayList<>(Arrays.asList("Bạn (Trưởng quỹ)", "Đức Trí", "Hoàng Nam")), "05/10/2026");

        Room r3 = new Room("room-3", "Quỹ Ăn Trưa & Coffee Team Tech", "TECH88", 0, "Chi phí ăn trưa, trà sữa xế chiều cho team công nghệ.", 4,
                new ArrayList<>(Arrays.asList("Bạn", "Quốc Huy (Trưởng quỹ)", "Mai Phương", "Tiến Dũng")), "10/10/2026");

        roomList.add(r1);
        roomList.add(r2);
        roomList.add(r3);

        // Mock Transactions cho Quỹ Đà Lạt (room-1)
        transactionList.add(new Transaction("tx-01", "room-1", 1000000, Transaction.TYPE_INCOME, "Đóng quỹ", "Đóng quỹ đợt 1 - Bạn", "Bạn", "12/10/2026 09:30"));
        transactionList.add(new Transaction("tx-02", "room-1", 1000000, Transaction.TYPE_INCOME, "Đóng quỹ", "Đóng quỹ đợt 1 - Tuấn Anh", "Tuấn Anh", "12/10/2026 10:15"));
        transactionList.add(new Transaction("tx-03", "room-1", 1000000, Transaction.TYPE_INCOME, "Đóng quỹ", "Đóng quỹ đợt 1 - Bảo Linh", "Bảo Linh", "12/10/2026 11:00"));
        transactionList.add(new Transaction("tx-04", "room-1", 450000, Transaction.TYPE_EXPENSE, "Ăn uống", "Ăn tối lẩu gà lá é quán Tao Ngộ", "Bảo Linh", "13/10/2026 19:45"));
        transactionList.add(new Transaction("tx-05", "room-1", 1200000, Transaction.TYPE_EXPENSE, "Thuê phòng", "Đặt cọc homestay Thung Lũng Mơ", "Bạn", "14/10/2026 08:30"));
        transactionList.add(new Transaction("tx-06", "room-1", 320000, Transaction.TYPE_EXPENSE, "Di chuyển", "Thuê 2 xe máy vi vu hồ Tuyền Lâm", "Tuấn Anh", "14/10/2026 14:20"));
        transactionList.add(new Transaction("tx-07", "room-1", 180000, Transaction.TYPE_EXPENSE, "Ăn uống", "Cà phê ngắm hoàng hôn đồi chè", "Minh Quân", "15/10/2026 16:30"));

        // Mock Transactions cho Quỹ Tiền Nhà (room-2)
        transactionList.add(new Transaction("tx-08", "room-2", 2500000, Transaction.TYPE_INCOME, "Đóng quỹ", "Tiền trọ tháng 10 - Đức Trí", "Đức Trí", "06/10/2026 18:00"));
        transactionList.add(new Transaction("tx-09", "room-2", 2500000, Transaction.TYPE_INCOME, "Đóng quỹ", "Tiền trọ tháng 10 - Bạn", "Bạn", "06/10/2026 18:05"));
        transactionList.add(new Transaction("tx-10", "room-2", 2500000, Transaction.TYPE_INCOME, "Đóng quỹ", "Tiền trọ tháng 10 - Hoàng Nam", "Hoàng Nam", "06/10/2026 19:10"));
        transactionList.add(new Transaction("tx-11", "room-2", 6000000, Transaction.TYPE_EXPENSE, "Tiền nhà", "Chuyển khoản tiền thuê nhà cho chủ trọ", "Bạn", "07/10/2026 08:00"));
        transactionList.add(new Transaction("tx-12", "room-2", 650000, Transaction.TYPE_EXPENSE, "Điện nước", "Thanh toán hóa đơn điện lực EVN", "Bạn", "08/10/2026 09:20"));

        // Mock Transactions cho Quỹ Ăn Trưa (room-3)
        transactionList.add(new Transaction("tx-13", "room-3", 500000, Transaction.TYPE_INCOME, "Đóng quỹ", "Nạp quỹ đầu tuần - Cả nhóm", "Quốc Huy", "16/10/2026 08:30"));
        transactionList.add(new Transaction("tx-14", "room-3", 220000, Transaction.TYPE_EXPENSE, "Ăn uống", "Cơm tấm sườn bì chả cho 4 người", "Bạn", "16/10/2026 12:15"));
        transactionList.add(new Transaction("tx-15", "room-3", 110000, Transaction.TYPE_EXPENSE, "Ăn uống", "Trà sữa Phúc Long trưa Thứ 6", "Mai Phương", "16/10/2026 14:00"));

        recalculateAllBalances();
        notifyDataChanged();
    }

    /**
     * Tính toán lại số dư của từng quỹ và tổng số dư của toàn bộ hệ thống
     */
    public synchronized void recalculateAllBalances() {
        double allTotal = 0;
        for (Room r : roomList) {
            double income = 0;
            double expense = 0;
            for (Transaction tx : transactionList) {
                if (tx.getRoomId().equals(r.getId())) {
                    if (tx.isIncome()) {
                        income += tx.getAmount();
                    } else {
                        expense += tx.getAmount();
                    }
                }
            }
            double balance = income - expense;
            r.setBalance(balance);
            allTotal += balance;
        }
        totalBalanceLiveData.postValue(allTotal);
    }

    public synchronized void notifyDataChanged() {
        roomsLiveData.postValue(new ArrayList<>(roomList));
        transactionsLiveData.postValue(new ArrayList<>(transactionList));
    }

    // ==========================================
    // GETTERS & QUERY APIS
    // ==========================================

    public LiveData<List<Room>> getRoomsLiveData() {
        return roomsLiveData;
    }

    public LiveData<List<Transaction>> getTransactionsLiveData() {
        return transactionsLiveData;
    }

    public LiveData<Double> getTotalBalanceLiveData() {
        return totalBalanceLiveData;
    }

    public synchronized Room getRoomSync(String roomId) {
        if (roomId == null) return null;
        for (Room r : roomList) {
            if (roomId.equals(r.getId())) {
                return r;
            }
        }
        return null;
    }

    public LiveData<Room> getRoomById(String roomId) {
        MutableLiveData<Room> result = new MutableLiveData<>();
        result.setValue(getRoomSync(roomId));
        return result;
    }

    public synchronized List<Transaction> getTransactionsForRoom(String roomId) {
        List<Transaction> list = new ArrayList<>();
        if (roomId == null) return list;
        for (Transaction tx : transactionList) {
            if (roomId.equals(tx.getRoomId())) {
                list.add(tx);
            }
        }
        return list;
    }

    public synchronized double getRoomTotalIncome(String roomId) {
        double sum = 0;
        if (roomId == null) return sum;
        for (Transaction tx : transactionList) {
            if (roomId.equals(tx.getRoomId()) && tx.isIncome()) {
                sum += tx.getAmount();
            }
        }
        return sum;
    }

    public synchronized double getRoomTotalExpense(String roomId) {
        double sum = 0;
        if (roomId == null) return sum;
        for (Transaction tx : transactionList) {
            if (roomId.equals(tx.getRoomId()) && tx.isExpense()) {
                sum += tx.getAmount();
            }
        }
        return sum;
    }

    public synchronized Map<String, Double> getCategoryBreakdown(String roomId) {
        Map<String, Double> map = new HashMap<>();
        if (roomId == null) return map;
        for (Transaction tx : transactionList) {
            if (roomId.equals(tx.getRoomId()) && tx.isExpense()) {
                String cat = tx.getCategory() != null ? tx.getCategory() : "Khác";
                Double current = map.get(cat);
                map.put(cat, (current != null ? current : 0.0) + tx.getAmount());
            }
        }
        return map;
    }

    // ==========================================
    // ACTIONS & MUTATIONS
    // ==========================================

    /**
     * Tạo quỹ mới
     * Giai đoạn sau: INSERT INTO rooms (name, description, join_code, created_at)
     */
    public synchronized Room createRoom(String name, String description) {
        String id = "room-" + (roomList.size() + 1);
        String code = generateJoinCode();
        String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        List<String> members = new ArrayList<>();
        members.add("Bạn (Trưởng quỹ)");

        Room newRoom = new Room(id, name, code, 0, description, 1, members, dateStr);
        roomList.add(0, newRoom);
        recalculateAllBalances();
        notifyDataChanged();
        return newRoom;
    }

    /**
     * Tham gia quỹ qua mã code
     * Giai đoạn sau: INSERT INTO room_members (room_id, user_id)
     */
    public synchronized Room joinRoomByCode(String code) {
        if (code == null) return null;
        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        for (Room r : roomList) {
            if (cleanCode.equalsIgnoreCase(r.getJoinCode())) {
                if (!r.getMembers().contains("Bạn")) {
                    r.getMembers().add("Bạn");
                    r.setMemberCount(r.getMembers().size());
                    notifyDataChanged();
                }
                return r;
            }
        }
        return null;
    }

    /**
     * Thêm giao dịch mới vào quỹ
     * Giai đoạn sau: INSERT INTO transactions (room_id, amount, type, category, note, created_by, created_at)
     */
    public synchronized boolean addTransaction(Transaction tx) {
        if (tx == null) return false;
        if (tx.getId() == null || tx.getId().isEmpty()) {
            tx.setId("tx-" + (transactionList.size() + 1));
        }
        if (tx.getCreatedAt() == null || tx.getCreatedAt().isEmpty()) {
            String timeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            tx.setCreatedAt(timeStr);
        }
        transactionList.add(0, tx);
        recalculateAllBalances();
        notifyDataChanged();
        return true;
    }

    // ==========================================
    // REALTIME & SUPABASE / POSTGRESQL INTEGRATION HOOKS
    // ==========================================

    /**
     * Điểm kết nối khi nhận được sự kiện INSERT/UPDATE từ Supabase PostgreSQL Realtime Channel
     * Cập nhật danh sách giao dịch và thông báo cho toàn bộ các ViewModel đang hoạt động.
     */
    public synchronized void onRemoteTransactionReceived(Transaction tx) {
        if (tx == null) return;
        // Kiểm tra xem transaction đã có chưa để cập nhật hoặc thêm mới
        boolean updated = false;
        for (int i = 0; i < transactionList.size(); i++) {
            if (transactionList.get(i).getId().equals(tx.getId())) {
                transactionList.set(i, tx);
                updated = true;
                break;
            }
        }
        if (!updated) {
            transactionList.add(0, tx);
        }
        recalculateAllBalances();
        notifyDataChanged();
    }

    /**
     * Điểm kết nối khi nhận được sự kiện cập nhật Room từ Supabase Realtime Channel
     */
    public synchronized void onRemoteRoomReceived(Room room) {
        if (room == null) return;
        boolean updated = false;
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getId().equals(room.getId())) {
                roomList.set(i, room);
                updated = true;
                break;
            }
        }
        if (!updated) {
            roomList.add(0, room);
        }
        recalculateAllBalances();
        notifyDataChanged();
    }

    private String generateJoinCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
