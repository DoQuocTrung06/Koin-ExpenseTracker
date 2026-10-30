package com.sharedfund.app.ui.transaction;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sharedfund.app.MainActivity;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Category;
import com.sharedfund.app.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class AddTransactionFragment extends Fragment {

    private static final String ARG_ROOM_ID = "arg_room_id";

    private String roomId;
    private TransactionViewModel viewModel;

    private ImageButton btnAddTxBack;
    private RadioGroup rgTxType;
    private RadioButton rbExpense;
    private RadioButton rbIncome;
    private TextInputLayout tilAmount;
    private TextInputEditText etAmount;
    private Spinner spinnerCategory;
    private TextInputLayout tilNote;
    private TextInputEditText etNote;
    private TextInputLayout tilCreatedBy;
    private TextInputEditText etCreatedBy;
    private MaterialButton btnSaveTransaction;

    private final List<String> expenseCategories = new ArrayList<>();
    private final List<String> incomeCategories = new ArrayList<>();

    public static AddTransactionFragment newInstance(String roomId) {
        AddTransactionFragment fragment = new AddTransactionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupSpinner(new String[0]);
        setupTextWatchers();
        setupViewModel();
        setupClickListeners();
    }

    private void initViews(View view) {
        btnAddTxBack = view.findViewById(R.id.btnAddTxBack);
        rgTxType = view.findViewById(R.id.rgTxType);
        rbExpense = view.findViewById(R.id.rbExpense);
        rbIncome = view.findViewById(R.id.rbIncome);
        tilAmount = view.findViewById(R.id.tilAmount);
        etAmount = view.findViewById(R.id.etAmount);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        tilNote = view.findViewById(R.id.tilNote);
        etNote = view.findViewById(R.id.etNote);
        tilCreatedBy = view.findViewById(R.id.tilCreatedBy);
        etCreatedBy = view.findViewById(R.id.etCreatedBy);
        btnSaveTransaction = view.findViewById(R.id.btnSaveTransaction);
    }

    private void setupSpinner(String[] categories) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupTextWatchers() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilAmount.getError() != null) {
                    tilAmount.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilNote.getError() != null) {
                    tilNote.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etCreatedBy.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tilCreatedBy.getError() != null) {
                    tilCreatedBy.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        viewModel.setAccessToken(session.getAccessToken());
        viewModel.setRoomId(roomId);
        RoomRepository.getInstance().configureAccessToken(session.getAccessToken());
        RoomRepository.getInstance().loadCategories(new RoomRepository.DataCallback<List<Category>>() {
            @Override public void onSuccess(List<Category> categories) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    expenseCategories.clear();
                    incomeCategories.clear();
                    for (Category category : categories) {
                        if (category.getName() == null || category.getType() == null) continue;
                        if ("expense".equalsIgnoreCase(category.getType()) || "chi".equalsIgnoreCase(category.getType()))
                            expenseCategories.add(category.getName());
                        else if ("income".equalsIgnoreCase(category.getType()) || "thu".equalsIgnoreCase(category.getType()))
                            incomeCategories.add(category.getName());
                    }
                    setupSpinner(rbExpense.isChecked()
                            ? expenseCategories.toArray(new String[0])
                            : incomeCategories.toArray(new String[0]));
                });
            }
            @Override public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                        "Failed to load categories: " + message, Toast.LENGTH_LONG).show());
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty()) {
                    btnSaveTransaction.setEnabled(true);
                    btnSaveTransaction.setText(R.string.btn_save_transaction);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                if (loading != null && loading) {
                    btnSaveTransaction.setEnabled(false);
                    btnSaveTransaction.setText("Đang lưu giao dịch...");
                } else {
                    btnSaveTransaction.setEnabled(true);
                    btnSaveTransaction.setText(R.string.btn_save_transaction);
                }
            }
        });

        viewModel.getAddSuccessEvent().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    Toast.makeText(requireContext(), R.string.tx_added_success, Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        requireActivity().getSupportFragmentManager().popBackStack();
                        ((MainActivity) getActivity()).openTransactions(roomId);
                    } else {
                        requireActivity().onBackPressed();
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        btnAddTxBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });

        rgTxType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbExpense) {
                    rbExpense.setBackgroundResource(R.drawable.bg_expense_badge);
                    rbExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_expense));
                    rbIncome.setBackgroundResource(R.drawable.bg_card);
                    rbIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    setupSpinner(expenseCategories.toArray(new String[0]));
                } else {
                    rbIncome.setBackgroundResource(R.drawable.bg_income_badge);
                    rbIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_income));
                    rbExpense.setBackgroundResource(R.drawable.bg_card);
                    rbExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    setupSpinner(incomeCategories.toArray(new String[0]));
                }
            }
        });

        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndSave();
            }
        });
    }

    private void validateAndSave() {
        tilAmount.setError(null);
        tilNote.setError(null);
        tilCreatedBy.setError(null);

        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
        String createdBy = etCreatedBy.getText() != null ? etCreatedBy.getText().toString().trim() : "";
        String category = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "";
        String type = rbExpense.isChecked() ? Transaction.TYPE_EXPENSE : Transaction.TYPE_INCOME;

        boolean isValid = true;

        if (amountStr.isEmpty()) {
            tilAmount.setError(getString(R.string.err_amount_empty));
            isValid = false;
        } else {
            try {
                double val = Double.parseDouble(amountStr.replace(".", "").replace(",", ""));
                if (val <= 0) {
                    tilAmount.setError(getString(R.string.err_amount_invalid));
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                tilAmount.setError(getString(R.string.err_amount_invalid));
                isValid = false;
            }
        }

        if (note.isEmpty()) {
            tilNote.setError(getString(R.string.err_note_empty));
            isValid = false;
        }

        if (createdBy.isEmpty()) {
            tilCreatedBy.setError(getString(R.string.err_creator_empty));
            isValid = false;
        }

        if (isValid) {
            btnSaveTransaction.setEnabled(false);
            btnSaveTransaction.setText("Đang lưu...");
            viewModel.submitTransaction(roomId, type, amountStr, category, note, createdBy);
        }
    }
}
