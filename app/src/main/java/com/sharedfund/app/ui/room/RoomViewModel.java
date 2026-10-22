package com.sharedfund.app.ui.room;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel cho màn hình Chi tiết quỹ (RoomDetailFragment)
 * Quản lý trạng thái quỹ, danh sách giao dịch gần nhất, tổng thu, tổng chi.
 * Được thiết kế theo cơ chế Reactive: Tự động lắng nghe thay đổi từ RoomRepository
 * (sẵn sàng tích hợp Supabase Realtime / PostgreSQL) để cập nhật giao diện ngay lập tức
 * mà không cần người dùng phải thoát ra vào lại màn hình.
 */
public class RoomViewModel extends ViewModel {
    private final RoomRepository repository;
    private String roomId;

    private final MutableLiveData<Room> roomLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> recentTransactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncomeLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalExpenseLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final Observer<List<Transaction>> transactionsObserver;
    private final Observer<List<Room>> roomsObserver;

    public RoomViewModel() {
        this.repository = RoomRepository.getInstance();

        // Tự động lắng nghe cập nhật dữ liệu từ Repository (Reactive Pattern)
        this.transactionsObserver = new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                if (roomId != null) {
                    refresh();
                }
            }
        };

        this.roomsObserver = new Observer<List<Room>>() {
            @Override
            public void onChanged(List<Room> rooms) {
                if (roomId != null) {
                    refresh();
                }
            }
        };

        repository.getTransactionsLiveData().observeForever(transactionsObserver);
        repository.getRoomsLiveData().observeForever(roomsObserver);
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
        refresh();
    }

    public String getRoomId() {
        return roomId;
    }

    public void refresh() {
        if (roomId == null) return;
        isLoading.setValue(true);
        Room r = repository.getRoomSync(roomId);
        roomLiveData.setValue(r);

        List<Transaction> all = repository.getTransactionsForRoom(roomId);
        // Lấy tối đa 5 giao dịch gần nhất
        List<Transaction> recent = new ArrayList<>();
        int count = Math.min(5, all.size());
        for (int i = 0; i < count; i++) {
            recent.add(all.get(i));
        }
        recentTransactionsLiveData.setValue(recent);

        totalIncomeLiveData.setValue(repository.getRoomTotalIncome(roomId));
        totalExpenseLiveData.setValue(repository.getRoomTotalExpense(roomId));
        isLoading.setValue(false);
    }

    public LiveData<Room> getRoom() {
        return roomLiveData;
    }

    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactionsLiveData;
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncomeLiveData;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpenseLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.getTransactionsLiveData().removeObserver(transactionsObserver);
        repository.getRoomsLiveData().removeObserver(roomsObserver);
    }
}
