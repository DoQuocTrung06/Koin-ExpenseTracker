package com.sharedfund.app.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Room;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private final RoomRepository roomRepository;
    private final MutableLiveData<List<Room>> roomsLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;
    private final MutableLiveData<String> errorMessageLiveData;

    public HomeViewModel() {
        this.roomRepository = new RoomRepository();
        this.roomsLiveData = new MutableLiveData<>();
        this.isLoadingLiveData = new MutableLiveData<>();
        this.errorMessageLiveData = new MutableLiveData<>();
    }

    public LiveData<List<Room>> getRoomsLiveData() {
        return roomsLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public void loadRooms() {
        isLoadingLiveData.setValue(true);
        roomRepository.getAllRooms(new RoomRepository.DataCallback<List<Room>>() {
            @Override
            public void onSuccess(List<Room> data) {
                isLoadingLiveData.postValue(false);
                roomsLiveData.postValue(data);
            }

            @Override
            public void onError(String errorMessage) {
                isLoadingLiveData.postValue(false);
                errorMessageLiveData.postValue(errorMessage);
            }
        });
    }

    public void createNewRoom(String name, String description, String joinCode, String createdBy) {
        isLoadingLiveData.setValue(true);
        Room newRoom = new Room(name, description, joinCode, 0.0, createdBy);
        roomRepository.createRoom(newRoom, new RoomRepository.DataCallback<Room>() {
            @Override
            public void onSuccess(Room data) {
                loadRooms();
            }

            @Override
            public void onError(String errorMessage) {
                isLoadingLiveData.postValue(false);
                errorMessageLiveData.postValue(errorMessage);
            }
        });
    }
}
