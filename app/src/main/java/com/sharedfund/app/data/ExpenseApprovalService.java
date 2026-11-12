package com.sharedfund.app.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sharedfund.app.SharedFundApp;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ExpenseApprovalService {

    private static volatile ExpenseApprovalService instance;

    private final OkHttpClient client;
    private final Gson gson;
    private volatile String accessToken = SupabaseConfig.SUPABASE_ANON_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Set<String> inFlightOperations = Collections.synchronizedSet(new HashSet<>());

    private ExpenseApprovalService() {
        this.client = new OkHttpClient.Builder()
                .dns(SafeDns.getInstance())
                .authenticator(new SupabaseAuthenticator(SharedFundApp.getInstance()))
                .build();
        this.gson = new Gson();
    }

    public static ExpenseApprovalService getInstance() {
        if (instance == null) {
            synchronized (ExpenseApprovalService.class) {
                if (instance == null) {
                    instance = new ExpenseApprovalService();
                }
            }
        }
        return instance;
    }

    public void setAccessToken(String accessToken) {
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            this.accessToken = accessToken;
        }
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

    private static String safeKey(String value) {
        return value != null ? value.trim() : "";
    }

    private <T> TransactionRepository.DataCallback<T> wrapRelease(final String opKey, final TransactionRepository.DataCallback<T> callback) {
        return new TransactionRepository.DataCallback<T>() {
            @Override
            public void onSuccess(T data) {
                if (opKey != null) {
                    inFlightOperations.remove(opKey);
                }
                if (callback != null) {
                    callback.onSuccess(data);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (opKey != null) {
                    inFlightOperations.remove(opKey);
                }
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        };
    }

    private static BigDecimal readBigDecimal(JsonObject obj, String field) {
        if (obj == null || !obj.has(field) || obj.get(field).isJsonNull()) {
            return BigDecimal.ZERO;
        }
        try {
            return obj.get(field).getAsBigDecimal();
        } catch (Exception e) {
            try {
                return BigDecimal.valueOf(obj.get(field).getAsDouble());
            } catch (Exception e2) {
                return BigDecimal.ZERO;
            }
        }
    }

    public void approveExpenseTransaction(String adminUserId, String transactionId,
                                          TransactionRepository.DataCallback<JsonObject> callback) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            if (callback != null) callback.onError("Không tìm thấy mã giao dịch.");
            return;
        }
        String opKey = "review_tx:" + transactionId.trim();
        if (!inFlightOperations.add(opKey)) {
            if (callback != null) callback.onError("Giao dịch đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        TransactionRepository.DataCallback<JsonObject> wrapped = wrapRelease(opKey, callback);
        JsonObject payload = new JsonObject();
        payload.addProperty("p_admin_user_id", adminUserId);
        payload.addProperty("p_transaction_id", transactionId);
        callJsonbRpc("approve_expense_transaction", payload, new TransactionRepository.DataCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                wrapped.onSuccess(result);
            }

            @Override
            public void onError(String message) {
                fallbackDirectApprove(adminUserId, transactionId, wrapped);
            }
        });
    }

    private void fallbackDirectApprove(String adminUserId, String transactionId, TransactionRepository.DataCallback<JsonObject> callback) {
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
                    String txStatus = txObj.has("status") && !txObj.get("status").isJsonNull() ? txObj.get("status").getAsString() : "";
                    if (!"PENDING".equalsIgnoreCase(txStatus.trim())) {
                        if (callback != null) callback.onError("Giao dịch này đã được xử lý trước đó!");
                        return;
                    }
                    String roomId = txObj.has("room_id") && !txObj.get("room_id").isJsonNull() ? txObj.get("room_id").getAsString() : null;
                    double amount = txObj.has("amount") && !txObj.get("amount").isJsonNull() ? txObj.get("amount").getAsDouble() : 0.0;
                    BigDecimal amountBd = readBigDecimal(txObj, "amount");
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
                                BigDecimal currentRoomBalanceBd = readBigDecimal(roomObj, "balance");
                                if (currentRoomBalanceBd.compareTo(amountBd) < 0) {
                                    if (callback != null) callback.onError("Số dư quỹ không đủ để thực hiện chi.");
                                    return;
                                }
                                BigDecimal estimatedNewBalanceBd = currentRoomBalanceBd.subtract(amountBd);
                                JsonObject patchTx = new JsonObject();
                                patchTx.addProperty("status", "APPROVED");
                                patchTx.addProperty("balance_after", estimatedNewBalanceBd);
                                patchTx.addProperty("approved_by", adminUserId);
                                patchTx.addProperty("approved_at", java.time.Instant.now().toString());
                                Request updateTxReq = new Request.Builder()
                                        .url(SupabaseConfig.REST_URL + "/transactions?id=eq." + transactionId + "&status=eq.PENDING")
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
                                    public void onResponse(Call call3, Response response3) throws IOException {
                                        String res3Body = response3.body() != null ? response3.body().string() : "";
                                        if (!response3.isSuccessful()) {
                                            if (callback != null) callback.onError("Cập nhật giao dịch thất bại: " + response3.code());
                                            return;
                                        }
                                        try {
                                            JsonArray updatedTxArr = JsonParser.parseString(res3Body).getAsJsonArray();
                                            if (updatedTxArr == null || updatedTxArr.size() == 0) {
                                                if (callback != null) callback.onError("Giao dịch này vừa được quản trị viên khác xử lý!");
                                                return;
                                            }
                                        } catch (Exception parseEx) {
                                            if (callback != null) callback.onError("Giao dịch này vừa được quản trị viên khác xử lý!");
                                            return;
                                        }
                                        deductRoomBalanceWithCas(
                                                roomId,
                                                amountBd,
                                                currentRoomBalanceBd,
                                                estimatedNewBalanceBd,
                                                transactionId,
                                                userId,
                                                note,
                                                3,
                                                callback
                                        );
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

    private void deductRoomBalanceWithCas(final String roomId, final BigDecimal amountBd,
                                          final BigDecimal expectedBalanceBd, final BigDecimal initialEstimatedBalanceBd,
                                          final String transactionId, final String userId, final String note,
                                          final int retriesLeft, final TransactionRepository.DataCallback<JsonObject> callback) {
        final BigDecimal nextBalanceBd = expectedBalanceBd.subtract(amountBd);
        JsonObject patchRoom = new JsonObject();
        patchRoom.addProperty("balance", nextBalanceBd);
        String casUrl = SupabaseConfig.REST_URL + "/rooms?id=eq." + roomId + "&balance=eq." + expectedBalanceBd.toPlainString();
        Request updateRoomReq = new Request.Builder()
                .url(casUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(RequestBody.create(gson.toJson(patchRoom), JSON))
                .build();
        client.newCall(updateRoomReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                rollbackPendingTransaction(transactionId);
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    rollbackPendingTransaction(transactionId);
                    if (callback != null) callback.onError("Trừ số dư quỹ thất bại: HTTP " + response.code());
                    return;
                }
                boolean casSuccess = false;
                try {
                    JsonArray updatedRoomArr = JsonParser.parseString(resBody).getAsJsonArray();
                    casSuccess = updatedRoomArr != null && updatedRoomArr.size() > 0;
                } catch (Exception ignored) {
                }
                if (casSuccess) {
                    if (initialEstimatedBalanceBd != null && !initialEstimatedBalanceBd.equals(nextBalanceBd)) {
                        updateTransactionBalanceAfter(transactionId, nextBalanceBd);
                    }
                    if (userId != null && !userId.trim().isEmpty()) {
                        creditWalletWithCas(userId, amountBd, 3, new Runnable() {
                            @Override
                            public void run() {
                                finishApproveSuccess(nextBalanceBd, callback);
                            }
                        });
                    } else {
                        finishApproveSuccess(nextBalanceBd, callback);
                    }
                    return;
                }

                if (retriesLeft <= 0) {
                    rollbackPendingTransaction(transactionId);
                    if (callback != null) callback.onError("Số dư quỹ bị xung đột do có giao dịch khác xử lý cùng lúc. Vui lòng thử lại!");
                    return;
                }

                String refetchUrl = SupabaseConfig.REST_URL + "/rooms?id=eq." + roomId + "&select=balance";
                Request refetchReq = new Request.Builder()
                        .url(refetchUrl)
                        .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                        .get()
                        .build();
                client.newCall(refetchReq).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call c2, IOException e2) {
                        rollbackPendingTransaction(transactionId);
                        if (callback != null) callback.onError("Không thể kiểm tra lại số dư quỹ: " + e2.getMessage());
                    }

                    @Override
                    public void onResponse(Call c2, Response r2) throws IOException {
                        String b2 = r2.body() != null ? r2.body().string() : "";
                        if (!r2.isSuccessful()) {
                            rollbackPendingTransaction(transactionId);
                            if (callback != null) callback.onError("HTTP " + r2.code() + ": " + b2);
                            return;
                        }
                        try {
                            JsonArray arr2 = JsonParser.parseString(b2).getAsJsonArray();
                            if (arr2 == null || arr2.size() == 0) {
                                rollbackPendingTransaction(transactionId);
                                if (callback != null) callback.onError("Không tìm thấy thông tin quỹ.");
                                return;
                            }
                            BigDecimal latestBalanceBd = readBigDecimal(arr2.get(0).getAsJsonObject(), "balance");
                            if (latestBalanceBd.compareTo(amountBd) < 0) {
                                rollbackPendingTransaction(transactionId);
                                if (callback != null) callback.onError("Số dư quỹ không đủ để thực hiện chi.");
                                return;
                            }
                            deductRoomBalanceWithCas(
                                    roomId,
                                    amountBd,
                                    latestBalanceBd,
                                    initialEstimatedBalanceBd,
                                    transactionId,
                                    userId,
                                    note,
                                    retriesLeft - 1,
                                    callback
                            );
                        } catch (Exception ex) {
                            rollbackPendingTransaction(transactionId);
                            if (callback != null) callback.onError(ex.getMessage());
                        }
                    }
                });
            }
        });
    }

    private void creditWalletWithCas(final String userId, final BigDecimal amountBd,
                                     final int retriesLeft, final Runnable onDone) {
        String profileUrl = SupabaseConfig.REST_URL + "/profiles?id=eq." + userId + "&select=wallet_balance";
        Request getProfReq = new Request.Builder()
                .url(profileUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .get()
                .build();
        client.newCall(getProfReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (onDone != null) onDone.run();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    if (onDone != null) onDone.run();
                    return;
                }
                try {
                    JsonArray pArr = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    if (pArr == null || pArr.size() == 0) {
                        if (onDone != null) onDone.run();
                        return;
                    }
                    JsonObject pObj = pArr.get(0).getAsJsonObject();
                    BigDecimal curWalletBd = readBigDecimal(pObj, "wallet_balance");
                    BigDecimal nextWalletBd = curWalletBd.add(amountBd);
                    JsonObject patchWallet = new JsonObject();
                    patchWallet.addProperty("wallet_balance", nextWalletBd);
                    Request upWReq = new Request.Builder()
                            .url(SupabaseConfig.REST_URL + "/profiles?id=eq." + userId + "&wallet_balance=eq." + curWalletBd.toPlainString())
                            .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "return=representation")
                            .patch(RequestBody.create(gson.toJson(patchWallet), JSON))
                            .build();
                    client.newCall(upWReq).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call c, IOException e) {
                            if (onDone != null) onDone.run();
                        }

                        @Override
                        public void onResponse(Call c, Response r) throws IOException {
                            String wb = r.body() != null ? r.body().string() : "";
                            boolean updated = false;
                            if (r.isSuccessful()) {
                                try {
                                    JsonArray uArr = JsonParser.parseString(wb).getAsJsonArray();
                                    updated = uArr != null && uArr.size() > 0;
                                } catch (Exception ignored) {
                                }
                            }
                            if (!updated && retriesLeft > 0) {
                                creditWalletWithCas(userId, amountBd, retriesLeft - 1, onDone);
                                return;
                            }
                            if (onDone != null) onDone.run();
                        }
                    });
                } catch (Exception ignored) {
                    if (onDone != null) onDone.run();
                }
            }
        });
    }

    private void finishApproveSuccess(BigDecimal newRoomBalanceBd, TransactionRepository.DataCallback<JsonObject> callback) {
        JsonObject successResult = new JsonObject();
        successResult.addProperty("success", true);
        successResult.addProperty("new_room_balance", newRoomBalanceBd != null ? newRoomBalanceBd.doubleValue() : 0.0);
        if (callback != null) callback.onSuccess(successResult);
    }

    private void updateTransactionBalanceAfter(String transactionId, BigDecimal finalBalance) {
        if (transactionId == null || finalBalance == null) return;
        JsonObject patch = new JsonObject();
        patch.addProperty("balance_after", finalBalance);
        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/transactions?id=eq." + transactionId)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .patch(RequestBody.create(gson.toJson(patch), JSON))
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });
    }

    private void rollbackPendingTransaction(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) return;
        JsonObject resetTx = new JsonObject();
        resetTx.addProperty("status", "PENDING");
        resetTx.add("balance_after", com.google.gson.JsonNull.INSTANCE);
        resetTx.add("approved_by", com.google.gson.JsonNull.INSTANCE);
        resetTx.add("approved_at", com.google.gson.JsonNull.INSTANCE);
        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/transactions?id=eq." + transactionId + "&status=eq.APPROVED")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .patch(RequestBody.create(gson.toJson(resetTx), JSON))
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });
    }

    public void rejectExpenseTransaction(String adminUserId, String transactionId, String reason,
                                         TransactionRepository.DataCallback<JsonObject> callback) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            if (callback != null) callback.onError("Không tìm thấy mã giao dịch.");
            return;
        }
        String opKey = "review_tx:" + transactionId.trim();
        if (!inFlightOperations.add(opKey)) {
            if (callback != null) callback.onError("Giao dịch đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        TransactionRepository.DataCallback<JsonObject> wrapped = wrapRelease(opKey, callback);
        JsonObject payload = new JsonObject();
        payload.addProperty("p_admin_user_id", adminUserId);
        payload.addProperty("p_transaction_id", transactionId);
        payload.addProperty("p_rejection_reason", reason);
        callJsonbRpc("reject_expense_transaction", payload, new TransactionRepository.DataCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                wrapped.onSuccess(result);
            }

            @Override
            public void onError(String message) {
                fallbackDirectReject(adminUserId, transactionId, reason, wrapped);
            }
        });
    }

    private void fallbackDirectReject(String adminUserId, String transactionId, String reason, TransactionRepository.DataCallback<JsonObject> callback) {
        JsonObject patchTx = new JsonObject();
        patchTx.addProperty("status", "REJECTED");
        patchTx.addProperty("rejection_reason", reason);
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/transactions?id=eq." + transactionId + "&status=eq.PENDING")
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
            public void onResponse(Call call, Response response) throws IOException {
                String resBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("Từ chối giao dịch thất bại: HTTP " + response.code());
                    return;
                }
                try {
                    JsonArray arr = JsonParser.parseString(resBody).getAsJsonArray();
                    if (arr == null || arr.size() == 0) {
                        if (callback != null) callback.onError("Giao dịch này vừa được quản trị viên khác xử lý!");
                        return;
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onError("Giao dịch này vừa được quản trị viên khác xử lý!");
                    return;
                }
                JsonObject successResult = new JsonObject();
                successResult.addProperty("success", true);
                if (callback != null) callback.onSuccess(successResult);
            }
        });
    }

    public void reviewWithdrawal(String adminUserId, String transactionId, boolean approve, String reason,
                                 TransactionRepository.DataCallback<JsonObject> callback) {
        if (approve) {
            approveExpenseTransaction(adminUserId, transactionId, callback);
        } else {
            rejectExpenseTransaction(adminUserId, transactionId, reason, callback);
        }
    }

    public void callJsonbRpc(String functionName, JsonObject payload, TransactionRepository.DataCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/rpc/" + functionName)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .build();
        client.newCall(request).enqueue(new Callback() {
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
                    JsonElement root = JsonParser.parseString(body);
                    if (!root.isJsonObject()) {
                        if (callback != null) callback.onError("Supabase returned an invalid RPC result.");
                        return;
                    }
                    JsonObject result = root.getAsJsonObject();
                    if (result.has("success") && !result.get("success").getAsBoolean()) {
                        if (callback != null) {
                            callback.onError(result.has("message") ? result.get("message").getAsString()
                                    : "Supabase rejected the operation.");
                        }
                        return;
                    }
                    if (callback != null) callback.onSuccess(result);
                } catch (Exception e) {
                    if (callback != null) callback.onError("Invalid transaction response: " + e.getMessage());
                }
            }
        });
    }
}
