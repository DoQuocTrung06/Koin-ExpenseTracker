package com.sharedfund.app.ui.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.sharedfund.app.data.RoomRepository;
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
    }

    public String getRoomId() {
        return roomId;
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
    public boolean submitTransaction(String targetRoomId, String type, String amountStr, String category, String note, String createdBy) {
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

        // 4. Kiểm tra người tạo
        if (createdBy == null || createdBy.trim().isEmpty()) {
            createdBy = "Bạn";
        }

        // Tạo Transaction model
        Transaction tx = new Transaction();
        tx.setRoomId(targetRoomId != null ? targetRoomId : this.roomId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setCategory(category.trim());
        tx.setNote(note.trim());
        tx.setCreatedBy(createdBy.trim());

        isLoading.setValue(true);
        boolean success = repository.addTransaction(tx);
        isLoading.setValue(false);

        if (success) {
            addSuccessEvent.setValue(true);
            return true;
        } else {
            errorMessage.setValue("Không thể lưu giao dịch. Vui lòng thử lại!");
            return false;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.getTransactionsLiveData().removeObserver(transactionsObserver);
    }
}
