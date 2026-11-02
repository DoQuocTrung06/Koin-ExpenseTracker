package com.sharedfund.app.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sharedfund.app.SharedFundApp;

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
import com.sharedfund.app.model.Profile;

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

public class RoomRepository {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface DataCallback<T> {
        void onSuccess(T value);
        void onError(String message);
    }

    private static volatile RoomRepository instance;

    private final List<Room> roomList = new ArrayList<>();
    private final List<Transaction> transactionList = new ArrayList<>();
    private final Map<String, List<String>> userRoomsMap = new HashMap<>();
    private String currentUserId = "";
    private String currentUserName = "User";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .dns(SafeDns.getInstance())
            .authenticator(new SupabaseAuthenticator(SharedFundApp.getInstance()))
            .build();
    private final Gson gson = new Gson();
    private String token = SupabaseConfig.SUPABASE_ANON_KEY;
    private SupabaseRealtimeClient realtimeClient;

    private final MutableLiveData<List<Room>> roomsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalBalanceLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> userWalletLiveData = new MutableLiveData<>(0.0);
    private double currentWalletBalance = 0.0;
    private final Map<String, String> userAvatarCache = new java.util.concurrent.ConcurrentHashMap<>();

    public String getUserAvatar(String userId) {
        if (userId == null) return null;
        return userAvatarCache.get(userId);
    }

    public void cacheUserAvatar(String userId, String avatarUrl) {
        if (userId != null && avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            userAvatarCache.put(userId, avatarUrl.trim());
        }
    }

    private RoomRepository() {
        roomsLiveData.setValue(new ArrayList<>());
        transactionsLiveData.setValue(new ArrayList<>());
        totalBalanceLiveData.setValue(0.0);
        userWalletLiveData.setValue(0.0);
        startRealtime();
    }

    public static synchronized RoomRepository getInstance() {
        if (instance == null) {
            instance = new RoomRepository();
        }
        return instance;
    }

    public synchronized void startRealtime() {
        if (realtimeClient == null) {
            realtimeClient = new SupabaseRealtimeClient();
            realtimeClient.setRealtimeListener(tx -> {
                onRemoteTransactionReceived(tx);
                if (currentUserId != null && currentUserId.equals(tx.getUserId())) {
                    loadUserProfile();
                }
            });
            realtimeClient.connect();
        }
    }

    public synchronized void setCurrentUser(String userId, String userName) {
        String nextUserId = userId != null ? userId : "";
        if (!nextUserId.equals(this.currentUserId)) {
            roomList.clear();
            transactionList.clear();
            userRoomsMap.clear();
            if ("user-001".equals(nextUserId)) {
                initMockData();
            }
        }
        this.currentUserId = nextUserId;
        this.currentUserName = (userName != null && !userName.isEmpty()) ? userName : "User";
        if (!userRoomsMap.containsKey(this.currentUserId)) {
            userRoomsMap.put(this.currentUserId, new ArrayList<>());
        }
        recalculateAllBalances();
        notifyDataChanged();
    }

    private void initMockData() {
        Room r1 = new Room("room-1", "Quỹ Du Lịch Đà Lạt 2026", "DALAT6", 0, "Quỹ gom tiền đi du lịch Đà Lạt mùa hoa dã quỳ của nhóm bạn thân.", 5,
                new ArrayList<>(java.util.Arrays.asList("Bạn (Trưởng quỹ)", "Tuấn Anh", "Bảo Linh", "Minh Quân", "Thu Hương")), "01/10/2026");
        Room r2 = new Room("room-2", "Quỹ Tiền Nhà & Sinh Hoạt Trọ", "TRO102", 0, "Quỹ đóng tiền phòng, điện nước và đồ dùng chung phòng 302.", 3,
                new ArrayList<>(java.util.Arrays.asList("Bạn (Trưởng quỹ)", "Đức Trí", "Hoàng Nam")), "05/10/2026");
        Room r3 = new Room("room-3", "Quỹ Ăn Trưa & Coffee Team Tech", "TECH88", 0, "Chi phí ăn trưa, trà sữa xế chiều cho team công nghệ.", 4,
                new ArrayList<>(java.util.Arrays.asList("Bạn", "Quốc Huy (Trưởng quỹ)", "Mai Phương", "Tiến Dũng")), "10/10/2026");

        roomList.add(r1);
        roomList.add(r2);
        roomList.add(r3);

        userRoomsMap.put("user-001", new ArrayList<>(java.util.Arrays.asList("room-1", "room-2", "room-3")));
        userRoomsMap.put("guest-001", new ArrayList<>(java.util.Arrays.asList("room-1", "room-2", "room-3")));

        transactionList.add(new Transaction("tx-01", "room-1", 1000000, Transaction.TYPE_INCOME, "Đóng quỹ", "Đóng quỹ đợt 1 - Bạn", "Bạn", "12/10/2026 09:30"));
        transactionList.add(new Transaction("tx-02", "room-1", 1000000, Transaction.TYPE_INCOME, "Đóng quỹ", "Đóng quỹ đợt 1 - Tuấn Anh", "Tuấn Anh", "12/10/2026 10:15"));
        transactionList.add(new Transaction("tx-03", "room-1", 1000000, Transaction.TYPE_INCOME, "Đóng quỹ", "Đóng quỹ đợt 1 - Bảo Linh", "Bảo Linh", "12/10/2026 11:00"));
        transactionList.add(new Transaction("tx-04", "room-1", 450000, Transaction.TYPE_EXPENSE, "Ăn uống", "Ăn tối lẩu gà lá é quán Tao Ngộ", "Bảo Linh", "13/10/2026 19:45"));
        transactionList.add(new Transaction("tx-05", "room-1", 1200000, Transaction.TYPE_EXPENSE, "Thuê phòng", "Đặt cọc homestay Thung Lũng Mơ", "Bạn", "14/10/2026 08:30"));
        transactionList.add(new Transaction("tx-06", "room-1", 320000, Transaction.TYPE_EXPENSE, "Di chuyển", "Thuê 2 xe máy vi vu hồ Tuyền Lâm", "Tuấn Anh", "14/10/2026 14:20"));
        transactionList.add(new Transaction("tx-07", "room-1", 180000, Transaction.TYPE_EXPENSE, "Ăn uống", "Cà phê ngắm hoàng hôn đồi Dã Chiến", "Minh Quân", "14/10/2026 17:30"));

        transactionList.add(new Transaction("tx-08", "room-2", 2500000, Transaction.TYPE_INCOME, "Đóng quỹ", "Tiền phòng tháng 10 - Bạn", "Bạn", "05/10/2026 08:00"));
        transactionList.add(new Transaction("tx-09", "room-2", 2500000, Transaction.TYPE_INCOME, "Đóng quỹ", "Tiền phòng tháng 10 - Đức Trí", "Đức Trí", "05/10/2026 08:30"));
        transactionList.add(new Transaction("tx-10", "room-2", 2500000, Transaction.TYPE_INCOME, "Đóng quỹ", "Tiền phòng tháng 10 - Hoàng Nam", "Hoàng Nam", "05/10/2026 09:00"));
        transactionList.add(new Transaction("tx-11", "room-2", 6000000, Transaction.TYPE_EXPENSE, "Tiền nhà", "Thanh toán tiền thuê trọ tháng 10", "Bạn", "05/10/2026 10:00"));
        transactionList.add(new Transaction("tx-12", "room-2", 850000, Transaction.TYPE_EXPENSE, "Điện nước", "Hóa đơn điện nước tháng 9", "Bạn", "08/10/2026 15:00"));

        transactionList.add(new Transaction("tx-13", "room-3", 500000, Transaction.TYPE_INCOME, "Đóng quỹ", "Đóng quỹ ăn trưa tuần 2 tháng 10", "Bạn", "16/10/2026 11:30"));
        transactionList.add(new Transaction("tx-14", "room-3", 220000, Transaction.TYPE_EXPENSE, "Ăn uống", "Cơm tấm sườn bì chả cho 4 người", "Bạn", "16/10/2026 12:15"));
        transactionList.add(new Transaction("tx-15", "room-3", 110000, Transaction.TYPE_EXPENSE, "Ăn uống", "Trà sữa Phúc Long trưa Thứ 6", "Mai Phương", "16/10/2026 14:00"));
    }

    public synchronized void registerNewUser(String userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = (userName != null && !userName.isEmpty()) ? userName : "User";
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
        String activeToken = token;
        if ((activeToken == null || activeToken.equals(SupabaseConfig.SUPABASE_ANON_KEY)) && SharedFundApp.getInstance() != null) {
            String sessionToken = UserSessionManager.getInstance(SharedFundApp.getInstance()).getAccessToken();
            if (sessionToken != null && !sessionToken.trim().isEmpty()) {
                activeToken = sessionToken;
            }
        }
        return new Request.Builder().url(SupabaseConfig.REST_URL + path)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + activeToken);
    }

    private String error(Response response, String body) {
        return "HTTP " + response.code() + (body.isEmpty() ? "" : ": " + body);
    }

    public void loadUserData(String userId, DataCallback<List<Room>> callback) {
        loadUserProfile(userId, null);
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
        client.newCall(request("/categories?select=id,name,type,icon_name,color_code&order=name.asc").get().build())
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
                            String name = "Member";
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
        if ("user-001".equals(userId)) {
            callback.onSuccess(getUserWalletBalance());
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
                    double balance = value == null || value.isJsonNull() ? 0.0 : value.getAsDouble();
                    setUserWalletBalance(balance);
                    callback.onSuccess(balance);
                } catch (Exception e) { callback.onError("Dữ liệu số dư ví không hợp lệ: " + e.getMessage()); }
            }
        });
    }

    public void createRoom(String name, String description, String userId, DataCallback<Room> callback) {
        createRoom(name, description, 0.0, userId, callback);
    }

    public void createRoom(String name, String description, double targetAmount, String userId, DataCallback<Room> callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("name", name);
        payload.addProperty("description", description);
        payload.addProperty("join_code", makeJoinCode());
        payload.addProperty("created_by", userId);
        payload.addProperty("balance", 0);
        payload.addProperty("currency", "VND");
        payload.addProperty("is_archived", false);
        if (targetAmount > 0) {
            payload.addProperty("target_amount", targetAmount);
        }
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
                    if (rows.size() == 0) { callback.onError("Fund room not found with this join code."); return; }
                    addMember(gson.fromJson(rows.get(0), Room.class), userId, "MEMBER", callback);
                } catch (Exception e) { callback.onError("Invalid room response: " + e.getMessage()); }
            }
        });
    }

    public void loadRoomMembers(String roomId, DataCallback<List<RoomMember>> callback) {
        if ("user-001".equals(currentUserId) || (roomId != null && roomId.startsWith("room-"))) {
            Room r = getRoomSync(roomId);
            List<RoomMember> mockMembers = new ArrayList<>();
            if (r != null && r.getMembers() != null) {
                double totalIncome = getRoomTotalIncome(roomId);
                double perMember = r.getMembers().size() > 0 ? totalIncome / r.getMembers().size() : 0.0;
                for (int i = 0; i < r.getMembers().size(); i++) {
                    String name = r.getMembers().get(i);
                    RoomMember m = new RoomMember();
                    m.setRoomId(roomId);
                    m.setUserId("user-mock-" + i);
                    m.setFullName(name);
                    m.setRole(name.contains("Trưởng quỹ") ? "OWNER" : "MEMBER");
                    m.setTotalContributed(perMember);
                    mockMembers.add(m);
                }
            }
            callback.onSuccess(mockMembers);
            return;
        }
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
                            if (p.has("avatar_url") && !p.get("avatar_url").isJsonNull()) {
                                String avUrl = p.get("avatar_url").getAsString();
                                member.setAvatarUrl(avUrl);
                                cacheUserAvatar(member.getUserId(), avUrl);
                            }
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

    public synchronized Room createRoom(String name, String description) {
        return createRoom(name, description, 0.0);
    }

    public synchronized Room createRoom(String name, String description, double targetAmount) {
        String id = "room-" + (roomList.size() + 1);
        String code = makeJoinCode();
        List<String> members = new ArrayList<>();
        String creatorLabel = currentUserName != null && !currentUserName.isEmpty()
                ? currentUserName + " (Trưởng quỹ)"
                : "Bạn (Trưởng quỹ)";
        members.add(creatorLabel);
        Room newRoom = new Room(id, name, code, 0, description, 1, members);
        if (targetAmount > 0) {
            newRoom.setTargetAmount(targetAmount);
        }
        roomList.add(0, newRoom);
        List<String> joined = userRoomsMap.get(currentUserId);
        if (joined == null) {
            joined = new ArrayList<>();
            userRoomsMap.put(currentUserId, joined);
        }
        if (!joined.contains(id)) {
            joined.add(0, id);
        }
        recalculateAllBalances();
        notifyDataChanged();
        return newRoom;
    }

    public synchronized Room joinRoomByCode(String code) {
        if (code == null) return null;
        String cleanCode = code.trim().toUpperCase(java.util.Locale.ROOT);
        for (Room r : roomList) {
            if (cleanCode.equalsIgnoreCase(r.getJoinCode())) {
                String memberLabel = currentUserName != null && !currentUserName.isEmpty()
                        ? currentUserName
                        : "Bạn";
                List<String> members = r.getMembers();
                if (members == null) {
                    members = new ArrayList<>();
                    r.setMembers(members);
                }
                if (!members.contains(memberLabel)) {
                    members.add(memberLabel);
                    r.setMemberCount(members.size());
                }
                List<String> joined = userRoomsMap.get(currentUserId);
                if (joined == null) {
                    joined = new ArrayList<>();
                    userRoomsMap.put(currentUserId, joined);
                }
                if (!joined.contains(r.getId())) {
                    joined.add(0, r.getId());
                }
                recalculateAllBalances();
                notifyDataChanged();
                return r;
            }
        }
        return null;
    }

    public synchronized boolean addTransaction(Transaction tx) {
        if (tx == null) return false;
        if (tx.getId() == null || tx.getId().isEmpty()) {
            tx.setId("tx-" + (transactionList.size() + 1));
        }
        transactionList.add(0, tx);
        recalculateAllBalances();
        notifyDataChanged();
        return true;
    }

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

    public synchronized List<Room> getAllSystemRooms() {
        return new ArrayList<>(roomList);
    }

    public synchronized void recalculateAllBalances() {
        for (Room r : roomList) {
            double income = 0;
            double expense = 0;
            for (Transaction tx : transactionList) {
                if (r.getId().equals(tx.getRoomId()) && affectsBalance(tx)) {
                    if (tx.isIncome()) {
                        income += tx.getAmount();
                    } else if (tx.isExpense() && (tx.isApproved() || tx.getStatus() == null)) {
                        expense += tx.getAmount();
                    }
                }
            }
            double balance = income - expense;
            r.setBalance(balance);
        }

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

    public LiveData<List<Room>> getRoomsLiveData() {
        return roomsLiveData;
    }

    public LiveData<List<Transaction>> getTransactionsLiveData() {
        return transactionsLiveData;
    }

    public LiveData<Double> getTotalBalanceLiveData() {
        return totalBalanceLiveData;
    }

    public LiveData<Double> getUserWalletLiveData() {
        return userWalletLiveData;
    }

    public synchronized double getUserWalletBalance() {
        return currentWalletBalance;
    }

    public synchronized void setUserWalletBalance(double balance) {
        this.currentWalletBalance = balance;
        userWalletLiveData.postValue(balance);
    }

    public boolean isCurrentUserAdmin(String roomId) {
        Room r = getRoomSync(roomId);
        if (r == null) return false;
        String role = r.getCurrentUserRole();
        return "ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role);
    }

    public void loadUserProfile() {
        loadUserProfile(currentUserId, null);
    }

    public void loadUserProfile(String userId, DataCallback<Profile> callback) {
        if (userId == null || userId.trim().isEmpty()) return;
        String path = "/profiles?id=eq." + userId + "&select=*";
        client.newCall(request(path).get().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError(error(response, body));
                    return;
                }
                try {
                    Profile[] list = gson.fromJson(body, Profile[].class);
                    if (list != null && list.length > 0) {
                        setUserWalletBalance(list[0].getWalletBalance());
                        Profile p = list[0];
                        if (UserSessionManager.getInstance() != null) {
                            UserSessionManager.getInstance().updateCustomProfile(
                                    p.getNickname(), p.getOccupation(), p.getEducation(), p.getBio()
                            );
                        }
                        if (callback != null) callback.onSuccess(list[0]);
                    } else if (callback != null) {
                        callback.onError("Profile not found");
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onError(e.getMessage());
                }
            }
        });
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

    public synchronized List<Transaction> getTransactionsForUser(String userId) {
        List<Transaction> list = new ArrayList<>();
        if (userId == null) return list;
        for (Transaction tx : transactionList) {
            if (userId.equals(tx.getUserId())) {
                list.add(tx);
            }
        }
        return list;
    }

    public synchronized List<Transaction> getContributionsForMember(String roomId, String userId) {
        List<Transaction> list = new ArrayList<>();
        if (roomId == null || userId == null) return list;
        for (Transaction tx : transactionList) {
            if (roomId.equals(tx.getRoomId()) && userId.equals(tx.getUserId()) && tx.isIncome()) {
                list.add(tx);
            }
        }
        return list;
    }

    public synchronized List<Room> searchRooms(String query) {
        return SearchHelper.filterRooms(roomList, query);
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
    public synchronized void onRemoteTransactionReceived(Transaction tx) {
        if (tx == null) return;
        boolean updated = false;
        for (int i = 0; i < transactionList.size(); i++) {
            if (transactionList.get(i).getId() != null && transactionList.get(i).getId().equals(tx.getId())) {
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

    public synchronized void onRemoteRoomReceived(Room room) {
        if (room == null) return;
        boolean updated = false;
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getId() != null && roomList.get(i).getId().equals(room.getId())) {
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

    public synchronized void replaceTransactionsForRoom(String roomId, List<Transaction> roomTransactions) {
        if (roomId == null) return;
        for (int i = transactionList.size() - 1; i >= 0; i--) {
            Transaction existing = transactionList.get(i);
            if (existing != null && roomId.equals(existing.getRoomId())) {
                transactionList.remove(i);
            }
        }
        if (roomTransactions != null) {
            transactionList.addAll(0, roomTransactions);
        }
        recalculateAllBalances();
        notifyDataChanged();
    }

    public void refreshRoomTransactions(String roomId, DataCallback<List<Transaction>> callback) {
        if (roomId == null || "user-001".equals(currentUserId) || roomId.startsWith("room-")) {
            if (callback != null) callback.onSuccess(getTransactionsForRoom(roomId));
            return;
        }
        String path = "/transactions?select=*&room_id=eq." + roomId + "&order=created_at.desc";
        client.newCall(request(path).get().build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError(error(response, body));
                    return;
                }
                try {
                    Type type = new TypeToken<List<Transaction>>() {}.getType();
                    List<Transaction> list = gson.fromJson(body, type);
                    List<Transaction> safeList = list != null ? list : new ArrayList<>();
                    replaceTransactionsForRoom(roomId, safeList);
                    if (callback != null) callback.onSuccess(safeList);
                } catch (Exception e) {
                    if (callback != null) callback.onError(e.getMessage());
                }
            }
        });
    }

    public void leaveRoom(String roomId, String userId, DataCallback<Boolean> callback) {
        Room room = getRoomSync(roomId);
        if (room != null && "OWNER".equalsIgnoreCase(room.getCurrentUserRole())) {
            callback.onError("Trưởng quỹ không thể rời phòng. Vui lòng chuyển quyền hoặc đóng quỹ.");
            return;
        }
        if ("user-001".equals(currentUserId) || (roomId != null && roomId.startsWith("room-"))) {
            if (room != null && room.getMembers() != null) {
                room.getMembers().remove(currentUserName);
                room.setMemberCount(Math.max(1, room.getMembers().size()));
            }
            List<String> list = userRoomsMap.get(currentUserId);
            if (list != null) list.remove(roomId);
            notifyDataChanged();
            callback.onSuccess(true);
            return;
        }
        String path = "/room_members?room_id=eq." + roomId + "&user_id=eq." + userId;
        client.newCall(request(path).delete().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code());
                    return;
                }
                loadUserData(currentUserId, new DataCallback<List<Room>>() {
                    @Override public void onSuccess(List<Room> value) { callback.onSuccess(true); }
                    @Override public void onError(String message) { callback.onSuccess(true); }
                });
            }
        });
    }

    public void updateRoom(String roomId, String name, String description, double targetAmount, DataCallback<Room> callback) {
        Room room = getRoomSync(roomId);
        if (room != null) {
            room.setName(name);
            room.setDescription(description);
            if (targetAmount >= 0) {
                room.setTargetAmount(targetAmount);
            }
            notifyDataChanged();
        }
        if ("user-001".equals(currentUserId) || (roomId != null && roomId.startsWith("room-"))) {
            if (room != null) {
                callback.onSuccess(room);
            } else {
                callback.onError("Không tìm thấy quỹ");
            }
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("name", name);
        payload.addProperty("description", description);
        if (targetAmount >= 0) {
            payload.addProperty("target_amount", targetAmount);
        }
        String path = "/rooms?id=eq." + roomId;
        client.newCall(request(path).patch(RequestBody.create(gson.toJson(payload), JSON))
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation").build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + body);
                    return;
                }
                try {
                    JsonArray rows = JsonParser.parseString(body).getAsJsonArray();
                    if (rows.size() > 0) {
                        Room updated = gson.fromJson(rows.get(0), Room.class);
                        if (room != null) {
                            room.setName(updated.getName());
                            room.setDescription(updated.getDescription());
                            room.setTargetAmount(updated.getTargetAmount());
                        }
                        notifyDataChanged();
                        callback.onSuccess(updated);
                    } else if (room != null) {
                        callback.onSuccess(room);
                    } else {
                        callback.onError("Không tìm thấy dữ liệu quỹ");
                    }
                } catch (Exception e) {
                    if (room != null) callback.onSuccess(room);
                    else callback.onError(e.getMessage());
                }
            }
        });
    }

    public void deleteRoom(String roomId, String adminUserId, DataCallback<Boolean> callback) {
        Room room = getRoomSync(roomId);
        if (room != null) {
            String role = room.getCurrentUserRole();
            boolean isOwner = "OWNER".equalsIgnoreCase(role) || (adminUserId != null && adminUserId.equals(room.getCreatedBy()));
            if (!isOwner) {
                callback.onError("Only room owner can delete the fund room.");
                return;
            }
            if (room.getBalance() > 0) {
                callback.onError("Cannot delete fund room with remaining balance. Please disburse or withdraw remaining funds first.");
                return;
            }
        }
        if ("user-001".equals(currentUserId) || (roomId != null && roomId.startsWith("room-"))) {
            if (room != null) roomList.remove(room);
            List<String> list = userRoomsMap.get(currentUserId);
            if (list != null) list.remove(roomId);
            notifyDataChanged();
            callback.onSuccess(true);
            return;
        }
        JsonObject params = new JsonObject();
        params.addProperty("p_room_id", roomId);
        params.addProperty("p_user_id", adminUserId);
        String path = "/rpc/delete_fund_room";
        client.newCall(request(path).post(RequestBody.create(gson.toJson(params), JSON)).build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + body);
                    return;
                }
                try {
                    JsonObject result = JsonParser.parseString(body).getAsJsonObject();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        loadUserData(currentUserId, new DataCallback<List<Room>>() {
                            @Override public void onSuccess(List<Room> value) { callback.onSuccess(true); }
                            @Override public void onError(String message) { callback.onSuccess(true); }
                        });
                    } else {
                        String msg = result.has("message") ? result.get("message").getAsString() : "Delete failed";
                        callback.onError(msg);
                    }
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    public void updateMemberRole(String roomId, String targetUserId, String newRole, DataCallback<Boolean> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("role", newRole);
        String path = "/room_members?room_id=eq." + roomId + "&user_id=eq." + targetUserId;
        client.newCall(request(path).patch(RequestBody.create(gson.toJson(body), JSON)).build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code());
                    return;
                }
                callback.onSuccess(true);
            }
        });
    }
}
