package com.sharedfund.app.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sharedfund.app.model.Room;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoomRepository {

    private final OkHttpClient client;
    private final Gson gson;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String errorMessage);
    }

    public RoomRepository() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public void getAllRooms(DataCallback<List<Room>> callback) {
        String url = SupabaseConfig.REST_URL + "/rooms?select=*&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    Type listType = new TypeToken<List<Room>>() {}.getType();
                    List<Room> rooms = gson.fromJson(json, listType);
                    callback.onSuccess(rooms);
                } else {
                    callback.onError("Server returned code: " + response.code());
                }
            }
        });
    }

    public void createRoom(Room room, DataCallback<Room> callback) {
        String url = SupabaseConfig.REST_URL + "/rooms";
        String jsonBody = gson.toJson(room);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
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
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    Type listType = new TypeToken<List<Room>>() {}.getType();
                    List<Room> createdRooms = gson.fromJson(json, listType);
                    if (createdRooms != null && !createdRooms.isEmpty()) {
                        callback.onSuccess(createdRooms.get(0));
                    } else {
                        callback.onSuccess(room);
                    }
                } else {
                    callback.onError("Create failed code: " + response.code());
                }
            }
        });
    }
}
