package com.sharedfund.app.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sharedfund.app.SharedFundApp;
import com.sharedfund.app.model.Transaction;

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
        if ((accessToken == null || accessToken.equals(SupabaseConfig.SUPABASE_ANON_KEY)) && SharedFundApp.getInstance() != null) {
            String token = UserSessionManager.getInstance(SharedFundApp.getInstance()).getAccessToken();
            if (token != null && !token.trim().isEmpty()) {
                return token;
            }
        }
        return accessToken;
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
        Transaction request = new Transaction();
        request.setRoomId(roomId);
        request.setUserId(userId);
        request.setCreatedBy(userName);
        request.setType("EXPENSE");
        request.setAmount(amount);
        request.setCategory(category);
        request.setNote(note);
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
        callJsonbRpc("approve_expense_transaction", payload, callback);
    }

    public void rejectExpenseTransaction(String adminUserId, String transactionId, String reason,
                                         DataCallback<JsonObject> callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("p_admin_user_id", adminUserId);
        payload.addProperty("p_transaction_id", transactionId);
        payload.addProperty("p_rejection_reason", reason);
        callJsonbRpc("reject_expense_transaction", payload, callback);
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
}
