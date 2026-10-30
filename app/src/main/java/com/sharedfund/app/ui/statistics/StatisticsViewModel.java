package com.sharedfund.app.ui.statistics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class StatisticsViewModel extends ViewModel {
    private final RoomRepository repository;
    private String roomId;

    private final MutableLiveData<Room> roomLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncomeLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalExpenseLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> netBalanceLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Map<String, Double>> categoryStatsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> topExpensesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final Observer<List<Transaction>> transactionsObserver;
    private final Observer<List<Room>> roomsObserver;

    public StatisticsViewModel() {
        this.repository = RoomRepository.getInstance();

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

    public void refresh() {
        if (roomId == null) return;
        isLoading.setValue(true);
        Room r = repository.getRoomSync(roomId);
        roomLiveData.setValue(r);

        double income = repository.getRoomTotalIncome(roomId);
        double expense = repository.getRoomTotalExpense(roomId);
        double net = income - expense;

        totalIncomeLiveData.setValue(income);
        totalExpenseLiveData.setValue(expense);
        netBalanceLiveData.setValue(net);

        Map<String, Double> breakdown = repository.getCategoryBreakdown(roomId);
        categoryStatsLiveData.setValue(breakdown);

        List<Transaction> all = repository.getTransactionsForRoom(roomId);
        List<Transaction> expenses = new ArrayList<>();
        for (Transaction tx : all) {
            if (tx.isExpense()) {
                expenses.add(tx);
            }
        }
        Collections.sort(expenses, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                return Double.compare(o2.getAmount(), o1.getAmount());
            }
        });
        List<Transaction> top = new ArrayList<>();
        int count = Math.min(5, expenses.size());
        for (int i = 0; i < count; i++) {
            top.add(expenses.get(i));
        }
        topExpensesLiveData.setValue(top);
        isLoading.setValue(false);
    }

    public LiveData<Room> getRoom() {
        return roomLiveData;
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncomeLiveData;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpenseLiveData;
    }

    public LiveData<Double> getNetBalance() {
        return netBalanceLiveData;
    }

    public LiveData<Map<String, Double>> getCategoryStats() {
        return categoryStatsLiveData;
    }

    public LiveData<List<Transaction>> getTopExpenses() {
        return topExpensesLiveData;
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