package com.sharedfund.app.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sharedfund.app.model.ExpenseCategory;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.transaction.TransactionAdapter;
import com.sharedfund.app.ui.widget.CategoryChipHelper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class RoomCategoryManager {

    private static volatile RoomCategoryManager instance;
    private final Gson gson = new Gson();
    private final List<ExpenseCategory> cachedCategories = Collections.synchronizedList(new ArrayList<>());

    private RoomCategoryManager() {}

    public static RoomCategoryManager getInstance() {
        if (instance == null) {
            synchronized (RoomCategoryManager.class) {
                if (instance == null) {
                    instance = new RoomCategoryManager();
                }
            }
        }
        return instance;
    }

    public Map<String, Double> calculateCategoryBreakdown(String roomId, List<Transaction> transactionList) {
        Map<String, Double> map = new HashMap<>();
        if (roomId == null || transactionList == null) return map;
        RoomRepository roomRepo = RoomRepository.getInstance();
        for (Transaction tx : transactionList) {
            if (tx == null) continue;
            if (roomId.equals(tx.getRoomId()) && roomRepo.affectsBalance(tx) && tx.isExpense()) {
                String cat = TransactionAdapter.resolveDisplayCategory(tx.getCategory(), tx.getNote());
                Double current = map.get(cat);
                map.put(cat, (current != null ? current : 0.0) + tx.getAmount());
            }
        }
        return map;
    }

    public void getExpenseCategories(String token, RoomRepository.DataCallback<List<ExpenseCategory>> callback) {
        if (!cachedCategories.isEmpty()) {
            if (callback != null) callback.onSuccess(new ArrayList<>(cachedCategories));
            return;
        }

        String url = SupabaseConfig.REST_URL + "/categories?select=*&order=id.asc";
        String effectiveToken = (token != null && !token.trim().isEmpty()) ? token : SupabaseConfig.SUPABASE_ANON_KEY;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + effectiveToken)
                .get()
                .build();

        RoomRepository.getInstance().getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                List<ExpenseCategory> fallback = getFallbackCategories();
                cachedCategories.clear();
                cachedCategories.addAll(fallback);
                if (callback != null) callback.onSuccess(fallback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    List<ExpenseCategory> fallback = getFallbackCategories();
                    cachedCategories.clear();
                    cachedCategories.addAll(fallback);
                    if (callback != null) callback.onSuccess(fallback);
                    return;
                }
                try {
                    Type listType = new TypeToken<List<ExpenseCategory>>() {}.getType();
                    List<ExpenseCategory> list = gson.fromJson(json, listType);
                    if (list == null || list.isEmpty()) {
                        list = getFallbackCategories();
                    }
                    cachedCategories.clear();
                    cachedCategories.addAll(list);
                    if (callback != null) callback.onSuccess(list);
                } catch (Exception ex) {
                    List<ExpenseCategory> fallback = getFallbackCategories();
                    cachedCategories.clear();
                    cachedCategories.addAll(fallback);
                    if (callback != null) callback.onSuccess(fallback);
                }
            }
        });
    }

    public List<ExpenseCategory> getFallbackCategories() {
        List<ExpenseCategory> list = new ArrayList<>();
        List<CategoryChipHelper.CategoryItem> items = CategoryChipHelper.getDefaultExpenseCategories();
        if (items != null) {
            int id = 1;
            for (CategoryChipHelper.CategoryItem item : items) {
                list.add(new ExpenseCategory(String.valueOf(id++), item.name, "EXPENSE", null));
            }
        }
        return list;
    }

    public void clearCache() {
        cachedCategories.clear();
    }
}
