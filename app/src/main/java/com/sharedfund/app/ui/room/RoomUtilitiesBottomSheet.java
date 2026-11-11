package com.sharedfund.app.ui.room;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sharedfund.app.R;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.model.RecurringRule;

import java.util.List;

public class RoomUtilitiesBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "arg_room_id";

    public interface OnUtilityActionListener {
        void onSplitBillSelected();
        void onRecurringSelected();
    }

    private OnUtilityActionListener listener;
    private String roomId;

    public static RoomUtilitiesBottomSheet newInstance() {
        return new RoomUtilitiesBottomSheet();
    }

    public static RoomUtilitiesBottomSheet newInstance(String roomId) {
        RoomUtilitiesBottomSheet fragment = new RoomUtilitiesBottomSheet();
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

    public void setListener(OnUtilityActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_room_utilities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnUtilitiesClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        View cardSplitBill = view.findViewById(R.id.cardOptionSplitBill);
        View cardRecurring = view.findViewById(R.id.cardOptionRecurring);
        TextView tvRecurringBadge = view.findViewById(R.id.tvRecurringOverdueBadge);

        if (tvRecurringBadge != null && roomId != null && !roomId.trim().isEmpty()) {
            new TransactionRepository().getRecurringRules(null, roomId, new TransactionRepository.DataCallback<List<RecurringRule>>() {
                @Override
                public void onSuccess(List<RecurringRule> rules) {
                    if (!isAdded() || getContext() == null) return;
                    int overdueCount = 0;
                    if (rules != null) {
                        for (RecurringRule rule : rules) {
                            if (rule != null && rule.isActive() && !rule.isPaidInCurrentCycle() && rule.getDaysDiffCurrentCycle() >= 0) {
                                overdueCount++;
                            }
                        }
                    }
                    int finalCount = overdueCount;
                    requireActivity().runOnUiThread(() -> {
                        if (tvRecurringBadge != null) {
                            if (finalCount > 0) {
                                tvRecurringBadge.setVisibility(View.VISIBLE);
                                tvRecurringBadge.setText(String.valueOf(finalCount));
                            } else {
                                tvRecurringBadge.setVisibility(View.GONE);
                            }
                        }
                    });
                }

                @Override
                public void onError(String error) {
                }
            });
        }

        if (cardSplitBill != null) {
            cardSplitBill.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onSplitBillSelected();
            });
        }

        if (cardRecurring != null) {
            cardRecurring.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onRecurringSelected();
            });
        }
    }
}
