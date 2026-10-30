package com.sharedfund.app.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.model.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import java.lang.reflect.Type;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Repository cung cấp dữ liệu cho ứng dụng SharedFund.
 * Repository không khởi tạo mock data; dữ liệu thật phải được tải từ Supabase.
 * Điểm kết nối (Integration Point): Sẵn sàng thay thế và tích hợp Supabase REST API & PostgreSQL Realtime.
 * Kiến trúc Reactive: Mọi thay đổi dữ liệu (từ người dùng nội bộ hoặc sự kiện Realtime bên ngoài)
 * sẽ tự động đồng bộ đến tất cả các ViewModel qua LiveData streams.
 */
public class RoomRepository {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface DataCallback<T> {
        void onSuccess(T value);
        void onError(String message);
    }
    private static volatile RoomRepository instance;

    // Bộ nhớ đệm cho dữ liệu đã tải từ Supabase
    private final List<Room> roomList = new ArrayList<>();
    private final List<Transaction> transactionList = new ArrayList<>();

    // Quản lý thành viên: userId -> danh sách roomId mà user đó đã tham gia/tạo
    private final Map<String, List<String>> userRoomsMap = new HashMap<>();
    private String currentUserId = "";
    private String currentUserName = "Bạn";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private String token = SupabaseConfig.SUPABASE_ANON_KEY;

    // LiveData streams
    private final MutableLiveData<List<Room>> roomsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalBalanceLiveData = new MutableLiveData<>();

    private RoomRepository() {
        // Supabase will populate these streams through repository API calls.
        roomsLiveData.setValue(new ArrayList<>());
        transactionsLiveData.setValue(new ArrayList<>());
        totalBalanceLiveData.setValue(0.0);
    }

    public static synchronized RoomRepository getInstance() {
        if (instance == null) {
            instance = new RoomRepository();
        }
        return instance;
    }

    /**
     * Chuyển đổi ngữ cảnh người dùng hiện tại
     */
    public synchronized void setCurrentUser(String userId, String userName) {
        String nextUserId = userId != null ? userId : "";
        if (!nextUserId.equals(this.currentUserId)) {
            roomList.clear();
            transactionList.clear();
            userRoomsMap.clear();
        }
        this.currentUserId = nextUserId;
        this.currentUserName = (userName != null && !userName.isEmpty()) ? userName : "Người dùng";
        if (!userRoomsMap.containsKey(this.currentUserId)) {
            userRoomsMap.put(this.currentUserId, new ArrayList<>());
        }
        recalculateAllBalances();
        notifyDataChanged();
    }

    /**
     * Đăng ký tài khoản mới: người dùng mới bắt đầu với 0 quỹ (không có sẵn quỹ nào)
     */
    public synchronized void registerNewUser(String userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = (userName != null && !userName.isEmpty()) ? userName : "Bạn";
        userRoomsMap.put(this.currentUserId, new ArrayList<>());
        recalculateAllBalances();
        notifyDataChanged();
    }

    public synchronized String getCurrentUserId() {
        return currentUserId;
    }

    public synchronized String getCurrentUserName() {
        return currentUserName;
    }

    public synchronized void configureAccessToken(String accessToken) {
        token = accessToken != null && !accessToken.trim().isEmpty()
                ? accessToken : SupabaseConfig.SUPABASE_ANON_KEY;
    }
    private Request.Builder request(String path) {
        return new Request.Builder().url(SupabaseConfig.REST_URL + path)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token);
    }

    private String error(Response response, String body) {
        return "HTTP " + response.code() + (body.isEmpty() ? "" : ": " + body);
    }

    public void loadUserData(String userId, DataCallback<List<Room>> callback) {
        String path = "/room_members?select=role,rooms(*)&user_id=eq." + userId;
        client.newCall(request(path).get().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) { callback.onError(error(response, body)); return; }
                List<Room> rooms = new ArrayList<>();
                try {
                    JsonArray rows = JsonParser.parseString(body).getAsJsonArray();
                    for (JsonElement rowElement : rows) {
                        JsonObject row = rowElement.getAsJsonObject();
                        JsonElement roomValue = row.get("rooms");
                        if (roomValue == null || roomValue.isJsonNull()) continue;
                        if (roomValue.isJsonArray()) {
                            for (JsonElement value : roomValue.getAsJsonArray()) {
                                Room room = gson.fromJson(value, Room.class);
                                room.setCurrentUserRole(row.has("role") ? row.get("role").getAsString() : "MEMBER");
                                rooms.add(room);
                            }
                        } else {
                            Room room = gson.fromJson(roomValue, Room.class);
                            room.setCurrentUserRole(row.has("role") ? row.get("role").getAsString() : "MEMBER");
                            rooms.add(room);
                        }
                    }
                } catch (Exception e) { callback.onError("Invalid rooms response: " + e.getMessage()); return; }
                loadTransactions(rooms, callback);
            }
        });
    }

    private void loadTransactions(List<Room> rooms, DataCallback<List<Room>> callback) {
        if (rooms.isEmpty()) {
            replaceRemoteData(rooms, new ArrayList<>());
            callback.onSuccess(rooms);
            return;
        }
        StringBuilder ids = new StringBuilder();
        for (Room room : rooms) {
            if (ids.length() > 0) ids.append(',');
            ids.append(room.getId());
        }
        String path = "/transactions?select=*&room_id=in.(" + ids + ")&order=created_at.desc";
        client.newCall(request(path).get().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) { callback.onError(error(response, body)); return; }
                Type type = new TypeToken<List<Transaction>>() {}.getType();
                List<Transaction> transactions = gson.fromJson(body, type);
                loadMembers(rooms, transactions != null ? transactions : new ArrayList<>(), callback);
            }
        });
    }

    public void loadCategories(DataCallback<List<Category>> callback) {
        client.newCall(request("/categories?select=id,name,type&order=name.asc").get().build())
                .enqueue(new okhttp3.Callback() {
                    @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) { callback.onError(error(response, body)); return; }
                        try {
                            Type type = new TypeToken<List<Category>>() {}.getType();
                            List<Category> categories = gson.fromJson(body, type);
                            callback.onSuccess(categories != null ? categories : new ArrayList<>());
                        } catch (Exception e) { callback.onError("Invalid categories response: " + e.getMessage()); }
                    }
                });
    }

    private void loadMembers(List<Room> rooms, List<Transaction> transactions, DataCallback<List<Room>> callback) {
        StringBuilder ids = new StringBuilder();
        for (Room room : rooms) {
            if (ids.length() > 0) ids.append(',');
            ids.append(room.getId());
        }
        String path = "/room_members?select=room_id,role,profiles(full_name)&room_id=in.(" + ids + ")";
        client.newCall(request(path).get().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) { callback.onError(error(response, body)); return; }
                try {
                    JsonArray members = JsonParser.parseString(body).getAsJsonArray();
                    for (Room room : rooms) {
                        List<String> names = new ArrayList<>();
                        for (JsonElement element : members) {
                            JsonObject row = element.getAsJsonObject();
                            if (!room.getId().equals(row.get("room_id").getAsString())) continue;
                            JsonElement profile = row.get("profiles");
                            String name = "Thành viên";
                            if (profile != null && profile.isJsonObject() && profile.getAsJsonObject().has("full_name")) {
                                name = profile.getAsJsonObject().get("full_name").getAsString();
                            }
                            names.add(name);
                        }
                        room.setMembers(names);
                        room.setMemberCount(names.size());
                    }
                } catch (Exception e) { callback.onError("Invalid room members response: " + e.getMessage()); return; }
                replaceRemoteData(rooms, transactions);
                callback.onSuccess(rooms);
            }
        });
    }

    public void loadWalletBalance(String userId, DataCallback<Double> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("Không xác định được tài khoản đang đăng nhập.");
            return;
        }
        String path = "/profiles?select=wallet_balance&id=eq." + userId;
        client.newCall(request(path).get().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) { callback.onError(error(response, body)); return; }
                try {
                    JsonArray rows = JsonParser.parseString(body).getAsJsonArray();
                    if (rows.size() == 0) { callback.onError("Không tìm thấy hồ sơ ví của tài khoản."); return; }
                    JsonObject profile = rows.get(0).getAsJsonObject();
                    JsonElement value = profile.get("wallet_balance");
                    callback.onSuccess(value == null || value.isJsonNull() ? 0.0 : value.getAsDouble());
                } catch (Exception e) { callback.onError("Dữ liệu số dư ví không hợp lệ: " + e.getMessage()); }
            }
        });
    }

    public void createRoom(String name, String description, String userId, DataCallback<Room> callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("name", name);
        payload.addProperty("description", description);
        payload.addProperty("join_code", makeJoinCode());
        payload.addProperty("created_by", userId);
        payload.addProperty("balance", 0);
        payload.addProperty("currency", "VND");
        payload.addProperty("is_archived", false);
        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        client.newCall(request("/rooms?select=*").post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation").build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) { callback.onError(error(response, responseBody)); return; }
                try {
                    JsonArray rows = JsonParser.parseString(responseBody).getAsJsonArray();
                    if (rows.size() == 0) { callback.onError("Supabase returned no room row."); return; }
                    Room room = gson.fromJson(rows.get(0), Room.class);
                    addMember(room, userId, "OWNER", callback);
                } catch (Exception e) { callback.onError("Invalid room response: " + e.getMessage()); }
            }
        });
    }

    public void joinRoom(String code, String userId, DataCallback<Room> callback) {
        String safeCode = code.trim().toUpperCase(java.util.Locale.ROOT);
        String path = "/rooms?select=*&join_code=eq." + safeCode;
        client.newCall(request(path).get().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) { callback.onError(error(response, body)); return; }
                try {
                    JsonArray rows = JsonParser.parseString(body).getAsJsonArray();
                    if (rows.size() == 0) { callback.onError("Không tìm thấy quỹ với mã tham gia này."); return; }
                    addMember(gson.fromJson(rows.get(0), Room.class), userId, "MEMBER", callback);
                } catch (Exception e) { callback.onError("Invalid room response: " + e.getMessage()); }
            }
        });
    }

    public void loadRoomMembers(String roomId, DataCallback<List<RoomMember>> callback) {
        String path = "/room_members?select=id,room_id,user_id,role,total_contributed,joined_at,profiles(full_name,email,avatar_url)&room_id=eq." + roomId;
        client.newCall(request(path).get().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) { callback.onError(error(response, body)); return; }
                try {
                    List<RoomMember> result = new ArrayList<>();
                    JsonArray rows = JsonParser.parseString(body).getAsJsonArray();
                    for (JsonElement element : rows) {
                        JsonObject row = element.getAsJsonObject();
                        RoomMember member = gson.fromJson(row, RoomMember.class);
                        JsonElement profile = row.get("profiles");
                        if (profile != null && profile.isJsonObject()) {
                            JsonObject p = profile.getAsJsonObject();
                            if (p.has("full_name")) member.setFullName(p.get("full_name").getAsString());
                            if (p.has("email")) member.setEmail(p.get("email").getAsString());
                            if (p.has("avatar_url")) member.setAvatarUrl(p.get("avatar_url").getAsString());
                        }
                        result.add(member);
                    }
                    callback.onSuccess(result);
                } catch (Exception e) { callback.onError("Invalid members response: " + e.getMessage()); }
            }
        });
    }

    private void addMember(Room room, String userId, String role, DataCallback<Room> callback) {
        room.setCurrentUserRole(role);
        JsonObject row = new JsonObject();
        row.addProperty("room_id", room.getId());
        row.addProperty("user_id", userId);
        row.addProperty("role", role);
        Request request = request("/room_members?on_conflict=room_id,user_id")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
                .post(RequestBody.create(gson.toJson(row), JSON)).build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) { callback.onError(error(response, body)); return; }
                callback.onSuccess(room);
            }
        });
    }

    private String makeJoinCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(java.util.Locale.ROOT);
    }
    /**
     * Lấy danh sách các quỹ mà người dùng hiện tại đã tham gia hoặc tạo
     */
    public synchronized List<Room> getUserRooms() {
        List<String> joinedIds = userRoomsMap.get(currentUserId);
        if (joinedIds == null) {
            return new ArrayList<>();
        }
        List<Room> result = new ArrayList<>();
        for (String id : joinedIds) {
            Room r = getRoomSync(id);
            if (r != null) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Lấy tất cả các quỹ trên toàn hệ thống (dành cho tra cứu mã mời hoặc admin)
     */
    public synchronized List<Room> getAllSystemRooms() {
        return new ArrayList<>(roomList);
    }

    /**
     * Tính toán lại số dư của từng quỹ và tổng số dư của người dùng hiện tại
     */
    public synchronized void recalculateAllBalances() {
        for (Room r : roomList) {
            double income = 0;
            double expense = 0;
            for (Transaction tx : transactionList) {
                if (r.getId().equals(tx.getRoomId()) && affectsBalance(tx)) {
                    if (tx.isIncome()) {
                        income += tx.getAmount();
                    } else {
                        expense += tx.getAmount();
                    }
                }
            }
            double balance = income - expense;
            r.setBalance(balance);
        }

        // Tổng số dư phản ánh đúng các quỹ mà user hiện tại tham gia
        double userTotal = 0;
        List<Room> myRooms = getUserRooms();
        for (Room r : myRooms) {
            userTotal += r.getBalance();
        }
        totalBalanceLiveData.postValue(userTotal);
    }

    public synchronized void notifyDataChanged() {
        roomsLiveData.postValue(new ArrayList<>(getUserRooms()));
        transactionsLiveData.postValue(new ArrayList<>(transactionList));
    }

    /** Replace the in-memory view with rows fetched from Supabase for the active user. */
    public synchronized void replaceRemoteData(List<Room> rooms, List<Transaction> transactions) {
        roomList.clear();
        transactionList.clear();
        List<String> ids = new ArrayList<>();
        if (rooms != null) {
            for (Room room : rooms) {
                if (room == null || room.getId() == null) continue;
                roomList.add(room);
                ids.add(room.getId());
            }
        }
        if (transactions != null) transactionList.addAll(transactions);
        userRoomsMap.put(currentUserId, ids);
        recalculateAllBalances();
        notifyDataChanged();
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
            if (roomId.equals(tx.getRoomId()) && affectsBalance(tx) && tx.isIncome()) {
                sum += tx.getAmount();
            }
        }
        return sum;
    }

    public synchronized double getRoomTotalExpense(String roomId) {
        double sum = 0;
        if (roomId == null) return sum;
        for (Transaction tx : transactionList) {
            if (roomId.equals(tx.getRoomId()) && affectsBalance(tx) && tx.isExpense()) {
                sum += tx.getAmount();
            }
        }
        return sum;
    }

    public synchronized Map<String, Double> getCategoryBreakdown(String roomId) {
        Map<String, Double> map = new HashMap<>();
        if (roomId == null) return map;
        for (Transaction tx : transactionList) {
            if (roomId.equals(tx.getRoomId()) && affectsBalance(tx) && tx.isExpense()) {
                String cat = tx.getCategory() != null ? tx.getCategory() : "Khác";
                Double current = map.get(cat);
                map.put(cat, (current != null ? current : 0.0) + tx.getAmount());
            }
        }
        return map;
    }

    private boolean affectsBalance(Transaction transaction) {
        if (transaction == null) return false;
        String status = transaction.getStatus();
        return status == null || status.trim().isEmpty()
                || "APPROVED".equalsIgnoreCase(status)
                || "SUCCESS".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status)
                || "CONFIRMED".equalsIgnoreCase(status)
                || "PAID".equalsIgnoreCase(status);
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

}
