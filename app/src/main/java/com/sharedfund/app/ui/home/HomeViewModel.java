package com.sharedfund.app.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Room;

import java.util.List;

/**
 * ViewModel cho màn hình Trang chủ (HomeFragment)
 * Quản lý danh sách quỹ, tổng số dư toàn bộ, trạng thái tải và các thao tác Tạo / Tham gia quỹ.
 */
public class HomeViewModel extends ViewModel {
    private final RoomRepository repository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();
    private String currentUserId = "";

    public HomeViewModel() {
        this.repository = RoomRepository.getInstance();
    }

    public LiveData<List<Room>> getRooms() {
        return repository.getRoomsLiveData();
    }

    public LiveData<Double> getTotalRoomBalance() {
        return repository.getTotalBalanceLiveData();
    }

    public LiveData<Double> getTotalBalance() {
        return repository.getTotalBalanceLiveData();
    }

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
    }

    public void loadRemoteData() {
        if (currentUserId.isEmpty()) {
            actionMessage.setValue("Vui lòng đăng nhập bằng tài khoản Supabase.");
            return;
        }
        isLoading.setValue(true);
        repository.loadUserData(currentUserId, new RoomRepository.DataCallback<List<Room>>() {
            @Override public void onSuccess(List<Room> rooms) {
                isLoading.postValue(false);
            }
            @Override public void onError(String message) {
                isLoading.postValue(false);
                actionMessage.postValue("Không tải được dữ liệu Supabase: " + message);
            }
        });
    }

    public void createRoom(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            actionMessage.setValue("Tên quỹ không được để trống!");
            return;
        }
        if (currentUserId.isEmpty()) {
            actionMessage.setValue("Vui lòng đăng nhập bằng tài khoản Supabase."); return;
        }
        isLoading.setValue(true);
        repository.createRoom(name.trim(), description != null ? description.trim() : "", currentUserId,
                new RoomRepository.DataCallback<Room>() {
                    @Override public void onSuccess(Room room) {
                        loadRemoteData();
                        actionMessage.postValue("Tạo quỹ \"" + room.getName() + "\" thành công!");
                    }
                    @Override public void onError(String message) {
                        isLoading.postValue(false);
                        actionMessage.postValue("Tạo quỹ thất bại: " + message);
                    }
                });
    }

    public void joinRoom(String joinCode) {
        if (joinCode == null || joinCode.trim().isEmpty()) {
            actionMessage.setValue("Vui lòng nhập mã tham gia!");
            return;
        }
        if (currentUserId.isEmpty()) {
            actionMessage.setValue("Vui lòng đăng nhập bằng tài khoản Supabase."); return;
        }
        isLoading.setValue(true);
        repository.joinRoom(joinCode.trim(), currentUserId, new RoomRepository.DataCallback<Room>() {
            @Override public void onSuccess(Room room) {
                loadRemoteData();
                actionMessage.postValue("Đã tham gia quỹ \"" + room.getName() + "\"!");
            }
            @Override public void onError(String message) {
                isLoading.postValue(false);
                actionMessage.postValue("Không thể tham gia quỹ: " + message);
            }
        });
    }
}
