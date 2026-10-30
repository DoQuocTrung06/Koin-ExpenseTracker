package com.sharedfund.app.ui.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.google.gson.JsonObject;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.SearchHelper;
import com.sharedfund.app.data.SupabaseAuthenticator;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionViewModel extends ViewModel {
    public enum FilterType {
        ALL, INCOME, EXPENSE
    }

    private final RoomRepository repository;
    private final TransactionRepository remoteRepository = new TransactionRepository();
    private String roomId;
    private FilterType currentFilter = FilterType.ALL;
    private String searchKeyword = "";
    private String statusFilter = "ALL";

    private final MutableLiveData<List<Transaction>> filteredTransactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addSuccessEvent = new MutableLiveData<>();
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();

    private final Observer<List<Transaction>> transactionsObserver;

    public TransactionViewModel() {
        this.repository = RoomRepository.getInstance();

        this.transactionsObserver = new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                if (roomId != null) {
                    applyFilter(currentFilter);
                }
            }
        };

        repository.getTransactionsLiveData().observeForever(transactionsObserver);
    }

    public LiveData<String> getActionMessage() {
        return actionMessage;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
        applyFilter(currentFilter);
        if (roomId != null && !roomId.trim().isEmpty()) {
            isLoading.setValue(true);
            remoteRepository.getTransactionsByRoom(roomId, new TransactionRepository.DataCallback<List<Transaction>>() {
                @Override public void onSuccess(List<Transaction> data) {
                    if (data != null) for (Transaction tx : data) repository.onRemoteTransactionReceived(tx);
                    isLoading.postValue(false);
                }
                @Override public void onError(String message) {
                    isLoading.postValue(false);
                    if (message != null && (message.contains("401") || message.contains("PGRST303") || message.contains("JWT expired"))) {
                        errorMessage.postValue("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
                        SupabaseAuthenticator.notifyExpired(null);
                    } else {
                        errorMessage.postValue("Failed to load transactions: " + message);
                    }
                }
            });
        }
    }

    public void setAccessToken(String token) {
        remoteRepository.setAccessToken(token);
    }

    public String getRoomId() {
        return roomId;
    }

    public void setSearchKeyword(String keyword) {
        this.searchKeyword = keyword != null ? keyword : "";
        applyFilter(currentFilter);
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setStatusFilter(String status) {
        this.statusFilter = status != null ? status : "ALL";
        applyFilter(currentFilter);
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setFilter(FilterType filter) {
        this.currentFilter = filter;
        applyFilter(filter);
    }

    public FilterType getCurrentFilter() {
        return currentFilter;
    }

    public void applyFilter(FilterType filter) {
        if (roomId == null) return;
        isLoading.setValue(true);
        List<Transaction> all = repository.getTransactionsForRoom(roomId);
        List<Transaction> byType = new ArrayList<>();
        if (filter == FilterType.ALL) {
            byType.addAll(all);
        } else if (filter == FilterType.INCOME) {
            for (Transaction tx : all) {
                if (tx.isIncome()) byType.add(tx);
            }
        } else if (filter == FilterType.EXPENSE) {
            for (Transaction tx : all) {
                if (tx.isExpense()) byType.add(tx);
            }
        }

        List<Transaction> result = SearchHelper.filterTransactions(byType, searchKeyword, statusFilter);
        filteredTransactionsLiveData.setValue(result);
        isLoading.setValue(false);
    }

    public void loadUserTransactions(String userId, TransactionRepository.DataCallback<List<Transaction>> callback) {
        remoteRepository.getUserTransactions(userId, callback);
    }

    public void loadFilteredRemote(String type, String status, TransactionRepository.DataCallback<List<Transaction>> callback) {
        if (roomId == null || roomId.trim().isEmpty()) {
            callback.onError("Room ID is required");
            return;
        }
        remoteRepository.getFilteredTransactions(roomId, type, status, callback);
    }

    public LiveData<List<Transaction>> getTransactions() {
        return filteredTransactionsLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getAddSuccessEvent() {
        return addSuccessEvent;
    }

    public void approveExpense(String adminUserId, String transactionId) {
        isLoading.setValue(true);
        remoteRepository.approveExpense(adminUserId, transactionId, new TransactionRepository.DataCallback<JsonObject>() {
            @Override public void onSuccess(JsonObject data) {
                isLoading.postValue(false);
                actionMessage.postValue("Approved");
                if (roomId != null) setRoomId(roomId);
            }
            @Override public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue("Approval failed: " + error);
            }
        });
    }

    public void rejectExpense(String adminUserId, String transactionId, String reason) {
        isLoading.setValue(true);
        remoteRepository.rejectExpense(adminUserId, transactionId, reason, new TransactionRepository.DataCallback<JsonObject>() {
            @Override public void onSuccess(JsonObject data) {
                isLoading.postValue(false);
                actionMessage.postValue("Rejected");
                if (roomId != null) setRoomId(roomId);
            }
            @Override public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue("Rejection failed: " + error);
            }
        });
    }

    public boolean submitTransaction(String targetRoomId, String type, String amountStr, String category, String note, String createdBy) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập số tiền");
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim().replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            errorMessage.setValue("Số tiền không hợp lệ");
            return false;
        }

        if (amount <= 0) {
            errorMessage.setValue("Số tiền giao dịch phải lớn hơn 0 ₫");
            return false;
        }

        if (category == null || category.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng chọn danh mục");
            return false;
        }

        if (note == null || note.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập lý do giao dịch");
            return false;
        }

        if (createdBy == null || createdBy.trim().isEmpty()) {
            createdBy = repository.getCurrentUserName();
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            createdBy = "Thành viên";
        }

        Transaction tx = new Transaction();
        tx.setRoomId(targetRoomId != null ? targetRoomId : this.roomId);
        tx.setUserId(repository.getCurrentUserId());
        tx.setType(Transaction.TYPE_INCOME.equalsIgnoreCase(type) || "INCOME".equalsIgnoreCase(type)
                ? "INCOME" : "EXPENSE");
        tx.setAmount(amount);
        tx.setCategory(category.trim());
        tx.setNote(note.trim());
        tx.setCreatedBy(createdBy.trim());
        tx.setTransactionDate(LocalDate.now().toString());

        if (tx.isExpense()) {
            boolean isAdmin = repository.isCurrentUserAdmin(tx.getRoomId());
            tx.setStatus(isAdmin ? Transaction.STATUS_APPROVED : Transaction.STATUS_PENDING);
        } else {
            tx.setStatus(Transaction.STATUS_APPROVED);
        }

        Room room = repository.getRoomSync(tx.getRoomId());
        if (room != null) {
            double newBalance = room.getBalance();
            if (tx.isIncome()) {
                newBalance += amount;
            } else if (tx.isApproved()) {
                newBalance -= amount;
            }
            tx.setBalanceAfter(newBalance);
        }

        isLoading.setValue(true);
        remoteRepository.createTransaction(tx, new TransactionRepository.DataCallback<Transaction>() {
            @Override public void onSuccess(Transaction created) {
                repository.onRemoteTransactionReceived(created);
                isLoading.postValue(false);
                addSuccessEvent.postValue(true);
            }
            @Override public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("Failed to create transaction: " + message);
            }
        });
        return true;
    }

    public void withdrawWallet(String userId, double amount, String bankName, String accountNumber,
                               String accountHolder, TransactionRepository.DataCallback<JsonObject> callback) {
        if (amount <= 0) {
            if (callback != null) callback.onError("Amount must be greater than zero");
            return;
        }
        isLoading.setValue(true);
        remoteRepository.withdrawWallet(userId, amount, bankName, accountNumber, accountHolder, new TransactionRepository.DataCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject data) {
                isLoading.postValue(false);
                actionMessage.postValue("Withdrawal processed");
                if (repository.getCurrentUserId().equals(userId)) {
                    repository.loadUserProfile();
                }
                if (callback != null) callback.onSuccess(data);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue("Withdrawal failed: " + error);
                if (callback != null) callback.onError(error);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.getTransactionsLiveData().removeObserver(transactionsObserver);
    }
}