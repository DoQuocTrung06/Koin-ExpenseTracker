package com.sharedfund.app.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.SearchHelper;
import com.sharedfund.app.data.SupabaseAuthenticator;
import com.sharedfund.app.model.Room;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private final RoomRepository repository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MediatorLiveData<List<Room>> filteredRooms = new MediatorLiveData<>();
    private final MutableLiveData<Double> personalWalletBalance = new MutableLiveData<>();
    private String currentUserId = "";

    public HomeViewModel() {
        this.repository = RoomRepository.getInstance();

        filteredRooms.addSource(repository.getRoomsLiveData(), rooms -> applyRoomFilter());
        filteredRooms.addSource(searchQuery, query -> applyRoomFilter());
        applyRoomFilter();
    }

    private void applyRoomFilter() {
        List<Room> all = repository.getRoomsLiveData().getValue();
        String query = searchQuery.getValue();
        filteredRooms.setValue(SearchHelper.filterRooms(all, query));
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query != null ? query : "");
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public LiveData<List<Room>> getRooms() {
        return filteredRooms;
    }

    public LiveData<Double> getTotalRoomBalance() {
        return repository.getTotalBalanceLiveData();
    }

    public LiveData<Double> getUserWalletBalance() {
        return repository.getUserWalletLiveData();
    }

    public LiveData<Double> getTotalBalance() {
        return repository.getTotalBalanceLiveData();
    }

    public LiveData<Double> getPersonalWalletBalance() { return personalWalletBalance; }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getActionMessage() {
        return actionMessage;
    }

    public void configureSupabase(String userId, String userName, String accessToken) {
        currentUserId = userId != null ? userId : "";
        repository.setCurrentUser(currentUserId, userName);
        repository.configureAccessToken(accessToken);
        loadRemoteData();
        refreshWalletBalance();
    }

    public void refreshWalletBalance() {
        if (currentUserId.isEmpty()) return;
        repository.loadWalletBalance(currentUserId, new RoomRepository.DataCallback<Double>() {
            @Override public void onSuccess(Double balance) { personalWalletBalance.postValue(balance); }
            @Override public void onError(String message) {
                actionMessage.postValue("Không tải được số dư ví cá nhân: " + message);
            }
        });
    }

    public void loadRemoteData() {
        if (currentUserId.isEmpty()) {
            actionMessage.setValue("Vui lòng đăng nhập");
            return;
        }
        isLoading.postValue(true);
        repository.loadUserData(currentUserId, new RoomRepository.DataCallback<List<Room>>() {
            @Override public void onSuccess(List<Room> rooms) {
                isLoading.postValue(false);
            }
            @Override public void onError(String message) {
                isLoading.postValue(false);
                if (message != null && (message.contains("401") || message.contains("PGRST303") || message.contains("JWT expired"))) {
                    actionMessage.postValue("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
                    SupabaseAuthenticator.notifyExpired(null);
                } else {
                    actionMessage.postValue("Failed to load data: " + message);
                }
            }
        });
    }

    private String getEffectiveUserId() {
        if (currentUserId != null && !currentUserId.isEmpty()) return currentUserId;
        return repository.getCurrentUserId();
    }

    public void createRoom(String name, String description) {
        createRoom(name, description, 0.0);
    }

    public void createRoom(String name, String description, double targetAmount) {
        if (name == null || name.trim().isEmpty()) {
            actionMessage.setValue("Tên quỹ không được để trống!");
            return;
        }
        String effectiveUserId = getEffectiveUserId();
        if (effectiveUserId.isEmpty() || effectiveUserId.startsWith("user-")) {
            isLoading.setValue(true);
            Room newRoom = repository.createRoom(name.trim(), description != null ? description.trim() : "", targetAmount);
            isLoading.setValue(false);
            if (newRoom != null) {
                applyRoomFilter();
                actionMessage.setValue("Tạo quỹ \"" + newRoom.getName() + "\" thành công!");
            } else {
                actionMessage.setValue("Tạo quỹ thất bại!");
            }
            return;
        }
        isLoading.setValue(true);
        repository.createRoom(name.trim(), description != null ? description.trim() : "", targetAmount, effectiveUserId,
                new RoomRepository.DataCallback<Room>() {
                    @Override public void onSuccess(Room room) {
                        loadRemoteData();
                        actionMessage.postValue("Tạo quỹ \"" + room.getName() + "\" thành công!");
                    }
                    @Override public void onError(String message) {
                        isLoading.postValue(false);
                        actionMessage.postValue("Lỗi tạo quỹ: " + message);
                    }
                });
    }

    public void joinRoom(String joinCode) {
        if (joinCode == null || joinCode.trim().isEmpty()) {
            actionMessage.setValue("Mã mời không được để trống!");
            return;
        }
        String effectiveUserId = getEffectiveUserId();
        if (effectiveUserId.isEmpty() || effectiveUserId.startsWith("user-")) {
            isLoading.setValue(true);
            Room room = repository.joinRoomByCode(joinCode.trim());
            isLoading.setValue(false);
            if (room != null) {
                applyRoomFilter();
                actionMessage.setValue("Tham gia quỹ \"" + room.getName() + "\" thành công!");
            } else {
                actionMessage.setValue("Không tìm thấy quỹ với mã tham gia này!");
            }
            return;
        }
        isLoading.setValue(true);
        repository.joinRoom(joinCode.trim().toUpperCase(), effectiveUserId,
                new RoomRepository.DataCallback<Room>() {
                    @Override public void onSuccess(Room room) {
                        loadRemoteData();
                        actionMessage.postValue("Tham gia quỹ \"" + room.getName() + "\" thành công!");
                    }
                    @Override public void onError(String message) {
                        isLoading.postValue(false);
                        actionMessage.postValue("Lỗi tham gia quỹ: " + message);
                    }
                });
    }
}