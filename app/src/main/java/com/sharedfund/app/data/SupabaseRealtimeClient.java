package com.sharedfund.app.data;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class SupabaseRealtimeClient {

    public interface RealtimeListener {
        void onTransactionInserted(Transaction transaction);
        default void onTransactionDeleted(String transactionId) {}
        default void onRoomUpdated(Room room) {}
        default void onRoomDeleted(String roomId) {}
        default void onRoomMemberChanged(String roomId, String eventType, String userId) {}
    }

    private static volatile SupabaseRealtimeClient instance;
    private static final int INITIAL_RECONNECT_DELAY_MS = 3000;
    private static final int MAX_RECONNECT_DELAY_MS = 30000;

    private WebSocketClient webSocketClient;
    private final Gson gson;
    private Handler mainHandler;
    private RealtimeListener listener;
    private Timer heartbeatTimer;
    private Timer reconnectTimer;
    private boolean isExplicitDisconnect = false;
    private String authToken;
    private int reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;

    public static synchronized SupabaseRealtimeClient getInstance() {
        if (instance == null) {
            instance = new SupabaseRealtimeClient();
        }
        return instance;
    }

    public SupabaseRealtimeClient() {
        this.gson = new Gson();
        try {
            if (Looper.getMainLooper() != null) {
                this.mainHandler = new Handler(Looper.getMainLooper());
            }
        } catch (Exception ignored) {
            this.mainHandler = null;
        }
    }

    public void setRealtimeListener(RealtimeListener listener) {
        this.listener = listener;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
        try {
            if (webSocketClient != null && webSocketClient.isOpen() && token != null && !token.trim().isEmpty()) {
                JsonObject tokenMsg = new JsonObject();
                tokenMsg.addProperty("topic", "realtime:public");
                tokenMsg.addProperty("event", "access_token");
                JsonObject payload = new JsonObject();
                payload.addProperty("access_token", token);
                tokenMsg.add("payload", payload);
                tokenMsg.addProperty("ref", "tok");
                webSocketClient.send(tokenMsg.toString());
            }
        } catch (Exception ignored) {}
    }

    public synchronized void connect() {
        isExplicitDisconnect = false;
        if (webSocketClient != null && webSocketClient.isOpen()) {
            return;
        }
        try {
            if (webSocketClient != null) {
                try {
                    webSocketClient.close();
                } catch (Exception ignored) {}
                webSocketClient = null;
            }
            URI uri = new URI(SupabaseConfig.REALTIME_WS_URL);
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    android.util.Log.d("SupabaseRealtime", "WS onOpen");
                    reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
                    startHeartbeat();
                    joinChannels();
                }

                @Override
                public void onMessage(String message) {
                    android.util.Log.d("SupabaseRealtime", "WS onMessage: " + message);
                    handleIncomingMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    android.util.Log.d("SupabaseRealtime", "WS onClose: " + code + " " + reason);
                    stopHeartbeat();
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    android.util.Log.d("SupabaseRealtime", "WS onError: " + (ex != null ? ex.getMessage() : "null"));
                }
            };
            webSocketClient.setDnsResolver(SafeDns.getInstance());
            webSocketClient.setConnectionLostTimeout(30);
            webSocketClient.connect();
        } catch (Exception ignored) {
            scheduleReconnect();
        }
    }

    private void joinChannels() {
        JsonObject joinMsg = new JsonObject();
        joinMsg.addProperty("topic", "realtime:public");
        joinMsg.addProperty("event", "phx_join");

        JsonObject payload = new JsonObject();
        JsonObject config = new JsonObject();
        JsonArray pgChanges = new JsonArray();

        JsonObject txChange = new JsonObject();
        txChange.addProperty("event", "*");
        txChange.addProperty("schema", "public");
        txChange.addProperty("table", "transactions");
        pgChanges.add(txChange);

        JsonObject roomsChange = new JsonObject();
        roomsChange.addProperty("event", "*");
        roomsChange.addProperty("schema", "public");
        roomsChange.addProperty("table", "rooms");
        pgChanges.add(roomsChange);

        JsonObject membersChange = new JsonObject();
        membersChange.addProperty("event", "*");
        membersChange.addProperty("schema", "public");
        membersChange.addProperty("table", "room_members");
        pgChanges.add(membersChange);

        config.add("postgres_changes", pgChanges);
        payload.add("config", config);

        String token = null;
        if (com.sharedfund.app.SharedFundApp.getInstance() != null) {
            try {
                UserSessionManager session = UserSessionManager.getInstance(com.sharedfund.app.SharedFundApp.getInstance());
                if (session != null) {
                    String sessionToken = session.getAccessToken();
                    if (sessionToken != null && !sessionToken.trim().isEmpty()) {
                        token = sessionToken;
                    }
                }
            } catch (Exception ignored) {}
        }
        if ((token == null || token.trim().isEmpty() || token.equals(SupabaseConfig.SUPABASE_ANON_KEY)) && this.authToken != null && !this.authToken.trim().isEmpty()) {
            token = this.authToken;
        }
        if (token == null || token.trim().isEmpty()) {
            token = SupabaseConfig.SUPABASE_ANON_KEY;
        }
        payload.addProperty("access_token", token);

        joinMsg.add("payload", payload);
        joinMsg.addProperty("ref", "1");
        joinMsg.addProperty("join_ref", "1");

        try {
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.send(joinMsg.toString());
            }
        } catch (Exception ignored) {}
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (webSocketClient != null && webSocketClient.isOpen()) {
                        JsonObject heartbeat = new JsonObject();
                        heartbeat.addProperty("topic", "phoenix");
                        heartbeat.addProperty("event", "heartbeat");
                        heartbeat.add("payload", new JsonObject());
                        heartbeat.addProperty("ref", "hb");
                        webSocketClient.send(heartbeat.toString());
                    }
                } catch (Exception ignored) {}
            }
        }, 25000, 25000);
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    private synchronized void scheduleReconnect() {
        if (isExplicitDisconnect) return;
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
        }
        int delay = reconnectDelayMs;
        reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
        reconnectTimer = new Timer();
        reconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (!isExplicitDisconnect && (webSocketClient == null || !webSocketClient.isOpen())) {
                        connect();
                    }
                } catch (Exception ignored) {}
            }
        }, delay);
    }

    private void handleIncomingMessage(String message) {
        try {
            JsonObject root = JsonParser.parseString(message).getAsJsonObject();
            String event = root.has("event") ? root.get("event").getAsString() : "";

            if ("postgres_changes".equals(event)) {
                if (root.has("payload")) {
                    JsonObject payload = root.getAsJsonObject("payload");
                    JsonObject data = null;
                    if (payload.has("data") && payload.get("data").isJsonObject()) {
                        data = payload.getAsJsonObject("data");
                    }
                    String table = "";
                    String type = "";
                    JsonObject record = null;
                    JsonObject oldRecord = null;
                    if (data != null) {
                        table = data.has("table") && !data.get("table").isJsonNull() ? data.get("table").getAsString() : "";
                        type = data.has("type") && !data.get("type").isJsonNull() ? data.get("type").getAsString() : "";
                        if (type.isEmpty() && data.has("eventType") && !data.get("eventType").isJsonNull()) {
                            type = data.get("eventType").getAsString();
                        }
                        if (data.has("record") && data.get("record").isJsonObject()) {
                            record = data.getAsJsonObject("record");
                        } else if (data.has("new") && data.get("new").isJsonObject()) {
                            record = data.getAsJsonObject("new");
                        }
                        if (data.has("old_record") && data.get("old_record").isJsonObject()) {
                            oldRecord = data.getAsJsonObject("old_record");
                        } else if (data.has("old") && data.get("old").isJsonObject()) {
                            oldRecord = data.getAsJsonObject("old");
                        }
                    } else {
                        table = payload.has("table") && !payload.get("table").isJsonNull() ? payload.get("table").getAsString() : "transactions";
                        type = payload.has("type") && !payload.get("type").isJsonNull() ? payload.get("type").getAsString() : "";
                        if (type.isEmpty() && payload.has("eventType") && !payload.get("eventType").isJsonNull()) {
                            type = payload.get("eventType").getAsString();
                        }
                        if (payload.has("record") && payload.get("record").isJsonObject()) {
                            record = payload.getAsJsonObject("record");
                        } else if (payload.has("new") && payload.get("new").isJsonObject()) {
                            record = payload.getAsJsonObject("new");
                        }
                        if (payload.has("old_record") && payload.get("old_record").isJsonObject()) {
                            oldRecord = payload.getAsJsonObject("old_record");
                        } else if (payload.has("old") && payload.get("old").isJsonObject()) {
                            oldRecord = payload.getAsJsonObject("old");
                        }
                    }

                    if ("transactions".equalsIgnoreCase(table)) {
                        if ("DELETE".equalsIgnoreCase(type)) {
                            String txId = null;
                            if (oldRecord != null && oldRecord.has("id") && !oldRecord.get("id").isJsonNull()) {
                                txId = oldRecord.get("id").getAsString();
                            } else if (record != null && record.has("id") && !record.get("id").isJsonNull()) {
                                txId = record.get("id").getAsString();
                            }
                            if (txId != null) {
                                notifyTransactionDeleted(txId);
                            }
                        } else if (record != null) {
                            Transaction transaction = gson.fromJson(record, Transaction.class);
                            notifyTransactionInserted(transaction);
                        }
                    } else if ("rooms".equalsIgnoreCase(table)) {
                        if ("DELETE".equalsIgnoreCase(type)) {
                            String roomId = null;
                            if (oldRecord != null && oldRecord.has("id") && !oldRecord.get("id").isJsonNull()) {
                                roomId = oldRecord.get("id").getAsString();
                            } else if (record != null && record.has("id") && !record.get("id").isJsonNull()) {
                                roomId = record.get("id").getAsString();
                            }
                            if (roomId != null) {
                                notifyRoomDeleted(roomId);
                            }
                        } else if (record != null) {
                            Room room = gson.fromJson(record, Room.class);
                            notifyRoomUpdated(room);
                        }
                    } else if ("room_members".equalsIgnoreCase(table)) {
                        String roomId = null;
                        String userId = null;
                        if ("DELETE".equalsIgnoreCase(type)) {
                            if (oldRecord != null) {
                                if (oldRecord.has("room_id") && !oldRecord.get("room_id").isJsonNull()) {
                                    roomId = oldRecord.get("room_id").getAsString();
                                }
                                if (oldRecord.has("user_id") && !oldRecord.get("user_id").isJsonNull()) {
                                    userId = oldRecord.get("user_id").getAsString();
                                }
                            }
                            if (roomId == null && record != null && record.has("room_id") && !record.get("room_id").isJsonNull()) {
                                roomId = record.get("room_id").getAsString();
                            }
                            if (userId == null && record != null && record.has("user_id") && !record.get("user_id").isJsonNull()) {
                                userId = record.get("user_id").getAsString();
                            }
                        } else {
                            if (record != null) {
                                if (record.has("room_id") && !record.get("room_id").isJsonNull()) {
                                    roomId = record.get("room_id").getAsString();
                                }
                                if (record.has("user_id") && !record.get("user_id").isJsonNull()) {
                                    userId = record.get("user_id").getAsString();
                                }
                            }
                            if (roomId == null && oldRecord != null && oldRecord.has("room_id") && !oldRecord.get("room_id").isJsonNull()) {
                                roomId = oldRecord.get("room_id").getAsString();
                            }
                            if (userId == null && oldRecord != null && oldRecord.has("user_id") && !oldRecord.get("user_id").isJsonNull()) {
                                userId = oldRecord.get("user_id").getAsString();
                            }
                        }
                        notifyRoomMemberChanged(roomId, type, userId);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void notifyTransactionInserted(Transaction transaction) {
        if (listener != null && transaction != null) {
            if (mainHandler != null) {
                mainHandler.post(() -> listener.onTransactionInserted(transaction));
            } else {
                listener.onTransactionInserted(transaction);
            }
        }
    }

    private void notifyTransactionDeleted(String transactionId) {
        if (listener != null && transactionId != null) {
            if (mainHandler != null) {
                mainHandler.post(() -> listener.onTransactionDeleted(transactionId));
            } else {
                listener.onTransactionDeleted(transactionId);
            }
        }
    }

    private void notifyRoomUpdated(Room room) {
        if (listener != null && room != null) {
            if (mainHandler != null) {
                mainHandler.post(() -> listener.onRoomUpdated(room));
            } else {
                listener.onRoomUpdated(room);
            }
        }
    }

    private void notifyRoomDeleted(String roomId) {
        if (listener != null && roomId != null) {
            if (mainHandler != null) {
                mainHandler.post(() -> listener.onRoomDeleted(roomId));
            } else {
                listener.onRoomDeleted(roomId);
            }
        }
    }

    private void notifyRoomMemberChanged(String roomId, String eventType, String userId) {
        if (listener != null) {
            if (mainHandler != null) {
                mainHandler.post(() -> listener.onRoomMemberChanged(roomId, eventType, userId));
            } else {
                listener.onRoomMemberChanged(roomId, eventType, userId);
            }
        }
    }

    public synchronized void disconnect() {
        isExplicitDisconnect = true;
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
        stopHeartbeat();
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
            reconnectTimer = null;
        }
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception ignored) {}
            webSocketClient = null;
        }
    }
}
