package com.sharedfund.app.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sharedfund.app.SharedFundApp;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.model.ExpenseCategory;
import com.sharedfund.app.model.RecurringRule;
import java.util.UUID;


import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TransactionRepository {

    private final OkHttpClient client;
    private final Gson gson;
    private String accessToken = SupabaseConfig.SUPABASE_ANON_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String errorMessage);
    }

    public TransactionRepository() {
        this.client = new OkHttpClient.Builder()
                .dns(SafeDns.getInstance())
                .authenticator(new SupabaseAuthenticator(SharedFundApp.getInstance()))
                .build();
        this.gson = new Gson();
    }

    public void setAccessToken(String accessToken) {
        if (accessToken != null && !accessToken.trim().isEmpty()) this.accessToken = accessToken;
    }

    private String getEffectiveToken() {
        if (SharedFundApp.getInstance() != null) {
            String token = UserSessionManager.getInstance(SharedFundApp.getInstance()).getAccessToken();
            if (token != null && !token.trim().isEmpty()) {
                return token;
            }
        }
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            return accessToken;
        }
        return SupabaseConfig.SUPABASE_ANON_KEY;
    }

    public void getTransactionsByRoom(String roomId, DataCallback<List<Transaction>> callback) {
        String url = SupabaseConfig.REST_URL + "/transactions?room_id=eq." + roomId + "&select=*&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<Transaction>>() {}.getType();
                List<Transaction> list = gson.fromJson(json, listType);
                callback.onSuccess(list);
            }
        });
    }

    public void createTransaction(Transaction transaction, DataCallback<Transaction> callback) {
        String url = SupabaseConfig.REST_URL + "/transactions";
        String jsonBody = gson.toJson(transaction);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<Transaction>>() {}.getType();
                List<Transaction> createdList = gson.fromJson(json, listType);
                if (createdList != null && !createdList.isEmpty()) callback.onSuccess(createdList.get(0));
                else callback.onError("Supabase accepted the request but returned no created transaction.");
            }
        });
    }

    public void transferWalletToRoom(String userId, String roomId, double amount, String note, DataCallback<JsonObject> callback) {
        contributeFromWallet(userId, roomId, amount, (note != null && !note.trim().isEmpty()) ? note : "Contribution from wallet", callback);
    }

    public void contributeFromWallet(String userId, String roomId, double amount, String note,
                                     DataCallback<JsonObject> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("p_user_id", userId);
        body.addProperty("p_room_id", roomId);
        body.addProperty("p_amount", amount);
        body.addProperty("p_note", note);
        callJsonbRpc("transfer_wallet_to_room", body, callback);
    }

    public void payRoomRecipient(String roomId, double amount, String category, String note,
                                 String bankBin, String accountNumber, String recipientName,
                                 String transferContent, DataCallback<JsonObject> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("p_room_id", roomId);
        body.addProperty("p_amount", amount);
        body.addProperty("p_category", category);
        body.addProperty("p_note", note);
        body.addProperty("p_bank_bin", bankBin);
        body.addProperty("p_account_number", accountNumber);
        body.addProperty("p_recipient_name", recipientName);
        body.addProperty("p_transfer_content", transferContent);
        callJsonbRpc("pay_room_qr_recipient", body, callback);
    }

    public void simulateSePayPaymentForDemo(String userId, String roomId, double amount,
                                            String reference, DataCallback<JsonObject> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("p_user_id", userId);
        if (roomId == null) body.add("p_room_id", com.google.gson.JsonNull.INSTANCE);
        else body.addProperty("p_room_id", roomId);
        body.addProperty("p_amount", amount);
        body.addProperty("p_reference", reference);
        callJsonbRpc("simulate_sepay_payment", body, callback);
    }

    public void submitWithdrawal(String userId, String userName, String roomId, double amount, String category, String note,
                                 boolean approveImmediately, DataCallback<Transaction> callback) {
        submitWithdrawal(userId, userName, roomId, amount, category, note, null, approveImmediately, false, null, callback);
    }

    public void submitWithdrawal(String userId, String userName, String roomId, double amount, String category, String note,
                                 String billImageUrl, boolean approveImmediately, DataCallback<Transaction> callback) {
        submitWithdrawal(userId, userName, roomId, amount, category, note, billImageUrl, approveImmediately, false, null, callback);
    }

    public void submitWithdrawal(String userId, String userName, String roomId, double amount, String category, String note,
                                 String billImageUrl, boolean approveImmediately, boolean isRecurring, String recurringRuleId, DataCallback<Transaction> callback) {
        Transaction request = new Transaction();
        request.setRoomId(roomId);
        request.setUserId(userId);
        request.setCreatedBy(userName);
        request.setType("EXPENSE");
        request.setAmount(amount);
        request.setCategory(category);
        request.setNote(note);
        request.setBillImageUrl(billImageUrl);
        request.setRecurring(isRecurring);
        if (recurringRuleId != null && !recurringRuleId.trim().isEmpty()) {
            request.setRecurringRuleId(recurringRuleId);
        }
        request.setStatus("PENDING");
        request.setTransactionDate(java.time.LocalDate.now().toString());
        createTransaction(request, new DataCallback<Transaction>() {
            @Override public void onSuccess(Transaction created) {
                if (!approveImmediately) {
                    callback.onSuccess(created);
                    return;
                }
                approveExpenseTransaction(userId, created.getId(), new DataCallback<JsonObject>() {
                    @Override public void onSuccess(JsonObject result) {
                        created.setStatus("APPROVED");
                        if (result.has("new_room_balance") && !result.get("new_room_balance").isJsonNull())
                            created.setBalanceAfter(result.get("new_room_balance").getAsDouble());
                        created.setApprovedBy(userId);
                        created.setApprovedAt(java.time.Instant.now().toString());
                        callback.onSuccess(created);
                    }
                    @Override public void onError(String message) {
                        callback.onError("Yêu cầu rút đã được ghi nhưng chưa duyệt tự động; giao dịch vẫn đang chờ duyệt: " + message);
                    }
                });
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    public void approveExpenseTransaction(String adminUserId, String transactionId,
                                          DataCallback<JsonObject> callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("p_admin_user_id", adminUserId);
        payload.addProperty("p_transaction_id", transactionId);
        callJsonbRpc("approve_expense_transaction", payload, new DataCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                if (callback != null) callback.onSuccess(result);
            }
            @Override
            public void onError(String message) {
                fallbackDirectApprove(adminUserId, transactionId, callback);
            }
        });
    }

    private void fallbackDirectApprove(String adminUserId, String transactionId, DataCallback<JsonObject> callback) {
        String txUrl = SupabaseConfig.REST_URL + "/transactions?id=eq." + transactionId + "&select=*";
        Request getTxReq = new Request.Builder()
                .url(txUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .get()
                .build();
        client.newCall(getTxReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("HTTP " + response.code() + ": " + body);
                    return;
                }
                try {
                    JsonArray array = JsonParser.parseString(body).getAsJsonArray();
                    if (array == null || array.size() == 0) {
                        if (callback != null) callback.onError("Không tìm thấy giao dịch.");
                        return;
                    }
                    JsonObject txObj = array.get(0).getAsJsonObject();
                    String roomId = txObj.has("room_id") && !txObj.get("room_id").isJsonNull() ? txObj.get("room_id").getAsString() : null;
                    double amount = txObj.has("amount") && !txObj.get("amount").isJsonNull() ? txObj.get("amount").getAsDouble() : 0.0;
                    String userId = txObj.has("user_id") && !txObj.get("user_id").isJsonNull() ? txObj.get("user_id").getAsString() : null;
                    String note = txObj.has("note") && !txObj.get("note").isJsonNull() ? txObj.get("note").getAsString() : "";
                    if (roomId == null) {
                        if (callback != null) callback.onError("Giao dịch không thuộc quỹ nào.");
                        return;
                    }
                    boolean isTreasurer = RoomRepository.getInstance().isUserRoomTreasurer(roomId, adminUserId);
                    boolean isOwner = RoomRepository.getInstance().isUserRoomOwner(roomId, adminUserId);
                    String roomUrl = SupabaseConfig.REST_URL + "/rooms?id=eq." + roomId + "&select=*";
                    Request getRoomReq = new Request.Builder()
                            .url(roomUrl)
                            .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                            .get()
                            .build();
                    client.newCall(getRoomReq).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call2, IOException e2) {
                            if (callback != null) callback.onError(e2.getMessage());
                        }
                        @Override
                        public void onResponse(Call call2, Response response2) throws IOException {
                            String rBody = response2.body() != null ? response2.body().string() : "";
                            if (!response2.isSuccessful()) {
                                if (callback != null) callback.onError("HTTP " + response2.code() + ": " + rBody);
                                return;
                            }
                            try {
                                JsonArray rArray = JsonParser.parseString(rBody).getAsJsonArray();
                                if (rArray == null || rArray.size() == 0) {
                                    if (callback != null) callback.onError("Không tìm thấy thông tin quỹ.");
                                    return;
                                }
                                JsonObject roomObj = rArray.get(0).getAsJsonObject();
                                String createdBy = roomObj.has("created_by") && !roomObj.get("created_by").isJsonNull() ? roomObj.get("created_by").getAsString() : "";
                                String desc = roomObj.has("description") && !roomObj.get("description").isJsonNull() ? roomObj.get("description").getAsString() : "";
                                boolean effectiveOwner = isOwner || (adminUserId != null && adminUserId.equalsIgnoreCase(createdBy));
                                boolean effectiveTreasurer = isTreasurer || (adminUserId != null && desc != null && desc.contains(adminUserId));
                                if (!effectiveOwner && !effectiveTreasurer) {
                                    if (callback != null) callback.onError("Bạn không có quyền quản lý quỹ này.");
                                    return;
                                }
                                if (!effectiveOwner && effectiveTreasurer && amount > 5000000.0) {
                                    if (callback != null) callback.onError("Khoản chi trên 5 triệu cần Chủ quỹ duyệt.");
                                    return;
                                }
                                double currentRoomBalance = roomObj.has("balance") && !roomObj.get("balance").isJsonNull() ? roomObj.get("balance").getAsDouble() : 0.0;
                                if (currentRoomBalance < amount) {
                                    if (callback != null) callback.onError("Số dư quỹ không đủ để thực hiện chi.");
                                    return;
                                }
                                double newRoomBalance = currentRoomBalance - amount;
                                JsonObject patchTx = new JsonObject();
                                patchTx.addProperty("status", "APPROVED");
                                patchTx.addProperty("balance_after", newRoomBalance);
                                patchTx.addProperty("approved_by", adminUserId);
                                patchTx.addProperty("approved_at", java.time.Instant.now().toString());
                                Request updateTxReq = new Request.Builder()
                                        .url(SupabaseConfig.REST_URL + "/transactions?id=eq." + transactionId)
                                        .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                                        .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                                        .addHeader("Content-Type", "application/json")
                                        .addHeader("Prefer", "return=representation")
                                        .patch(RequestBody.create(gson.toJson(patchTx), JSON))
                                        .build();
                                client.newCall(updateTxReq).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call3, IOException e3) {
                                        if (callback != null) callback.onError(e3.getMessage());
                                    }
                                    @Override
                                    public void onResponse(Call call3, Response response3) {
                                        if (!response3.isSuccessful()) {
                                            if (callback != null) callback.onError("Cập nhật giao dịch thất bại: " + response3.code());
                                            return;
                                        }
                                        JsonObject patchRoom = new JsonObject();
                                        patchRoom.addProperty("balance", newRoomBalance);
                                        Request updateRoomReq = new Request.Builder()
                                                .url(SupabaseConfig.REST_URL + "/rooms?id=eq." + roomId)
                                                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                                                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                                                .addHeader("Content-Type", "application/json")
                                                .patch(RequestBody.create(gson.toJson(patchRoom), JSON))
                                                .build();
                                        client.newCall(updateRoomReq).enqueue(new Callback() {
                                            @Override public void onFailure(Call call4, IOException e4) {}
                                            @Override public void onResponse(Call call4, Response response4) {}
                                        });
                                        if (note != null && note.contains("Ví Koin") && userId != null) {
                                            String profileUrl = SupabaseConfig.REST_URL + "/profiles?id=eq." + userId + "&select=*";
                                            Request getProfReq = new Request.Builder()
                                                    .url(profileUrl)
                                                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                                                    .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                                                    .get()
                                                    .build();
                                            client.newCall(getProfReq).enqueue(new Callback() {
                                                @Override public void onFailure(Call call5, IOException e5) {}
                                                @Override
                                                public void onResponse(Call call5, Response response5) throws IOException {
                                                    if (response5.isSuccessful() && response5.body() != null) {
                                                        try {
                                                            JsonArray pArr = JsonParser.parseString(response5.body().string()).getAsJsonArray();
                                                            if (pArr != null && pArr.size() > 0) {
                                                                JsonObject pObj = pArr.get(0).getAsJsonObject();
                                                                double curWallet = pObj.has("wallet_balance") && !pObj.get("wallet_balance").isJsonNull() ? pObj.get("wallet_balance").getAsDouble() : 0.0;
                                                                JsonObject patchWallet = new JsonObject();
                                                                patchWallet.addProperty("wallet_balance", curWallet + amount);
                                                                Request upWReq = new Request.Builder()
                                                                        .url(SupabaseConfig.REST_URL + "/profiles?id=eq." + userId)
                                                                        .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                                                                        .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                                                                        .addHeader("Content-Type", "application/json")
                                                                        .patch(RequestBody.create(gson.toJson(patchWallet), JSON))
                                                                        .build();
                                                                client.newCall(upWReq).enqueue(new Callback() {
                                                                    @Override public void onFailure(Call c, IOException e) {}
                                                                    @Override public void onResponse(Call c, Response r) {}
                                                                });
                                                            }
                                                        } catch (Exception ignored) {}
                                                    }
                                                }
                                            });
                                        }
                                        JsonObject successResult = new JsonObject();
                                        successResult.addProperty("success", true);
                                        successResult.addProperty("new_room_balance", newRoomBalance);
                                        if (callback != null) callback.onSuccess(successResult);
                                    }
                                });
                            } catch (Exception e) {
                                if (callback != null) callback.onError(e.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    if (callback != null) callback.onError(e.getMessage());
                }
            }
        });
    }

    public void rejectExpenseTransaction(String adminUserId, String transactionId, String reason,
                                         DataCallback<JsonObject> callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("p_admin_user_id", adminUserId);
        payload.addProperty("p_transaction_id", transactionId);
        payload.addProperty("p_rejection_reason", reason);
        callJsonbRpc("reject_expense_transaction", payload, new DataCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                if (callback != null) callback.onSuccess(result);
            }
            @Override
            public void onError(String message) {
                fallbackDirectReject(adminUserId, transactionId, reason, callback);
            }
        });
    }

    private void fallbackDirectReject(String adminUserId, String transactionId, String reason, DataCallback<JsonObject> callback) {
        JsonObject patchTx = new JsonObject();
        patchTx.addProperty("status", "REJECTED");
        patchTx.addProperty("rejection_reason", reason);
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/transactions?id=eq." + transactionId)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(RequestBody.create(gson.toJson(patchTx), JSON))
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("Từ chối giao dịch thất bại: HTTP " + response.code());
                    return;
                }
                JsonObject successResult = new JsonObject();
                successResult.addProperty("success", true);
                if (callback != null) callback.onSuccess(successResult);
            }
        });
    }

    public void reviewWithdrawal(String adminUserId, String transactionId, boolean approve, String reason,
                                 DataCallback<JsonObject> callback) {
        if (approve) approveExpenseTransaction(adminUserId, transactionId, callback);
        else rejectExpenseTransaction(adminUserId, transactionId, reason, callback);
    }

    private void callJsonbRpc(String functionName, JsonObject payload, DataCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/rpc/" + functionName)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + body);
                    return;
                }
                try {
                    JsonElement root = JsonParser.parseString(body);
                    if (!root.isJsonObject()) { callback.onError("Supabase returned an invalid RPC result."); return; }
                    JsonObject result = root.getAsJsonObject();
                    if (result.has("success") && !result.get("success").getAsBoolean()) {
                        callback.onError(result.has("message") ? result.get("message").getAsString()
                                : "Supabase rejected the operation.");
                        return;
                    }
                    callback.onSuccess(result);
                } catch (Exception e) { callback.onError("Invalid transaction response: " + e.getMessage()); }
            }
        });
    }

    public void approveExpense(String adminUserId, String transactionId, DataCallback<JsonObject> callback) {
        approveExpenseTransaction(adminUserId, transactionId, callback);
    }

    public void rejectExpense(String adminUserId, String transactionId, String reason, DataCallback<JsonObject> callback) {
        rejectExpenseTransaction(adminUserId, transactionId, reason, callback);
    }

    public void refreshRoomTransactions(String roomId, DataCallback<List<Transaction>> callback) {
        getTransactionsByRoom(roomId, callback);
    }
    public void findConfirmedPayment(String roomId, double amount, String createdAfter,
                                     DataCallback<Transaction> callback) {
        try {
            String after = URLEncoder.encode(createdAfter, StandardCharsets.UTF_8.name());
            String roomFilter = (roomId == null || "WALLET".equalsIgnoreCase(roomId))
                    ? "room_id=is.null"
                    : "room_id=eq." + roomId;
            String url = SupabaseConfig.REST_URL + "/transactions?" + roomFilter
                    + "&amount=eq." + (long) amount + "&created_at=gte." + after
                    + "&select=*&order=created_at.desc";
            Request request = new Request.Builder().url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + getEffectiveToken()).get().build();
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        callback.onError("HTTP " + response.code() + ": " + json); return;
                    }
                    Type listType = new TypeToken<List<Transaction>>() {}.getType();
                    List<Transaction> rows = gson.fromJson(json, listType);
                    if (rows != null) for (Transaction transaction : rows) {
                        String status = transaction.getStatus();
                        if (status != null && ("success".equalsIgnoreCase(status)
                                || "completed".equalsIgnoreCase(status)
                                || "confirmed".equalsIgnoreCase(status)
                                || "approved".equalsIgnoreCase(status)
                                || "paid".equalsIgnoreCase(status))) {
                            callback.onSuccess(transaction); return;
                        }
                    }
                    callback.onSuccess(null);
                }
            });
        } catch (Exception e) { callback.onError("Invalid payment timestamp: " + e.getMessage()); }
    }

    public void getUserTransactions(String userId, DataCallback<List<Transaction>> callback) {
        String url = SupabaseConfig.REST_URL + "/transactions?user_id=eq." + userId + "&select=*&order=created_at.desc";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<Transaction>>() {}.getType();
                List<Transaction> list = gson.fromJson(json, listType);
                callback.onSuccess(list);
            }
        });
    }

    public void getFilteredTransactions(String roomId, String type, String status, DataCallback<List<Transaction>> callback) {
        StringBuilder urlBuilder = new StringBuilder(SupabaseConfig.REST_URL + "/transactions?room_id=eq." + roomId);
        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            urlBuilder.append("&type=eq.").append(type.trim().toUpperCase());
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            urlBuilder.append("&status=eq.").append(status.trim().toUpperCase());
        }
        urlBuilder.append("&select=*&order=created_at.desc");

        Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<Transaction>>() {}.getType();
                List<Transaction> list = gson.fromJson(json, listType);
                callback.onSuccess(list);
            }
        });
    }

    public void withdrawWallet(String userId, double amount, String bankName, String accountNumber,
                               String accountHolder, DataCallback<JsonObject> callback) {
        String url = SupabaseConfig.REST_URL + "/rpc/withdraw_wallet";
        JsonObject params = new JsonObject();
        params.addProperty("p_user_id", userId);
        params.addProperty("p_amount", amount);
        params.addProperty("p_bank_name", bankName != null ? bankName : "");
        params.addProperty("p_account_number", accountNumber != null ? accountNumber : "");
        params.addProperty("p_account_holder", accountHolder != null ? accountHolder : "");

        RequestBody body = RequestBody.create(params.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                try {
                    JsonObject result = JsonParser.parseString(json).getAsJsonObject();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        callback.onSuccess(result);
                    } else {
                        String msg = result.has("message") ? result.get("message").getAsString() : "Withdrawal failed";
                        callback.onError(msg);
                    }
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    public void findConfirmedWalletPayment(String userId, double amount, String createdAfter,
                                           DataCallback<Transaction> callback) {
        try {
            String after = URLEncoder.encode(createdAfter, StandardCharsets.UTF_8.name());
            String url = SupabaseConfig.REST_URL + "/transactions?user_id=eq." + userId
                    + "&room_id=is.null&amount=eq." + (long) amount
                    + "&created_at=gte." + after
                    + "&select=*&order=created_at.desc";
            Request request = new Request.Builder().url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + getEffectiveToken()).get().build();
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        callback.onError("HTTP " + response.code() + ": " + json); return;
                    }
                    Type listType = new TypeToken<List<Transaction>>() {}.getType();
                    List<Transaction> rows = gson.fromJson(json, listType);
                    if (rows != null) for (Transaction transaction : rows) {
                        String status = transaction.getStatus();
                        if (isConfirmedStatus(status)) { callback.onSuccess(transaction); return; }
                    }
                    callback.onSuccess(null);
                }
            });
        } catch (Exception e) { callback.onError("Invalid wallet payment timestamp: " + e.getMessage()); }
    }

    private boolean isConfirmedStatus(String status) {
        return status != null && ("success".equalsIgnoreCase(status)
                || "completed".equalsIgnoreCase(status)
                || "confirmed".equalsIgnoreCase(status)
                || "paid".equalsIgnoreCase(status));
    }

    public void getExpenseCategories(DataCallback<List<ExpenseCategory>> callback) {
        String url = SupabaseConfig.REST_URL + "/expense_categories?select=*&order=id.asc";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<ExpenseCategory>>() {}.getType();
                List<ExpenseCategory> list = gson.fromJson(json, listType);
                callback.onSuccess(list);
            }
        });
    }

    public void uploadReceiptImage(byte[] imageBytes, String mimeType, DataCallback<String> callback) {
        if (imageBytes == null || imageBytes.length == 0) {
            callback.onError("Dữ liệu ảnh trống");
            return;
        }
        String ext = "jpg";
        if ("image/png".equalsIgnoreCase(mimeType)) ext = "png";
        else if ("image/webp".equalsIgnoreCase(mimeType)) ext = "webp";
        String fileName = "receipt_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
        String uploadUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/receipts/" + fileName;
        String publicUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/receipts/" + fileName;

        RequestBody body = RequestBody.create(imageBytes, MediaType.get(mimeType != null ? mimeType : "image/jpeg"));
        Request request = new Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + responseBody);
                    return;
                }
                callback.onSuccess(publicUrl);
            }
        });
    }

    public void getRecurringRules(String userId, String roomId, DataCallback<List<RecurringRule>> callback) {
        StringBuilder urlBuilder = new StringBuilder(SupabaseConfig.REST_URL + "/recurring_rules?");
        if (roomId != null && !roomId.trim().isEmpty()) {
            urlBuilder.append("room_id=eq.").append(roomId);
        } else if (userId != null && !userId.trim().isEmpty()) {
            urlBuilder.append("user_id=eq.").append(userId).append("&scope=eq.PERSONAL");
        } else {
            urlBuilder.append("select=*");
        }
        urlBuilder.append("&select=*&order=created_at.desc");

        Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<RecurringRule>>() {}.getType();
                List<RecurringRule> list = gson.fromJson(json, listType);
                callback.onSuccess(list);
            }
        });
    }

    public void createRecurringRule(RecurringRule rule, DataCallback<RecurringRule> callback) {
        String url = SupabaseConfig.REST_URL + "/recurring_rules";
        String jsonBody = gson.toJson(rule);
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<RecurringRule>>() {}.getType();
                List<RecurringRule> createdList = gson.fromJson(json, listType);
                if (createdList != null && !createdList.isEmpty()) callback.onSuccess(createdList.get(0));
                else callback.onError("Supabase returned empty recurring rule.");
            }
        });
    }

    public void toggleRecurringRule(String ruleId, boolean isActive, DataCallback<Boolean> callback) {
        String url = SupabaseConfig.REST_URL + "/recurring_rules?id=eq." + ruleId;
        JsonObject patch = new JsonObject();
        patch.addProperty("is_active", isActive);
        RequestBody body = RequestBody.create(patch.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .addHeader("Content-Type", "application/json")
                .patch(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String json = response.body() != null ? response.body().string() : "";
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                callback.onSuccess(isActive);
            }
        });
    }

    public void deleteRecurringRule(String ruleId, DataCallback<Boolean> callback) {
        String url = SupabaseConfig.REST_URL + "/recurring_rules?id=eq." + ruleId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String json = response.body() != null ? response.body().string() : "";
                    callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                callback.onSuccess(true);
            }
        });
    }

    public void updateRecurringRule(RecurringRule rule, DataCallback<RecurringRule> callback) {
        if (rule == null || rule.getId() == null) {
            if (callback != null) callback.onError("Rule ID is required for update.");
            return;
        }
        String url = SupabaseConfig.REST_URL + "/recurring_rules?id=eq." + rule.getId();
        String jsonBody = gson.toJson(rule);
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<RecurringRule>>() {}.getType();
                List<RecurringRule> updatedList = gson.fromJson(json, listType);
                if (updatedList != null && !updatedList.isEmpty()) {
                    if (callback != null) callback.onSuccess(updatedList.get(0));
                } else if (callback != null) {
                    callback.onSuccess(rule);
                }
            }
        });
    }

}
