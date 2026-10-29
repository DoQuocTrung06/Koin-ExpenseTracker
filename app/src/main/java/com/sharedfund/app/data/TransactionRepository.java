package com.sharedfund.app.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public void setAccessToken(String accessToken) {
        if (accessToken != null && !accessToken.trim().isEmpty()) this.accessToken = accessToken;
    }

    public void getTransactionsByRoom(String roomId, DataCallback<List<Transaction>> callback) {
        String url = SupabaseConfig.REST_URL + "/transactions?room_id=eq." + roomId + "&select=*&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
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
                .addHeader("Authorization", "Bearer " + accessToken)
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

    /** Read the SePay webhook result; the client never inserts a payment-confirmation row. */
    public void findConfirmedPayment(String roomId, double amount, String createdAfter,
                                     DataCallback<Transaction> callback) {
        try {
            String after = URLEncoder.encode(createdAfter, StandardCharsets.UTF_8.name());
            String url = SupabaseConfig.REST_URL + "/transactions?room_id=eq." + roomId
                    + "&amount=eq." + (long) amount + "&created_at=gte." + after
                    + "&select=*&order=created_at.desc";
            Request request = new Request.Builder().url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + accessToken).get().build();
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
                                || "paid".equalsIgnoreCase(status))) {
                            callback.onSuccess(transaction); return;
                        }
                    }
                    callback.onSuccess(null);
                }
            });
        } catch (Exception e) { callback.onError("Invalid payment timestamp: " + e.getMessage()); }
    }
}
