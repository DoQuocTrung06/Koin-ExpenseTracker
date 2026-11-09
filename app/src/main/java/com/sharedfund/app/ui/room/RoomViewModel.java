package com.sharedfund.app.ui.room;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.transaction.TransactionAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomViewModel extends ViewModel {

    public enum FilterType {
        ALL,
        INCOME,
        EXPENSE
    }

    public enum TimeFilterType {
        ALL,
        TODAY,
        THIS_WEEK,
        THIS_MONTH,
        CUSTOM_RANGE
    }

    private final RoomRepository repository;
    private String roomId;
    private FilterType currentFilter = FilterType.ALL;
    private TimeFilterType currentTimeFilter = TimeFilterType.ALL;
    private Long customStartMillis = null;
    private Long customEndMillis = null;

    private final MutableLiveData<Room> roomLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> recentTransactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> pendingTransactionsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Transaction>> allTransactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> filteredTransactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Double>> categoryStatsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<RoomMember>> membersLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncomeLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalExpenseLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();

    private final Observer<List<Transaction>> transactionsObserver;
    private final Observer<List<Room>> roomsObserver;

    public RoomViewModel() {
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
        repository.refreshRoomTransactions(roomId, null);
    }

    public String getRoomId() {
        return roomId;
    }

    public void refresh() {
        if (roomId == null) return;
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(this::refresh);
            return;
        }
        isLoading.setValue(true);
        Room r = repository.getRoomSync(roomId);
        roomLiveData.setValue(r);

        List<Transaction> all = repository.getTransactionsForRoom(roomId);
        List<Transaction> confirmedHistory = new ArrayList<>();
        List<Transaction> recent = new ArrayList<>();
        List<Transaction> pending = new ArrayList<>();
        if (all != null) {
            for (Transaction tx : all) {
                if (tx.isPending()) {
                    pending.add(tx);
                } else {
                    if (repository.affectsBalance(tx)) {
                        confirmedHistory.add(tx);
                    }
                    if (recent.size() < 5) {
                        recent.add(tx);
                    }
                }
            }
        }
        allTransactionsLiveData.setValue(confirmedHistory);
        recentTransactionsLiveData.setValue(recent);
        pendingTransactionsLiveData.setValue(pending);
        applyFilter(currentFilter);
        loadMembers();
        isLoading.setValue(false);
    }

    public void setFilter(FilterType filter) {
        this.currentFilter = filter;
        applyFilter(filter);
    }

    public void setTimeFilter(TimeFilterType timeFilter, Long startMillis, Long endMillis) {
        this.currentTimeFilter = timeFilter;
        this.customStartMillis = startMillis;
        this.customEndMillis = endMillis;
        applyFilter(currentFilter);
    }

    public FilterType getCurrentFilter() {
        return currentFilter;
    }

    public TimeFilterType getCurrentTimeFilter() {
        return currentTimeFilter;
    }

    private boolean matchesTime(Transaction tx) {
        if (currentTimeFilter == TimeFilterType.ALL || tx == null) return true;
        Date txDate = TransactionAdapter.parseDate(tx.getCreatedAt());
        if (txDate == null) return true;
        long txMillis = txDate.getTime();

        if (currentTimeFilter == TimeFilterType.TODAY) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long start = cal.getTimeInMillis();
            long end = start + (24L * 60L * 60L * 1000L) - 1;
            return txMillis >= start && txMillis <= end;
        } else if (currentTimeFilter == TimeFilterType.THIS_WEEK) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long start = cal.getTimeInMillis();
            long end = start + (7L * 24L * 60L * 60L * 1000L) - 1;
            return txMillis >= start && txMillis <= end;
        } else if (currentTimeFilter == TimeFilterType.THIS_MONTH) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long start = cal.getTimeInMillis();
            cal.add(Calendar.MONTH, 1);
            long end = cal.getTimeInMillis() - 1;
            return txMillis >= start && txMillis <= end;
        } else if (currentTimeFilter == TimeFilterType.CUSTOM_RANGE) {
            if (customStartMillis != null && customEndMillis != null) {
                Calendar s = Calendar.getInstance();
                s.setTimeInMillis(customStartMillis);
                s.set(Calendar.HOUR_OF_DAY, 0);
                s.set(Calendar.MINUTE, 0);
                s.set(Calendar.SECOND, 0);
                s.set(Calendar.MILLISECOND, 0);

                Calendar e = Calendar.getInstance();
                e.setTimeInMillis(customEndMillis);
                e.set(Calendar.HOUR_OF_DAY, 23);
                e.set(Calendar.MINUTE, 59);
                e.set(Calendar.SECOND, 59);
                e.set(Calendar.MILLISECOND, 999);

                return txMillis >= s.getTimeInMillis() && txMillis <= e.getTimeInMillis();
            }
        }
        return true;
    }

    private void applyFilter(FilterType filter) {
        List<Transaction> all = allTransactionsLiveData.getValue();
        if (all == null) {
            filteredTransactionsLiveData.setValue(new ArrayList<>());
            totalIncomeLiveData.setValue(0.0);
            totalExpenseLiveData.setValue(0.0);
            categoryStatsLiveData.setValue(new HashMap<>());
            return;
        }

        double periodIncome = 0.0;
        double periodExpense = 0.0;
        Map<String, Double> periodCategoryStats = new HashMap<>();

        List<Transaction> filtered = new ArrayList<>();
        for (Transaction tx : all) {
            if (matchesTime(tx)) {
                if (tx.isIncome()) {
                    periodIncome += tx.getAmount();
                } else if (tx.isExpense()) {
                    periodExpense += tx.getAmount();
                    String cat = tx.getCategory() != null && !tx.getCategory().trim().isEmpty() ? tx.getCategory() : "Khác";
                    periodCategoryStats.put(cat, periodCategoryStats.getOrDefault(cat, 0.0) + tx.getAmount());
                }

                if (filter == FilterType.ALL) {
                    filtered.add(tx);
                } else if (filter == FilterType.INCOME && tx.isIncome()) {
                    filtered.add(tx);
                } else if (filter == FilterType.EXPENSE && tx.isExpense()) {
                    filtered.add(tx);
                }
            }
        }

        totalIncomeLiveData.setValue(periodIncome);
        totalExpenseLiveData.setValue(periodExpense);
        categoryStatsLiveData.setValue(periodCategoryStats);
        filteredTransactionsLiveData.setValue(filtered);
    }

    public void loadMembers() {
        if (roomId == null) return;
        repository.loadRoomMembers(roomId, new RoomRepository.DataCallback<List<RoomMember>>() {
            @Override
            public void onSuccess(List<RoomMember> list) {
                try {
                    membersLiveData.setValue(list);
                } catch (IllegalStateException e) {
                    membersLiveData.postValue(list);
                }
            }

            @Override
            public void onError(String message) {
                Room r = roomLiveData.getValue();
                if (r != null && r.getMembers() != null) {
                    List<RoomMember> fallback = new ArrayList<>();
                    double totalIncome = totalIncomeLiveData.getValue() != null ? totalIncomeLiveData.getValue() : 0.0;
                    double perMember = r.getMembers().size() > 0 ? totalIncome / r.getMembers().size() : 0.0;
                    for (int i = 0; i < r.getMembers().size(); i++) {
                        String name = r.getMembers().get(i);
                        RoomMember m = new RoomMember();
                        m.setRoomId(roomId);
                        m.setUserId("user-member-" + i);
                        m.setFullName(name);
                        m.setRole(name.contains("Chủ quỹ") ? "OWNER" : "MEMBER");
                        m.setTotalContributed(perMember);
                        fallback.add(m);
                    }
                    try {
                        membersLiveData.setValue(fallback);
                    } catch (IllegalStateException e) {
                        membersLiveData.postValue(fallback);
                    }
                }
            }
        });
    }

    public LiveData<Room> getRoom() {
        return roomLiveData;
    }

    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactionsLiveData;
    }

    public LiveData<List<Transaction>> getPendingTransactions() {
        return pendingTransactionsLiveData;
    }

    public LiveData<List<Transaction>> getFilteredTransactions() {
        return filteredTransactionsLiveData;
    }

    public LiveData<Map<String, Double>> getCategoryStats() {
        return categoryStatsLiveData;
    }

    public LiveData<List<RoomMember>> getMembers() {
        return membersLiveData;
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

    public LiveData<String> getActionMessage() {
        return actionMessage;
    }

    public void updateRoom(String name, String description, double targetAmount, RoomRepository.DataCallback<Room> callback) {
        if (roomId == null) return;
        repository.updateRoom(roomId, name, description, targetAmount, new RoomRepository.DataCallback<Room>() {
            @Override
            public void onSuccess(Room value) {
                try {
                    roomLiveData.setValue(value);
                } catch (IllegalStateException e) {
                    roomLiveData.postValue(value);
                }
                refresh();
                if (callback != null) callback.onSuccess(value);
            }

            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(message);
            }
        });
    }

    public void leaveRoom(String userId, RoomRepository.DataCallback<Boolean> callback) {
        if (roomId == null) return;
        repository.leaveRoom(roomId, userId, callback);
    }

    public void deleteRoom(String adminUserId, RoomRepository.DataCallback<Boolean> callback) {
        if (roomId == null) return;
        repository.deleteRoom(roomId, adminUserId, callback);
    }

    public void removeMember(String targetUserId, RoomRepository.DataCallback<Boolean> callback) {
        if (roomId == null) return;
        repository.removeMember(roomId, targetUserId, callback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.getTransactionsLiveData().removeObserver(transactionsObserver);
        repository.getRoomsLiveData().removeObserver(roomsObserver);
    }
}