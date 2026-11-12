package com.sharedfund.app.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sharedfund.app.SharedFundApp;
import com.sharedfund.app.model.RecurringRule;
import com.sharedfund.app.model.Transaction;

import java.io.IOException;
import java.net.URLEncoder;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecurringTransactionService {

    private static volatile RecurringTransactionService instance;

    private final OkHttpClient client;
    private final Gson gson;
    private volatile String accessToken = SupabaseConfig.SUPABASE_ANON_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Set<String> inFlightOperations = Collections.synchronizedSet(new HashSet<>());
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private RecurringTransactionService() {
        this.client = new OkHttpClient.Builder()
                .dns(SafeDns.getInstance())
                .authenticator(new SupabaseAuthenticator(SharedFundApp.getInstance()))
                .build();
        this.gson = new Gson();
    }

    public static RecurringTransactionService getInstance() {
        if (instance == null) {
            synchronized (RecurringTransactionService.class) {
                if (instance == null) {
                    instance = new RecurringTransactionService();
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

    public static String computeRecurringCycleKey(String frequency, ZonedDateTime zonedNow) {
        ZonedDateTime target = zonedNow != null ? zonedNow : ZonedDateTime.now(VN_ZONE);
        if ("DAILY".equalsIgnoreCase(frequency)) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).format(target);
        }
        if ("WEEKLY".equalsIgnoreCase(frequency)) {
            int weekYear = target.get(IsoFields.WEEK_BASED_YEAR);
            int weekOfYear = target.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            return String.format(Locale.US, "%04d-W%02d", weekYear, weekOfYear);
        }
        return DateTimeFormatter.ofPattern("yyyy-MM", Locale.US).format(target);
    }

    public static String computeRecurringCycleStartIsoUtc(String frequency, ZonedDateTime zonedNow) {
        ZonedDateTime target = zonedNow != null ? zonedNow : ZonedDateTime.now(VN_ZONE);
        ZoneId zone = target.getZone();
        ZonedDateTime startOfCycle;
        if ("DAILY".equalsIgnoreCase(frequency)) {
            startOfCycle = target.toLocalDate().atStartOfDay(zone);
        } else if ("WEEKLY".equalsIgnoreCase(frequency)) {
            startOfCycle = target.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone);
        } else {
            startOfCycle = target.toLocalDate().withDayOfMonth(1).atStartOfDay(zone);
        }
        return startOfCycle.toInstant().toString();
    }

    public static String buildRecurringIdempotencyUuid(String ruleId, String cycleKey, int rejectedCount) {
        String safeRule = ruleId != null ? ruleId.trim() : "";
        String safeCycle = cycleKey != null ? cycleKey.trim() : "";
        String seed = "recurring:" + safeRule + ":" + safeCycle + ":attempt:" + Math.max(0, rejectedCount);
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static boolean isActiveRecurringTxStatus(String status) {
        if (status == null) return false;
        String norm = status.trim().toUpperCase(Locale.ROOT);
        return "PENDING".equals(norm)
                || "APPROVED".equals(norm)
                || "COMPLETED".equals(norm)
                || "SUCCESS".equals(norm)
                || "CONFIRMED".equals(norm)
                || "PAID".equals(norm);
    }

    public void getRecurringRules(String userId, String roomId, TransactionRepository.DataCallback<List<RecurringRule>> callback) {
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
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<RecurringRule>>() {}.getType();
                List<RecurringRule> list = gson.fromJson(json, listType);
                if (callback != null) callback.onSuccess(list);
            }
        });
    }

    public void createRecurringRule(RecurringRule rule, TransactionRepository.DataCallback<RecurringRule> callback) {
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
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                Type listType = new TypeToken<List<RecurringRule>>() {}.getType();
                List<RecurringRule> createdList = gson.fromJson(json, listType);
                if (createdList != null && !createdList.isEmpty()) {
                    if (callback != null) callback.onSuccess(createdList.get(0));
                } else if (callback != null) {
                    callback.onError("Supabase returned empty recurring rule.");
                }
            }
        });
    }

    public void toggleRecurringRule(String ruleId, boolean isActive, TransactionRepository.DataCallback<Boolean> callback) {
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
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String json = response.body() != null ? response.body().string() : "";
                    if (callback != null) callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                if (callback != null) callback.onSuccess(isActive);
            }
        });
    }

    public void deleteRecurringRule(String ruleId, TransactionRepository.DataCallback<Boolean> callback) {
        String url = SupabaseConfig.REST_URL + "/recurring_rules?id=eq." + ruleId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String json = response.body() != null ? response.body().string() : "";
                    if (callback != null) callback.onError("HTTP " + response.code() + ": " + json);
                    return;
                }
                if (callback != null) callback.onSuccess(true);
            }
        });
    }

    public void updateRecurringRule(RecurringRule rule, TransactionRepository.DataCallback<RecurringRule> callback) {
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
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
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

    public void submitRecurringWithdrawal(TransactionRepository txRepo, String userId, String userName, String roomId, double amount,
                                          String category, String note, boolean approveImmediately,
                                          String recurringRuleId, String frequency, TransactionRepository.DataCallback<Transaction> callback) {
        if (recurringRuleId == null || recurringRuleId.trim().isEmpty()) {
            if (txRepo != null) {
                txRepo.submitWithdrawal(userId, userName, roomId, amount, category, note, null, approveImmediately, true, null, callback);
            } else if (callback != null) {
                callback.onError("Mã khoản chi định kỳ không hợp lệ.");
            }
            return;
        }
        String opKey = "recurring_pay:" + safeKey(recurringRuleId);
        if (!inFlightOperations.add(opKey)) {
            if (callback != null) callback.onError("Khoản chi định kỳ này đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        TransactionRepository.DataCallback<Transaction> wrapped = wrapRelease(opKey, callback);
        executeRecurringSubmission(txRepo, userId, userName, roomId, amount, category, note, approveImmediately, recurringRuleId.trim(), frequency, wrapped);
    }

    public void resolveAndSubmitRecurring(TransactionRepository txRepo, String userId, String userName, String roomId, double amount,
                                          String category, String note, boolean approveImmediately, String recurringRuleId,
                                          TransactionRepository.DataCallback<Transaction> callback) {
        String opKey = "recurring_pay:" + safeKey(recurringRuleId);
        if (!inFlightOperations.add(opKey)) {
            if (callback != null) callback.onError("Khoản chi định kỳ này đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        TransactionRepository.DataCallback<Transaction> wrapped = wrapRelease(opKey, callback);
        String ruleUrl = SupabaseConfig.REST_URL + "/recurring_rules?id=eq." + recurringRuleId.trim() + "&select=frequency";
        Request ruleReq = new Request.Builder()
                .url(ruleUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .get()
                .build();
        client.newCall(ruleReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                executeRecurringSubmission(txRepo, userId, userName, roomId, amount, category, note, approveImmediately, recurringRuleId.trim(), RecurringRule.FREQUENCY_MONTHLY, wrapped);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String freq = RecurringRule.FREQUENCY_MONTHLY;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray arr = JsonParser.parseString(response.body().string()).getAsJsonArray();
                        if (arr != null && arr.size() > 0) {
                            JsonObject row = arr.get(0).getAsJsonObject();
                            if (row.has("frequency") && !row.get("frequency").isJsonNull()) {
                                freq = row.get("frequency").getAsString();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                executeRecurringSubmission(txRepo, userId, userName, roomId, amount, category, note, approveImmediately, recurringRuleId.trim(), freq, wrapped);
            }
        });
    }

    public void executeRecurringSubmission(TransactionRepository txRepo, String userId, String userName, String roomId, double amount,
                                           String category, String note, boolean approveImmediately,
                                           String recurringRuleId, String frequency, TransactionRepository.DataCallback<Transaction> callback) {
        final String ruleId = recurringRuleId != null ? recurringRuleId.trim() : "";
        if (ruleId.isEmpty()) {
            if (callback != null) callback.onError("Mã khoản chi định kỳ không hợp lệ.");
            return;
        }

        final ZonedDateTime nowVn = ZonedDateTime.now(VN_ZONE);
        final String cycleKey = computeRecurringCycleKey(frequency, nowVn);
        String cycleStartIso = computeRecurringCycleStartIsoUtc(frequency, nowVn);

        String encodedStart;
        try {
            encodedStart = URLEncoder.encode(cycleStartIso, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            encodedStart = cycleStartIso;
        }
        String checkUrl = SupabaseConfig.REST_URL + "/transactions?recurring_rule_id=eq." + ruleId
                + "&created_at=gte." + encodedStart
                + "&select=id,status";

        Request checkReq = new Request.Builder()
                .url(checkUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + getEffectiveToken())
                .get()
                .build();

        try {
            client.newCall(checkReq).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) callback.onError("Không thể kiểm tra lịch sử chi định kỳ: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        if (callback != null) callback.onError("HTTP " + response.code() + ": " + body);
                        return;
                    }
                    int rejectedCount = 0;
                    try {
                        JsonArray rows = JsonParser.parseString(body).getAsJsonArray();
                        if (rows != null) {
                            for (JsonElement el : rows) {
                                JsonObject row = el.getAsJsonObject();
                                String status = row.has("status") && !row.get("status").isJsonNull()
                                        ? row.get("status").getAsString() : "";
                                if (isActiveRecurringTxStatus(status)) {
                                    if (callback != null) callback.onError("Khoản chi định kỳ này đang chờ duyệt hoặc đã thanh toán trong kỳ!");
                                    return;
                                }
                                if ("REJECTED".equalsIgnoreCase(status.trim())) {
                                    rejectedCount++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (callback != null) callback.onError("Không thể kiểm tra lịch sử chi định kỳ: " + e.getMessage());
                        return;
                    }

                    String deterministicId = buildRecurringIdempotencyUuid(ruleId, cycleKey, rejectedCount);
                    Transaction request = new Transaction();
                    request.setId(deterministicId);
                    request.setRoomId(roomId);
                    request.setUserId(userId);
                    request.setCreatedBy(userName);
                    request.setType("EXPENSE");
                    request.setAmount(amount);
                    request.setCategory(category);
                    request.setNote(note);
                    request.setRecurring(true);
                    request.setRecurringRuleId(ruleId);
                    request.setStatus("PENDING");
                    request.setTransactionDate(java.time.LocalDate.now(VN_ZONE).toString());

                    if (txRepo == null) {
                        if (callback != null) callback.onError("TransactionRepository instance is null.");
                        return;
                    }

                    txRepo.createTransaction(request, new TransactionRepository.DataCallback<Transaction>() {
                        @Override
                        public void onSuccess(Transaction created) {
                            if (!approveImmediately) {
                                if (callback != null) callback.onSuccess(created);
                                return;
                            }
                            txRepo.approveExpenseTransaction(userId, created.getId(), new TransactionRepository.DataCallback<JsonObject>() {
                                @Override
                                public void onSuccess(JsonObject result) {
                                    created.setStatus("APPROVED");
                                    if (result.has("new_room_balance") && !result.get("new_room_balance").isJsonNull()) {
                                        created.setBalanceAfter(result.get("new_room_balance").getAsDouble());
                                    }
                                    created.setApprovedBy(userId);
                                    created.setApprovedAt(java.time.Instant.now().toString());
                                    if (callback != null) callback.onSuccess(created);
                                }

                                @Override
                                public void onError(String message) {
                                    if (callback != null) callback.onError("Yêu cầu rút đã được ghi nhưng chưa duyệt tự động; giao dịch vẫn đang chờ duyệt: " + message);
                                }
                            });
                        }

                        @Override
                        public void onError(String message) {
                            if (callback != null) callback.onError(message);
                        }
                    });
                }
            });
        } catch (Exception e) {
            if (callback != null) callback.onError("Lỗi khởi tạo giao dịch định kỳ: " + e.getMessage());
        }
    }
}
