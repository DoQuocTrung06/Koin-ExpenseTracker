package com.sharedfund.app.ui.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.model.Transaction;

import java.util.List;

public class TransactionViewModel extends ViewModel {

    private final TransactionRepository transactionRepository;
    private final MutableLiveData<List<Transaction>> transactionsLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;
    private final MutableLiveData<String> errorMessageLiveData;
    private final MutableLiveData<Boolean> isTransactionAddedSuccess;

    public TransactionViewModel() {
        this.transactionRepository = new TransactionRepository();
        this.transactionsLiveData = new MutableLiveData<>();
        this.isLoadingLiveData = new MutableLiveData<>();
        this.errorMessageLiveData = new MutableLiveData<>();
        this.isTransactionAddedSuccess = new MutableLiveData<>();
    }

    public LiveData<List<Transaction>> getTransactionsLiveData() {
        return transactionsLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<Boolean> getIsTransactionAddedSuccess() {
        return isTransactionAddedSuccess;
    }

    public void loadTransactions(String roomId) {
        isLoadingLiveData.setValue(true);
        transactionRepository.getTransactionsByRoom(roomId, new TransactionRepository.DataCallback<List<Transaction>>() {
            @Override
            public void onSuccess(List<Transaction> data) {
                isLoadingLiveData.postValue(false);
                transactionsLiveData.postValue(data);
            }

            @Override
            public void onError(String errorMessage) {
                isLoadingLiveData.postValue(false);
                errorMessageLiveData.postValue(errorMessage);
            }
        });
    }

    public void addTransaction(Transaction transaction) {
        isLoadingLiveData.setValue(true);
        transactionRepository.createTransaction(transaction, new TransactionRepository.DataCallback<Transaction>() {
            @Override
            public void onSuccess(Transaction data) {
                isLoadingLiveData.postValue(false);
                isTransactionAddedSuccess.postValue(true);
                loadTransactions(transaction.getRoomId());
            }

            @Override
            public void onError(String errorMessage) {
                isLoadingLiveData.postValue(false);
                errorMessageLiveData.postValue(errorMessage);
            }
        });
    }
}
