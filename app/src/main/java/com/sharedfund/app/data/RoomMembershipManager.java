package com.sharedfund.app.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sharedfund.app.model.Room;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoomMembershipManager {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static volatile RoomMembershipManager instance;

    private final Set<String> inFlightJoinDecisions = Collections.synchronizedSet(new HashSet<>());

    private RoomMembershipManager() {}

    public static RoomMembershipManager getInstance() {
        if (instance == null) {
            synchronized (RoomMembershipManager.class) {
                if (instance == null) {
                    instance = new RoomMembershipManager();
                }
            }
        }
        return instance;
    }

    public void processJoinRequestDecision(String roomId, String userId, String email, boolean approve, RoomRepository.DataCallback<Boolean> callback) {
        if (roomId == null || roomId.trim().isEmpty()) {
            if (callback != null) callback.onError("Không xác định được quỹ.");
            return;
        }
        String targetKey = (userId != null && !userId.trim().isEmpty()) ? userId.trim() : (email != null ? email.trim().toLowerCase(Locale.ROOT) : "");
        String opKey = roomId.trim() + ":" + targetKey;
        if (!inFlightJoinDecisions.add(opKey)) {
            if (callback != null) callback.onError("Yêu cầu tham gia này đang được xử lý, vui lòng chờ giây lát.");
            return;
        }
        RoomRepository.DataCallback<Boolean> wrapped = new RoomRepository.DataCallback<Boolean>() {
            @Override public void onSuccess(Boolean value) {
                inFlightJoinDecisions.remove(opKey);
                if (callback != null) callback.onSuccess(value);
            }
            @Override public void onError(String message) {
                inFlightJoinDecisions.remove(opKey);
                if (callback != null) callback.onError(message);
            }
        };
        executeJoinDecisionWithCas(roomId.trim(), userId, email, approve, 3, wrapped);
    }

    private void executeJoinDecisionWithCas(String roomId, String userId, String email, boolean approve, int retriesLeft, RoomRepository.DataCallback<Boolean> callback) {
        RoomRepository roomRepo = RoomRepository.getInstance();
        Request getReq = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/rooms?id=eq." + roomId + "&select=*")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .get()
                .build();
        roomRepo.getClient().newCall(getReq).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("HTTP " + response.code() + (body.isEmpty() ? "" : ": " + body));
                    return;
                }
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    if (arr == null || arr.size() == 0) {
                        if (callback != null) callback.onError("Không tìm thấy thông tin quỹ.");
                        return;
                    }
                    JsonObject roomObj = arr.get(0).getAsJsonObject();
                    String oldRawDesc = (roomObj.has("description") && !roomObj.get("description").isJsonNull())
                            ? roomObj.get("description").getAsString() : null;
                    Room latest = roomRepo.getGson().fromJson(roomObj, Room.class);
                    java.util.List<Room.JoinRequestMeta> list = new java.util.ArrayList<>(latest.getPendingJoinRequests());
                    boolean found = false;
                    for (int i = list.size() - 1; i >= 0; i--) {
                        Room.JoinRequestMeta it = list.get(i);
                        if ((userId != null && userId.equals(it.userId)) || (email != null && email.equalsIgnoreCase(it.email))) {
                            list.remove(i);
                            found = true;
                        }
                    }
                    if (!found) {
                        if (callback != null) callback.onError("Yêu cầu tham gia này đã được quản trị viên khác xử lý!");
                        return;
                    }
                    String fullDesc = Room.buildFullDescription(latest.getDescription(), latest.getTreasurerIds(), latest.isPrivateRoom(), list, latest.getRecurringNudgeTimestamps());
                    roomRepo.patchRoomDescriptionCas(roomId, oldRawDesc, fullDesc, new RoomRepository.DataCallback<Boolean>() {
                        @Override public void onSuccess(Boolean value) {
                            String approverName = roomRepo.getCurrentUserName();
                            if (approve && userId != null && !userId.startsWith("mock-")) {
                                roomRepo.addMember(latest, userId, "MEMBER", new RoomRepository.DataCallback<Room>() {
                                    @Override public void onSuccess(Room r) {
                                        roomRepo.dispatchJoinResultNotification(com.sharedfund.app.SharedFundApp.getInstance(), roomId, userId, null, true, approverName);
                                        if (callback != null) callback.onSuccess(true);
                                    }
                                    @Override public void onError(String message) {
                                        if (callback != null) callback.onError(message);
                                    }
                                });
                            } else {
                                if (userId != null && !userId.startsWith("mock-")) {
                                    roomRepo.dispatchJoinResultNotification(com.sharedfund.app.SharedFundApp.getInstance(), roomId, userId, null, false, approverName);
                                }
                                if (callback != null) callback.onSuccess(true);
                            }
                        }
                        @Override public void onError(String message) {
                            if ("CAS_CONFLICT".equals(message) && retriesLeft > 0) {
                                executeJoinDecisionWithCas(roomId, userId, email, approve, retriesLeft - 1, callback);
                            } else if ("CAS_CONFLICT".equals(message)) {
                                if (callback != null) callback.onError("Hệ thống đang bận xử lý yêu cầu đồng thời, vui lòng thử lại.");
                            } else {
                                if (callback != null) callback.onError(message);
                            }
                        }
                    });
                } catch (Exception e) {
                    if (callback != null) callback.onError(e.getMessage());
                }
            }
        });
    }

    public void removeJoinRequest(String roomId, String userId, String email) {
        if (roomId == null) return;
        processJoinRequestDecision(roomId, userId, email, false, null);
    }

    public void updateMemberRole(String roomId, String targetUserId, String newRole, RoomRepository.DataCallback<Boolean> callback) {
        RoomRepository roomRepo = RoomRepository.getInstance();
        String path = "/room_members?room_id=eq." + roomId + "&user_id=eq." + targetUserId;
        JsonObject body = new JsonObject();
        body.addProperty("role", newRole);
        roomRepo.getClient().newCall(roomRepo.request(path).patch(RequestBody.create(roomRepo.getGson().toJson(body), JSON)).build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("HTTP " + response.code());
                    return;
                }
                roomRepo.refreshRoomMembers(roomId);
                if (callback != null) callback.onSuccess(true);
            }
        });
    }

    public void transferRoomOwnership(String roomId, String oldOwnerUserId, String newOwnerUserId, RoomRepository.DataCallback<Boolean> callback) {
        RoomRepository roomRepo = RoomRepository.getInstance();
        String fetchPath = "/rooms?id=eq." + roomId + "&select=*";
        roomRepo.getClient().newCall(roomRepo.request(fetchPath).get().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    if (arr.size() > 0) {
                        JsonObject roomObj = arr.get(0).getAsJsonObject();
                        Room latest = roomRepo.getGson().fromJson(roomObj, Room.class);
                        Set<String> cleanTreasurers = new HashSet<>(latest.getTreasurerIds());
                        cleanTreasurers.remove(newOwnerUserId);
                        String cleanDesc = latest.getDescription();
                        boolean isPriv = latest.isPrivateRoom();
                        java.util.List<Room.JoinRequestMeta> reqs = latest.getPendingJoinRequests();
                        Map<String, Long> nudges = latest.getRecurringNudgeTimestamps();
                        String fullDesc = Room.buildFullDescription(cleanDesc, cleanTreasurers, isPriv, reqs, nudges);

                        String roomPatchPath = "/rooms?id=eq." + roomId;
                        JsonObject roomBody = new JsonObject();
                        roomBody.addProperty("created_by", newOwnerUserId);
                        roomBody.addProperty("description", fullDesc);

                        Request patchRoomReq = new Request.Builder()
                                .url(SupabaseConfig.REST_URL + roomPatchPath)
                                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                                .addHeader("Content-Type", "application/json")
                                .patch(RequestBody.create(roomRepo.getGson().toJson(roomBody), JSON))
                                .build();

                        roomRepo.getClient().newCall(patchRoomReq).enqueue(new okhttp3.Callback() {
                            @Override public void onFailure(Call call, IOException e) {
                                if (callback != null) callback.onError(e.getMessage());
                            }
                            @Override public void onResponse(Call call, Response resp2) {
                                updateMemberRole(roomId, newOwnerUserId, "OWNER", new RoomRepository.DataCallback<Boolean>() {
                                    @Override public void onSuccess(Boolean v1) {
                                        if (oldOwnerUserId != null && !oldOwnerUserId.isEmpty()) {
                                            updateMemberRole(roomId, oldOwnerUserId, "MEMBER", new RoomRepository.DataCallback<Boolean>() {
                                                @Override public void onSuccess(Boolean v2) {
                                                    finalizeTransfer(roomId, oldOwnerUserId, newOwnerUserId, fullDesc, callback);
                                                }
                                                @Override public void onError(String m2) {
                                                    finalizeTransfer(roomId, oldOwnerUserId, newOwnerUserId, fullDesc, callback);
                                                }
                                            });
                                        } else {
                                            finalizeTransfer(roomId, oldOwnerUserId, newOwnerUserId, fullDesc, callback);
                                        }
                                    }
                                    @Override public void onError(String m1) {
                                        if (callback != null) callback.onError(m1);
                                    }
                                });
                            }
                        });
                        return;
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onError(e.getMessage());
                    return;
                }
                if (callback != null) callback.onError("Room not found");
            }
        });
    }

    private void finalizeTransfer(String roomId, String oldOwnerUserId, String newOwnerUserId, String fullDesc, RoomRepository.DataCallback<Boolean> callback) {
        RoomRepository roomRepo = RoomRepository.getInstance();
        Room local = roomRepo.getRoomSync(roomId);
        if (local != null) {
            local.setCreatedBy(newOwnerUserId);
            local.setDescription(fullDesc);
            String currentId = roomRepo.getCurrentUserId();
            if (currentId != null && !currentId.isEmpty()) {
                if (currentId.equals(newOwnerUserId)) {
                    local.setCurrentUserRole("OWNER");
                } else if (local.getTreasurerIds() != null && local.getTreasurerIds().contains(currentId)) {
                    local.setCurrentUserRole("TREASURER");
                } else {
                    local.setCurrentUserRole("MEMBER");
                }
            }
            roomRepo.notifyDataChanged();
        }
        roomRepo.refreshRoomMembers(roomId);
        if (callback != null) callback.onSuccess(true);
    }

    public void removeMember(String roomId, String targetUserId, RoomRepository.DataCallback<Boolean> callback) {
        RoomRepository roomRepo = RoomRepository.getInstance();
        String path = "/room_members?room_id=eq." + roomId + "&user_id=eq." + targetUserId;
        roomRepo.getClient().newCall(roomRepo.request(path).delete().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("HTTP " + response.code());
                    return;
                }
                roomRepo.refreshRoomMembers(roomId);
                if (callback != null) callback.onSuccess(true);
            }
        });
    }
}
