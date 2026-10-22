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

    public HomeViewModel() {
        this.repository = RoomRepository.getInstance();
    }

    public LiveData<List<Room>> getRooms() {
        return repository.getRoomsLiveData();
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

    public void createRoom(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            actionMessage.setValue("Tên quỹ không được để trống!");
            return;
        }
        isLoading.setValue(true);
        // Giả lập thao tác bất đồng bộ
        Room newRoom = repository.createRoom(name.trim(), description != null ? description.trim() : "");
        isLoading.setValue(false);
        if (newRoom != null) {
            actionMessage.setValue("Tạo quỹ \"" + newRoom.getName() + "\" thành công!");
        } else {
            actionMessage.setValue("Tạo quỹ thất bại!");
        }
    }

    public void joinRoom(String joinCode) {
        if (joinCode == null || joinCode.trim().isEmpty()) {
            actionMessage.setValue("Vui lòng nhập mã tham gia!");
            return;
        }
        isLoading.setValue(true);
        Room room = repository.joinRoomByCode(joinCode.trim());
        isLoading.setValue(false);
        if (room != null) {
            actionMessage.setValue("Đã tham gia quỹ \"" + room.getName() + "\"!");
        } else {
            actionMessage.setValue("Không tìm thấy quỹ với mã tham gia này!");
        }
    }
}
