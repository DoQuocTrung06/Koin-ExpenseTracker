package com.sharedfund.app.ui.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel cho màn hình Danh sách giao dịch và Thêm giao dịch (TransactionFragment & AddTransactionFragment)
 * Quản lý bộ lọc, danh sách giao dịch theo quỹ, xác thực dữ liệu và trạng thái thêm giao dịch.
 * Thiết kế Reactive: Tự động cập nhật danh sách giao dịch khi Repository có dữ liệu mới.
 */
public class TransactionViewModel extends ViewModel {
    public enum FilterType {
        ALL, INCOME, EXPENSE
    }

    private final RoomRepository repository;
    private final TransactionRepository remoteRepository = new TransactionRepository();
    private String roomId;
    private FilterType currentFilter = FilterType.ALL;

    private final MutableLiveData<List<Transaction>> filteredTransactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addSuccessEvent = new MutableLiveData<>();

    private final Observer<List<Transaction>> transactionsObserver;

    public TransactionViewModel() {
        this.repository = RoomRepository.getInstance();

        // Reactive stream: Lắng nghe cập nhật giao dịch tự động
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
                    errorMessage.postValue("Không tải được giao dịch từ Supabase: " + message);
                }
            });
        }
    }

    public void setAccessToken(String token) { remoteRepository.setAccessToken(token); }

    public String getRoomId() {
        return roomId;
    }

    public boolean isCurrentUserOwner() {
        com.sharedfund.app.model.Room room = repository.getRoomSync(roomId);
        if (room == null) return false;
        String role = room.getCurrentUserRole();
        if (role != null) return "ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role);
        return repository.getCurrentUserId() != null
                && repository.getCurrentUserId().equals(room.getCreatedBy());
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
        if (filter == FilterType.ALL) {
            filteredTransactionsLiveData.setValue(all);
        } else if (filter == FilterType.INCOME) {
            List<Transaction> list = new ArrayList<>();
            for (Transaction tx : all) {
                if (tx.isIncome()) list.add(tx);
            }
            filteredTransactionsLiveData.setValue(list);
        } else if (filter == FilterType.EXPENSE) {
            List<Transaction> list = new ArrayList<>();
            for (Transaction tx : all) {
                if (tx.isExpense()) list.add(tx);
            }
            filteredTransactionsLiveData.setValue(list);
        }
        isLoading.setValue(false);
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

    /**
     * Kiểm tra dữ liệu nhập (Validation) và thêm giao dịch mới
     */
    public boolean submitTransaction(String targetRoomId, String type, String amountStr, String category, String note) {
        // 1. Kiểm tra số tiền
        if (amountStr == null || amountStr.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập số tiền!");
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim().replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            errorMessage.setValue("Số tiền không hợp lệ!");
            return false;
        }

        if (amount <= 0) {
            errorMessage.setValue("Số tiền giao dịch phải lớn hơn 0 ₫!");
            return false;
        }

        // 2. Kiểm tra danh mục
        if (category == null || category.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng chọn danh mục!");
            return false;
        }

        // 3. Kiểm tra lời tựa / ghi chú
        if (note == null || note.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập nội dung hoặc lời tựa giao dịch!");
            return false;
        }

        final String operationRoomId = targetRoomId != null ? targetRoomId : this.roomId;
        if (operationRoomId == null || operationRoomId.trim().isEmpty()) {
            errorMessage.setValue("Không xác định được quỹ.");
            return false;
        }
        final boolean income = Transaction.TYPE_INCOME.equalsIgnoreCase(type) || "INCOME".equalsIgnoreCase(type);
        final boolean owner = isCurrentUserOwner();
        if (!income && owner) {
            com.sharedfund.app.model.Room room = repository.getRoomSync(operationRoomId);
            if (room == null || room.getBalance() < amount) {
                errorMessage.setValue("Số dư quỹ không đủ để thực hiện khoản chi.");
                return false;
            }
        }

        isLoading.setValue(true);
        TransactionRepository.DataCallback<Transaction> transactionCallback = new TransactionRepository.DataCallback<Transaction>() {
            @Override public void onSuccess(Transaction created) {
                repository.onRemoteTransactionReceived(created);
                isLoading.postValue(false);
                addSuccessEvent.postValue(true);
            }
            @Override public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("Không thể lưu giao dịch lên Supabase: " + message);
            }
        };
        if (income) {
            String userId = repository.getCurrentUserId();
            remoteRepository.contributeFromWallet(userId, operationRoomId, amount, note.trim(),
                    new TransactionRepository.DataCallback<com.google.gson.JsonObject>() {
                        @Override public void onSuccess(com.google.gson.JsonObject result) {
                            remoteRepository.refreshRoomTransactions(operationRoomId,
                                    new TransactionRepository.DataCallback<List<Transaction>>() {
                                        @Override public void onSuccess(List<Transaction> transactions) {
                                            if (transactions != null) for (Transaction tx : transactions)
                                                repository.onRemoteTransactionReceived(tx);
                                            isLoading.postValue(false);
                                            addSuccessEvent.postValue(true);
                                        }
                                        @Override public void onError(String message) {
                                            isLoading.postValue(false);
                                            errorMessage.postValue("Đã nạp ví vào quỹ nhưng không tải lại được lịch sử: " + message);
                                        }
                                    });
                        }
                        @Override public void onError(String message) {
                            isLoading.postValue(false);
                            errorMessage.postValue("Không thể nạp từ ví cá nhân: " + message);
                        }
                    });
        } else {
            remoteRepository.submitWithdrawal(repository.getCurrentUserId(), repository.getCurrentUserName(), operationRoomId, amount,
                    category.trim(), note.trim(), owner, transactionCallback);
        }
        return true;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.getTransactionsLiveData().removeObserver(transactionsObserver);
    }
}
