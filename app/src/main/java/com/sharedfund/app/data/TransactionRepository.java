package com.sharedfund.app.data;

import com.google.gson.Gson;
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
        String url = SupabaseConfig.REST_URL + "/rpc/transfer_wallet_to_room";
        JsonObject params = new JsonObject();
        params.addProperty("p_user_id", userId);
        params.addProperty("p_room_id", roomId);
        params.addProperty("p_amount", amount);
        params.addProperty("p_note", (note != null && !note.trim().isEmpty()) ? note : "Contribution from wallet");

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
                        String msg = result.has("message") ? result.get("message").getAsString() : "Transaction failed";
                        callback.onError(msg);
                    }
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    public void approveExpense(String adminUserId, String transactionId, DataCallback<JsonObject> callback) {
        String url = SupabaseConfig.REST_URL + "/rpc/approve_expense_transaction";
        JsonObject params = new JsonObject();
        params.addProperty("p_admin_user_id", adminUserId);
        params.addProperty("p_transaction_id", transactionId);

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
                        String msg = result.has("message") ? result.get("message").getAsString() : "Approval failed";
                        callback.onError(msg);
                    }
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    public void rejectExpense(String adminUserId, String transactionId, String reason, DataCallback<JsonObject> callback) {
        String url = SupabaseConfig.REST_URL + "/rpc/reject_expense_transaction";
        JsonObject params = new JsonObject();
        params.addProperty("p_admin_user_id", adminUserId);
        params.addProperty("p_transaction_id", transactionId);
        params.addProperty("p_rejection_reason", reason != null ? reason : "");

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
                        String msg = result.has("message") ? result.get("message").getAsString() : "Rejection failed";
                        callback.onError(msg);
                    }
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
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
}
