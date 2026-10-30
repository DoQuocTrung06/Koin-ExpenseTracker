package com.sharedfund.app.data;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sharedfund.app.model.Transaction;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class SupabaseRealtimeClient {

    public interface RealtimeListener {
        void onTransactionInserted(Transaction transaction);
    }

    private WebSocketClient webSocketClient;
    private final Gson gson;
    private Handler mainHandler;
    private RealtimeListener listener;
    private Timer heartbeatTimer;

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

    public void connect() {
        try {
            URI uri = new URI(SupabaseConfig.REALTIME_WS_URL);
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    startHeartbeat();
                    joinTransactionChannel();
                }

                @Override
                public void onMessage(String message) {
                    handleIncomingMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    stopHeartbeat();
                }

                @Override
                public void onError(Exception ex) {
                }
            };
            webSocketClient.setDnsResolver(SafeDns.getInstance());
            webSocketClient.connect();
        } catch (Exception ignored) {
        }
    }

    private void joinTransactionChannel() {
        JsonObject joinMsg = new JsonObject();
        joinMsg.addProperty("topic", "realtime:transactions");
        joinMsg.addProperty("event", "phx_join");

        JsonObject payload = new JsonObject();
        JsonObject config = new JsonObject();
        com.google.gson.JsonArray pgChanges = new com.google.gson.JsonArray();
        JsonObject change = new JsonObject();
        change.addProperty("event", "*");
        change.addProperty("schema", "public");
        change.addProperty("table", "transactions");
        pgChanges.add(change);

        config.add("postgres_changes", pgChanges);
        payload.add("config", config);
        payload.addProperty("access_token", SupabaseConfig.SUPABASE_ANON_KEY);

        joinMsg.add("payload", payload);
        joinMsg.addProperty("ref", "1");
        joinMsg.addProperty("join_ref", "1");

        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(joinMsg.toString());
        }
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (webSocketClient != null && webSocketClient.isOpen()) {
                    JsonObject heartbeat = new JsonObject();
                    heartbeat.addProperty("topic", "phoenix");
                    heartbeat.addProperty("event", "heartbeat");
                    heartbeat.add("payload", new JsonObject());
                    heartbeat.addProperty("ref", "hb");
                    webSocketClient.send(heartbeat.toString());
                }
            }
        }, 25000, 25000);
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    private void handleIncomingMessage(String message) {
        try {
            JsonObject root = JsonParser.parseString(message).getAsJsonObject();
            String event = root.has("event") ? root.get("event").getAsString() : "";

            if ("postgres_changes".equals(event)) {
                if (root.has("payload")) {
                    JsonObject payload = root.getAsJsonObject("payload");
                    JsonObject record = null;
                    if (payload.has("data") && payload.getAsJsonObject("data").has("record")) {
                        record = payload.getAsJsonObject("data").getAsJsonObject("record");
                    } else if (payload.has("record")) {
                        record = payload.getAsJsonObject("record");
                    } else if (payload.has("new")) {
                        record = payload.getAsJsonObject("new");
                    }
                    if (record != null) {
                        Transaction transaction = gson.fromJson(record, Transaction.class);
                        notifyListener(transaction);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void notifyListener(Transaction transaction) {
        if (listener != null && transaction != null) {
            if (mainHandler != null) {
                mainHandler.post(() -> listener.onTransactionInserted(transaction));
            } else {
                listener.onTransactionInserted(transaction);
            }
        }
    }

    public void disconnect() {
        stopHeartbeat();
        if (webSocketClient != null) {
            webSocketClient.close();
            webSocketClient = null;
        }
    }
}
