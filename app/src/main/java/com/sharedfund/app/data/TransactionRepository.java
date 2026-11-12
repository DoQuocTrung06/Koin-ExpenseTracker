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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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
    private static final Set<String> inFlightOperations = Collections.synchronizedSet(new HashSet<>());
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            this.accessToken = accessToken;
            RecurringTransactionService.getInstance().setAccessToken(accessToken);
            ExpenseApprovalService.getInstance().setAccessToken(accessToken);
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

    private <T> DataCallback<T> wrapRelease(final String opKey, final DataCallback<T> callback) {
        return new DataCallback<T>() {
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

    public static String computeRecurringCycleKey(String frequency, ZonedDateTime zonedNow) {
        return RecurringTransactionService.computeRecurringCycleKey(frequency, zonedNow);
    }

    public static String computeRecurringCycleStartIsoUtc(String frequency, ZonedDateTime zonedNow) {
        return RecurringTransactionService.computeRecurringCycleStartIsoUtc(frequency, zonedNow);
    }

    public static String buildRecurringIdempotencyUuid(String ruleId, String cycleKey, int rejectedCount) {
        return RecurringTransactionService.buildRecurringIdempotencyUuid(ruleId, cycleKey, rejectedCount);
    }

    public static boolean isActiveRecurringTxStatus(String status) {
        return RecurringTransactionService.isActiveRecurringTxStatus(status);
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
                    String lower = json.toLowerCase(Locale.ROOT);
                    if (response.code() == 409 || lower.contains("23505") || lower.contains("duplicate key")) {
                        if (transaction != null && transaction.isRecurring()) {
                            callback.onError("Khoản chi định kỳ này đang được xử lý hoặc đã thanh toán trong kỳ!");
                        } else {
                            callback.onError("Giao dịch này đã được ghi nhận hoặc xử lý trước đó!");
                        }
                        return;
                    }
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
        String opKey = "contribute:" + safeKey(userId) + ":" + safeKey(roomId);
        if (!inFlightOperations.add(opKey)) {
            if (callback != null) callback.onError("Giao dịch góp quỹ đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        DataCallback<JsonObject> wrapped = wrapRelease(opKey, callback);
        JsonObject body = new JsonObject();
        body.addProperty("p_user_id", userId);
        body.addProperty("p_room_id", roomId);
        body.addProperty("p_amount", amount);
        body.addProperty("p_note", note);
        callJsonbRpc("transfer_wallet_to_room", body, wrapped);
    }

    public void payRoomRecipient(String roomId, double amount, String category, String note,
                                 String bankBin, String accountNumber, String recipientName,
                                 String transferContent, DataCallback<JsonObject> callback) {
        String opKey = "qr_pay:" + safeKey(roomId);
        if (!inFlightOperations.add(opKey)) {
            if (callback != null) callback.onError("Giao dịch chuyển khoản đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        DataCallback<JsonObject> wrapped = wrapRelease(opKey, callback);
        JsonObject body = new JsonObject();
        body.addProperty("p_room_id", roomId);
        body.addProperty("p_amount", amount);
        body.addProperty("p_category", category);
        body.addProperty("p_note", note);
        body.addProperty("p_bank_bin", bankBin);
        body.addProperty("p_account_number", accountNumber);
        body.addProperty("p_recipient_name", recipientName);
        body.addProperty("p_transfer_content", transferContent);
        callJsonbRpc("pay_room_qr_recipient", body, wrapped);
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

    public void submitWithdrawalWithId(String clientRequestId, String userId, String userName, String roomId,
                                       double amount, String category, String note, String billImageUrl,
                                       boolean approveImmediately, DataCallback<Transaction> callback) {
        String opKey = "withdraw:" + safeKey(userId) + ":" + safeKey(roomId);
        if (!inFlightOperations.add(opKey)) {
            if (callback != null) callback.onError("Giao dịch rút quỹ đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        DataCallback<Transaction> wrapped = wrapRelease(opKey, callback);
        executeStandardWithdrawal(clientRequestId, userId, userName, roomId, amount, category, note, billImageUrl, approveImmediately, wrapped);
    }

    public void submitRecurringWithdrawal(String userId, String userName, String roomId, double amount,
                                          String category, String note, boolean approveImmediately,
                                          String recurringRuleId, String frequency, DataCallback<Transaction> callback) {
        RecurringTransactionService.getInstance().submitRecurringWithdrawal(this, userId, userName, roomId, amount, category, note, approveImmediately, recurringRuleId, frequency, callback);
    }

    public void submitWithdrawal(String userId, String userName, String roomId, double amount, String category, String note,
                                 String billImageUrl, boolean approveImmediately, boolean isRecurring, String recurringRuleId, DataCallback<Transaction> callback) {
        if (isRecurring && recurringRuleId != null && !recurringRuleId.trim().isEmpty()) {
            RecurringTransactionService.getInstance().resolveAndSubmitRecurring(this, userId, userName, roomId, amount, category, note, approveImmediately, recurringRuleId, callback);
            return;
        }
        String opKey = "withdraw:" + safeKey(userId) + ":" + safeKey(roomId);
        if (!inFlightOperations.add(opKey)) {
            if (callback != null) callback.onError("Giao dịch rút quỹ đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        DataCallback<Transaction> wrapped = wrapRelease(opKey, callback);
        executeStandardWithdrawal(null, userId, userName, roomId, amount, category, note, billImageUrl, approveImmediately, wrapped);
    }

    private void executeStandardWithdrawal(String clientRequestId, String userId, String userName, String roomId,
                                           double amount, String category, String note, String billImageUrl,
                                           boolean approveImmediately, DataCallback<Transaction> callback) {
        Transaction request = new Transaction();
        if (clientRequestId != null && !clientRequestId.trim().isEmpty()) {
            request.setId(clientRequestId.trim());
        }
        request.setRoomId(roomId);
        request.setUserId(userId);
        request.setCreatedBy(userName);
        request.setType("EXPENSE");
        request.setAmount(amount);
        request.setCategory(category);
        request.setNote(note);
        request.setBillImageUrl(billImageUrl);
        request.setRecurring(false);
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
        ExpenseApprovalService.getInstance().approveExpenseTransaction(adminUserId, transactionId, callback);
    }

    public void rejectExpenseTransaction(String adminUserId, String transactionId, String reason,
                                         DataCallback<JsonObject> callback) {
        ExpenseApprovalService.getInstance().rejectExpenseTransaction(adminUserId, transactionId, reason, callback);
    }

    public void reviewWithdrawal(String adminUserId, String transactionId, boolean approve, String reason,
                                 DataCallback<JsonObject> callback) {
        ExpenseApprovalService.getInstance().reviewWithdrawal(adminUserId, transactionId, approve, reason, callback);
    }

    void callJsonbRpc(String functionName, JsonObject payload, DataCallback<JsonObject> callback) {
        ExpenseApprovalService.getInstance().callJsonbRpc(functionName, payload, callback);
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
        String opKey = "wallet_withdraw:" + safeKey(userId);
        if (!inFlightOperations.add(opKey)) {
            if (callback != null) callback.onError("Yêu cầu rút tiền ví đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        DataCallback<JsonObject> wrapped = wrapRelease(opKey, callback);
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
                wrapped.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    wrapped.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                try {
                    JsonObject result = JsonParser.parseString(json).getAsJsonObject();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        wrapped.onSuccess(result);
                    } else {
                        String msg = result.has("message") ? result.get("message").getAsString() : "Withdrawal failed";
                        wrapped.onError(msg);
                    }
                } catch (Exception e) {
                    wrapped.onError("Parse error: " + e.getMessage());
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
        RoomCategoryManager.getInstance().getExpenseCategories(getEffectiveToken(), new RoomRepository.DataCallback<List<ExpenseCategory>>() {
            @Override
            public void onSuccess(List<ExpenseCategory> data) {
                if (callback != null) callback.onSuccess(data);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
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
        RecurringTransactionService.getInstance().getRecurringRules(userId, roomId, callback);
    }

    public void createRecurringRule(RecurringRule rule, DataCallback<RecurringRule> callback) {
        RecurringTransactionService.getInstance().createRecurringRule(rule, callback);
    }

    public void toggleRecurringRule(String ruleId, boolean isActive, DataCallback<Boolean> callback) {
        RecurringTransactionService.getInstance().toggleRecurringRule(ruleId, isActive, callback);
    }

    public void deleteRecurringRule(String ruleId, DataCallback<Boolean> callback) {
        RecurringTransactionService.getInstance().deleteRecurringRule(ruleId, callback);
    }

    public void updateRecurringRule(RecurringRule rule, DataCallback<RecurringRule> callback) {
        RecurringTransactionService.getInstance().updateRecurringRule(rule, callback);
    }

}
